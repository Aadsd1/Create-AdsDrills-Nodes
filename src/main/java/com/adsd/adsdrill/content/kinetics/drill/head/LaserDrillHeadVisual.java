package com.adsd.adsdrill.content.kinetics.drill.head;


import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.adsd.adsdrill.etc.AdsDrillPartialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;

import java.util.function.Consumer;

public class LaserDrillHeadVisual extends KineticBlockEntityVisual<LaserDrillHeadBlockEntity> {

    // 정적 모델은 TransformedInstance로, 회전 모델은 RotatingInstance로 선언
    protected RotatingInstance cog1;
    protected RotatingInstance cog2;


    public LaserDrillHeadVisual(VisualizationContext context, LaserDrillHeadBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        // 1. 회전 모델 1 생성
        cog1 = createRotatingInstanceFor(AdsDrillPartialModels.LASER_COG1);

        // 2. 회전 모델 2 생성
        cog2 = createRotatingInstanceFor(AdsDrillPartialModels.LASER_COG2);

        Direction facing = this.blockState.getValue(LaserDrillHeadBlock.FACING);

        // rotateToFace를 사용하여 모델 전체의 방향을 설정합니다.
        cog1.setPosition(getVisualPosition())
                .rotateToFace(Direction.UP, facing); // UP을 기준으로 만들어진 모델을 facing 방향으로 회전

        cog2.setPosition(getVisualPosition())
                .rotateToFace(Direction.UP, facing); // UP을 기준으로 만들어진 모델을 facing 방향으로 회전


        // 초기 회전 상태 업데이트
        updateRotation();
    }

    private void updateRotation() {
        float speed = this.blockEntity.getVisualSpeed();
        // 헤드가 설치된 방향(FACING)을 기준으로 회전축을 동적으로 결정합니다.
        // 모델이 UP을 기준으로 만들어졌으므로, FACING 방향의 Y축이 회전축이 됩니다.
        Direction.Axis axis = this.blockState.getValue(DirectionalKineticBlock.FACING).getAxis();
        // rotateToFace를 사용하여 모델 전체의 방향을 설정합니다.

        cog1.setup(blockEntity, axis, speed).setChanged();
        cog2.setup(blockEntity, axis, -speed).setChanged();
    }

    @Override
    public void update(float partialTick) {
        updateRotation();
    }

    private RotatingInstance createRotatingInstanceFor(PartialModel model) {
        return instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(model)).createInstance();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(cog1, cog2);
    }

    @Override
    protected void _delete() {
        cog1.delete();
        cog2.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(cog1);
        consumer.accept(cog2);
    }
}