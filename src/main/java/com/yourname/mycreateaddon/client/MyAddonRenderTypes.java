package com.yourname.mycreateaddon.client;


import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.yourname.mycreateaddon.MyCreateAddon;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.OptionalDouble;

public class MyAddonRenderTypes extends RenderType {

    // RenderType 상속을 위한 필수 생성자
    private MyAddonRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    // 1. 텍스처 경로 지정
    private static final ResourceLocation LASER_BEAM_TEXTURE = ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "textures/effect/laser_beam.png");

    // 2. 커스텀 렌더 타입 정의
    public static final RenderType LASER_BEAM = create(
            MyCreateAddon.MOD_ID + ":laser_beam",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, // 정점 포맷: 위치, 색상, 텍스처, 라이트맵
            VertexFormat.Mode.QUADS, // 4개의 정점으로 면을 그림
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER) // 바닐라의 에너지 효과 셰이더 사용
                    .setTextureState(new TextureStateShard(LASER_BEAM_TEXTURE, false, false)) // 위에서 정의한 텍스처 사용
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY) // 반투명 설정
                    .setCullState(NO_CULL) // 모든 면을 그림 (빔이 얇으므로)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)
    );

    // [!!! 신규 추가 !!!] 노드 실루엣을 위한 렌더 타입
    public static final RenderType NODE_SILHOUETTE = create(
            MyCreateAddon.MOD_ID + ":node_silhouette",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.LINES,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(3.0D))) // 선 두께 (3.0)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY) // 반투명
                    .setOutputState(TRANSLUCENT_TARGET)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL) // 모든 면을 그림
                    .setDepthTestState(NO_DEPTH_TEST) // 다른 블록을 통과해서 보이도록 설정
                    .createCompositeState(false)
    );

    // 1차 렌더링 (보이는 부분용) - 새로 추가
    public static final RenderType NODE_SILHOUETTE_DEPTH_TESTED = create(
            MyCreateAddon.MOD_ID + ":node_silhouette_depth_tested",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.LINES,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(3.0D))) // 선 두께는 동일하게
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST) // [!!!] 표준 깊이 테스트 활성화
                    .createCompositeState(false)
    );
}