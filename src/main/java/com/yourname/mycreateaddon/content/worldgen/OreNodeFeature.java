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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final float MAX_BONUS_MULTIPLIER = 3.0f;
    private static final int EFFECTIVE_DISTANCE = 10;

    // [추가] 스캔 결과를 담을 간단한 record
    private record OreScanResult(Map<Item, Float> composition, Map<Item, Block> itemToBlockMap) {}

    public OreNodeFeature(Codec<OreNodeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OreNodeConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        OreNodeConfiguration config = context.config();
        RandomSource random = context.random();

        BlockState backgroundState = level.getBlockState(origin);
        if (!backgroundState.isSolidRender(level, origin)) {
            return false;
        }
        Block backgroundBlock = backgroundState.getBlock();

        level.setBlock(origin, MyAddonBlocks.ORE_NODE.get().defaultBlockState(), 3);
        BlockEntity be = level.getBlockEntity(origin);

        if (be instanceof OreNodeBlockEntity nodeBE) {
            // [수정] 두 개의 맵을 모두 생성
            OreScanResult scanResult = scanAndGenerateOreData(context, origin, random);
            Map<Item, Float> composition = scanResult.composition();
            Map<Item, Block> itemToBlockMap = scanResult.itemToBlockMap();

            if (composition.isEmpty()) {
                composition.put(Items.RAW_IRON, 1.0f);
                itemToBlockMap.put(Items.RAW_IRON, Blocks.IRON_ORE);
            }

            // [수정] 대표 블록을 찾을 때 두 맵을 모두 활용
            Block representativeOreBlock = findRepresentativeOreBlock(composition, itemToBlockMap);

            int maxYield = config.totalYield().sample(random);
            float hardness = 0.5f + random.nextFloat() * 1.5f;
            float richness = 0.8f + random.nextFloat() * 0.7f;
            float regeneration = random.nextFloat() < 0.2f ? (0.00025f + random.nextFloat() * 0.00075f) : 0f;

            // [수정] configure 메서드에 두 맵을 모두 전달
            nodeBE.configure(composition, itemToBlockMap, maxYield, hardness, richness, regeneration, backgroundBlock, representativeOreBlock);
            return true;
        }

        return false;
    }
    // [수정] 대표 블록을 찾는 로직이 매우 간단해집니다.
    private Block findRepresentativeOreBlock(Map<Block, Float> composition) {
        return composition.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Blocks.IRON_ORE); // 실패 시 기본값
    }

    // [수정] 대표 블록을 찾는 로직
    private Block findRepresentativeOreBlock(Map<Item, Float> composition, Map<Item, Block> itemToBlockMap) {
        // 가장 비중이 높은 아이템을 찾음
        Optional<Item> representativeItemOpt = composition.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        // 해당 아이템에 매핑된 블록을 반환
        return representativeItemOpt.map(itemToBlockMap::get).orElse(Blocks.IRON_ORE);
    }

    // [수정] 스캔 로직을 두 개의 맵을 반환하도록 변경
    private OreScanResult scanAndGenerateOreData(FeaturePlaceContext<OreNodeConfiguration> context, BlockPos pos, RandomSource randomSource) {
        WorldGenLevel level = context.level();
        int nodeYLevel = pos.getY();

        Holder<Biome> biomeHolder = level.getBiome(pos);
        BiomeGenerationSettings generationSettings = biomeHolder.value().getGenerationSettings();
        HolderSet<PlacedFeature> oreFeatures = generationSettings.features().get(GenerationStep.Decoration.UNDERGROUND_ORES.ordinal());

        Map<Item, Float> weightedSelection = new HashMap<>();
        Map<Item, Block> itemToBlockMap = new HashMap<>(); // 아이템-블록 매핑 맵
        WorldGenerationContext worldGenContext = new WorldGenerationContext(context.chunkGenerator(), level);

        var registryAccess = context.level().registryAccess();
        var blockRegistry = registryAccess.registryOrThrow(Registries.BLOCK);

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
                        TagKey<Block> oresTag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));
                        float baseWeight = blockRegistry.wrapAsHolder(oreBlock).is(oresTag) ? 10.0f : 0.5f;

                        // ... (가중치 계산 로직은 동일) ...
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
                        itemToBlockMap.put(oreItem, oreBlock); // 매핑 정보 추가
                    }
                }
            }
        }

        if (weightedSelection.isEmpty()) {
            return new OreScanResult(Collections.emptyMap(), Collections.emptyMap());
        }

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

        return new OreScanResult(finalComposition, itemToBlockMap);
    }


    private int getAverageOreDepth(PlacedFeature feature, WorldGenerationContext context) {
        for (PlacementModifier modifier : feature.placement()) {
            if (modifier instanceof HeightRangePlacement heightRange) {
                try {
                    if (heightRange_heightField == null) {
                        heightRange_heightField = HeightRangePlacement.class.getDeclaredField("height");
                        heightRange_heightField.setAccessible(true);
                    }
                    Object heightProvider = heightRange_heightField.get(heightRange);

                    if (heightProvider instanceof UniformHeight uniformHeight) {
                        if (uniformHeight_minInclusiveField == null) {
                            uniformHeight_minInclusiveField = UniformHeight.class.getDeclaredField("minInclusive");
                            uniformHeight_minInclusiveField.setAccessible(true);
                        }
                        if (uniformHeight_maxInclusiveField == null) {
                            uniformHeight_maxInclusiveField = UniformHeight.class.getDeclaredField("maxInclusive");
                            uniformHeight_maxInclusiveField.setAccessible(true);
                        }

                        VerticalAnchor minAnchor = (VerticalAnchor) uniformHeight_minInclusiveField.get(uniformHeight);
                        VerticalAnchor maxAnchor = (VerticalAnchor) uniformHeight_maxInclusiveField.get(uniformHeight);

                        int minY = minAnchor.resolveY(context);
                        int maxY = maxAnchor.resolveY(context);

                        return (minY + maxY) / 2;
                    }

                } catch (NoSuchFieldException | IllegalAccessException e) {
                    //e.printStackTrace();
                    return Integer.MIN_VALUE;
                }
            }
        }
        return Integer.MIN_VALUE;
    }
    private Item getOreItem(Block oreBlock, WorldGenLevel level) {
        Item oreBlockItem = oreBlock.asItem();
        if (oreBlockItem == Items.AIR) {
            return Items.AIR;
        }

        var recipeManager = Objects.requireNonNull(level.getServer()).getRecipeManager();

        for (var recipeHolder : recipeManager.getAllRecipesFor(RecipeType.SMELTING)) {
            SmeltingRecipe recipe = recipeHolder.value();

            if (recipe.getIngredients().getFirst().test(new ItemStack(oreBlockItem))) {
                ItemStack resultStack = recipe.getResultItem(level.registryAccess());
                Item resultItem = resultStack.getItem();

                ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(resultItem);
                if (resultId.getPath().endsWith("_ingot")) {
                    String rawMaterialName = resultId.getPath().replace("_ingot", "");
                    ResourceLocation rawId = ResourceLocation.fromNamespaceAndPath(resultId.getNamespace(), "raw_" + rawMaterialName);

                    Item rawItem = BuiltInRegistries.ITEM.get(rawId);
                    if (rawItem != Items.AIR) {
                        return rawItem;
                    }
                }
                return resultItem;
            }
        }
        return oreBlockItem;
    }

}