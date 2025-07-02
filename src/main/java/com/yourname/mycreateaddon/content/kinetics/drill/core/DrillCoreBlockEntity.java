package com.yourname.mycreateaddon.content.kinetics.drill.core;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.drill.head.IDrillHead;
import com.yourname.mycreateaddon.content.kinetics.drill.head.RotaryDrillHeadBlockEntity;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;



public class DrillCoreBlockEntity extends KineticBlockEntity {

    // ... (Enum과 필드 선언은 이전과 동일) ...
    private enum InvalidityReason {
        NONE,
        LOOP_DETECTED,
        MULTIPLE_CORES,
        TOO_MANY_MODULES,
        HEAD_MISSING // 오류는 아니지만, 작동 불가 상태를 나타내기 위함
    }


    private BlockPos cachedHeadPos = null;

    // --- 구조 관련 필드 ---
    private Set<BlockPos> structureCache = new HashSet<>();
    private boolean structureValid = false;
    private InvalidityReason invalidityReason = InvalidityReason.NONE;
    private boolean needsStructureCheck = true;
    private static final int MAX_STRUCTURE_RANGE = 16;
    private static final int MAX_MODULES = 16;

    // --- 모듈 효과 집계 필드 ---
    private float totalSpeedBonus = 0f;
    private float totalStressImpact = 0f;

    public DrillCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isStructureValid() {
        return this.structureValid;
    }

