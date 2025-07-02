package com.yourname.mycreateaddon.content.kinetics.node;


import com.yourname.mycreateaddon.MyCreateAddon;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class OreNodeModelGeometry implements IUnbakedGeometry<OreNodeModelGeometry> {

    @Override
    public @NotNull BakedModel bake(@NotNull IGeometryBakingContext context, ModelBaker baker, @NotNull Function<Material, TextureAtlasSprite> spriteGetter, @NotNull ModelState modelState, @NotNull ItemOverrides overrides) {
        // 1. 코어 모델을 굽습니다.
        BakedModel coreBaseModel = baker.bake(ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "block/ore_node_core_base"),modelState,spriteGetter);
        BakedModel coreHighlightModel = baker.bake(ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "block/ore_node_core_highlight"),modelState,spriteGetter);
        BakedModel defaultBackgroundModel = baker.bake(ResourceLocation.withDefaultNamespace("block/stone"),modelState,spriteGetter);

        return new OreNodeBakedModel(defaultBackgroundModel, coreBaseModel, coreHighlightModel);
    }
}