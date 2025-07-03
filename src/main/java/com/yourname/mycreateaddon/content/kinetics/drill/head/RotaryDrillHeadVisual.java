package com.yourname.mycreateaddon.content.kinetics.drill.head;


import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.yourname.mycreateaddon.etc.MyAddonPartialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.Direction;

import java.util.function.Consumer;





public class RotaryDrillHeadVisual extends KineticBlockEntityVisual<RotaryDrillHeadBlockEntity> {

    protected RotatingInstance rotatingModel;

    public RotaryDrillHeadVisual(VisualizationContext context, RotaryDrillHeadBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Direction facing = this.blockState.getValue(DirectionalKineticBlock.FACING);

        rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING,
                Models.partial(MyAddonPartialModels.ROTARY_DRILL_HEAD)).createInstance();
        rotatingModel.setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH,facing);

        update(partialTick);
    }

    private void updateRotation() {
        rotatingModel.setPosition(getVisualPosition())
                .setup(blockEntity,this.blockState.getValue(RotaryDrillHeadBlock.FACING).getAxis(),this.blockEntity.getVisualSpeed())
                .setChanged();
    }

    @Override
    public void update(float partialTick) {
        updateRotation();

        // [핵심 수정] 더 이상 코어에 직접 접근하지 않습니다.
        // 자신의 BlockEntity가 동기화 받은 최신 heat 값을 사용합니다.
        float heat = blockEntity.getClientHeat();
        float heatRatio = heat / 100f;

        int r = 255;
        int g = 255 - (int)(heatRatio * (255 - 96));
        int b = 255 - (int)(heatRatio * (255 - 96));

        Color color = new Color(r, g, b);
        rotatingModel.setColor(color);
    }

    @Override
    public void updateLight(float partialTick) {
        relight(rotatingModel);
    }

    @Override
    protected void _delete() {
        rotatingModel.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(rotatingModel);
    }

}
