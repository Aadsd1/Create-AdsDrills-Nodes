package com.yourname.mycreateaddon.content.kinetics.node;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Objects;

public class NodeFrameBlockEntityRenderer extends SmartBlockEntityRenderer<NodeFrameBlockEntity> {

    public NodeFrameBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(NodeFrameBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        ItemRenderer itemRenderer = net.minecraft.client.Minecraft.getInstance().getItemRenderer();
        ItemStackHandler inventory = be.inventory;

        float[][] dataPositions = {
                {0.25f, 0.2f, 0.25f}, {0.5f, 0.2f, 0.25f}, {0.75f, 0.2f, 0.25f},
                {0.25f, 0.2f, 0.5f},  {0.5f, 0.2f, 0.5f},  {0.75f, 0.2f, 0.5f},
                {0.25f, 0.2f, 0.75f}, {0.5f, 0.2f, 0.75f}, {0.75f, 0.2f, 0.75f}
        };

        // [수정] AnimationTickHolder.getRenderTime() 대신 월드 시간을 사용합니다.
        // partialTicks를 더해주어 프레임 사이의 움직임을 부드럽게 만듭니다.
        float time = Objects.requireNonNull(be.getLevel()).getGameTime() + partialTicks;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            ms.pushPose();
            ms.translate(dataPositions[i][0], dataPositions[i][1], dataPositions[i][2]);
            ms.scale(0.4f, 0.4f, 0.4f);
            ms.mulPose(Axis.YP.rotationDegrees(time * 0.5f % 360));
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, ms, buffer, be.getLevel(), 0);
            ms.popPose();
        }

        ItemStack coreStack = inventory.getStackInSlot(9);
        if (!coreStack.isEmpty()) {
            ms.pushPose();
            ms.translate(0.5f, 0.7f, 0.5f);
            ms.scale(0.5f, 0.5f, 0.5f);
            ms.mulPose(Axis.YP.rotationDegrees(time % 360));
            itemRenderer.renderStatic(coreStack, ItemDisplayContext.FIXED, light, overlay, ms, buffer, be.getLevel(), 0);
            ms.popPose();
        }
    }
}