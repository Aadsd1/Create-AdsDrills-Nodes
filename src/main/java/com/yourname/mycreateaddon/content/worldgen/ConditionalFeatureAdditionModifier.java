package com.yourname.mycreateaddon.content.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yourname.mycreateaddon.config.MyAddonConfigs;
import com.yourname.mycreateaddon.registry.MyAddonBiomeModifiers;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// [핵심 수정] biomes 필드를 제거하여 레코드를 단순화
public record ConditionalFeatureAdditionModifier(Holder<PlacedFeature> feature) implements BiomeModifier {

    public static MapCodec<ConditionalFeatureAdditionModifier> makeCodec() {
        return RecordCodecBuilder.mapCodec(builder -> builder.group(
                // [핵심 수정] feature 필드만 인코딩/디코딩
                PlacedFeature.CODEC.fieldOf("feature").forGetter(ConditionalFeatureAdditionModifier::feature)
        ).apply(builder, ConditionalFeatureAdditionModifier::new));
    }

    @Override
    public void modify(@NotNull Holder<Biome> biome, @NotNull Phase phase, ModifiableBiomeInfo.BiomeInfo.@NotNull Builder builder) {
        // [핵심 수정] biomes.contains() 체크를 완전히 제거. 이 Modifier는 모든 바이옴에 대해 실행됨.
        if (phase == Phase.ADD) {
            List<? extends String> allowedDimensions = MyAddonConfigs.SERVER.allowedDimensions.get();

            boolean inOverworld = biome.is(BiomeTags.IS_OVERWORLD);
            boolean inNether = biome.is(BiomeTags.IS_NETHER);
            boolean inEnd = biome.is(BiomeTags.IS_END);

            // 설정 파일의 값에 따라 피처 추가 여부를 런타임에 결정
            if ((allowedDimensions.contains("minecraft:overworld") && inOverworld) ||
                    (allowedDimensions.contains("minecraft:the_nether") && inNether) ||
                    (allowedDimensions.contains("minecraft:the_end") && inEnd)) {
                builder.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, this.feature);
            }
        }
    }

    @Override
    public @NotNull MapCodec<? extends BiomeModifier> codec() {
        return MyAddonBiomeModifiers.CONDITIONAL_ADDITION.get();
    }
}