package com.yourname.mycreateaddon.content.kinetics.drill;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.module.Frame.FrameModuleBlock;
import com.yourname.mycreateaddon.content.kinetics.module.Frame.FrameModuleBlockEntity;
import com.yourname.mycreateaddon.registry.MyAddonBlocks; // SpeedModuleBlock을 위해 임포트 (가정)
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
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
                if (level.getBlockEntity(modulePos) instanceof FrameModuleBlockEntity frameBE) {
                    frameBE.updateVisualConnections(new HashSet<>(), 0f);
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

        Set<BlockPos> newStructure = new HashSet<>();
        Map<BlockPos, Set<Direction>> moduleConnections = new HashMap<>();
        boolean wasValid = this.structureValid;

        Set<BlockPos> visitedInScan = new HashSet<>();
        List<BlockPos> foundFunctionalModules = new ArrayList<>();
        List<BlockPos> foundOtherCores = new ArrayList<>();

        StructureCheckResult result = searchStructureRecursive(
                this.worldPosition, null, visitedInScan, foundFunctionalModules, foundOtherCores, moduleConnections, 0);

        if (!result.loopDetected && !result.multipleCoresDetected && foundFunctionalModules.size() <= MAX_MODULES) {
            this.structureValid = true;
            newStructure.addAll(foundFunctionalModules);
            newStructure.remove(this.worldPosition);

            float currentSpeed = getSpeed();
            Direction coreFacing = this.getBlockState().getValue(DirectionalKineticBlock.FACING);
            Direction coreBack = coreFacing.getOpposite();

            for (BlockPos modulePos : newStructure) {
                if (level.getBlockEntity(modulePos) instanceof FrameModuleBlockEntity frameBE) {
                    Set<Direction> connections = new HashSet<>(moduleConnections.getOrDefault(modulePos, new HashSet<>()));

                    if (modulePos.equals(this.worldPosition.relative(coreFacing))) {
                        connections.remove(coreBack);
                    }
                    if (modulePos.equals(this.worldPosition.relative(coreBack))) {
                        connections.remove(coreFacing);
                    }

                    frameBE.updateVisualConnections(connections, currentSpeed);
                }
            }

        } else {
            this.structureValid = false;
            if (result.multipleCoresDetected) {
                MyCreateAddon.LOGGER.warn("Multiple cores detected in structure starting at {}!", worldPosition);
                for (BlockPos corePos : foundOtherCores) {
                    if (level.getBlockEntity(corePos) instanceof DrillCoreBlockEntity otherCore) {
                        otherCore.scheduleStructureCheck();
                    }
                }
            }
        }

        if (!this.structureCache.equals(newStructure) || this.structureValid != wasValid) {
            Set<BlockPos> detachedModules = new HashSet<>(this.structureCache);
            detachedModules.removeAll(newStructure);
            for (BlockPos detachedPos : detachedModules) {
                if (level.getBlockEntity(detachedPos) instanceof FrameModuleBlockEntity frameBE) {
                    frameBE.updateVisualConnections(new HashSet<>(), 0f);
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
        return block instanceof DrillCoreBlock || block instanceof FrameModuleBlock;
    }

    public boolean isModuleConnectedAt(Direction absoluteDirection) {
        BlockPos adjacentPos = this.worldPosition.relative(absoluteDirection);
        return this.structureCache.contains(adjacentPos);
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putBoolean("StructureValid", this.structureValid);

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
        if (level != null && !level.isClientSide) {
            float currentSpeed = getSpeed();
            if (lastKnownSpeed != currentSpeed) {
                scheduleStructureCheck();
                lastKnownSpeed = currentSpeed;
            }

            if (needsStructureCheck) {
                scanAndValidateStructure();
                needsStructureCheck = false;
            }
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        Component structureStatus = structureValid
                ? Component.translatable("tooltip.mycreateaddon.structure.valid", structureCache.size()).withStyle(ChatFormatting.GREEN)
                : Component.translatable("tooltip.mycreateaddon.structure.invalid").withStyle(ChatFormatting.RED);
        tooltip.add(Component.literal(" ").append(structureStatus));
        return true;
    }
}