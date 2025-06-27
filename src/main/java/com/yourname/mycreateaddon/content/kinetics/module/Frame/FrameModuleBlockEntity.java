package com.yourname.mycreateaddon.content.kinetics.module.Frame;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.DrillCoreBlockEntity;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;



public class FrameModuleBlockEntity extends KineticBlockEntity {

    // 서버 측에서만 사용하는 상태 필드
    private Set<Direction> visualConnections = new HashSet<>();
    private float visualSpeed = 0f;

    // ▼▼▼ 클라이언트 Visual이 직접 읽어갈 NBT 데이터 보관함 ▼▼▼
    private CompoundTag clientVisualNBT = new CompoundTag();

    public FrameModuleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // 이 메서드는 서버에서 구조가 변경될 때 호출됩니다.
    public void updateVisualConnections(Set<Direction> connections, float speed) {
        if (this.visualConnections.equals(connections) && this.visualSpeed == speed) {
            return;
        }
        this.visualConnections = connections;
        this.visualSpeed = speed;
        setChanged();
        sendData(); // 클라이언트에 변경 사항 전송
    }

    // ▼▼▼ Visual이 NBT 데이터에 접근할 수 있도록 getter 추가 ▼▼▼
    public CompoundTag getClientVisualNBT() {
        return clientVisualNBT;
    }

    // --- 동력 관련 오버라이드는 기존과 동일 ---
    // ...

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (clientPacket) {
            // 서버 측의 상태를 NBT에 기록하여 클라이언트로 보냅니다.
            if (!visualConnections.isEmpty()) {
                compound.put("VisualConnections", new IntArrayTag(visualConnections.stream().mapToInt(Direction::ordinal).toArray()));
            }
            compound.putFloat("VisualSpeed", visualSpeed);
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (clientPacket) {
            // ▼▼▼ 클라이언트에서는 필드를 업데이트하는 대신, 받은 NBT를 보관하고 Visual 업데이트를 요청합니다. ▼▼▼
            this.clientVisualNBT = compound; // 서버로부터 받은 NBT를 그대로 저장
            VisualizationHelper.queueUpdate(this); // Visual에게 업데이트하라고 알림
        }
    }
}