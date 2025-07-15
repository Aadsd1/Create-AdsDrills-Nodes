package com.yourname.mycreateaddon.content.worldgen;


import com.mojang.serialization.Codec;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlock;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlockEntity;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
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

        // [핵심 수정 1] setBlock을 하기 전에 원래 블록 상태를 저장합니다.
        BlockState originalState = level.getBlockState(origin);

        // 유효성 검사는 원래 블록을 기준으로 합니다.
        if (!originalState.isSolidRender(level, origin)) {
            return false;
        }
        Block backgroundBlock = originalState.getBlock(); // 불순물 및 배경 텍스처용

        // 이제 setBlock을 호출합니다.
        level.setBlock(origin, MyAddonBlocks.ORE_NODE.get().defaultBlockState(), 3);
        BlockEntity be = level.getBlockEntity(origin);

        if (be instanceof OreNodeBlockEntity nodeBE) {
            // [핵심 수정 2] 저장해둔 원래 블록 상태(originalState)를 스캔 메서드에 전달합니다.
            OreScanResult scanResult = scanAndGenerateOreData(context, origin, random, originalState);
            Map<Item, Float> composition = scanResult.composition();
            Map<Item, Block> itemToBlockMap = scanResult.itemToBlockMap();

            if (composition.isEmpty()) {
                composition.put(Items.RAW_IRON, 1.0f);
                itemToBlockMap.put(Items.RAW_IRON, Blocks.IRON_ORE);
            }

            // 대표 블록 찾기
            Block representativeOreBlock = findRepresentativeOreBlock(composition, itemToBlockMap);


            int maxYield = config.totalYield().sample(random);
            float hardness = 0.5f + random.nextFloat() * 1.5f;
            float richness = 0.8f + random.nextFloat() * 0.7f;

            // [핵심 수정] 재생성 기본 값을 10배 상향 조정
            // 이제 regeneration 필드는 초당 0.01 ~ 0.1개의 아이템을 재생하는 속도를 의미합니다.
            float regeneration = 0.0005f + random.nextFloat() * 0.0045f;

            // [수정] 강력한 재생 노드의 등장 확률을 15%로 높이고, 보너스를 조정
            if (random.nextFloat() < 0.15f) {
                // 기존 재생력에 1.5 ~ 3.5배의 보너스를 부여
                regeneration *= (1.5f + random.nextFloat() * 2.0f);
            }


            // --- [신규] 유체 스캔 및 할당 로직 ---
            FluidStack fluidToPlace = FluidStack.EMPTY;
            int fluidCapacity = 0;
            // 설정된 확률에 따라 유체 할당 시도
            if (random.nextFloat() < config.fluidChance()) {
                fluidToPlace = scanForBiomeFluid(context, random);
                if (!fluidToPlace.isEmpty()) {
                    fluidCapacity = config.fluidCapacity().sample(random);
                }
            }

            // [핵심 수정 3] configure 메서드에는 원래 블록을 전달합니다.
            nodeBE.configureFromFeature(scanResult.composition(), scanResult.itemToBlockMap(),
                    maxYield, hardness, richness, regeneration,
                    backgroundBlock, representativeOreBlock,
                    fluidToPlace, fluidCapacity);
            return true;
        }

        return false;
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
    private OreScanResult scanAndGenerateOreData(FeaturePlaceContext<OreNodeConfiguration> context, BlockPos pos, RandomSource randomSource, BlockState originalState) {
        WorldGenLevel level = context.level();

        Holder<Biome> biomeHolder = level.getBiome(pos);
        BiomeGenerationSettings generationSettings = biomeHolder.value().getGenerationSettings();
        HolderSet<PlacedFeature> oreFeatures = generationSettings.features().get(GenerationStep.Decoration.UNDERGROUND_ORES.ordinal());

        Map<Item, Float> weightedSelection = new HashMap<>();
        Map<Item, Block> itemToBlockMap = new HashMap<>(); // 아이템-블록 매핑 맵

        var registryAccess = context.level().registryAccess();
        var blockRegistry = registryAccess.registryOrThrow(Registries.BLOCK);

        for (Holder<PlacedFeature> placedFeatureHolder : oreFeatures) {
            Optional<PlacedFeature> placedFeatureOpt = placedFeatureHolder.unwrapKey().flatMap(level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)::getOptional);
            if (placedFeatureOpt.isEmpty()) continue;

            PlacedFeature placedFeature = placedFeatureOpt.get();
            ConfiguredFeature<?, ?> configuredFeature = placedFeature.feature().value();


            if (configuredFeature.config() instanceof OreConfiguration oreConfig) {
                for (OreConfiguration.TargetBlockState target : oreConfig.targetStates) {

                    Block oreBlock = target.state.getBlock();

                    if (oreBlock instanceof OreNodeBlock) {
                        continue;
                    }

                    Item oreItem = getOreItem(oreBlock, level);


                    if (oreItem != Items.AIR) {
                        TagKey<Block> oresTag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));

                        float weight = blockRegistry.wrapAsHolder(oreBlock).is(oresTag) ? oreConfig.size : 0.5f;

                        weightedSelection.merge(oreItem, weight, Float::sum);
                        itemToBlockMap.putIfAbsent(oreItem, oreBlock);
                    }
                }
            }
        }


