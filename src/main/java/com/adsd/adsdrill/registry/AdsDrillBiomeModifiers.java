package com.adsd.adsdrill.registry;


import com.mojang.serialization.MapCodec;
import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.content.worldgen.ConditionalFeatureAdditionModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder; // [신규] import
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class AdsDrillBiomeModifiers {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS, AdsDrillAddon.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<ConditionalFeatureAdditionModifier>> CONDITIONAL_ADDITION =
            BIOME_MODIFIER_SERIALIZERS.register("conditional_feature_addition", ConditionalFeatureAdditionModifier::makeCodec);

    public static void register(IEventBus eventBus) {
        BIOME_MODIFIER_SERIALIZERS.register(eventBus);
    }
}