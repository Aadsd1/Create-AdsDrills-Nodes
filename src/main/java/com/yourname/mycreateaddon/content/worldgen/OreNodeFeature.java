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
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class OreNodeFeature extends Feature<OreNodeConfiguration> {

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
        Map<Item, Float> composition = scanAndGenerateComposition(level, origin, randomSource);

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

    /**
     * 현재 위치의 바이옴 정보를 스캔하여 원래 생성될 광물을 기반으로 노드 조성을 결정합니다.
     * @param level 월드 레벨
     * @param pos 생성 위치
     * @param randomSource 마인크래프트의 RandomSource
     * @return <아이템, 비율> 맵
     */
    private Map<Item, Float> scanAndGenerateComposition(WorldGenLevel level, BlockPos pos, RandomSource randomSource) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        BiomeGenerationSettings generationSettings = biomeHolder.value().getGenerationSettings();

        // 지하 광물 생성 단계에 등록된 모든 PlacedFeature 목록을 가져옵니다.
        HolderSet<PlacedFeature> oreFeatures = generationSettings.features().get(GenerationStep.Decoration.UNDERGROUND_ORES.ordinal());


        Map<Item, Float> weightedSelection = new HashMap<>();

        for (Holder<PlacedFeature> placedFeatureHolder : oreFeatures) {
            // PlacedFeature -> ConfiguredFeature -> Feature -> OreConfiguration 순서로 탐색합니다.
            Optional<PlacedFeature> placedFeatureOpt = placedFeatureHolder.unwrapKey().flatMap(level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)::getOptional);
            if (placedFeatureOpt.isEmpty()) continue;

            ConfiguredFeature<?, ?> configuredFeature = placedFeatureOpt.get().feature().value();

            // 바닐라 OreFeature인지 확인합니다.
            if (configuredFeature.feature() == Feature.ORE) {
                if (configuredFeature.config() instanceof OreConfiguration oreConfig) {
                    for (OreConfiguration.TargetBlockState target : oreConfig.targetStates) {
                        Block oreBlock = target.state.getBlock();
                        // getOreItem 호출 시 level 객체를 추가로 전달합니다.
                        Item oreItem = getOreItem(oreBlock, level);
                        if (oreItem != Items.AIR) {

                            weightedSelection.put(oreItem, 10f);
                        }
                    }
                }
            }
        }


        if (weightedSelection.isEmpty()) {
            return Collections.emptyMap();
        }

        // ---- 최종 조합 생성 ----
        // Collections.shuffle 등 java.util의 클래스와 호환성을 위해 새로운 Random 인스턴스를 생성합니다.
        Random random = new Random(randomSource.nextLong());

        // 1~3 종류의 광물을 무작위로 선택합니다.
        int oreTypeCount = 1 + random.nextInt(Math.min(3, weightedSelection.size()));

        List<Map.Entry<Item, Float>> potentialOres = new ArrayList<>(weightedSelection.entrySet());
        Collections.shuffle(potentialOres, random); // 매번 다른 조합이 나오도록 리스트를 섞습니다.

        Map<Item, Float> finalComposition = new HashMap<>();
        float totalWeight = 0;
        for (int i = 0; i < oreTypeCount; i++) {
            Map.Entry<Item, Float> entry = potentialOres.get(i);
            finalComposition.put(entry.getKey(), entry.getValue());
            totalWeight += entry.getValue();
        }

        // 최종 선택된 광물들의 비율을 합이 1.0이 되도록 정규화합니다.
        for (Map.Entry<Item, Float> entry : finalComposition.entrySet()) {
            entry.setValue(entry.getValue() / totalWeight);
        }

        return finalComposition;
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