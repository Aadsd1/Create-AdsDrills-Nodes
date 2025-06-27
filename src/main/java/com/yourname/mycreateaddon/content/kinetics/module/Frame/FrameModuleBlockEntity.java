package com.yourname.mycreateaddon.content.kinetics.module.Frame;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;


public class FrameModuleBlockEntity extends KineticBlockEntity {

    // 렌더링을 위한 상태 필드
    private Set<Direction> visualConnections = new HashSet<>();
    private float visualSpeed = 0f;

    public FrameModuleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state); // <<<--- 기본 생성자 사용
    }

    // 코어가 이 메서드를 호출하여 렌더링 상태를 업데이트합니다.
    public void updateVisualConnections(Set<Direction> connections, float speed) {
        if (this.visualConnections.equals(connections) && this.visualSpeed == speed) {
            return;
        }
        this.visualConnections = connections;
        this.visualSpeed = speed;
        setChanged();
        sendData();
    }

    // Visual이 사용할 getter
    public Set<Direction> getVisualConnections() {
        return visualConnections;
    }

    public float getVisualSpeed() {
        return visualSpeed;
    }

    // =================================================================================
    //  ▼▼▼ 동력 네트워크 격리를 위한 오버라이드 ▼▼▼
    // =================================================================================

    @Override
    public float getGeneratedSpeed() {
        // 이 블록은 동력을 생성하지 않음
        return 0;
    }

    @Override
    public float calculateStressApplied() {
        // 이 블록은 스트레스를 가하지 않음
        return 0;
    }

    @Override
    public float calculateAddedStressCapacity() {
        // 이 블록은 스트레스 용량을 추가하지 않음
        return 0;
    }

    @Override
    public void attachKinetics() {
        // 동력 네트워크에 연결되지 않도록 아무 작업도 하지 않음
    }


    // =================================================================================
    //  NBT 처리 (이전과 동일)
    // =================================================================================

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (clientPacket) {
            if (!visualConnections.isEmpty()) {
                int[] dirs = visualConnections.stream().mapToInt(Direction::ordinal).toArray();
                compound.put("VisualConnections", new IntArrayTag(dirs));
            }
            if (visualSpeed != 0) {
                compound.putFloat("VisualSpeed", visualSpeed);
            }
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (clientPacket) {
            visualConnections.clear();
            if (compound.contains("VisualConnections")) {
                int[] dirs = compound.getIntArray("VisualConnections");
                for (int dirOrdinal : dirs) {
                    if (dirOrdinal >= 0 && dirOrdinal < Direction.values().length) {
                        visualConnections.add(Direction.values()[dirOrdinal]);
                    }
                }
            }
            visualSpeed = compound.getFloat("VisualSpeed");
        }
    }
}