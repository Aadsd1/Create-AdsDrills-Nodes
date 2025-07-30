package com.adsd.adsdrill.client;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.registry.AdsDrillItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = AdsDrillAddon.MOD_ID, value = Dist.CLIENT)
public class NodeTargetRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        ItemStack heldStack = player.getMainHandItem();
        if (heldStack.getItem() != AdsDrillItems.NETHERITE_NODE_LOCATOR.get()) {
            return;
        }

        CustomData data = heldStack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        CompoundTag nbt = data.copyTag();
        if (!nbt.contains("TargetPos")) return;

        BlockPos targetPos = NbtUtils.readBlockPos(nbt, "TargetPos").orElse(null);
        if (targetPos == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 cameraPos = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        AABB boundingBox = new AABB(targetPos).inflate(0.005);
        float r = 0.6f, g = 0.2f, b = 0.9f;


        // --- 1차 렌더링: 깊이 테스트 활성화 (보이는 부분만, 약간 투명하게) ---
        VertexConsumer consumerDepthTested = bufferSource.getBuffer(AdsDrillRenderTypes.NODE_SILHOUETTE_DEPTH_TESTED);
        LevelRenderer.renderLineBox(poseStack, consumerDepthTested, boundingBox, r, g, b, 0.4f); // 알파값 0.4

        // --- 2차 렌더링: 깊이 테스트 비활성화 (전체, 더 선명하게) ---
        VertexConsumer consumerNoDepth = bufferSource.getBuffer(AdsDrillRenderTypes.NODE_SILHOUETTE);
        LevelRenderer.renderLineBox(poseStack, consumerNoDepth, boundingBox, r, g, b, 0.7f); // 알파값 0.7

        poseStack.popPose();

        // 각 렌더 타입에 대해 배치를 종료합니다.
        bufferSource.endBatch(AdsDrillRenderTypes.NODE_SILHOUETTE_DEPTH_TESTED);
        bufferSource.endBatch(AdsDrillRenderTypes.NODE_SILHOUETTE);
    }
}