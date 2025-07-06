package com.yourname.mycreateaddon.content.kinetics.drill.head;


import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class PumpHeadBlockEntity extends KineticBlockEntity {

    protected BlockPos cachedCorePos;
    private float visualSpeed = 0f;

    public PumpHeadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void updateVisualSpeed(float speed) {
        if (this.visualSpeed == speed) {
            return;
        }
        this.visualSpeed = speed;
        setChanged();
        sendData();
    }

    public float getVisualSpeed() {
        return visualSpeed;
    }

    public void setCore(BlockPos corePos) {
        if (corePos == null || !corePos.equals(cachedCorePos)) {
            cachedCorePos = corePos;
            if (corePos == null) {
                updateVisualSpeed(0);
            }
            setChanged();
            sendData();
        }
    }

    public void updateCoreConnection() {
        if (level == null || level.isClientSide) return;

        Direction facing = getBlockState().getValue(PumpHeadBlock.FACING);
        BlockPos potentialCorePos = worldPosition.relative(facing.getOpposite());

        if (level.getBlockEntity(potentialCorePos) instanceof DrillCoreBlockEntity) {
            setCore(potentialCorePos);
        } else {
            setCore(null);
        }
    }

    @Nullable
    public DrillCoreBlockEntity getCore() {
        if (level != null && cachedCorePos != null) {
            if (level.getBlockEntity(cachedCorePos) instanceof DrillCoreBlockEntity core) {
                return core;
            }
        }
        return null;
    }

    // 동력 네트워크 독립성 확보
    @Override public float getTheoreticalSpeed() { return visualSpeed; }
    @Override public float calculateStressApplied() { return 0; }
    @Override public float calculateAddedStressCapacity() { return 0; }
    @Override public void attachKinetics() { /* 이 블록은 자체적으로 네트워크에 참여하지 않습니다. */ }

    // NBT 데이터 처리
    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (cachedCorePos != null) {
            compound.put("CorePos", NbtUtils.writeBlockPos(cachedCorePos));
        }
        if (clientPacket) {
            compound.putFloat("VisualSpeed", visualSpeed);
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (compound.contains("CorePos")) {
            cachedCorePos = NbtUtils.readBlockPos(compound, "CorePos").orElse(null);
        } else {
            cachedCorePos = null;
        }
        if (clientPacket) {
            visualSpeed = compound.getFloat("VisualSpeed");
        }
    }
}