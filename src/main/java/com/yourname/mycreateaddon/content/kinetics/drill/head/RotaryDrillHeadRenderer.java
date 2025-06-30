package com.yourname.mycreateaddon.content.kinetics.drill.head;


import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class RotaryDrillHeadRenderer extends KineticBlockEntityRenderer<RotaryDrillHeadBlockEntity> {

    public RotaryDrillHeadRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
//@Override
//protected SuperByteBuffer getRotatedModel(RotaryDrillHeadBlockEntity be, BlockState state) {
//    return CachedBufferer.partial(MyAddonPartialModels.ROTARY_DRILL_HEAD, state);
//}

}