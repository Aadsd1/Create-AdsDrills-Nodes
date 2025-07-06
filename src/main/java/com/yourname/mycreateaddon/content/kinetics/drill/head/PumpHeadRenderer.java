package com.yourname.mycreateaddon.content.kinetics.drill.head;


import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.yourname.mycreateaddon.etc.MyAddonPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class PumpHeadRenderer extends KineticBlockEntityRenderer<PumpHeadBlockEntity> {
    public PumpHeadRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }


    @Override
    protected SuperByteBuffer getRotatedModel(PumpHeadBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(MyAddonPartialModels.PUMP_HEAD_COG, state);
    }
}