package com.yourname.mycreateaddon.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.worldgen.ConditionalFeatureAdditionModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder; // [신규] import
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class MyAddonBiomeModifiers {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS, MyCreateAddon.MOD_ID);

    // [핵심 수정] DeferredHolder의 제네릭 타입을 MapCodec으로 변경
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<ConditionalFeatureAdditionModifier>> CONDITIONAL_ADDITION =
            BIOME_MODIFIER_SERIALIZERS.register("conditional_feature_addition", ConditionalFeatureAdditionModifier::makeCodec);

    public static void register(IEventBus eventBus) {
        BIOME_MODIFIER_SERIALIZERS.register(eventBus);
    }
}