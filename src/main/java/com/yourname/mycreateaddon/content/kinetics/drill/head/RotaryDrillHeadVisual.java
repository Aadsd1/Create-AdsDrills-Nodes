package com.yourname.mycreateaddon.content.kinetics.drill.head;


import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.yourname.mycreateaddon.etc.MyAddonPartialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
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

        updateRotation();
    }

    private void updateRotation() {
        rotatingModel.setPosition(getVisualPosition())
                .setRotationAxis(getRotationAxis())
                .setRotationalSpeed(getSpeed())
                .setChanged();
    }

    @Override
    public void update(float partialTick) {
        updateRotation();
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

    private Direction.Axis getRotationAxis() {
        return this.blockState.getValue(RotaryDrillHeadBlock.FACING).getAxis();
    }

    private float getSpeed() {
        return this.blockEntity.getVisualSpeed();
    }
}
