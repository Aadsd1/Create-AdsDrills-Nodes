package com.yourname.mycreateaddon.content.kinetics.node;


import com.mojang.datafixers.util.Pair;
import com.yourname.mycreateaddon.MyCreateAddon;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

public class OreNodeModelGeometry implements IUnbakedGeometry<OreNodeModelGeometry> {

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
        // 1. 코어 모델을 굽습니다.
        BakedModel coreModel = baker.bake(
                ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "block/ore_node_core"),
                modelState,
                spriteGetter
        );

        // 2. 기본 배경 모델(돌)을 굽습니다.
        BakedModel defaultBackgroundModel = baker.bake(
                ResourceLocation.withDefaultNamespace("block/stone"),
                modelState,
                spriteGetter
        );

        // 두 모델을 함께 사용하는 새로운 BakedModel 인스턴스를 생성하여 반환합니다.
        return new OreNodeBakedModel(defaultBackgroundModel, coreModel);
    }
}