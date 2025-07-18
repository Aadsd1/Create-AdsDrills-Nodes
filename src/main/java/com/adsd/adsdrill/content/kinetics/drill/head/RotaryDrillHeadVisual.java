package com.adsd.adsdrill.content.kinetics.drill.head;


import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.adsd.adsdrill.etc.AdsDrillPartialModels;
import com.adsd.adsdrill.registry.AdsDrillBlocks;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.Direction;

import java.util.function.Consumer;





public class RotaryDrillHeadVisual extends KineticBlockEntityVisual<RotaryDrillHeadBlockEntity> {


    protected RotatingInstance bodyModel;
    protected RotatingInstance tipModel;

    private PartialModel currentTipPartial = null;

    public RotaryDrillHeadVisual(VisualizationContext context, RotaryDrillHeadBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        PartialModel bodyPartial;
        if (this.blockState.getBlock() == AdsDrillBlocks.NETHERITE_ROTARY_DRILL_HEAD.get()) {
            bodyPartial = AdsDrillPartialModels.NETHERITE_DRILL_BODY;
        } else if (this.blockState.getBlock() == AdsDrillBlocks.DIAMOND_ROTARY_DRILL_HEAD.get()) {
            bodyPartial = AdsDrillPartialModels.DIAMOND_DRILL_BODY;
        } else {
            bodyPartial = AdsDrillPartialModels.IRON_DRILL_BODY;
        }

        Direction facing = this.blockState.getValue(DirectionalKineticBlock.FACING);

        bodyModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(bodyPartial)).createInstance();
        bodyModel.setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, facing);

        update(partialTick);
    }

    private void updateRotation() {
        Direction facing = this.blockState.getValue(DirectionalKineticBlock.FACING);
        float speed = this.blockEntity.getVisualSpeed();

        bodyModel.setPosition(getVisualPosition()).setup(blockEntity, facing.getAxis(), speed).setChanged();
        if (tipModel != null) {
            tipModel.setPosition(getVisualPosition()).setup(blockEntity, facing.getAxis(), speed).setChanged();
        }
    }

    @Override
    public void update(float partialTick) {
        updateRotation();

        float heat = blockEntity.getClientHeat();
        float heatRatio = heat / 100f;
        int heatR = 255;
        int heatG = 255 - (int) (heatRatio * (255 - 96));
        int heatB = 255 - (int) (heatRatio * (255 - 96));
        Color heatOverlayColor = new Color(heatR, heatG, heatB);

        bodyModel.setColor(heatOverlayColor);

        updateTipModel(heatOverlayColor);
    }

    private void updateTipModel(Color heatOverlayColor) {
        PartialModel requiredTipPartial;

        if (blockEntity.hasSilkTouch()) {
            requiredTipPartial = AdsDrillPartialModels.EMERALD_DRILL_TIP;
        } else if (blockEntity.getFortuneLevel() > 0) {
            requiredTipPartial = AdsDrillPartialModels.GOLD_DRILL_TIP;
        } else {
            if (this.blockState.getBlock() == AdsDrillBlocks.NETHERITE_ROTARY_DRILL_HEAD.get()) {
                requiredTipPartial = AdsDrillPartialModels.NETHERITE_DRILL_TIP;
            } else if (this.blockState.getBlock() == AdsDrillBlocks.DIAMOND_ROTARY_DRILL_HEAD.get()) {
                requiredTipPartial = AdsDrillPartialModels.DIAMOND_DRILL_TIP;
            } else {
                requiredTipPartial = AdsDrillPartialModels.IRON_DRILL_TIP;
            }
        }

        if (currentTipPartial != requiredTipPartial) {
            if (tipModel != null) {
                tipModel.delete();
            }
            Direction facing = this.blockState.getValue(DirectionalKineticBlock.FACING);
            tipModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(requiredTipPartial)).createInstance();
            tipModel.setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, facing);
            currentTipPartial = requiredTipPartial;
            relight(tipModel);
        }

        if (tipModel != null) {
            tipModel.setColor(heatOverlayColor);
        }
    }

    @Override
    protected void _delete() {
        bodyModel.delete();
        tipModel.delete();
    }

    @Override
    public void updateLight(float partialTick) {

        relight(bodyModel);
        relight(tipModel);
    }


    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {

        consumer.accept(bodyModel);
        consumer.accept(tipModel);

    }

}
