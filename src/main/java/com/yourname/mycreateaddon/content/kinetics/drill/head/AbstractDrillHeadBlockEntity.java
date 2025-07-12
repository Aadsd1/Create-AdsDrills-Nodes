package com.yourname.mycreateaddon.content.kinetics.drill.head;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
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

/**
 * 모든 기계식 드릴 헤드 BlockEntity의 공통 로직을 담는 추상 클래스입니다.
 * 코어 연결, 시각적 속도 동기화, NBT 처리 등의 중복 코드를 관리합니다.
 */
public abstract class AbstractDrillHeadBlockEntity extends KineticBlockEntity {

    protected BlockPos cachedCorePos;
    private float visualSpeed = 0f;

    public AbstractDrillHeadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void updateVisualSpeed(float speed) {
        if (this.visualSpeed == speed) return;
        this.visualSpeed = speed;
        setChanged();
        sendData();
    }

    public float getVisualSpeed() {
        return visualSpeed;
    }

    public void setCore(@Nullable BlockPos corePos) {
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

        // [핵심 수정] DirectionalKineticBlock의 static FACING 프로퍼티를 직접 사용합니다.
        // 이 BE를 사용하는 모든 블록은 DirectionalKineticBlock을 상속하므로 안전합니다.
        Direction facing = getBlockState().getValue(DirectionalKineticBlock.FACING);

        BlockPos potentialCorePos = worldPosition.relative(facing.getOpposite());
        if (level.getBlockEntity(potentialCorePos) instanceof DrillCoreBlockEntity) {
            setCore(potentialCorePos);
        } else {
            setCore(null);
        }
    }

    @Nullable
    public DrillCoreBlockEntity getCore() {
        if (level != null && cachedCorePos != null && level.getBlockEntity(cachedCorePos) instanceof DrillCoreBlockEntity core) {
            return core;
        }
        return null;
    }

    // 동력 네트워크 독립성 확보
    @Override public float getTheoreticalSpeed() { return visualSpeed; }
    @Override public float calculateStressApplied() { return 0; }
    @Override public float calculateAddedStressCapacity() { return 0; }
    @Override public void attachKinetics() {}

    // NBT 데이터 처리 (공통 부분)
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
        cachedCorePos = compound.contains("CorePos") ? NbtUtils.readBlockPos(compound, "CorePos").orElse(null) : null;
        if (clientPacket) {
            visualSpeed = compound.getFloat("VisualSpeed");
        }
    }
}