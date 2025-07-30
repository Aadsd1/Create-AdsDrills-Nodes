package com.adsd.adsdrill.content.kinetics.drill.head;


import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.adsd.adsdrill.etc.AdsDrillPartialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.Direction;

import java.util.function.Consumer;

public class PumpHeadVisual extends KineticBlockEntityVisual<PumpHeadBlockEntity> {


    protected RotatingInstance cog;

    public PumpHeadVisual(VisualizationContext context, PumpHeadBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        // 1. 파셜 모델(cog) 인스턴스를 생성합니다.
        cog = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AdsDrillPartialModels.PUMP_HEAD_COG))
                .createInstance();

        // 2. 초기 위치와 회전축을 설정합니다.
        Direction facing = this.blockState.getValue(DirectionalKineticBlock.FACING);
        cog.setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, facing); // 모델이 바라보는 기본 방향(SOUTH)에서 실제 facing으로 회전

        // 3. 첫 프레임의 속도를 업데이트합니다.
        updateRotation();
    }

    @Override
    public void update(float partialTick) {
        // 매 프레임 속도를 업데이트합니다.
        updateRotation();
    }

    private void updateRotation() {
        // BE에서 현재 시각적 속도를 가져옵니다.
        float speed = this.blockEntity.getVisualSpeed();
        // 헤드의 방향에 맞는 회전축을 설정합니다.
        Direction.Axis axis = this.blockState.getValue(DirectionalKineticBlock.FACING).getAxis();

        // 톱니(cog) 인스턴스의 회전 상태를 업데이트합니다.
        cog.setup(blockEntity, axis, speed).setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        // 주변 광원 정보를 톱니 모델에 적용합니다.
        relight(cog);
    }

    @Override
    protected void _delete() {
        // 인스턴스를 삭제합니다.
        cog.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        // 블록 파괴 시 파티클 효과를 위해 인스턴스를 전달합니다.
        consumer.accept(cog);
    }
}