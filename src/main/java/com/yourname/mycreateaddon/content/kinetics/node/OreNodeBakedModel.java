package com.yourname.mycreateaddon.content.kinetics.node;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class OreNodeBakedModel implements BakedModel {

    private final BakedModel defaultModel;

    // 생성자에서 Baker 대신 기본 모델(돌)을 받도록 수정
    public OreNodeBakedModel(BakedModel defaultModel) {
        this.defaultModel = defaultModel;
    }

    // [핵심 수정 1] NeoForge 최신 버전에서는 이 getQuads 메서드를 오버라이드해야 합니다.
    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand, @Nonnull ModelData extraData, @Nullable RenderType renderType) {
        // ModelData에서 BlockEntity를 가져옵니다.
        OreNodeBlockEntity be = extraData.get(OreNodeBlockEntity.MODEL_DATA_PROPERTY);
        if (be == null) {
            // BE 데이터가 없으면 기본 모델(돌)의 쿼드를 반환
            return defaultModel.getQuads(state, side, rand, extraData, renderType);
        }

        Minecraft mc = Minecraft.getInstance();
        BlockModelShaper blockModelShaper = mc.getBlockRenderer().getBlockModelShaper();

        // BlockEntity에서 배경 및 광석 블록 ID를 가져옵니다.
        ResourceLocation backgroundId = be.getBackgroundBlockId();
        ResourceLocation oreId = be.getOreBlockId();

        // ID를 사용하여 실제 BakedModel을 가져옵니다.
        BakedModel backgroundModel = getModel(blockModelShaper, backgroundId);
        BakedModel oreModel = getModel(blockModelShaper, oreId);

        // 두 모델의 쿼드(면)를 합쳐서 반환합니다.
        List<BakedQuad> quads = new ArrayList<>();
        quads.addAll(backgroundModel.getQuads(state, side, rand, extraData, renderType));
        quads.addAll(oreModel.getQuads(state, side, rand, extraData, renderType));

        return quads;
    }

    // [핵심 수정 2] getModel 메서드 로직 수정
    private BakedModel getModel(BlockModelShaper shaper, ResourceLocation id) {
        // 블록 ID로부터 해당 블록의 기본 상태를 가져와 모델을 얻습니다.
        // 이 방법이 null을 반환할 확률이 가장 낮습니다.
        return BuiltInRegistries.BLOCK.getOptional(id)
                .map(block -> shaper.getBlockModel(block.defaultBlockState()))
                .orElseGet(() -> shaper.getBlockModel(Blocks.STONE.defaultBlockState())); // 못찾으면 돌 모델 반환
    }

    // [핵심 수정 3] 이 메서드는 ModelData가 없는 경우(아이템 렌더링 등)를 위해 반드시 필요합니다.
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand) {
        // ModelData가 없으므로 기본 모델을 사용합니다.
        return defaultModel.getQuads(state, side, rand);
    }

    // --- 나머지 인터페이스 메서드 구현 ---

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return defaultModel.getParticleIcon();
    }

    // 파티클 아이콘을 ModelData를 통해 가져오도록 변경 (더 정확한 파티클 효과)
    @Override
    public TextureAtlasSprite getParticleIcon(@Nonnull ModelData data) {
        OreNodeBlockEntity be = data.get(OreNodeBlockEntity.MODEL_DATA_PROPERTY);
        if (be != null) {
            return getModel(Minecraft.getInstance().getBlockRenderer().getBlockModelShaper(), be.getBackgroundBlockId()).getParticleIcon(data);
        }
        return getParticleIcon();
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}