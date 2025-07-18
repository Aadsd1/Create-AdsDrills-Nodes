package com.adsd.adsdrill.content.kinetics.node;


import com.adsd.adsdrill.AdsDrillAddon;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class OreNodeModelGeometry implements IUnbakedGeometry<OreNodeModelGeometry> {

    // 필요한 모델들의 ResourceLocation을 상수로 정의합니다.
    private static final ResourceLocation CORE_BASE_MODEL = ResourceLocation.fromNamespaceAndPath(AdsDrillAddon.MOD_ID, "block/ore_node_core_base");
    private static final ResourceLocation CORE_HIGHLIGHT_MODEL = ResourceLocation.fromNamespaceAndPath(AdsDrillAddon.MOD_ID, "block/ore_node_core_highlight");
    private static final ResourceLocation DEFAULT_BACKGROUND_MODEL = ResourceLocation.withDefaultNamespace("block/stone");


    @Override
    public @NotNull BakedModel bake(@NotNull IGeometryBakingContext context, ModelBaker baker, @NotNull Function<Material, TextureAtlasSprite> spriteGetter, @NotNull ModelState modelState, @NotNull ItemOverrides overrides) {
        // bake 메서드 안에서 필요한 모든 모델을 굽습니다.
        BakedModel coreBase = baker.bake(CORE_BASE_MODEL, modelState, spriteGetter);
        BakedModel coreHighlight = baker.bake(CORE_HIGHLIGHT_MODEL, modelState, spriteGetter);
        BakedModel defaultBackground = baker.bake(DEFAULT_BACKGROUND_MODEL, modelState, spriteGetter);

        // 구워진 모델들을 생성자에 전달하여 최종 BakedModel을 생성합니다.
        // 이 패턴이 NeoForge에서 가장 표준적인 방식입니다.
        return new OreNodeBakedModel(defaultBackground, coreBase, coreHighlight, overrides);
    }
}