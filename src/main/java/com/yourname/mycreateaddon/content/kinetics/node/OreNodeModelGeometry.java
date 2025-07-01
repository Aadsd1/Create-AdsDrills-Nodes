package com.yourname.mycreateaddon.content.kinetics.node;


import com.mojang.datafixers.util.Pair;
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

    // [핵심 수정] bake 메서드 시그니처에서 마지막 파라미터(modelLocation) 제거
    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
        // 기본 모델(돌)을 미리 구워놓고, 이를 fallback 및 기본값으로 사용합니다.
        BakedModel defaultModel = baker.bake(
                ResourceLocation.withDefaultNamespace("block/stone"),
                modelState,
                spriteGetter
        );
        // 생성한 기본 모델을 OreNodeBakedModel 생성자에 넘겨줍니다.
        return new OreNodeBakedModel(defaultModel);
    }

}