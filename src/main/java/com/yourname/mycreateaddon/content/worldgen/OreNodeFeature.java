package com.yourname.mycreateaddon.content.worldgen;


import com.mojang.serialization.Codec;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlockEntity;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;

import java.lang.reflect.Field;
import java.util.*;

public class OreNodeFeature extends Feature<OreNodeConfiguration> {

    private static Field heightRange_heightField;
    private static Field uniformHeight_minInclusiveField;
    private static Field uniformHeight_maxInclusiveField;

    public OreNodeFeature(Codec<OreNodeConfiguration> codec) {
        super(codec);
    }


    @Override
    public boolean place(FeaturePlaceContext<OreNodeConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        OreNodeConfiguration config = context.config();
        RandomSource randomSource = context.random();

        if (!level.getBlockState(origin).isSolidRender(level, origin)) {
            return false;
        }

        // 월드 생성 정보를 스캔하여 동적으로 광물 조성을 결정합니다.
        Map<Item, Float> composition = scanAndGenerateComposition(context, origin, randomSource);

        // 만약 스캔 결과 아무것도 찾지 못했다면, 안전장치로 철을 생성합니다.
        if (composition.isEmpty()) {
            composition.put(Items.RAW_IRON, 1.0f);
        }

        int totalYield = config.totalYield().sample(randomSource);
        int miningResistance = config.miningResistance().sample(randomSource);

        level.setBlock(origin, MyAddonBlocks.ORE_NODE.get().defaultBlockState(), 3);
        BlockEntity be = level.getBlockEntity(origin);
        if (be instanceof OreNodeBlockEntity nodeBE) {
            nodeBE.configure(composition, totalYield, miningResistance);
            return true;
        }

        return false;
    }

    private static final float MAX_BONUS_MULTIPLIER = 3.0f; // 깊이가 완벽하게 일치할 때 가중치에 곱해지는 최대 배수 (기본 가중치 10.0f -> 최대 30.0f)
    private static final int EFFECTIVE_DISTANCE = 10; // 이 거리(블록)를 벗어나면 깊이 보너스가 거의 적용되지 않습니다.


