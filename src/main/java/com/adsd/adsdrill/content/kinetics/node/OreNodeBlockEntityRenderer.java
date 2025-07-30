package com.adsd.adsdrill.content.kinetics.node;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector3f;


public class OreNodeBlockEntityRenderer extends SmartBlockEntityRenderer<OreNodeBlockEntity> {

    public OreNodeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(OreNodeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        if (!shouldRender(be)) {
            return;
        }

        RenderContext context = createRenderContext(be);
        renderDestroyOverlay(ms, buffer, context, overlay);
    }

    // === 렌더링 조건 체크 ===
    private boolean shouldRender(OreNodeBlockEntity be) {
        float progress = be.getClientMiningProgress();
        BlockPos miningDrillPos = be.getMiningDrillPos();
        Level level = be.getLevel();

        return miningDrillPos != null && progress > 0 && level != null;
    }

    // === 렌더링 컨텍스트 생성 ===
    private RenderContext createRenderContext(OreNodeBlockEntity be) {
        float progress = be.getClientMiningProgress();
        BlockPos miningDrillPos = be.getMiningDrillPos();
        Level level = be.getLevel();

        int stage = (int) Mth.clamp(progress * 10.0F, 0, 9);

        Direction face = Direction.getNearest(
                (float)(miningDrillPos.getX() - be.getBlockPos().getX()),
                (float)(miningDrillPos.getY() - be.getBlockPos().getY()),
                (float)(miningDrillPos.getZ() - be.getBlockPos().getZ())
        );

        BlockPos lightPos = be.getBlockPos().relative(face);
        assert level != null;
        int finalLight = LevelRenderer.getLightColor(level, lightPos);

        TextureAtlas textureAtlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite sprite = textureAtlas.getSprite(ModelBakery.DESTROY_STAGES.get(stage));

        return new RenderContext(stage, face, finalLight, sprite);
    }

    // === 실제 렌더링 ===
    private void renderDestroyOverlay(PoseStack ms, MultiBufferSource buffer, RenderContext context, int overlay) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout()); // 대안 1: cutout

        ms.pushPose();

        Vector3f offset = context.face.step();
        ms.translate(offset.x() * 0.003f, offset.y() * 0.003f, offset.z() * 0.003f);

        Matrix4f matrix = ms.last().pose();
        renderFaceQuad(consumer, matrix, context, overlay);

        ms.popPose();
    }

    // === 면별 정점 렌더링 ===
    private void renderFaceQuad(VertexConsumer consumer, Matrix4f matrix, RenderContext context, int overlay) {
        TextureAtlasSprite sprite = context.sprite;
        Direction face = context.face;
        int light = context.light;

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        float r = 0.7f, g = 0.7f, b = 0.7f, a = 1f;


        switch (face) {
            case DOWN: // Y-
                consumer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 1, 0, 0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 1, 0, 1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 0, 0, 1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                break;
            case UP: // Y+
                consumer.addVertex(matrix, 0, 1, 1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 1, 1, 1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 1, 1, 0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 0, 1, 0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                break;
            case NORTH: // Z-
                consumer.addVertex(matrix, 0, 1, 0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 1, 1, 0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 1, 0, 0).setColor(r, g, b, a).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, a).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                break;
            case SOUTH: // Z+
                consumer.addVertex(matrix, 1, 1, 1).setColor(r, g, b, a).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 0, 1, 1).setColor(r, g, b, a).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 0, 0, 1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 1, 0, 1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                break;
            case WEST: // X-
                consumer.addVertex(matrix, 0, 1, 1).setColor(r, g, b, a).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 0, 1, 0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, a).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 0, 0, 1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                break;
            case EAST: // X+
                consumer.addVertex(matrix, 1, 1, 0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 1, 1, 1).setColor(r, g, b, a).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 1, 0, 1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                consumer.addVertex(matrix, 1, 0, 0).setColor(r, g, b, a).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
                break;
        }
    }

    // === 렌더링 컨텍스트 데이터 클래스 ===
    private record RenderContext(int stage, Direction face, int light, TextureAtlasSprite sprite) {}
}