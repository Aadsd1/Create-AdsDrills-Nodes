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
import net.minecraft.world.level.block.entity.BlockEntity;
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
    private BlockPos cachedNodePos = null;

    // --- 구조 관련 필드 ---
    private Set<BlockPos> structureCache = new HashSet<>();
    private boolean structureValid = false;
    private InvalidityReason invalidityReason = InvalidityReason.NONE;
    private boolean needsStructureCheck = true;
    private static final int MAX_STRUCTURE_RANGE = 16;
    private static final int MAX_MODULES = 128;

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
                    moduleBE.updateVisualConnections(new HashSet<>(), 0f);
                }
            }
        }
    }

    // ... (scanAndValidateStructure 및 하위 메서드들은 이전과 동일) ...
    public void scheduleStructureCheck() {
        if (level != null && !level.isClientSide) {
            this.needsStructureCheck = true;
        }
    }

    private void scanAndValidateStructure() {
        if (level == null || level.isClientSide()) return;

        this.cachedHeadPos = null;
        this.cachedNodePos = null;

        float currentSpeedBonus = 0f;
        float currentStressImpact = 0f;

        Set<BlockPos> newStructure = new HashSet<>();
        Map<BlockPos, Set<Direction>> moduleConnections = new HashMap<>();
        boolean wasValid = this.structureValid;
        float oldSpeed = getSpeed();

        Set<BlockPos> visitedInScan = new HashSet<>();
        List<BlockPos> foundFunctionalModules = new ArrayList<>();
        List<BlockPos> foundOtherCores = new ArrayList<>();

        this.structureValid = false;

        Direction outputDir = getBlockState().getValue(DirectionalKineticBlock.FACING).getOpposite();

        StructureCheckResult result = searchStructureRecursive(
                this.worldPosition, outputDir, visitedInScan, foundFunctionalModules, foundOtherCores, moduleConnections, 0);

        if (result.loopDetected) {
            this.invalidityReason = InvalidityReason.LOOP_DETECTED;
        } else if (result.multipleCoresDetected) {
            this.invalidityReason = InvalidityReason.MULTIPLE_CORES;
        } else if ((foundFunctionalModules.size() - 1) > MAX_MODULES) { // -1 to exclude core itself
            this.invalidityReason = InvalidityReason.TOO_MANY_MODULES;
        } else {
            this.structureValid = true;
            this.invalidityReason = InvalidityReason.NONE;

            newStructure.addAll(foundFunctionalModules);
            newStructure.remove(this.worldPosition);

            for (BlockPos modulePos : newStructure) {
                if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                    ModuleType type = moduleBE.getModuleType();
                    currentSpeedBonus += type.getSpeedBonus();
                    currentStressImpact += type.getStressImpact();
                }
            }
        }

        if (this.structureValid) {
            BlockPos potentialHeadPos = this.worldPosition.relative(outputDir);
            BlockState headState = level.getBlockState(potentialHeadPos);

            if (headState.getBlock() instanceof IDrillHead &&
                    headState.getValue(DirectionalKineticBlock.FACING) == outputDir) {
                this.cachedHeadPos = potentialHeadPos;
                this.cachedNodePos = potentialHeadPos.relative(outputDir);
            } else {
                this.invalidityReason = InvalidityReason.HEAD_MISSING;
            }
        }

        this.totalSpeedBonus = this.structureValid ? currentSpeedBonus : 0f;
        this.totalStressImpact = this.structureValid ? currentStressImpact : 0f;

        if (this.structureValid) {
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
            Set<BlockPos> allModulesToClear = new HashSet<>(this.structureCache);
            allModulesToClear.addAll(newStructure);
            allModulesToClear.remove(this.worldPosition);

            for (BlockPos modulePos : allModulesToClear) {
                if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                    moduleBE.updateVisualConnections(new HashSet<>(), 0f);
                }
            }
        }

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
        if (level == null || level.isClientSide) {
            return;
        }

        if (needsStructureCheck) {
            scanAndValidateStructure();
            needsStructureCheck = false;
        }

        // 작동 가능 조건: 구조가 유효하고, 헤드가 있으며, 과부하 상태가 아님
        if (structureValid && invalidityReason != InvalidityReason.HEAD_MISSING && cachedHeadPos != null && !isOverStressed()) {

            BlockEntity be = level.getBlockEntity(cachedHeadPos);
            // [FIXED] RotaryDrillHeadBlockEntity 타입인지 명확히 확인합니다.
            if (be instanceof RotaryDrillHeadBlockEntity headBE && headBE.getBlockState().getBlock() instanceof IDrillHead headBlock) {

                // [FIXED] 코어의 속도와 반대 방향으로 회전하도록 속도를 직접 주입(Push)합니다.
                headBE.updateVisualSpeed(-getSpeed());

                // 헤드의 onDrillTick 호출
                headBlock.onDrillTick(level, cachedHeadPos, be.getBlockState(), this);

            } else {
                // 헤드가 파괴되거나 변경된 경우, 즉시 구조 재검사
                scheduleStructureCheck();
            }
        } else if (cachedHeadPos != null) {
            // [NEW] 구조가 유효하지 않더라도, 연결된 헤드가 있다면 속도를 0으로 리셋합니다.
            BlockEntity be = level.getBlockEntity(cachedHeadPos);
            if (be instanceof RotaryDrillHeadBlockEntity headBE) {
                headBE.updateVisualSpeed(0);
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