    private Map<Item, Float> scanAndGenerateComposition(FeaturePlaceContext<OreNodeConfiguration> context, BlockPos pos, RandomSource randomSource) {
        WorldGenLevel level = context.level();
        int nodeYLevel = pos.getY();

        Holder<Biome> biomeHolder = level.getBiome(pos);
        BiomeGenerationSettings generationSettings = biomeHolder.value().getGenerationSettings();
        HolderSet<PlacedFeature> oreFeatures = generationSettings.features().get(GenerationStep.Decoration.UNDERGROUND_ORES.ordinal());

        Map<Item, Float> weightedSelection = new HashMap<>();

        WorldGenerationContext worldGenContext = new WorldGenerationContext(context.chunkGenerator(), level);

        for (Holder<PlacedFeature> placedFeatureHolder : oreFeatures) {
            Optional<PlacedFeature> placedFeatureOpt = placedFeatureHolder.unwrapKey().flatMap(level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)::getOptional);
            if (placedFeatureOpt.isEmpty()) continue;

            PlacedFeature placedFeature = placedFeatureOpt.get();
            ConfiguredFeature<?, ?> configuredFeature = placedFeature.feature().value();

            if (configuredFeature.feature() == Feature.ORE && configuredFeature.config() instanceof OreConfiguration oreConfig) {
                for (OreConfiguration.TargetBlockState target : oreConfig.targetStates) {
                    Block oreBlock = target.state.getBlock();
                    Item oreItem = getOreItem(oreBlock, level);

                    if (oreItem != Items.AIR) {
                        Holder<Block> blockHolder = oreBlock.builtInRegistryHolder();
                        TagKey<Block> oresTag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));
                        float baseWeight = blockHolder.is(oresTag) ? 10.0f : 0.5f;

                        // 깊이 보너스 계산
                        int averageOreY = getAverageOreDepth(placedFeature, worldGenContext);
                        float depthMultiplier = 1.0f;

                        if (averageOreY != Integer.MIN_VALUE) {
                            int distance = Math.abs(nodeYLevel - averageOreY);
                            if (distance < EFFECTIVE_DISTANCE) {
                                float closeness = 1.0f - ((float) distance / EFFECTIVE_DISTANCE);
                                depthMultiplier = ((MAX_BONUS_MULTIPLIER - 1.0f) * closeness) + 1.0f;
                            }
                        }

                        float finalWeight = baseWeight * depthMultiplier;
                        weightedSelection.put(oreItem, finalWeight);
                    }
                }
            }
        }

        // 최종 조합 생성
        if (weightedSelection.isEmpty()) return Collections.emptyMap();
        Random random = new Random(randomSource.nextLong());
        int oreTypeCount = 1 + random.nextInt(Math.min(3, weightedSelection.size()));
        List<Map.Entry<Item, Float>> potentialOres = new ArrayList<>(weightedSelection.entrySet());
        Collections.shuffle(potentialOres, random);
        Map<Item, Float> finalComposition = new HashMap<>();
        float totalWeight = 0;
        for (int i = 0; i < oreTypeCount; i++) {
            Map.Entry<Item, Float> entry = potentialOres.get(i);
            finalComposition.put(entry.getKey(), entry.getValue());
            totalWeight += entry.getValue();
        }
        for (Map.Entry<Item, Float> entry : finalComposition.entrySet()) {
            entry.setValue(entry.getValue() / totalWeight);
        }
        return finalComposition;
    }

    /**
     * PlacedFeature의 배치 설정자를 분석하여 평균 생성 높이를 추정합니다.
     * (정확한 2단계 리플렉션을 사용하는 최종 안정화 버전)
     *
     * @param feature 분석할 PlacedFeature
     * @param context Y 좌표 계산에 필요한 WorldGenerationContext
     * @return 계산된 평균 Y 좌표. 찾지 못하면 Integer.MIN_VALUE 반환.
     */
    private int getAverageOreDepth(PlacedFeature feature, WorldGenerationContext context) {
        for (PlacementModifier modifier : feature.placement()) {
            if (modifier instanceof HeightRangePlacement heightRange) {
                try {
                    // --- 1단계: HeightRangePlacement에서 'height' 필드 가져오기 ---
                    if (heightRange_heightField == null) {
                        heightRange_heightField = HeightRangePlacement.class.getDeclaredField("height");
                        heightRange_heightField.setAccessible(true);
                    }
                    // height 필드에서 HeightProvider 객체를 가져옴
                    Object heightProvider = heightRange_heightField.get(heightRange);

                    // --- 2단계: 가져온 객체가 UniformHeight일 경우, 내부 필드 접근 ---
                    if (heightProvider instanceof UniformHeight uniformHeight) {
                        if (uniformHeight_minInclusiveField == null) {
                            uniformHeight_minInclusiveField = UniformHeight.class.getDeclaredField("minInclusive");
                            uniformHeight_minInclusiveField.setAccessible(true);
                        }
                        if (uniformHeight_maxInclusiveField == null) {
                            uniformHeight_maxInclusiveField = UniformHeight.class.getDeclaredField("maxInclusive");
                            uniformHeight_maxInclusiveField.setAccessible(true);
                        }

                        // uniformHeight 객체에서 VerticalAnchor 객체들을 가져옴
                        VerticalAnchor minAnchor = (VerticalAnchor) uniformHeight_minInclusiveField.get(uniformHeight);
                        VerticalAnchor maxAnchor = (VerticalAnchor) uniformHeight_maxInclusiveField.get(uniformHeight);

                        int minY = minAnchor.resolveY(context);
                        int maxY = maxAnchor.resolveY(context);

                        return (minY + maxY) / 2;
                    }

                } catch (NoSuchFieldException | IllegalAccessException e) {
                    // 이 오류는 이제 발생하지 않아야 하지만, 안전을 위해 남겨둡니다.
                    e.printStackTrace();
                    return Integer.MIN_VALUE;
                }
            }
        }
        return Integer.MIN_VALUE;
    }
    /**
     * 제련 레시피를 역추적하여 광석 블록에 해당하는 아이템을 자동으로 찾아 반환합니다.
     * @param oreBlock 광석 블록
     * @param level 월드 레벨
     * @return 해당하는 광물 아이템
     */

    private Item getOreItem(Block oreBlock, WorldGenLevel level) {
        Item oreBlockItem = oreBlock.asItem();
        if (oreBlockItem == Items.AIR) {
            return Items.AIR;
        }

        // 서버의 레시피 매니저에 접근합니다.
        // 레시피 데이터는 월드 생성 중에도 안전하게 읽을 수 있습니다.
        var recipeManager = level.getServer().getRecipeManager();

        // 모든 제련 레시피(SmeltingRecipe)를 순회합니다.
        for (var recipeHolder : recipeManager.getAllRecipesFor(RecipeType.SMELTING)) {
            SmeltingRecipe recipe = recipeHolder.value();

            // 레시피의 '재료'가 현재 '광석 블록 아이템'과 일치하는지 확인합니다.
            if (recipe.getIngredients().get(0).test(new ItemStack(oreBlockItem))) {
                // 레시피의 '결과물' (예: 철 주괴)을 가져옵니다.
                ItemStack resultStack = recipe.getResultItem(level.registryAccess());
                Item resultItem = resultStack.getItem();

                // 결과물이 주괴(Ingot)라면, 해당하는 원시 광물(Raw Material)을 찾아 반환합니다.
                // 이것은 이름 기반의 규칙이지만, 대부분의 모드가 이 명명 규칙을 따릅니다.
                ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(resultItem);
                if (resultId.getPath().endsWith("_ingot")) {
                    String rawMaterialName = resultId.getPath().replace("_ingot", "");
                    // 예: "iron_ingot" -> "raw_iron"
                    ResourceLocation rawId = ResourceLocation.fromNamespaceAndPath(resultId.getNamespace(), "raw_" + rawMaterialName);

                    Item rawItem = BuiltInRegistries.ITEM.get(rawId);
                    // 만약 해당하는 원시 광물이 존재한다면 그것을 반환합니다.
                    if (rawItem != Items.AIR) {
                        return rawItem;
                    }
                }
                // 주괴가 아니거나(예: 다이아몬드), 원시 광물이 없는 경우, 제련 결과물 자체를 반환합니다.
                return resultItem;
            }
        }

        // 제련 레시피를 찾지 못한 경우, 최후의 수단으로 블록 아이템 자신을 반환합니다.
        return oreBlockItem;
    }

}