// 스캔된 광물이 없으면 빈 결과 반환
        if (weightedSelection.isEmpty()) {
            return new OreScanResult(Collections.emptyMap(), Collections.emptyMap());
        }


// 2. 가중치가 높은 순서대로 '주요 광물 후보' 리스트를 정렬합니다.
        List<Item> potentialOres = weightedSelection.entrySet().stream()
                .sorted(Map.Entry.<Item, Float>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

// 3. 노드에 포함될 '주요 광물'의 개수를 1~3개 사이에서 결정합니다.
        int oreTypeCount = 1 + randomSource.nextInt(Math.min(3, potentialOres.size()));

// 4. 결정된 개수에 맞는 프리셋을 랜덤하게 선택합니다.
        final int finalOreTypeCount = oreTypeCount;
        List<RatioPreset> suitablePresets = PRESETS.stream()
                .filter(p -> p.ratios().size() == finalOreTypeCount)
                .toList();

        if (suitablePresets.isEmpty()) {
            return new OreScanResult(Collections.emptyMap(), Collections.emptyMap());
        }
        RatioPreset selectedPreset = suitablePresets.get(randomSource.nextInt(suitablePresets.size()));

// 5. 선택된 프리셋을 기반으로 '주요 광물'의 비율을 결정합니다.
        Map<Item, Float> finalComposition = new HashMap<>();
        float totalRatio = 0f;

        for (int i = 0; i < oreTypeCount; i++) {
            Item selectedOre = potentialOres.get(i);
            float baseRatio = selectedPreset.ratios().get(i);
            float noise = (randomSource.nextFloat() - 0.5f) * 0.3f; // 비율에 약간의 변동성 추가
            float finalRatio = baseRatio * (1 + noise);

            finalComposition.put(selectedOre, finalRatio);
            totalRatio += finalRatio;
        }

// [핵심 수정] '불순물' 추가 로직 변경
        float remainingRatio = 1.0f - totalRatio;
        if (remainingRatio > 0.05f) {
            // '주요 광물'로 선택되지 않은 나머지 후보들 중에서 불순물을 선택합니다.
            List<Item> impurityCandidates = new ArrayList<>(potentialOres);
            impurityCandidates.removeAll(finalComposition.keySet());

            if (!impurityCandidates.isEmpty()) {
                // 나머지 후보들 중에서 하나를 랜덤하게 골라 남은 공간을 채웁니다.
                Item selectedImpurity = impurityCandidates.get(randomSource.nextInt(impurityCandidates.size()));
                finalComposition.put(selectedImpurity, remainingRatio);
                totalRatio += remainingRatio;
            }
            // 만약 나머지 후보가 없다면(모든 광물이 주요 광물로 선택됨), 남은 공간은 비워둡니다.
        }

// 7. 모든 비율의 합이 1.0이 되도록 최종 정규화합니다.
        final float finalTotalRatio = totalRatio;
        finalComposition.replaceAll((item, ratio) -> ratio / finalTotalRatio);

        return new OreScanResult(finalComposition, itemToBlockMap);
    }
    /**
     * [업그레이드] 바이옴 특성과 피처를 스캔하여 노드에 할당할 유체를 결정하는 메서드
     * 이제 LakeFeature와 SpringFeature를 모두 탐색합니다.
     */@SuppressWarnings("deprecation")
    private FluidStack scanForBiomeFluid(FeaturePlaceContext<OreNodeConfiguration> context, RandomSource random) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        Holder<Biome> biomeHolder = level.getBiome(pos);

        Map<Fluid, Float> weightedFluids = new HashMap<>();

        // 1. 바이옴의 기본 특성에 따라 기본 유체(물, 용암) 가중치 부여 (기존과 동일)
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
                GenerationStep.Decoration.FLUID_SPRINGS // 1.20+ 에서 추가된 명시적인 샘 단계
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

                // 2-1. LakeFeature 확인 (기존 로직)
                if (config instanceof LakeFeature.Configuration lakeConfig) {
                    BlockState fluidBlockState = lakeConfig.fluid().getState(random, pos);
                    Fluid fluid = fluidBlockState.getFluidState().getType();
                    if (fluid != Fluids.EMPTY) {
                        // [핵심 수정] 물 호수일 경우 훨씬 높은 가중치를 부여
                        float weight = (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) ? 4.0f : 1.0f;
                        weightedFluids.merge(fluid, weight, Float::sum);
                    }
                }
                // [신규] 2-2. SpringFeature 확인
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

        // 3. 가중치 기반으로 최종 유체 랜덤 선택 (기존과 동일)
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