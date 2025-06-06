package com.yourname.mycreateaddon.content.kinetics.drill;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class DrillCoreRenderer extends KineticBlockEntityRenderer<DrillCoreBlockEntity> {

    public DrillCoreRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(DrillCoreBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        // Flywheel 환경에서는 이 코드가 거의 호출되지 않으므로,
        // 만약을 대비한 최소한의 기본 렌더링만 남겨두거나 비워둡니다.
        // super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
    }
}