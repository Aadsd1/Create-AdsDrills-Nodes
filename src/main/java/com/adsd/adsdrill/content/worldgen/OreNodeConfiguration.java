package com.adsd.adsdrill.content.worldgen;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record OreNodeConfiguration(
        IntProvider totalYield,
        IntProvider miningResistance,
        // 유체 관련 설정
        IntProvider fluidCapacity, // 유체 저장 용량 범위
        float fluidChance // 유체를 가질 확률 (0.0 ~ 1.0)
) implements FeatureConfiguration {
    public static final Codec<OreNodeConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.CODEC.fieldOf("total_yield").forGetter(OreNodeConfiguration::totalYield),
            IntProvider.CODEC.fieldOf("mining_resistance").forGetter(OreNodeConfiguration::miningResistance),
            // 코덱에 추가
            IntProvider.CODEC.fieldOf("fluid_capacity").forGetter(OreNodeConfiguration::fluidCapacity),
            Codec.FLOAT.fieldOf("fluid_chance").forGetter(OreNodeConfiguration::fluidChance)
    ).apply(instance, OreNodeConfiguration::new));
}