package com.adsd.adsdrill.content.worldgen;


import com.mojang.serialization.Codec;
import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlock;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlockEntity;
import com.adsd.adsdrill.registry.AdsDrillBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
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
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

public class OreNodeFeature extends Feature<OreNodeConfiguration> {
    // [신규] 비율 프리셋을 정의하는 내부 record
    private record RatioPreset(String name, List<Float> ratios) {}

    // [신규] 사용 가능한 모든 프리셋 리스트
    private static final List<RatioPreset> PRESETS = List.of(
            // 1개 광물 집중형
            new RatioPreset("Dominant",   List.of(0.85f)),
            new RatioPreset("Rich Vein",  List.of(0.70f)),

            // 2개 광물 혼합형
            new RatioPreset("Balanced Pair", List.of(0.45f, 0.45f)),
            new RatioPreset("Major/Minor",   List.of(0.60f, 0.30f)),
            new RatioPreset("Alloy",         List.of(0.50f, 0.40f)),

            // 3개 광물 혼합형
            new RatioPreset("Trio",          List.of(0.30f, 0.30f, 0.30f)),
            new RatioPreset("Tiered Trio",   List.of(0.50f, 0.30f, 0.15f)),
            new RatioPreset("Scattered Mix", List.of(0.40f, 0.25f, 0.25f))
    );

    private record OreScanResult(Map<Item, Float> weightedComposition, Map<Item, Block> itemToBlockMap) {}


