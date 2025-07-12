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

        // --- [!!! 핵심 수정: 아이템 렌더링 경로 분기 !!!] ---
        // renderType이 null이면, 인벤토리에서 아이템으로 렌더링되는 상황입니다.
        if (renderType == null) {
            // 아이템일 경우, 기본 모습(돌 배경 + 기본 코어)만 보여줍니다.
            // ModelData는 비워두어 기본값을 사용하게 합니다.

            // 배경 모델의 모든 쿼드를 가져옵니다.
            List<BakedQuad> itemQuads = new ArrayList<>(this.defaultBackgroundModel.getQuads(state, side, rand, ModelData.EMPTY, null));

            // 코어 모델의 모든 쿼드에 기본 틴트(1, 2)를 적용하여 가져옵니다.
            // 아이템 렌더링 시에는 ClientSetup의 ItemColor 핸들러가 이 틴트 인덱스를 사용합니다.
            for (BakedQuad quad : this.coreBaseModel.getQuads(state, side, rand, ModelData.EMPTY, null)) {
                itemQuads.add(new BakedQuad(quad.getVertices(), 1, quad.getDirection(), quad.getSprite(), quad.isShade()));
            }
            for (BakedQuad quad : this.coreHighlightModel.getQuads(state, side, rand, ModelData.EMPTY, null)) {
                itemQuads.add(new BakedQuad(quad.getVertices(), 2, quad.getDirection(), quad.getSprite(), quad.isShade()));
            }
            return itemQuads;
        }

        // --- 아래부터는 기존의 월드 내 블록 렌더링 로직입니다 ---

        // 1. 배경 블록 상태를 먼저 가져옵니다.
        BlockState backgroundState = extraData.get(OreNodeBlockEntity.BACKGROUND_STATE);
        if (backgroundState == null) {
            backgroundState = Blocks.STONE.defaultBlockState();
        }
        BakedModel backgroundModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(backgroundState);

        List<BakedQuad> finalQuads = new ArrayList<>();

        // 2. 배경 블록이 현재 요청된 렌더 타입을 지원하는 경우에만 해당 쿼드를 가져와 처리합니다.
        if (backgroundModel.getRenderTypes(backgroundState, rand, extraData).contains(renderType)) {
            for (BakedQuad quad : backgroundModel.getQuads(backgroundState, side, rand, extraData, renderType)) {
                if (quad.isTinted()) {
                    finalQuads.add(new BakedQuad(quad.getVertices(), 0, quad.getDirection(), quad.getSprite(), quad.isShade()));
                } else {
                    finalQuads.add(quad);
                }
            }
        }

        // 3. 현재 렌더 타입이 cutout이라면, 코어 모델도 함께 추가합니다.
        if (renderType.equals(RenderType.cutout())) {
            for (BakedQuad quad : coreBaseModel.getQuads(state, side, rand, extraData, renderType)) {
                finalQuads.add(new BakedQuad(quad.getVertices(), 1, quad.getDirection(), quad.getSprite(), quad.isShade()));
            }
            for (BakedQuad quad : coreHighlightModel.getQuads(state, side, rand, extraData, renderType)) {
                finalQuads.add(new BakedQuad(quad.getVertices(), 2, quad.getDirection(), quad.getSprite(), quad.isShade()));
            }
        }

        return finalQuads;
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        // [!!! 핵심 수정: 배경 블록의 렌더 타입 + 우리 코어의 렌더 타입을 모두 합쳐서 반환 !!!]
        BlockState backgroundState = data.get(OreNodeBlockEntity.BACKGROUND_STATE);
        if (backgroundState == null) {
            backgroundState = Blocks.STONE.defaultBlockState();
        }
        BakedModel backgroundModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(backgroundState);

        // 배경 모델이 사용하는 모든 렌더 타입(예: SOLID, CUTOUT)을 가져옵니다.
        ChunkRenderTypeSet backgroundTypes = backgroundModel.getRenderTypes(backgroundState, rand, data);

        // 우리 코어 모델이 사용하는 CUTOUT 타입을 여기에 추가합니다.
        return ChunkRenderTypeSet.union(backgroundTypes, ChunkRenderTypeSet.of(RenderType.cutout()));
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon(@Nonnull ModelData data) {
        BlockState backgroundState = data.get(OreNodeBlockEntity.BACKGROUND_STATE);
        if (backgroundState == null) {
            backgroundState = Blocks.STONE.defaultBlockState();
        }
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(backgroundState).getParticleIcon(data);
    }


    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return getParticleIcon(ModelData.EMPTY);
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