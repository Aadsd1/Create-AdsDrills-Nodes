package com.yourname.mycreateaddon.content.worldgen;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record OreNodeConfiguration(
        IntProvider totalYield,
        IntProvider miningResistance
) implements FeatureConfiguration {
    public static final Codec<OreNodeConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.CODEC.fieldOf("total_yield").forGetter(OreNodeConfiguration::totalYield),
            IntProvider.CODEC.fieldOf("mining_resistance").forGetter(OreNodeConfiguration::miningResistance)
    ).apply(instance, OreNodeConfiguration::new));
}