package com.yourname.mycreateaddon.content.kinetics.node;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
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
    private final BakedModel artificialBackgroundModel;
    private final ItemOverrides overrides;

    public OreNodeBakedModel(BakedModel defaultBackground, BakedModel coreBase, BakedModel coreHighlight, BakedModel artificialBackground, ItemOverrides overrides) {
        this.defaultBackgroundModel = defaultBackground;
        this.coreBaseModel = coreBase;
        this.coreHighlightModel = coreHighlight;
        this.artificialBackgroundModel = artificialBackground;
        this.overrides = overrides;
    }

    private BakedModel getModelForState(BlockState state) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand, @Nonnull ModelData extraData, @Nullable RenderType renderType) {
        if (renderType == RenderType.cutout()) {
            List<BakedQuad> quads = new ArrayList<>();
            quads.addAll(coreBaseModel.getQuads(state, side, rand, extraData, renderType));
            quads.addAll(coreHighlightModel.getQuads(state, side, rand, extraData, renderType));
            return quads;
        }

        OreNodeBlockEntity be = extraData.get(OreNodeBlockEntity.MODEL_DATA_PROPERTY);
        if (be != null) {
            if (be instanceof ArtificialNodeBlockEntity) {
                return artificialBackgroundModel.getQuads(state, side, rand, extraData, renderType);
            } else {
                ResourceLocation backgroundId = be.getBackgroundBlockId();
                Block backgroundBlock = BuiltInRegistries.BLOCK.get(backgroundId);
                if (backgroundBlock != Blocks.AIR) {
                    BlockState backgroundState = backgroundBlock.defaultBlockState();
                    return getModelForState(backgroundState).getQuads(backgroundState, side, rand, extraData, renderType);
                }
            }
        }

        return defaultBackgroundModel.getQuads(state, side, rand, extraData, renderType);
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon(@Nonnull ModelData data) {
        OreNodeBlockEntity be = data.get(OreNodeBlockEntity.MODEL_DATA_PROPERTY);
        if (be != null) {
            if (be instanceof ArtificialNodeBlockEntity) {
                return artificialBackgroundModel.getParticleIcon(data);
            }
            ResourceLocation backgroundId = be.getBackgroundBlockId();
            Block backgroundBlock = BuiltInRegistries.BLOCK.get(backgroundId);
            if (backgroundBlock != Blocks.AIR) {
                return getModelForState(backgroundBlock.defaultBlockState()).getParticleIcon(data);
            }
        }
        return defaultBackgroundModel.getParticleIcon(data);
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return this.getParticleIcon(ModelData.EMPTY);
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