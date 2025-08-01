package com.adsd.adsdrill.content.worldgen;


import com.mojang.serialization.Codec;
import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlock;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlockEntity;
import com.adsd.adsdrill.registry.AdsDrillBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
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
import java.util.concurrent.ConcurrentHashMap;

public class OreNodeFeature extends Feature<OreNodeConfiguration> {

    private record OreScanResult(Map<Item, Float> weightedComposition, Map<Item, Block> itemToBlockMap) {}
    private record BiomeScanResult(OreScanResult oreData, FluidStack fluidData) {}
    
    private static final Map<ResourceKey<Biome>, BiomeScanResult> BIOME_SCAN_CACHE = new ConcurrentHashMap<>();

    public OreNodeFeature(Codec<OreNodeConfiguration> codec) {
        super(codec);
    }

    public static void clearCache() {
        BIOME_SCAN_CACHE.clear();
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


            // 1. 현재 바이옴의 고유 키(ResourceKey)를 가져옵니다.
            Optional<ResourceKey<Biome>> biomeKeyOpt = level.getBiome(origin).unwrapKey();
            if (biomeKeyOpt.isEmpty()) {
                return false; // 바이옴 키를 얻을 수 없으면 생성을 중단합니다.
            }
            ResourceKey<Biome> biomeKey = biomeKeyOpt.get();

            // 1a. 캐시에 해당 바이옴의 데이터가 있는지 확인하고, 없으면 스캔 후 캐시에 저장합니다.
            BiomeScanResult scanResult = BIOME_SCAN_CACHE.computeIfAbsent(biomeKey, key -> {
                OreScanResult oreScanResult = scanAndGenerateOreData(context, origin);
                FluidStack fluidScanResult = scanForBiomeFluid(context, random);
                return new BiomeScanResult(oreScanResult, fluidScanResult);
            });

            OreScanResult biomeScanResult = scanResult.oreData();
            FluidStack scannedFluid = scanResult.fluidData();

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


                if (random.nextFloat() < stats.fluidChance()) {
                    // 1. 적용할 Fluid Pool 결정
                    List<AdsDrillConfigs.FluidPoolEntry> activeFluidPool = null;

                    for (AdsDrillConfigs.BiomeOverride override : profile.biomeOverrides()) {
                        // biome_overrides에 fluid_pool이 정의되어 있는지 확인
                        if (override.fluidPool() != null && !override.fluidPool().isEmpty()) {
                            for (String biomeIdentifier : override.biomes()) {
                                boolean isMatch = false;
                                if (biomeIdentifier.startsWith("#")) {
                                    TagKey<Biome> tagKey = TagKey.create(Registries.BIOME, ResourceLocation.parse(biomeIdentifier.substring(1)));
                                    if (currentBiomeHolder.is(tagKey)) isMatch = true;
                                } else {
                                    if (currentBiomeHolder.unwrapKey().map(key -> key.location().toString().equals(biomeIdentifier)).orElse(false)) isMatch = true;
                                }
                                if (isMatch) {
                                    activeFluidPool = override.fluidPool();
                                    break;
                                }
                            }
                        }
                        if (activeFluidPool != null) break;
                    }

                    // 바이옴 override가 없으면 차원의 전역 fluid_pool 사용
                    if (activeFluidPool == null) {
                        activeFluidPool = profile.fluidPool();
                    }

                    // 2. Fluid Pool을 기반으로 액체 생성 또는 동적 스캔
                    if (activeFluidPool != null && !activeFluidPool.isEmpty()) {
                        fluidToPlace = selectFluidFromPool(activeFluidPool, random);
                    } else {
                        // 설정된 풀이 없으면, 캐시된 스캔 결과(scannedFluid)를 사용합니다.
                        fluidToPlace = scannedFluid;
                    }


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
                    fluidToPlace = scannedFluid;
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
    private FluidStack selectFluidFromPool(List<AdsDrillConfigs.FluidPoolEntry> fluidPool, RandomSource random) {
        double totalWeight = fluidPool.stream().mapToDouble(AdsDrillConfigs.FluidPoolEntry::weight).sum();
        if (totalWeight <= 0) {
            return FluidStack.EMPTY;
        }

        double randomValue = random.nextDouble() * totalWeight;
        for (AdsDrillConfigs.FluidPoolEntry entry : fluidPool) {
            randomValue -= entry.weight();
            if (randomValue <= 0) {
                Optional<Fluid> fluidOpt = BuiltInRegistries.FLUID.getOptional(ResourceLocation.parse(entry.fluidId()));
                return fluidOpt.map(fluid -> new FluidStack(fluid, 1)).orElse(FluidStack.EMPTY);
            }
        }

        return FluidStack.EMPTY; // 비정상적인 경우
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

    /**
     * 바이옴에서 스캔된 광물 후보군과 그 가중치를 기반으로,
     * 최종적으로 광맥 노드를 구성할 광물들의 종류와 비율을 결정합니다.
     * 이 프로세스는 설정 파일(config)의 규칙에 따라 동적으로 이루어집니다.
     *
     * @param weightedSelection 바이옴 스캔을 통해 얻은, 광물 아이템과 그 가중치(등장 확률)가 담긴 맵.
     * @param randomSource      월드 생성을 위한 Random 인스턴스.
     * @return 최종적으로 결정된 광물 구성 (Item -> 비율(0.0~1.0)). 이 맵의 모든 비율의 합은 1.0이 됩니다.
     */
    private Map<Item, Float> generateFinalComposition(Map<Item, Float> weightedSelection, RandomSource randomSource) {
        // 함수 시작 시, 유효한 광물 후보가 없으면 빈 맵을 반환하여 오류를 방지합니다.
        if (weightedSelection.isEmpty()) {
            return Collections.emptyMap();
        }

        // 최종 결과를 담을 맵과, 처리 과정에서 수정될 광물 후보 리스트를 준비합니다.
        Map<Item, Float> finalComposition = new HashMap<>();
        List<Item> candidatePool = new ArrayList<>(weightedSelection.keySet());

        /*
         * [1단계: 생성할 광물 종류의 개수 결정]
         * 서버 설정 파일(adsdrill-server.toml)에 정의된 minOreTypesPerNode와 maxOreTypesPerNode 값을 읽어옵니다.
         * 이 설정값 범위 내에서 이번에 생성할 노드에 몇 종류의 광물이 섞일지를 무작위로 결정합니다.
         */
        int minTypes = AdsDrillConfigs.SERVER.minOreTypesPerNode.get();
        int maxTypes = AdsDrillConfigs.SERVER.maxOreTypesPerNode.get();
        // 설정된 최대치가 실제 후보 광물 수보다 많을 수 없으므로, 둘 중 작은 값을 사용합니다.
        int maxPossible = Math.min(maxTypes, candidatePool.size());
        int minPossible = Math.min(minTypes, maxPossible);
        // 최종적으로 생성될 광물 종류의 개수를 min과 max 사이에서 무작위로 선택합니다.
        int oreTypeCount = minPossible;
        if (maxPossible > minPossible) {
            oreTypeCount += randomSource.nextInt(maxPossible - minPossible + 1);
        }

        /*
         * [2단계: 노드를 구성할 실제 광물 종류 선택]
         * 1단계에서 결정된 개수(oreTypeCount)만큼, 바이옴 스캔으로 얻은 가중치에 따라 광물을 무작위로 선택합니다.
         * 가중치가 높은 광물(해당 바이옴에서 더 흔한 광물)이 더 높은 확률로 선택됩니다.
         */
        List<Item> selectedOres = new ArrayList<>();
        // 모든 광물 후보의 가중치 총합을 계산합니다. 이 값은 가중치 기반 무작위 추첨의 전체 확률 범위가 됩니다.
        float totalOreWeight = (float) weightedSelection.values().stream().mapToDouble(f -> f).sum();

        // 결정된 광물 종류 개수만큼 반복하여 추첨을 진행합니다.
        for (int i = 0; i < oreTypeCount && totalOreWeight > 0; i++) {
            // 0부터 가중치 총합 사이의 무작위 값을 뽑습니다.
            float randomVal = randomSource.nextFloat() * totalOreWeight;
            float currentWeight = 0;
            Item chosenOre = null;

            // 모든 후보를 순회하며 무작위 값이 어느 후보의 가중치 범위에 속하는지 찾습니다.
            for (Item candidate : candidatePool) {
                currentWeight += weightedSelection.get(candidate);
                if (randomVal <= currentWeight) {
                    chosenOre = candidate;
                    break;
                }
            }

            // 후보가 성공적으로 선택되면,
            if (chosenOre != null) {
                selectedOres.add(chosenOre);         // 최종 선택 목록에 추가하고,
                candidatePool.remove(chosenOre);      // 다음 추첨에서 중복 선택되지 않도록 후보 목록에서 제거합니다.
                totalOreWeight -= weightedSelection.get(chosenOre); // 전체 가중치에서도 해당 후보의 가중치를 뺍니다.
            }
        }

        // 드물게 아무 광물도 선택되지 않은 경우, 가장 가중치가 높았던 광물 하나를 안전하게 추가합니다.
        if (selectedOres.isEmpty()) {
            weightedSelection.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .ifPresent(selectedOres::add);
        }

        /*
         * [3단계: 광물 비율 프리셋 선택]
         * 2단계에서 선택된 광물들의 구성 비율을 결정할 '프리셋'을 선택합니다.
         * 예를 들어, 2종류의 광물이 선택되었다면 "50%/40% 합금형" 프리셋이나 "60%/30% 주/부 광물형" 프리셋 등이 후보가 될 수 있습니다.
         */
        final int finalOreTypeCount = selectedOres.size();
        // 설정 파일에서 불러온 모든 프리셋 중, 현재 선택된 광물 개수와 일치하는 프리셋만 필터링합니다.
        List<AdsDrillConfigs.RatioPresetConfig> suitablePresets = AdsDrillConfigs.getRatioPresets().stream()
                .filter(p -> p.ratios().size() == finalOreTypeCount)
                .toList();

        // 만약 적합한 프리셋이 없다면, 첫 번째 선택된 광물이 100%를 차지하는 것으로 처리하고 함수를 종료합니다.
        if (suitablePresets.isEmpty() || selectedOres.isEmpty()) {
            finalComposition.put(selectedOres.getFirst(), 1.0f);
            return finalComposition;
        }

        // 적합한 프리셋들의 가중치 총합을 계산하여, 가중치 기반으로 하나의 프리셋을 무작위로 선택합니다.
        // 가중치가 높은 프리셋(예: 단일 광물 위주)이 더 자주 선택될 수 있습니다.
        double totalPresetWeight = suitablePresets.stream().mapToDouble(AdsDrillConfigs.RatioPresetConfig::weight).sum();
        double randomVal = randomSource.nextDouble() * totalPresetWeight;
        AdsDrillConfigs.RatioPresetConfig selectedPreset = null;

        for (AdsDrillConfigs.RatioPresetConfig preset : suitablePresets) {
            randomVal -= preset.weight();
            if (randomVal <= 0) {
                selectedPreset = preset;
                break;
            }
        }
        // 부동소수점 오류 등으로 인해 선택되지 않았을 경우를 대비해, 마지막 프리셋을 기본값으로 사용합니다.
        if (selectedPreset == null) {
            selectedPreset = suitablePresets.getLast();
        }

        /*
         * [4단계: 최종 비율 계산 및 노이즈 추가]
         * 선택된 프리셋의 비율을 기반으로 각 광물의 최종 비율을 계산합니다.
         * 약간의 무작위성(노이즈)을 추가하여 모든 노드가 조금씩 다른 구성을 갖도록 합니다.
         */
        float totalRatio = 0f;
        for (int i = 0; i < selectedOres.size(); i++) {
            Item ore = selectedOres.get(i);
            float baseRatio = selectedPreset.ratios().get(i).floatValue();
            // 프리셋 기본 비율에 -5% ~ +5%의 무작위 변동을 줍니다.
            float noise = (randomSource.nextFloat() - 0.5f) * 0.1f;
            float finalRatio = Math.max(0, baseRatio * (1 + noise)); // 비율이 음수가 되지 않도록 보정
            finalComposition.put(ore, finalRatio);
            totalRatio += finalRatio;
        }

        /*
         * [5단계: 불순물 추가 및 정규화]
         * 4단계까지의 비율 총합이 1.0(100%)이 아닐 수 있습니다.
         * 남은 비율이 충분하다면, 2단계에서 선택되지 않았던 다른 광물을 '불순물'로 추가합니다.
         * 마지막으로 모든 비율의 합이 정확히 1.0이 되도록 모든 값을 보정(정규화)합니다.
         */
        float remainingRatio = 1.0f - totalRatio;
        // 남은 비율이 5% 이상이고, 추가할 수 있는 다른 광물 후보가 있다면,
        if (remainingRatio > 0.05f && !candidatePool.isEmpty()) {
            // 후보군 중에서 무작위로 하나를 골라 남은 비율을 모두 할당합니다.
            Item selectedImpurity = candidatePool.get(randomSource.nextInt(candidatePool.size()));
            finalComposition.put(selectedImpurity, remainingRatio);
            totalRatio += remainingRatio;
        }

        // 모든 비율의 합계(totalRatio)로 각 비율을 나누어, 최종 합이 정확히 1.0이 되도록 정규화합니다.
        final float finalTotalRatio = totalRatio;
        if (finalTotalRatio <= 0) return Collections.emptyMap(); // 0으로 나누는 오류 방지
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
        // WorldGenLevel에서 RecipeManager와 RegistryAccess를 가져옵니다.
        RecipeManager recipeManager = Objects.requireNonNull(level.getServer()).getRecipeManager();
        RegistryAccess registryAccess = level.registryAccess();

        // 중앙 헬퍼 메서드를 호출합니다.
        return AdsDrillConfigs.getOreItemFromBlock(oreBlock, recipeManager, registryAccess);
    }

}