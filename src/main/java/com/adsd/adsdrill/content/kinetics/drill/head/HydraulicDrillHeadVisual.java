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

public class HydraulicDrillHeadVisual extends KineticBlockEntityVisual<HydraulicDrillHeadBlockEntity> {

    protected RotatingInstance headModel;

    public HydraulicDrillHeadVisual(VisualizationContext context, HydraulicDrillHeadBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        headModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AdsDrillPartialModels.HYDRAULIC_DRILL_HEAD))
                .createInstance();

        Direction facing = this.blockState.getValue(DirectionalKineticBlock.FACING);
        headModel.setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, facing);

        updateRotation();
    }

    private void updateRotation() {
        float speed = this.blockEntity.getVisualSpeed();
        Direction.Axis axis = this.blockState.getValue(DirectionalKineticBlock.FACING).getAxis();
        headModel.setup(blockEntity, axis, speed).setChanged();
    }

    @Override
    public void update(float partialTick) {
        updateRotation();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(headModel);
    }

    @Override
    protected void _delete() {
        headModel.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(headModel);
    }
}