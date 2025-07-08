package com.yourname.mycreateaddon.client;



import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.head.LaserDrillHeadBlock;
import com.yourname.mycreateaddon.content.kinetics.drill.head.LaserDrillHeadBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = MyCreateAddon.MOD_ID, value = Dist.CLIENT)
public class LaserBeamRenderer {

    private static final Map<BlockPos, LaserDrillHeadBlockEntity> ACTIVE_LASERS = new ConcurrentHashMap<>();

    public static void addLaser(LaserDrillHeadBlockEntity laser) {
        ACTIVE_LASERS.put(laser.getBlockPos(), laser);
    }

    public static void removeLaser(BlockPos pos) {
        ACTIVE_LASERS.remove(pos);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE_LASERS.isEmpty()) {
            return;
        }

        var poseStack = event.getPoseStack();
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        // [핵심 수정] 우리가 만든 커스텀 렌더 타입을 사용합니다.
        VertexConsumer buffer = bufferSource.getBuffer(MyAddonRenderTypes.LASER_BEAM);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        for (LaserDrillHeadBlockEntity laser : ACTIVE_LASERS.values()) {
            if (laser.isRemoved() || !laser.hasLevel()) {
                removeLaser(laser.getBlockPos());
                continue;
            }
            List<BlockPos> targets = laser.activeTargets;


            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getGameTime() % 20 == 0) {
                MyCreateAddon.LOGGER.info("[CLIENT] Laser at {} has {} active targets. Target list: {}", laser.getBlockPos(), targets.size(), targets);
            }

            if (targets.isEmpty()) continue;

            BlockState headState = laser.getBlockState();
            // 블록 상태가 올바른지 한번 더 확인
            if (!(headState.getBlock() instanceof LaserDrillHeadBlock)) continue;

            Direction facing = headState.getValue(LaserDrillHeadBlock.FACING);
            Vec3 blockCenter = Vec3.atCenterOf(laser.getBlockPos());
            // FACING 방향으로 0.5 블록만큼 이동한 위치를 시작점으로 설정
            Vec3 start = blockCenter.add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.5));
            for (BlockPos targetPos : targets) {
                Vec3 end = Vec3.atCenterOf(targetPos);
                renderBeam(poseStack, buffer, start, end, 0.05f, camera, laser);
            }
        }

        poseStack.popPose();
        // [핵심 수정] 커스텀 렌더 타입의 그리기를 완료합니다.
        bufferSource.endBatch(MyAddonRenderTypes.LASER_BEAM);
    }

    private static void renderBeam(PoseStack poseStack, VertexConsumer buffer, Vec3 start, Vec3 end, float thickness, net.minecraft.client.Camera camera, LaserDrillHeadBlockEntity laser) {
        if (end.subtract(start).lengthSqr() < 1.0) return;

        Matrix4f matrix = poseStack.last().pose();

        Quaternionf cameraRotation = new Quaternionf().rotateYXZ(-camera.getYRot() * ((float)Math.PI / 180F), camera.getXRot() * ((float)Math.PI / 180F), 0.0f);
        Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(cameraRotation);

        Vector3f p1 = new Vector3f((float)start.x, (float)start.y, (float)start.z).add(new Vector3f(right).mul(thickness));
        Vector3f p2 = new Vector3f((float)end.x, (float)end.y, (float)end.z).add(new Vector3f(right).mul(thickness));
        Vector3f p3 = new Vector3f((float)end.x, (float)end.y, (float)end.z).add(new Vector3f(right).mul(-thickness));
        Vector3f p4 = new Vector3f((float)start.x, (float)start.y, (float)start.z).add(new Vector3f(right).mul(-thickness));

        LaserDrillHeadBlockEntity.OperatingMode mode = laser.getMode();
        int r, g, b, a = 128;
        switch (mode) {
            case RESONANCE -> { r = 50; g = 150; b = 255; } // 푸른색
            case DECOMPOSITION -> { r = 200; g = 50; b = 255; } // 보라색
            case WIDE_BEAM -> { r = 255; g = 50; b = 50; } // 붉은색 (기본)
            default -> { r = 255; g = 255; b = 255; } // 예외 상황 흰색
        }
        buffer.addVertex(matrix, p1.x, p1.y, p1.z).setColor(r, g, b, a).setUv(0, 0).setUv2(240,240);
        buffer.addVertex(matrix, p2.x, p2.y, p2.z).setColor(r, g, b, a).setUv(1,0).setUv2(240, 240);
        buffer.addVertex(matrix, p3.x, p3.y, p3.z).setColor(r, g, b, a).setUv(1,1).setUv2(240, 240);
        buffer.addVertex(matrix, p4.x, p4.y, p4.z).setColor(r, g, b, a).setUv(0,1).setUv2(240, 240);
    }
}