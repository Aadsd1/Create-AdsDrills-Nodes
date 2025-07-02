package com.yourname.mycreateaddon.content.kinetics.node;

import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class OreNodeBakedModel implements BakedModel {

    private final BakedModel defaultBackgroundModel;
    private final BakedModel coreModel;

    public OreNodeBakedModel(BakedModel defaultBackgroundModel, BakedModel coreModel) {
        this.defaultBackgroundModel = defaultBackgroundModel;
        this.coreModel = coreModel;
    }

    private BakedModel getModel(BlockState state) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        return dispatcher.getBlockModel(state);
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand, @Nonnull ModelData extraData, @Nullable RenderType renderType) {
        // [핵심 로직 변경]
        // 1. 최종 쿼드를 담을 리스트를 생성합니다.

        // 2. 배경 모델의 쿼드를 가져옵니다.
        OreNodeBlockEntity be = extraData.get(OreNodeBlockEntity.MODEL_DATA_PROPERTY);
        BakedModel backgroundModelToUse = defaultBackgroundModel;
        BlockState backgroundStateToUse = Blocks.STONE.defaultBlockState();

        if (be != null) {
            ResourceLocation backgroundId = be.getBackgroundBlockId();
            if (!backgroundId.equals(MyAddonBlocks.ORE_NODE.getId())) {
                Block backgroundBlock = BuiltInRegistries.BLOCK.get(backgroundId);
                backgroundStateToUse = backgroundBlock.defaultBlockState();
                backgroundModelToUse = getModel(backgroundStateToUse);
            }
        }
        // 요청된 renderType에 해당하는 배경의 쿼드를 리스트에 추가합니다.
        List<BakedQuad> quads = new ArrayList<>(backgroundModelToUse.getQuads(backgroundStateToUse, side, rand, ModelData.EMPTY, renderType));

        // 3. 코어 모델의 쿼드를 가져옵니다.
        // 코어는 항상 CUTOUT 레이어에서만 그려져야 합니다.
        if (renderType == RenderType.cutout()) {
            quads.addAll(coreModel.getQuads(state, side, rand, extraData, renderType));
        }

        // 4. 조합된 쿼드 리스트를 반환합니다.
        return quads;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand) {
        List<BakedQuad> quads = new ArrayList<>();
        quads.addAll(defaultBackgroundModel.getQuads(state, side, rand));
        quads.addAll(coreModel.getQuads(state, side, rand));
        return quads;
    }

    // --- 나머지 인터페이스 메서드는 이전과 동일 ---
    @Override
    public boolean useAmbientOcclusion() { return defaultBackgroundModel.useAmbientOcclusion(); }
    @Override
    public boolean isGui3d() { return defaultBackgroundModel.isGui3d(); }
    @Override
    public boolean usesBlockLight() { return defaultBackgroundModel.usesBlockLight(); }
    @Override
    public boolean isCustomRenderer() { return false; }
    @Override@SuppressWarnings("deprecation")
    public @NotNull TextureAtlasSprite getParticleIcon() { return defaultBackgroundModel.getParticleIcon(); }
    @Override
    public @NotNull ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        // 배경 블록이 어떤 렌더 타입을 사용할지 모르므로, 가능한 모든 타입을 반환해주는 것이 가장 안전합니다.
        // 하지만 여기서는 배경과 코어가 사용하는 SOLID와 CUTOUT만 명시해도 충분합니다.
        return ChunkRenderTypeSet.of(RenderType.solid(), RenderType.cutout());
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon(@Nonnull ModelData data) {
        OreNodeBlockEntity be = data.get(OreNodeBlockEntity.MODEL_DATA_PROPERTY);
        if (be != null) {
            ResourceLocation backgroundId = be.getBackgroundBlockId();
            Block backgroundBlock = BuiltInRegistries.BLOCK.get(backgroundId);
            if (backgroundBlock != Blocks.AIR) {
                return getModel(backgroundBlock.defaultBlockState()).getParticleIcon(data);
            }
        }
        return getParticleIcon();
    }
}