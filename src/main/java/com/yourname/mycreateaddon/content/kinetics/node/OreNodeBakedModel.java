package com.yourname.mycreateaddon.content.kinetics.node;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class OreNodeBakedModel implements IDynamicBakedModel {

    private final BakedModel defaultBackgroundModel;
    private final BakedModel coreBaseModel;
    private final BakedModel coreHighlightModel;
    private final ItemOverrides overrides;

    public OreNodeBakedModel(BakedModel defaultBackground, BakedModel coreBase, BakedModel coreHighlight, ItemOverrides overrides) {
        this.defaultBackgroundModel = defaultBackground;
        this.coreBaseModel = coreBase;
        this.coreHighlightModel = coreHighlight;
        this.overrides = overrides;
    }


    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand, @Nonnull ModelData extraData, @Nullable RenderType renderType) {
        // [핵심] 렌더링 레이어(RenderType)에 따라 로직을 완벽하게 분리합니다.

        // 1. 틴트가 적용될 '코어' 부분은 CUTOUT 레이어에서만 렌더링합니다.
        if (renderType == RenderType.cutout()) {
            List<BakedQuad> quads = new ArrayList<>();
            // 우리는 모델의 면 정보만 제공하면, BlockColor 핸들러가 tintindex를 보고 알아서 색을 칠해줍니다.
            quads.addAll(coreBaseModel.getQuads(state, side, rand, extraData, renderType));
            quads.addAll(coreHighlightModel.getQuads(state, side, rand, extraData, renderType));
            return quads;
        }

        // 2. '배경' 부분은 SOLID 레이어(또는 그 외)에서만 렌더링합니다.
        if (renderType == RenderType.solid()) {
            // ModelData에서 배경 정보를 가져옵니다.
            BlockState backgroundState = extraData.get(OreNodeBlockEntity.BACKGROUND_STATE);
            if (backgroundState == null) {
                backgroundState = Blocks.STONE.defaultBlockState(); // 데이터가 없으면 기본값
            }

            // 해당 배경 상태의 모델을 가져와서 그 면 정보만 반환합니다.
            BakedModel backgroundModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(backgroundState);
            return backgroundModel.getQuads(backgroundState, side, rand, extraData, renderType);
        }

        // cutout이나 solid가 아닌 다른 렌더 타입 요청에는 빈 리스트를 반환하여 오류를 방지합니다.
        return List.of();
    }

    // 파티클 아이콘은 배경 블록의 것을 사용합니다.
    @Override
    public @NotNull TextureAtlasSprite getParticleIcon(@Nonnull ModelData data) {
        BlockState backgroundState = data.get(OreNodeBlockEntity.BACKGROUND_STATE);
        if (backgroundState == null) {
            backgroundState = Blocks.STONE.defaultBlockState();
        }
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(backgroundState).getParticleIcon(data);
    }
    // 나머지 메서드들은 이전과 동일하게 유지
    @Override
    @SuppressWarnings("deprecation")
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return getParticleIcon(ModelData.EMPTY);
    }
    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.solid(), RenderType.cutout());
    }

    // --- 나머지 BakedModel의 기본 메서드들 ---
    @Override
    public boolean useAmbientOcclusion() { return true; }
    @Override
    public boolean isGui3d() { return defaultBackgroundModel.isGui3d(); }
    @Override
    public boolean usesBlockLight() { return true; }
    @Override
    public boolean isCustomRenderer() { return false; }
    @Override
    public @NotNull ItemOverrides getOverrides() { return this.overrides; }
}