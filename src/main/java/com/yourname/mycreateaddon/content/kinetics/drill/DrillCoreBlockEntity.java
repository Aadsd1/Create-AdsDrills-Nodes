package com.yourname.mycreateaddon.content.kinetics.drill;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleBlock;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.module.ModuleType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;



public class DrillCoreBlockEntity extends KineticBlockEntity {

    // --- 구조 관련 필드 ---
    private Set<BlockPos> structureCache = new HashSet<>();
    private boolean structureValid = false;
    private boolean needsStructureCheck = true;
    private static final int MAX_STRUCTURE_RANGE = 16;
    private static final int MAX_MODULES = 128;
    private float lastKnownSpeed = 0f;

    // --- 모듈 효과 집계 필드 ---
    private float totalSpeedBonus = 0f;

    public DrillCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 이 블록이 파괴될 때 DrillCoreBlock에 의해 호출되는 정리 메서드.
     */
    public void onBroken() {
        if (level != null && !level.isClientSide()) {
            // 구조 캐시에 저장된 모든 모듈에게 비활성화 신호를 보냅니다.
            for (BlockPos modulePos : structureCache) {
                if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                    moduleBE.updateVisualConnections(new HashSet<>(), 0f);
                }
            }
        }
    }

    // ==================================================
    //  구조 검사 및 관리 로직
    // ==================================================

    public void scheduleStructureCheck() {
        if (level != null && !level.isClientSide) {
            this.needsStructureCheck = true;
        }
    }

    private void scanAndValidateStructure() {
        if (level == null || level.isClientSide()) return;

        // 효과 집계 변수 초기화
        float currentSpeedBonus = 0f;

        Set<BlockPos> newStructure = new HashSet<>();
        Map<BlockPos, Set<Direction>> moduleConnections = new HashMap<>();
        boolean wasValid = this.structureValid;
        float oldSpeed = getSpeed();

        Set<BlockPos> visitedInScan = new HashSet<>();
        List<BlockPos> foundFunctionalModules = new ArrayList<>();
        List<BlockPos> foundOtherCores = new ArrayList<>();

        StructureCheckResult result = searchStructureRecursive(
                this.worldPosition, null, visitedInScan, foundFunctionalModules, foundOtherCores, moduleConnections, 0);

        // 집계된 효과를 BE 필드에 먼저 반영합니다. getSpeed()가 이를 사용하기 때문입니다.
        if (!result.loopDetected && !result.multipleCoresDetected && (foundFunctionalModules.size() - 1) <= MAX_MODULES) {
            for (BlockPos modulePos : foundFunctionalModules) {
                if(level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                    ModuleType type = moduleBE.getModuleType();
                    currentSpeedBonus += type.getSpeedBonus();
                }
            }
        }
        this.totalSpeedBonus = currentSpeedBonus;

        // 유효성 검사 및 구조 업데이트
        if (this.totalSpeedBonus == currentSpeedBonus && !result.loopDetected && !result.multipleCoresDetected && (foundFunctionalModules.size() - 1) <= MAX_MODULES) {
            this.structureValid = true;
            newStructure.addAll(foundFunctionalModules);
            newStructure.remove(this.worldPosition); // 구조 캐시에서는 코어 자신을 제외

            // 렌더링 정보 전파
            float newSpeed = getSpeed();
            Direction coreFacing = this.getBlockState().getValue(DirectionalKineticBlock.FACING);
            Direction coreBack = coreFacing.getOpposite();

            for (BlockPos modulePos : newStructure) {
                if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                    Set<Direction> connections = new HashSet<>(moduleConnections.getOrDefault(modulePos, new HashSet<>()));

                    if (modulePos.equals(this.worldPosition.relative(coreFacing))) {
                        connections.remove(coreBack);
                    }
                    if (modulePos.equals(this.worldPosition.relative(coreBack))) {
                        connections.remove(coreFacing);
                    }

                    moduleBE.updateVisualConnections(connections, newSpeed);
                }
            }

        } else {
            this.structureValid = false;
            this.totalSpeedBonus = 0f; // 구조가 유효하지 않으면 보너스도 0
            if (result.multipleCoresDetected) {
                MyCreateAddon.LOGGER.warn("Multiple cores detected in structure starting at {}!", worldPosition);
                for (BlockPos corePos : foundOtherCores) {
                    if (level.getBlockEntity(corePos) instanceof DrillCoreBlockEntity otherCore) {
                        otherCore.scheduleStructureCheck();
                    }
                }
            }
        }

        // 구조나 속도에 변경이 있을 경우 클라이언트에 알림
        if (!this.structureCache.equals(newStructure) || this.structureValid != wasValid || getSpeed() != oldSpeed) {
            Set<BlockPos> detachedModules = new HashSet<>(this.structureCache);
            detachedModules.removeAll(newStructure);
            for (BlockPos detachedPos : detachedModules) {
                if (level.getBlockEntity(detachedPos) instanceof GenericModuleBlockEntity moduleBE) {
                    moduleBE.updateVisualConnections(new HashSet<>(), 0f);
                }
            }

            this.structureCache = newStructure;
            setChanged();
            sendData();
        }
    }

    private record StructureCheckResult(boolean loopDetected, boolean multipleCoresDetected) {}

    private StructureCheckResult searchStructureRecursive(BlockPos currentPos, Direction from, Set<BlockPos> visited, List<BlockPos> functionalModules, List<BlockPos> otherCores, Map<BlockPos, Set<Direction>> moduleConnections, int depth) {
        if (depth > MAX_STRUCTURE_RANGE || !visited.add(currentPos)) {
            return new StructureCheckResult(from != null, false);
        }

        boolean isThisCore = currentPos.equals(this.worldPosition);
        BlockState currentState = level.getBlockState(currentPos);
        Block currentBlock = currentState.getBlock();

        if (currentBlock instanceof DrillCoreBlock && !isThisCore) {
            otherCores.add(currentPos);
            return new StructureCheckResult(false, true);
        }

        if (!isValidStructureBlock(currentBlock)) {
            return new StructureCheckResult(false, false);
        }

        functionalModules.add(currentPos);

        for (Direction dir : Direction.values()) {
            if (from != null && dir == from.getOpposite()) {
                continue;
            }

            BlockPos neighborPos = currentPos.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            BlockState neighborState = level.getBlockState(neighborPos);
            if (isValidStructureBlock(neighborState.getBlock())) {
                moduleConnections.computeIfAbsent(currentPos, k -> new HashSet<>()).add(dir);
                moduleConnections.computeIfAbsent(neighborPos, k -> new HashSet<>()).add(dir.getOpposite());

                StructureCheckResult result = searchStructureRecursive(neighborPos, dir, visited, functionalModules, otherCores, moduleConnections, depth + 1);
                if (result.loopDetected || result.multipleCoresDetected) {
                    return result;
                }
            }
        }
        return new StructureCheckResult(false, false);
    }

    private boolean isValidStructureBlock(Block block) {
        return block instanceof DrillCoreBlock || block instanceof GenericModuleBlock;
    }

    // ==================================================
    //  Visual과의 연동을 위한 헬퍼 메서드
    // ==================================================

    public boolean isModuleConnectedAt(Direction absoluteDirection) {
        if (!absoluteDirection.getAxis().isHorizontal()) return false;
        BlockPos adjacentPos = this.worldPosition.relative(absoluteDirection);
        return this.structureCache.contains(adjacentPos);
    }

    // ==================================================
    //  속도 및 스트레스 계산
    // ==================================================

    @Override
    public float getSpeed() {
        if (isOverStressed()) return 0;
        // getTheoreticalSpeed()는 이미 보너스가 적용된 속도를 반환합니다.
        return getTheoreticalSpeed();
    }

    @Override
    public float getTheoreticalSpeed() {
        // 부모 클래스의 속도(기본 속도)에 계산된 보너스를 곱합니다.
        return super.getTheoreticalSpeed() * (1.0f + this.totalSpeedBonus);
    }

    // ==================================================
    //  NBT 데이터 처리
    // ==================================================
    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putBoolean("StructureValid", this.structureValid);
        compound.putFloat("SpeedBonus", this.totalSpeedBonus);

        if (clientPacket) {
            ListTag cacheList = new ListTag();
            for (BlockPos pos : structureCache) {
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("X", pos.getX());
                posTag.putInt("Y", pos.getY());
                posTag.putInt("Z", pos.getZ());
                cacheList.add(posTag);
            }
            compound.put("StructureCache", cacheList);
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        this.structureValid = compound.getBoolean("StructureValid");
        this.totalSpeedBonus = compound.getFloat("SpeedBonus");

        if (clientPacket) {
            Set<BlockPos> newCache = new HashSet<>();
            if (compound.contains("StructureCache", 9)) {
                ListTag cacheList = compound.getList("StructureCache", 10);
                for (int i = 0; i < cacheList.size(); i++) {
                    CompoundTag posTag = cacheList.getCompound(i);
                    newCache.add(new BlockPos(
                            posTag.getInt("X"),
                            posTag.getInt("Y"),
                            posTag.getInt("Z")
                    ));
                }
            }
            this.structureCache = newCache;
        } else {
            needsStructureCheck = true;
        }
    }

    // ==================================================
    //  틱 업데이트 및 효과
    // ==================================================

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide) {
            // 기본 속도(입력 속도)가 변경되었는지 확인
            float currentBaseSpeed = super.getTheoreticalSpeed();
            if (lastKnownSpeed != currentBaseSpeed) {
                scheduleStructureCheck();
                lastKnownSpeed = currentBaseSpeed;
            }

            if (needsStructureCheck) {
                scanAndValidateStructure();
                needsStructureCheck = false;
            }
        }
    }

    // ==================================================
    //  Goggle 정보 표시
    // ==================================================

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // 먼저 부모 클래스의 툴팁(기본 속도, 스트레스 등)을 추가합니다.
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        // 구조 상태 툴팁 추가
        Component structureStatus = structureValid
                ? Component.translatable("tooltip.mycreateaddon.structure.valid", structureCache.size()).withStyle(ChatFormatting.GREEN)
                : Component.translatable("tooltip.mycreateaddon.structure.invalid").withStyle(ChatFormatting.RED);
        tooltip.add(Component.literal(" ").append(structureStatus));

        // 속도 보너스 툴팁 추가
        if (structureValid && totalSpeedBonus > 0) {
            tooltip.add(Component.literal(" ")
                    .append(Component.translatable("tooltip.mycreateaddon.speed_bonus")
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" +" + (int)(totalSpeedBonus * 100) + "%")
                            .withStyle(ChatFormatting.AQUA))
            );
        }

        return true;
    }
}