    public OreNodeFeature(Codec<OreNodeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OreNodeConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        OreNodeConfiguration featureConfig = context.config();
        RandomSource random = context.random();

        BlockState originalState = level.getBlockState(origin);
        if (!originalState.isSolidRender(level, origin)) {
            return false;
        }

        level.setBlock(origin, AdsDrillBlocks.ORE_NODE.get().defaultBlockState(), 3);
        BlockEntity be = level.getBlockEntity(origin);

        if (be instanceof OreNodeBlockEntity nodeBE) {
            // 1. 바이옴 스캔으로 기본 가중치 데이터를 얻습니다.
            OreScanResult biomeScanResult = scanAndGenerateOreData(context, origin);
            Map<Item, Float> finalWeightedSelection = new HashMap<>(biomeScanResult.weightedComposition());
            Map<Item, Block> finalItemToBlockMap = new HashMap<>(biomeScanResult.itemToBlockMap());

            // 2. 현재 차원의 프로필을 가져옵니다.
            ResourceLocation dimensionId = level.getLevel().dimension().location();
            Optional<AdsDrillConfigs.DimensionGenerationProfile> profileOpt = AdsDrillConfigs.SERVER.getProfileForDimension(dimensionId);

            int maxYield;
            float hardness, richness, regeneration;
            FluidStack fluidToPlace = FluidStack.EMPTY;
            int fluidCapacity = 0;

            if (profileOpt.isPresent()) {
                AdsDrillConfigs.DimensionGenerationProfile profile = profileOpt.get();
                AdsDrillConfigs.DimensionGenerationProfile.Stats stats = profile.stats();

                // 2a. 스탯을 프로필 값으로 덮어씁니다.
                maxYield = UniformInt.of(stats.minYield(), stats.maxYield()).sample(random);
                hardness = (float) (stats.minHardness() + random.nextDouble() * (stats.maxHardness() - stats.minHardness()));
                richness = (float) (stats.minRichness() + random.nextDouble() * (stats.maxRichness() - stats.minRichness()));
                regeneration = (float) (stats.minRegeneration() + random.nextDouble() * (stats.maxRegeneration() - stats.minRegeneration()));

                // 3. 차원의 전역 ore_pool을 먼저 적용합니다.
                applyOrePool(finalWeightedSelection, finalItemToBlockMap, profile.orePool());

                // 4. 현재 바이옴에 맞는 biome_overrides를 찾아 적용합니다.
                Holder<Biome> currentBiomeHolder = level.getBiome(origin);
                for (AdsDrillConfigs.BiomeOverride override : profile.biomeOverrides()) {
                    for (String biomeIdentifier : override.biomes()) {
                        boolean isMatch = false;
                        // '#'으로 시작하면 태그로 인식
                        if (biomeIdentifier.startsWith("#")) {
                            TagKey<Biome> tagKey = TagKey.create(Registries.BIOME, ResourceLocation.parse(biomeIdentifier.substring(1)));
                            if (currentBiomeHolder.is(tagKey)) {
                                isMatch = true;
                            }
                        } else { // 아니면 바이옴 ID로 인식
                            if (currentBiomeHolder.unwrapKey().map(key -> key.location().toString().equals(biomeIdentifier)).orElse(false)) {
                                isMatch = true;
                            }
                        }

                        // 일치하는 바이옴/태그를 찾으면, 해당 override의 ore_pool을 적용하고 다음 override로 넘어갑니다.
                        if (isMatch) {
                            applyOrePool(finalWeightedSelection, finalItemToBlockMap, override.orePool());
                            break;
                        }
                    }
                }

                // 2c. 유체 설정을 프로필 값으로 덮어씁니다.
                if (random.nextFloat() < stats.fluidChance()) {
                    fluidToPlace = scanForBiomeFluid(context, random);
                    if (!fluidToPlace.isEmpty()) {
                        fluidCapacity = UniformInt.of(stats.minFluidCapacity(), stats.maxFluidCapacity()).sample(random);
                    }
                }

            } else {
                // --- 프로필이 없을 경우: 기본 로직을 사용합니다. ---
                maxYield = featureConfig.totalYield().sample(random);
                hardness = 0.5f + random.nextFloat() * 1.5f;
                richness = 0.8f + random.nextFloat() * 0.7f;
                regeneration = 0.0005f + random.nextFloat() * 0.0045f;
                if (random.nextFloat() < 0.15f) {
                    regeneration *= (1.5f + random.nextFloat() * 2.0f);
                }
                if (random.nextFloat() < featureConfig.fluidChance()) {
                    fluidToPlace = scanForBiomeFluid(context, random);
                    if (!fluidToPlace.isEmpty()) {
                        fluidCapacity = featureConfig.fluidCapacity().sample(random);
                    }
                }
            }

            // 5. 최종적으로 병합된 가중치 목록으로 노드 구성을 결정합니다.
            Map<Item, Float> finalComposition = generateFinalComposition(finalWeightedSelection, random);

            if (finalComposition.isEmpty()) {
                finalComposition.put(Items.RAW_IRON, 1.0f);
                finalItemToBlockMap.put(Items.RAW_IRON, Blocks.IRON_ORE);
            }

            Block representativeOreBlock = findRepresentativeOreBlock(finalComposition, finalItemToBlockMap);

            nodeBE.configureFromFeature(finalComposition, finalItemToBlockMap,
                    maxYield, hardness, richness, regeneration,
                    originalState.getBlock(), representativeOreBlock,
                    fluidToPlace, fluidCapacity);

            return true;
        }
        return false;
    }
    private void applyOrePool(Map<Item, Float> weightedSelection, Map<Item, Block> itemToBlockMap, List<AdsDrillConfigs.DimensionGenerationProfile.OrePoolEntry> orePool) {
        if (orePool.isEmpty()) return;

        for (AdsDrillConfigs.DimensionGenerationProfile.OrePoolEntry entry : orePool) {
            Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(entry.itemId()));
            Optional<Block> blockOpt = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(entry.blockId()));

