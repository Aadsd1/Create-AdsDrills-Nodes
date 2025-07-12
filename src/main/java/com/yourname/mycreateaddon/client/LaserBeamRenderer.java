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

import java.util.ArrayList;
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
        List<LaserDrillHeadBlockEntity> lasersToRender = new ArrayList<>(ACTIVE_LASERS.values());

        // 2. 이제 원본 맵이 아닌, 안전한 복사본을 순회합니다.
        for (LaserDrillHeadBlockEntity laser : lasersToRender) {
            // 레이저가 제거되었는지 확인하는 로직은 여전히 필요합니다.
            // 이 검사는 원본 맵을 수정하는 removeLaser를 호출할 수 있지만,
            // 현재 순회중인 lasersToRender 리스트에는 영향을 주지 않으므로 안전합니다.
            if (laser.isRemoved() || !laser.hasLevel()) {
                // 원본 맵에서는 제거하여 다음 프레임부터는 렌더링되지 않도록 합니다.
                removeLaser(laser.getBlockPos());
                continue;
            }
            List<BlockPos> targets = laser.activeTargets;



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
        Vector3f beamDirection = new Vector3f((float)(end.x - start.x), (float)(end.y - start.y), (float)(end.z - start.z));
        float beamLength = beamDirection.length();

        if (beamLength < 1.0f) return;
        beamDirection.normalize(); // 축으로 사용하기 위해 정규화

        // 2. 시간에 따라 회전 각도를 계산합니다.
        // 코어의 시각적 속도(visualSpeed)를 가져와 회전 속도에 반영합니다.
        // 속도가 0이면 회전도 멈춥니다.
        float coreSpeed = laser.getCore() != null ? laser.getCore().getVisualSpeed() : 0;
        long time = System.currentTimeMillis();
        // 속도에 비례하여 회전 속도가 결정됩니다. 10000L은 회전 주기를 조절하는 값입니다.
        float angle = (time % (long)(10000 / (Math.abs(coreSpeed) / 64f + 1f))) / (10000f / (Math.abs(coreSpeed) / 64f + 1f)) * 360f;

        // 3. 빔의 방향 벡터를 축으로 하는 회전 쿼터니언을 생성합니다.
        Quaternionf beamAxisRotation = new Quaternionf().fromAxisAngleRad(beamDirection, (float)Math.toRadians(angle));

        // 4. 카메라를 마주보는 기본 평면 벡터를 계산합니다. (기존 로직)
        Quaternionf cameraRotation = new Quaternionf().rotateYXZ(-camera.getYRot() * ((float)Math.PI / 180F), camera.getXRot() * ((float)Math.PI / 180F), 0.0f);
        Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(cameraRotation);

        // 5. 기본 평면 벡터에 빔 축 회전을 적용하여 최종 평면 벡터를 구합니다.
        right.rotate(beamAxisRotation);

        // [!!! 수정된 부분 끝 !!!]

        Matrix4f matrix = poseStack.last().pose();

        // 이제 회전이 적용된 'right' 벡터를 사용하여 꼭짓점 위치를 계산합니다.
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