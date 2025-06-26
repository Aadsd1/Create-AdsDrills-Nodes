package com.yourname.mycreateaddon.content.kinetics.drill;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.yourname.mycreateaddon.etc.MyAddonPartialModels;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.createmod.catnip.render.CachedBuffers;

public class DrillCoreRenderer extends KineticBlockEntityRenderer<DrillCoreBlockEntity> {

    public DrillCoreRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

//    @Override
  //  protected void renderSafe(DrillCoreBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
    //                          int light, int overlay) {
    //}

}