            if (itemOpt.isPresent() && blockOpt.isPresent()) {
                Item item = itemOpt.get();
                float baseWeight = weightedSelection.getOrDefault(item, 0.0f);
                float multipliedWeight = (float) (baseWeight * entry.weight_multiplier());
                float finalWeight = (float) (multipliedWeight + entry.weight_add());

                if (finalWeight > 0) {
                    weightedSelection.put(item, finalWeight);
                    itemToBlockMap.putIfAbsent(item, blockOpt.get());
                } else {
                    weightedSelection.remove(item);
                }
            }
        }
    }

    private Map<Item, Float> generateFinalComposition(Map<Item, Float> weightedSelection, RandomSource randomSource) {
        if (weightedSelection.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Item, Float> finalComposition = new HashMap<>();
        List<Item> candidatePool = new ArrayList<>(weightedSelection.keySet());

        // 1. 노드에 포함될 광물 종류 개수를 설정에서 가져와 결정
        int minTypes = AdsDrillConfigs.SERVER.minOreTypesPerNode.get();
        int maxTypes = AdsDrillConfigs.SERVER.maxOreTypesPerNode.get();

        int maxPossible = Math.min(maxTypes, candidatePool.size());
        int minPossible = Math.min(minTypes, maxPossible);

        int oreTypeCount = minPossible;
        if (maxPossible > minPossible) {
            oreTypeCount += randomSource.nextInt(maxPossible - minPossible + 1);
        }
        // 2. 가중치 기반으로 oreTypeCount만큼 광물을 무작위로 선택
        List<Item> selectedOres = new ArrayList<>();
        float totalWeight = (float) weightedSelection.values().stream().mapToDouble(f -> f).sum();

        for (int i = 0; i < oreTypeCount && totalWeight > 0; i++) {
            float randomVal = randomSource.nextFloat() * totalWeight;
            float currentWeight = 0;
            Item chosenOre = null;

            for (Item candidate : candidatePool) {
                currentWeight += weightedSelection.get(candidate);
                if (randomVal <= currentWeight) {
                    chosenOre = candidate;
                    break;
                }
            }

            if (chosenOre != null) {
                selectedOres.add(chosenOre);
                candidatePool.remove(chosenOre); // 중복 선택 방지
                totalWeight -= weightedSelection.get(chosenOre);
            }
        }

        if (selectedOres.isEmpty()) {
            // 만약 아무것도 선택되지 않았다면(드문 경우), 가장 가중치가 높은 하나를 선택
            weightedSelection.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .ifPresent(selectedOres::add);
        }

        // 3. 선택된 광물들에 대해 비율 프리셋 적용
        List<RatioPreset> suitablePresets = PRESETS.stream()
                .filter(p -> p.ratios().size() == selectedOres.size())
                .toList();

        if (suitablePresets.isEmpty() || selectedOres.isEmpty()) {
            return Collections.emptyMap();
        }

        RatioPreset selectedPreset = suitablePresets.get(randomSource.nextInt(suitablePresets.size()));

        float totalRatio = 0f;
        for (int i = 0; i < selectedOres.size(); i++) {
            Item ore = selectedOres.get(i);
            float baseRatio = selectedPreset.ratios().get(i);
            float noise = (randomSource.nextFloat() - 0.5f) * 0.3f; // +/- 15% 변동
            float finalRatio = Math.max(0, baseRatio * (1 + noise)); // 비율이 음수가 되지 않도록
            finalComposition.put(ore, finalRatio);
            totalRatio += finalRatio;
        }

        // 4. 불순물 추가 및 정규화
        float remainingRatio = 1.0f - totalRatio;
        if (remainingRatio > 0.05f && !candidatePool.isEmpty()) {
            Item selectedImpurity = candidatePool.get(randomSource.nextInt(candidatePool.size()));
            finalComposition.put(selectedImpurity, remainingRatio);
            totalRatio += remainingRatio;
        }

        final float finalTotalRatio = totalRatio;
        if (finalTotalRatio <= 0) return Collections.emptyMap();
        finalComposition.replaceAll((item, ratio) -> ratio / finalTotalRatio);

        return finalComposition;
    }


    // 대표 블록을 찾는 로직
    private Block findRepresentativeOreBlock(Map<Item, Float> composition, Map<Item, Block> itemToBlockMap) {
        // 가장 비중이 높은 아이템을 찾음
        Optional<Item> representativeItemOpt = composition.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        // 해당 아이템에 매핑된 블록을 반환
        return representativeItemOpt.map(itemToBlockMap::get).orElse(Blocks.IRON_ORE);
    }

    private OreScanResult scanAndGenerateOreData(FeaturePlaceContext<OreNodeConfiguration> context, BlockPos pos) {

        final TagKey<Block> oresTag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));
        WorldGenLevel level = context.level();
        BiomeGenerationSettings generationSettings = level.getBiome(pos).value().getGenerationSettings();
        HolderSet<PlacedFeature> oreFeatures = generationSettings.features().get(GenerationStep.Decoration.UNDERGROUND_ORES.ordinal());

        // 각 광물 아이템별로 발견된 모든 size 값을 리스트로 저장합니다.
        Map<Item, List<Float>> allFoundWeights = new HashMap<>();
        Map<Item, Block> itemToBlockMap = new HashMap<>();

        var registryAccess = context.level().registryAccess();
        var blockRegistry = registryAccess.registryOrThrow(Registries.BLOCK);
        List<String> blacklist = new ArrayList<>(AdsDrillConfigs.SERVER.modBlacklist.get());

        for (Holder<PlacedFeature> placedFeatureHolder : oreFeatures) {
            placedFeatureHolder.unwrapKey()
                    .flatMap(level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)::getOptional)
                    .ifPresent(placedFeature -> {
                        ResourceLocation featureId = placedFeature.feature().unwrapKey().map(ResourceKey::location).orElse(null);
                        if (featureId != null && blacklist.contains(featureId.getNamespace())) {
                            return; // 블랙리스트에 있는 모드의 피처는 건너뜁니다.
                        }

                        if (placedFeature.feature().value().config() instanceof OreConfiguration oreConfig) {
                            for (OreConfiguration.TargetBlockState target : oreConfig.targetStates) {
                                Block oreBlock = target.state.getBlock();
                                if (oreBlock instanceof OreNodeBlock) continue;

                                Item oreItem = getOreItem(oreBlock, level);
                                if (oreItem != Items.AIR) {
                                    float weight = blockRegistry.wrapAsHolder(oreBlock).is(oresTag) ? oreConfig.size : 0.5f;

                                    // 해당 아이템의 리스트에 현재 size 값을 추가합니다.
                                    allFoundWeights.computeIfAbsent(oreItem, k -> new ArrayList<>()).add(weight);
                                    itemToBlockMap.putIfAbsent(oreItem, oreBlock);
                                }
                            }
                        }
                    });
        }

        Map<Item, Float> finalWeightedSelection = new HashMap<>();
        for (Map.Entry<Item, List<Float>> entry : allFoundWeights.entrySet()) {
            List<Float> weights = entry.getValue();
            if (weights.isEmpty()) continue;

            // 리스트를 정렬합니다.
            Collections.sort(weights);

            // 중앙값을 계산합니다.
            float median;
            int size = weights.size();
            if (size % 2 == 0) {
                // 개수가 짝수이면, 가운데 두 값의 평균을 사용합니다.
                median = (weights.get(size / 2 - 1) + weights.get(size / 2)) / 2.0f;
            } else {
                // 개수가 홀수이면, 가운데 값을 그대로 사용합니다.
                median = weights.get(size / 2);
            }

            // 계산된 중앙값을 최종 가중치로 설정합니다.
            finalWeightedSelection.put(entry.getKey(), median);
        }

        return new OreScanResult(finalWeightedSelection, itemToBlockMap);
    }

    /**
     * 바이옴 특성과 피처를 스캔하여 노드에 할당할 유체를 결정하는 메서드
     * 이제 LakeFeature와 SpringFeature를 모두 탐색합니다.
     */
    @SuppressWarnings("deprecation")
    private FluidStack scanForBiomeFluid(FeaturePlaceContext<OreNodeConfiguration> context, RandomSource random) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        Holder<Biome> biomeHolder = level.getBiome(pos);

        Map<Fluid, Float> weightedFluids = new HashMap<>();

        // 1. 바이옴의 기본 특성에 따라 기본 유체(물, 용암) 가중치 부여
        if (biomeHolder.is(BiomeTags.IS_NETHER)) {
            weightedFluids.put(Fluids.LAVA, 20.0f);
        } else if (biomeHolder.is(BiomeTags.IS_OCEAN) || biomeHolder.is(BiomeTags.IS_RIVER)) {
            weightedFluids.put(Fluids.WATER, 25.0f);
        } else {
            weightedFluids.put(Fluids.WATER, 10.0f);
            if (biomeHolder.value().getBaseTemperature() > 1.0f) {
                weightedFluids.put(Fluids.LAVA, 5.0f);
            }
        }

        // 2. 모드 호환성을 위한 동적 스캔 (통합 버전)
        BiomeGenerationSettings generationSettings = biomeHolder.value().getGenerationSettings();

        // 샘(Spring)과 호수(Lake)가 주로 위치하는 모든 관련 단계를 확인
        List<GenerationStep.Decoration> stepsToScan = List.of(
                GenerationStep.Decoration.LAKES,
                GenerationStep.Decoration.LOCAL_MODIFICATIONS,
                GenerationStep.Decoration.UNDERGROUND_DECORATION,
                GenerationStep.Decoration.FLUID_SPRINGS
        );

        for (GenerationStep.Decoration step : stepsToScan) {
            // 해당 단계에 피처가 없으면 건너뛰기
            if (step.ordinal() >= generationSettings.features().size()) continue;

            HolderSet<PlacedFeature> features = generationSettings.features().get(step.ordinal());

            for (Holder<PlacedFeature> placedFeatureHolder : features) {
                Optional<PlacedFeature> placedFeatureOpt = placedFeatureHolder.unwrapKey()
                        .flatMap(level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)::getOptional);

                if (placedFeatureOpt.isEmpty()) continue;

                ConfiguredFeature<?, ?> configuredFeature = placedFeatureOpt.get().feature().value();
                Object config = configuredFeature.config();

                // 2-1. LakeFeature 확인
                if (config instanceof LakeFeature.Configuration lakeConfig) {
                    BlockState fluidBlockState = lakeConfig.fluid().getState(random, pos);
                    Fluid fluid = fluidBlockState.getFluidState().getType();
                    if (fluid != Fluids.EMPTY) {
                        //물 호수일 경우 훨씬 높은 가중치를 부여
                        float weight = (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) ? 4.0f : 1.0f;
                        weightedFluids.merge(fluid, weight, Float::sum);
                    }
                }
                // 2-2. SpringFeature 확인
                else if (config instanceof SpringConfiguration springConfig) {
                    // SpringConfiguration은 FluidState를 직접 가지고 있습니다.
                    Fluid fluid = springConfig.state.getType();
                    if (fluid != Fluids.EMPTY) {
                        // 샘은 비교적 작으므로, 호수보다는 낮은 가중치 부여
                        weightedFluids.merge(fluid, 2.0f, Float::sum);
                    }
                }
            }
        }

        // 3. 가중치 기반으로 최종 유체 랜덤 선택
        if (weightedFluids.isEmpty()) {
            return FluidStack.EMPTY;
        }

        float totalWeight = 0;
        for (Float weight : weightedFluids.values()) {
            totalWeight += weight;
        }

        float randomValue = random.nextFloat() * totalWeight;
        for (Map.Entry<Fluid, Float> entry : weightedFluids.entrySet()) {
            randomValue -= entry.getValue();
            if (randomValue <= 0) {
                return new FluidStack(entry.getKey(), 1);
            }
        }

        return FluidStack.EMPTY;
    }

    private Item getOreItem(Block oreBlock, WorldGenLevel level) {
        // [!!! 신규: 수동 매핑 우선 확인 !!!]
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(oreBlock);
        Optional<ResourceLocation> manualItemId = AdsDrillConfigs.SERVER.getManualMappingForItem(blockId);
        if (manualItemId.isPresent()) {
            return BuiltInRegistries.ITEM.getOptional(manualItemId.get()).orElse(Items.AIR);
        }

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