    public void onBroken() {
        if (level != null && !level.isClientSide()) {
            Set<BlockPos> allToClear = new HashSet<>(structureCache);
            if (cachedHeadPos != null) allToClear.add(cachedHeadPos);

            for (BlockPos modulePos : allToClear) {
                if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                    moduleBE.updateVisualConnections(new HashSet<>());
                }
            }
        }
    }

    public void scheduleStructureCheck() {
        if (level != null && !level.isClientSide) {
            this.needsStructureCheck = true;
        }
    }

    private void scanAndValidateStructure() {
        if (level == null || level.isClientSide()) return;

        Set<BlockPos> oldStructureCache = new HashSet<>(this.structureCache);

        // --- 1. 상태 초기화 ---
        this.structureValid = false;
        this.invalidityReason = InvalidityReason.NONE;
        this.totalSpeedBonus = 0f;
        this.totalStressImpact = 0f;
        this.cachedHeadPos = null; // 매번 초기화

        Set<BlockPos> newStructureCache = new HashSet<>();
        Map<BlockPos, Set<Direction>> moduleConnections = new HashMap<>();
        Direction searchDir = getBlockState().getValue(DirectionalKineticBlock.FACING).getOpposite();

        // --- 2. 모듈 구조 탐색 및 검증 ---
        Set<BlockPos> visitedInScan = new HashSet<>();
        List<BlockPos> foundFunctionalModules = new ArrayList<>();
        List<BlockPos> foundOtherCores = new ArrayList<>();

        StructureCheckResult result = searchStructureRecursive(this.worldPosition, searchDir, visitedInScan, foundFunctionalModules, foundOtherCores, moduleConnections, 0);

        if (result.loopDetected()) {
            this.invalidityReason = InvalidityReason.LOOP_DETECTED;
        } else if (result.multipleCoresDetected()) {
            this.invalidityReason = InvalidityReason.MULTIPLE_CORES;
        } else if (foundFunctionalModules.size() - 1 > MAX_MODULES) {
            this.invalidityReason = InvalidityReason.TOO_MANY_MODULES;
        } else {
            // 모듈 구조에 문제가 없으면, 일단 '구조는 유효'하다고 판단합니다.
            this.structureValid = true;

            newStructureCache.addAll(foundFunctionalModules);
            newStructureCache.remove(this.worldPosition);

            for (BlockPos modulePos : newStructureCache) {
                if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                    ModuleType type = moduleBE.getModuleType();
                    this.totalSpeedBonus += type.getSpeedBonus();
                    this.totalStressImpact += type.getStressImpact();
                }
            }
        }

        // --- 3. 헤드 존재 여부 확인 ---
        // [핵심 수정] 이 로직을 structureValid와 독립적으로 실행하여 cachedHeadPos를 확실히 설정합니다.
        BlockPos potentialHeadPos = this.worldPosition.relative(searchDir);
        BlockState headState = level.getBlockState(potentialHeadPos);

        if (headState.getBlock() instanceof IDrillHead && headState.getValue(DirectionalKineticBlock.FACING) == searchDir) {
            this.cachedHeadPos = potentialHeadPos;
            BlockPos cachedNodePos = potentialHeadPos.relative(searchDir);
        } else if (this.structureValid) {
            // 모듈 구조는 괜찮은데 헤드가 없는 경우에만 경고를 설정합니다.
            this.invalidityReason = InvalidityReason.HEAD_MISSING;
        }

        // --- 4. 캐시 및 시각 효과 업데이트 ---
        this.structureCache = newStructureCache;
        Set<BlockPos> detachedModules = new HashSet<>(oldStructureCache);
        detachedModules.removeAll(newStructureCache);
        for (BlockPos detachedPos : detachedModules) {
            if (level.getBlockEntity(detachedPos) instanceof GenericModuleBlockEntity moduleBE) {
                moduleBE.updateVisualConnections(new HashSet<>());
                moduleBE.updateVisualSpeed(0f);
            }
        }
        for (BlockPos modulePos : newStructureCache) {
            if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                Set<Direction> connections = moduleConnections.getOrDefault(modulePos, new HashSet<>());
                moduleBE.updateVisualConnections(connections);
            }
        }

        setChanged();
        sendData();
    }

    private record StructureCheckResult(boolean loopDetected, boolean multipleCoresDetected) {}

    private StructureCheckResult searchStructureRecursive(BlockPos currentPos, Direction from, Set<BlockPos> visited, List<BlockPos> functionalModules, List<BlockPos> otherCores, Map<BlockPos, Set<Direction>> moduleConnections, int depth) {
        if (depth > MAX_STRUCTURE_RANGE || !visited.add(currentPos)) {
            return new StructureCheckResult(from != null, false);
        }

        boolean isThisCore = currentPos.equals(this.worldPosition);
        assert level != null;
        BlockState currentState = level.getBlockState(currentPos);
        Block currentBlock = currentState.getBlock();

        if (isThisCore && from != null && from == currentState.getValue(DirectionalKineticBlock.FACING)) {
            return new StructureCheckResult(false, false);
        }

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
            if (isThisCore && dir == currentState.getValue(DirectionalKineticBlock.FACING)) {
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
    public boolean isModuleConnectedAt(Direction absoluteDirection) {
        BlockPos adjacentPos = this.worldPosition.relative(absoluteDirection);
        return this.structureCache.contains(adjacentPos);
    }

    /**
     * Visual이 출력축 렌더링 여부를 결정하는 데 사용합니다.
     * @return 헤드가 존재하고 캐시되었으면 true
     */
    public boolean hasHead() {
        return this.cachedHeadPos != null;
    }

    @Override
    public float getSpeed() {
        if (isOverStressed()) return 0;
        return super.getTheoreticalSpeed() * (1.0f + this.totalSpeedBonus);
    }
    public float getInputSpeed() {
        return super.getTheoreticalSpeed();
    }
    @Override
    public float calculateStressApplied() {
        if (!structureValid || invalidityReason == InvalidityReason.HEAD_MISSING) return 0;
        return this.totalStressImpact;
    }

    // ... (write, read, addToGoggleTooltip은 이전과 동일) ...
    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putBoolean("StructureValid", this.structureValid);
        compound.putFloat("SpeedBonus", this.totalSpeedBonus);
        compound.putFloat("StressImpact", this.totalStressImpact);
        compound.putInt("InvalidityReason", this.invalidityReason.ordinal());

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
        this.totalStressImpact = compound.getFloat("StressImpact");

        int reasonOrdinal = compound.getInt("InvalidityReason");
        if (reasonOrdinal >= 0 && reasonOrdinal < InvalidityReason.values().length) {
            this.invalidityReason = InvalidityReason.values()[reasonOrdinal];
        } else {
            this.invalidityReason = InvalidityReason.NONE;
        }

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



    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;

        if (needsStructureCheck) {
            scanAndValidateStructure();
            needsStructureCheck = false;
        }

        // --- 1. 최종 속도 계산 ---
        // 모듈 구조가 유효하고 과부하가 아닐 때만 속도가 있습니다.
        boolean isStructureOk = this.structureValid && !isOverStressed();
        float finalSpeed = isStructureOk ? getSpeed() : 0;

        // --- 2. 모든 모듈 속도 업데이트 ---
        for (BlockPos modulePos : this.structureCache) {
            if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                moduleBE.updateVisualSpeed(finalSpeed);
            }
        }

        // --- 3. 헤드 업데이트 (시각 효과와 실제 작동 분리) ---
        boolean canMine = isStructureOk && hasHead();

        // [핵심 수정] 헤드의 '시각적 회전'은 모듈과 동일한 조건(isStructureOk)을 따릅니다.
        // 이렇게 하면 헤드가 존재하기만 하면 모듈과 함께 회전합니다.
        Direction outputDir = getBlockState().getValue(DirectionalKineticBlock.FACING).getOpposite();
        BlockPos potentialHeadPos = this.worldPosition.relative(outputDir);
        if (level.getBlockEntity(potentialHeadPos) instanceof RotaryDrillHeadBlockEntity headBE) {
            headBE.updateVisualSpeed(-finalSpeed); // 모듈 구조만 유효하면 회전

            // '채굴'은 더 엄격한 조건(canMine)을 따릅니다.
            if (canMine && headBE.getBlockState().getBlock() instanceof IDrillHead headBlock) {
                headBlock.onDrillTick(level, potentialHeadPos, headBE.getBlockState(), this);
            }
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.translatable("goggle.mycreateaddon.drill_core.header").withStyle(ChatFormatting.GRAY));
        MutableComponent status;
        if (structureValid && invalidityReason != InvalidityReason.HEAD_MISSING) {
            status = Component.translatable("goggle.mycreateaddon.drill_core.valid", structureCache.size())
                    .withStyle(ChatFormatting.GREEN);
        } else {
            status = Component.translatable("goggle.mycreateaddon.drill_core.invalid")
                    .withStyle(ChatFormatting.RED);
        }
        tooltip.add(Component.literal(" ").append(status));
        if (!structureValid || invalidityReason != InvalidityReason.NONE) {
            String reasonKey = switch (invalidityReason) {
                case LOOP_DETECTED -> "goggle.mycreateaddon.drill_core.reason.loop_detected";
                case MULTIPLE_CORES -> "goggle.mycreateaddon.drill_core.reason.multiple_cores";
                case TOO_MANY_MODULES -> "goggle.mycreateaddon.drill_core.reason.too_many_modules";
                case HEAD_MISSING -> "goggle.mycreateaddon.drill_core.reason.head_missing";
                default -> null;
            };

            if (reasonKey != null) {
                MutableComponent reason = (invalidityReason == InvalidityReason.TOO_MANY_MODULES)
                        ? Component.translatable(reasonKey, MAX_MODULES)
                        : Component.translatable(reasonKey);
                tooltip.add(Component.literal(" ").append(reason.withStyle(ChatFormatting.DARK_RED)));
            }
        }
        if (structureValid && (totalSpeedBonus > 0 || totalStressImpact > 0)) {
            tooltip.add(Component.literal(""));
            if (totalSpeedBonus > 0) {
                tooltip.add(Component.literal(" ")
                        .append(Component.translatable("goggle.mycreateaddon.drill_core.speed_bonus").withStyle(ChatFormatting.GRAY))
                        .append(": ")
                        .append(Component.literal("+" + (int)(totalSpeedBonus * 100) + "%")
                                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true))));
            }
            if (totalStressImpact > 0) {
                tooltip.add(Component.literal(" ")
                        .append(Component.translatable("goggle.mycreateaddon.drill_core.stress_impact").withStyle(ChatFormatting.GRAY))
                        .append(": ")
                        .append(Component.literal("+" + totalStressImpact + " SU")
                                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true))));
            }
        }
        return true;
    }
}