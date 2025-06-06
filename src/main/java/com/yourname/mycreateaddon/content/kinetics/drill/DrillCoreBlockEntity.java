package com.yourname.mycreateaddon.content.kinetics.drill;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.module.FrameModuleBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DrillCoreBlockEntity extends KineticBlockEntity {

    // --- 구조 관련 필드 ---
    private List<BlockPos> structureCache = new ArrayList<>();
    private boolean structureValid = false;
    private boolean needsStructureCheck = true;
    private static final int MAX_STRUCTURE_RANGE = 16;
    private static final int MAX_MODULES = 128;

    public DrillCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
        structureCache.clear();
        structureValid = false;

        Set<BlockPos> visitedInScan = new HashSet<>();
        Set<BlockPos> path = new HashSet<>();
        List<BlockPos> foundFunctionalModules = new ArrayList<>();

        boolean loopDetected = searchStructureRecursive(this.worldPosition, visitedInScan, path, foundFunctionalModules, 0);

        // --- 여기에 조건을 추가합니다! ---
        if (!loopDetected && foundFunctionalModules.size() <= MAX_MODULES && !foundFunctionalModules.isEmpty()) {
            structureValid = true;
            structureCache.addAll(foundFunctionalModules);
            MyCreateAddon.LOGGER.debug("Structure validated at {}. Found {} functional modules.", worldPosition, structureCache.size());
        } else {
            structureCache.clear();
            if (loopDetected) {
                MyCreateAddon.LOGGER.warn("Loop detected in structure starting at {}!", worldPosition);
            } else if (foundFunctionalModules.size() > MAX_MODULES) {
                MyCreateAddon.LOGGER.warn("Structure at {} exceeds functional module limit ({} > {})!", worldPosition, foundFunctionalModules.size(), MAX_MODULES);
            }
        }
        setChanged();
        sendData();
    }

    private boolean searchStructureRecursive(BlockPos currentPos, Set<BlockPos> visitedInScan, Set<BlockPos> path, List<BlockPos> foundFunctionalModules, int depth) {
        if (depth > MAX_STRUCTURE_RANGE || !level.isInWorldBounds(currentPos)) return false;
        if (!path.add(currentPos)) return true; // 루프 감지
        if (!visitedInScan.add(currentPos)) {
            path.remove(currentPos);
            return false;
        }

        boolean isCore = currentPos.equals(this.worldPosition);
        BlockState currentState = level.getBlockState(currentPos);
        Block currentBlock = currentState.getBlock();

        if (!isCore && !(currentBlock instanceof FrameModuleBlock || isActualModule(currentBlock))) {
            path.remove(currentPos);
            return false;
        }

        if (!isCore && isActualModule(currentBlock)) {
            foundFunctionalModules.add(currentPos);
            if (foundFunctionalModules.size() > MAX_MODULES) {
                path.remove(currentPos);
                return false;
            }
        }

        boolean loopFoundInChildren = false;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = currentPos.relative(dir);
            if (neighborPos.equals(this.worldPosition) || isValidStructureBlock(neighborPos)) {
                if (searchStructureRecursive(neighborPos, visitedInScan, path, foundFunctionalModules, depth + 1)) {
                    loopFoundInChildren = true;
                }
            }
        }

        path.remove(currentPos);
        return loopFoundInChildren;
    }

    private boolean isValidStructureBlock(BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        Block block = level.getBlockState(pos).getBlock();
        return block instanceof FrameModuleBlock || isActualModule(block);
    }

    private boolean isActualModule(Block block) {
        return false; // TODO: 실제 기능성 모듈 클래스 instanceof 검사
    }

    // ==================================================
    //  NBT 데이터 처리
    // ==================================================

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putBoolean("StructureValid", this.structureValid);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        this.structureValid = compound.getBoolean("StructureValid");
        if (!clientPacket) {
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
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        Component structureStatus = structureValid
                ? Component.translatable("tooltip.mycreateaddon.structure.valid", structureCache.size()).withStyle(ChatFormatting.GREEN)
                : Component.translatable("tooltip.mycreateaddon.structure.invalid").withStyle(ChatFormatting.RED);
        tooltip.add(Component.literal(" ").append(structureStatus));
        return true;
    }

    // ==================================================
    //  렌더러를 위한 헬퍼 메서드
    // ==================================================

    public Direction getSourceFacing() {
        if (this.source == null) {
            return Direction.DOWN;
        }
        return Direction.getNearest(
                this.source.getX() - this.worldPosition.getX(),
                this.source.getY() - this.worldPosition.getY(),
                this.source.getZ() - this.worldPosition.getZ()
        );
    }
}