package com.yourname.mycreateaddon.content.kinetics.module;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class GenericModuleRenderer extends KineticBlockEntityRenderer<GenericModuleBlockEntity> {

    // 생성자 이름 변경
    public GenericModuleRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}