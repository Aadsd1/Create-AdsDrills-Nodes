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
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

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

            // [수정] configure 메서드에 유체 정보 전달
            nodeBE.configure(scanResult.composition(), scanResult.itemToBlockMap(),
                    maxYield, hardness, richness, regeneration,
                    backgroundBlock, representativeOreBlock,
                    fluidToPlace, fluidCapacity);
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

        // 1. 선택된 광물들에 랜덤 가중치 추가
        Map<Item, Float> randomizedWeights = new HashMap<>();
        for (Map.Entry<Item, Float> entry : weightedSelection.entrySet()) {
            // 기존 가중치에 0.75 ~ 1.25 사이의 랜덤 배율을 곱함
            float randomMultiplier = 0.75f + randomSource.nextFloat() * 0.5f;
            randomizedWeights.put(entry.getKey(), entry.getValue() * randomMultiplier);
        }

        // 2. 랜덤화된 가중치를 기준으로 상위 N개 광물 선택
        Random random = new Random(randomSource.nextLong());
        int oreTypeCount = 1 + random.nextInt(Math.min(3, randomizedWeights.size()));

        List<Map.Entry<Item, Float>> potentialOres = new ArrayList<>(randomizedWeights.entrySet());
        // 가중치 역순으로 정렬
        potentialOres.sort(Map.Entry.<Item, Float>comparingByValue().reversed());

        Map<Item, Float> finalComposition = new HashMap<>();
        float totalWeight = 0;

        // 정렬된 리스트에서 상위 N개만 선택
        for (int i = 0; i < oreTypeCount; i++) {
            Map.Entry<Item, Float> entry = potentialOres.get(i);
            finalComposition.put(entry.getKey(), entry.getValue());
            totalWeight += entry.getValue();
        }

        // 3. 최종 비율 계산 (기존과 동일)
        for (Map.Entry<Item, Float> entry : finalComposition.entrySet()) {
            entry.setValue(entry.getValue() / totalWeight);
        }

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