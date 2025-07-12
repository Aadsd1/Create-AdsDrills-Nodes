package com.yourname.mycreateaddon.registry;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.worldgen.OreNodeConfiguration;
import com.yourname.mycreateaddon.content.worldgen.OreNodeFeature;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import com.tterrag.registrate.util.entry.RegistryEntry;//
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;



public class MyAddonFeatures {


    private static final RegistryEntry<Feature<?>, OreNodeFeature> ORE_NODE_FEATURE_ENTRY = MyCreateAddon.registrate()
            .generic("ore_node", Registries.FEATURE, () -> new OreNodeFeature(OreNodeConfiguration.CODEC))
            .register();

    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_NODE_CONFIGURED_FEATURE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "ore_node"));

    public static final ResourceKey<PlacedFeature> ORE_NODE_PLACED_FEATURE =
            ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "ore_node"));

    public static void bootstrapConfiguredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Feature<OreNodeConfiguration>으로 정확하게 캐스팅
        Feature<OreNodeConfiguration> feature = ORE_NODE_FEATURE_ENTRY.get();

        context.register(ORE_NODE_CONFIGURED_FEATURE,
                new ConfiguredFeature<>(feature,
                        // [핵심 수정] OreNodeConfiguration 생성자에 새로운 인자 추가
                        new OreNodeConfiguration(
                                UniformInt.of(1000, 3000),     // totalYield
                                UniformInt.of(150, 250),      // miningResistance
                                UniformInt.of(5000, 20000),   // fluidCapacity (5~20 버킷)
                                0.4f                          // fluidChance (40% 확률로 유체를 가짐)
                        )
                )
        );
    }

    public static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        var configuredFeatureRegistry = context.lookup(Registries.CONFIGURED_FEATURE);
        Holder<ConfiguredFeature<?, ?>> configuredFeatureHolder = configuredFeatureRegistry.getOrThrow(ORE_NODE_CONFIGURED_FEATURE);

        context.register(ORE_NODE_PLACED_FEATURE,
                new PlacedFeature(
                        configuredFeatureHolder,
                        List.of(
                                CountPlacement.of(8),
                                InSquarePlacement.spread(),
                                HeightRangePlacement.uniform(
                                        VerticalAnchor.absolute(-64),
                                        VerticalAnchor.absolute(80)
                                ),
                                BiomeFilter.biome()
                        )
                )
        );
    }

    public static void register() {
        // 클래스 로드용
    }
}