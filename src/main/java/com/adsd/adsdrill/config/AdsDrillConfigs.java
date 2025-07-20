package com.adsd.adsdrill.config;


import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.crafting.NodeRecipe;
import com.adsd.adsdrill.crafting.Quirk;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import com.electronwill.nightconfig.core.Config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class AdsDrillConfigs {

    public static final MyAddonServerConfig SERVER;
    private static final ModConfigSpec SERVER_SPEC;

    private static final Map<ResourceLocation, DimensionGenerationProfile> dimensionProfileCache = new ConcurrentHashMap<>();
    // 수동 매핑 캐시
    private static final Map<ResourceLocation, ResourceLocation> manualOreMapCache = new ConcurrentHashMap<>();

    private static final Map<Quirk, QuirkConfig> quirkConfigCache = new ConcurrentHashMap<>();

    static {
        final ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();
        SERVER = new MyAddonServerConfig(serverBuilder);
        SERVER_SPEC = serverBuilder.build();
    }

    // 특성 설정 데이터를 담을 Record
    public record QuirkConfig(
            boolean isEnabled,
            double chance,
            List<ResourceLocation> lootTables,
            List<String> blacklistedTiers,
            double minFluidPercentage,
            double maxFluidPercentage,
            double hardnessMultiplier,
            double valueMultiplier
    ) {
        // 기본값을 가진 생성자
        public QuirkConfig() {
            this(true, 0.05, List.of(BuiltInLootTables.SIMPLE_DUNGEON.location()), List.of(), 0.6, 0.7, 0.8,1.33);
        }
    }

    public record BiomeOverride(
            List<String> biomes, // 적용할 바이옴 ID 및 태그 리스트 (예: ["minecraft:desert", "#minecraft:is_jungle"])
            List<DimensionGenerationProfile.OrePoolEntry> orePool
    ) {
    }

    // 설정 데이터를 저장할 record
    public record DimensionGenerationProfile(
            ResourceLocation dimensionId,
            Stats stats,
            List<OrePoolEntry> orePool,
            List<BiomeOverride> biomeOverrides
    ) {
        public record Stats(
                int minYield, int maxYield,
                double minHardness, double maxHardness,
                double minRichness, double maxRichness,
                double minRegeneration, double maxRegeneration,
                double fluidChance,
                int minFluidCapacity, int maxFluidCapacity
        ) {
        }

        public record OrePoolEntry(
                String itemId,
                String blockId,
                double weight_add,
                double weight_multiplier
        ) {
        }
    }


    public static class MyAddonServerConfig {
        public final ModConfigSpec.ConfigValue<List<? extends String>> allowedDimensions;

        //월드 생성 관련 설정
        public final ModConfigSpec.ConfigValue<List<? extends String>> modBlacklist;
        public final ModConfigSpec.ConfigValue<List<? extends Config>> manualOreMappings;

        // 차원 프로필 설정
        public final ModConfigSpec.ConfigValue<List<? extends Config>> dimensionProfiles;

        public final ModConfigSpec.ConfigValue<List<? extends Config>> nodeCombinationRecipes;

        // ... 드릴 코어, 열 시스템 등 설정 ...
        public final ModConfigSpec.IntValue brassDrillMaxModules;
        public final ModConfigSpec.IntValue steelDrillMaxModules;
        public final ModConfigSpec.IntValue netheriteDrillMaxModules;
        public final ModConfigSpec.DoubleValue brassDrillSpeedBonus;
        public final ModConfigSpec.DoubleValue steelDrillSpeedBonus;
        public final ModConfigSpec.DoubleValue netheriteDrillSpeedBonus;
        public final ModConfigSpec.DoubleValue brassDrillBaseStress;
        public final ModConfigSpec.DoubleValue steelDrillBaseStress;
        public final ModConfigSpec.DoubleValue netheriteDrillBaseStress;
        public final ModConfigSpec.DoubleValue heatBoostStartThreshold;
        public final ModConfigSpec.DoubleValue heatOverloadStartThreshold;
        public final ModConfigSpec.DoubleValue heatCooldownResetThreshold;
        public final ModConfigSpec.IntValue laserDecompositionTime;
        public final ModConfigSpec.IntValue laserEnergyPerDecompositionTick;
        public final ModConfigSpec.IntValue laserEnergyPerMiningTick;
        public final ModConfigSpec.IntValue nodeFrameRequiredProgress;
        public final ModConfigSpec.DoubleValue heatEfficiencyBonus;
        public final ModConfigSpec.DoubleValue heatOverloadPenalty;
        public final ModConfigSpec.IntValue laserWideBeamMaxTargets;
        public final ModConfigSpec.IntValue laserRange;
        public final ModConfigSpec.IntValue brassCoreSpeedRequirement;
        public final ModConfigSpec.IntValue steelCoreSpeedRequirement;
        public final ModConfigSpec.IntValue netheriteCoreSpeedRequirement;
        public final ModConfigSpec.IntValue brassCoreFailurePenalty;
        public final ModConfigSpec.IntValue steelCoreFailurePenalty;
        public final ModConfigSpec.IntValue minOreTypesPerNode;
        public final ModConfigSpec.IntValue maxOreTypesPerNode;
        public final ModConfigSpec.IntValue laserMiningAmount;
        public final ModConfigSpec.DoubleValue rotarySpeedDivisor;
        public final ModConfigSpec.IntValue hydraulicWaterConsumption;
        public final ModConfigSpec.IntValue pumpRatePer64RPM;

        public final Map<Quirk, ModConfigSpec.BooleanValue> quirkEnabled = new EnumMap<>(Quirk.class);
        public final Map<Quirk, ModConfigSpec.DoubleValue> quirkChance = new EnumMap<>(Quirk.class);
        public final Map<Quirk, ModConfigSpec.ConfigValue<List<? extends String>>> quirkLootTables = new EnumMap<>(Quirk.class);
        public final Map<Quirk, ModConfigSpec.ConfigValue<List<? extends String>>> quirkBlacklistedTiers = new EnumMap<>(Quirk.class);
        public final Map<Quirk, ModConfigSpec.DoubleValue> quirkMinFluidPercentage = new EnumMap<>(Quirk.class);
        public final Map<Quirk, ModConfigSpec.DoubleValue> quirkMaxFluidPercentage = new EnumMap<>(Quirk.class);
        public final Map<Quirk, ModConfigSpec.DoubleValue> quirkHardnessMultiplier = new EnumMap<>(Quirk.class);
        public final Map<Quirk, ModConfigSpec.DoubleValue> quirkValueMultiplier = new EnumMap<>(Quirk.class);

        public MyAddonServerConfig(ModConfigSpec.Builder builder) {
            builder.comment("My Create Addon Server-Side Configurations").push("general");


            builder.push("drill_core");
            brassDrillMaxModules = builder.comment("Maximum modules for the Brass Drill Core.").defineInRange("brassDrillMaxModules", 8, 1, 64);
            steelDrillMaxModules = builder.comment("Maximum modules for the Steel Drill Core.").defineInRange("steelDrillMaxModules", 16, 1, 64);
            netheriteDrillMaxModules = builder.comment("Maximum modules for the Netherite Drill Core.").defineInRange("netheriteDrillMaxModules", 24, 1, 64);
            brassDrillSpeedBonus = builder.comment("Speed bonus multiplier for the Brass Drill Core.").defineInRange("brassDrillSpeedBonus", 1.0, 0.1, 10.0);
            steelDrillSpeedBonus = builder.comment("Speed bonus multiplier for the Steel Drill Core.").defineInRange("steelDrillSpeedBonus", 1.25, 0.1, 10.0);
            netheriteDrillSpeedBonus = builder.comment("Speed bonus multiplier for the Netherite Drill Core.").defineInRange("netheriteDrillSpeedBonus", 1.5, 0.1, 10.0);
            brassDrillBaseStress = builder.comment("Base stress impact (SU) of the Brass Drill Core.").defineInRange("brassDrillBaseStress", 4.0, 0.0, 1024.0);
            steelDrillBaseStress = builder.comment("Base stress impact (SU) of the Steel Drill Core.").defineInRange("steelDrillBaseStress", 6.0, 0.0, 1024.0);
            netheriteDrillBaseStress = builder.comment("Base stress impact (SU) of the Netherite Drill Core.").defineInRange("netheriteDrillBaseStress", 8.0, 0.0, 1024.0);
            builder.pop();

            // 드릴 헤드 설정 섹션
            builder.push("drill_heads");
            laserMiningAmount = builder.comment("Mining amount per operation for the Laser Drill Head.").defineInRange("laserMiningAmount", 20, 1, 256);
            rotarySpeedDivisor = builder.comment("Divisor to calculate mining amount from RPM for Rotary Drill Heads. Lower value means more output per RPM.").defineInRange("rotarySpeedDivisor", 20.0, 1.0, 512.0);
            hydraulicWaterConsumption = builder.comment("Water (mB) consumed per tick by the Hydraulic Drill Head.").defineInRange("hydraulicWaterConsumption", 50, 1, 1000);
            pumpRatePer64RPM = builder.comment("Base fluid pumping rate (mB/t) for the Pump Head at 64 RPM.").defineInRange("pumpRatePer64RPM", 250, 10, 10000);
            builder.pop();

            builder.push("heat_system");
            heatEfficiencyBonus = builder.comment("Efficiency multiplier in the Optimal Boost heat range (e.g., 2.0 for 200%).").defineInRange("heatEfficiencyBonus", 2.0, 1.0, 10.0);
            heatOverloadPenalty = builder.comment("Penalty multiplier in the Overloading heat range. Higher values mean faster efficiency loss.").defineInRange("heatOverloadPenalty", 2.0, 1.0, 100.0);
            heatOverloadStartThreshold = builder.comment("Heat percentage (%) to start overloading (efficiency loss).").defineInRange("heatOverloadStartThreshold", 90.0, 0.0, 100.0);
            heatBoostStartThreshold = builder.comment("Heat percentage (%) to start the optimal speed boost.").defineInRange("heatBoostStartThreshold", 40.0, 0.0, 100.0);
            heatCooldownResetThreshold = builder.comment("Heat percentage (%) required to cool down to before restarting after an overheat.").defineInRange("heatCooldownResetThreshold", 30.0, 0.0, 100.0);
            builder.pop();

            builder.push("laser_drill");
            laserWideBeamMaxTargets = builder.comment("Maximum number of nodes the Laser Head can target automatically in Wide-Beam mode.").defineInRange("laserWideBeamMaxTargets", 4, 1, 16);
            laserRange = builder.comment("Maximum range (in blocks) the Laser Head can automatically find targets.").defineInRange("laserRange", 16, 8, 64);
            laserDecompositionTime = builder.comment("Time in ticks for the laser's Decomposition mode to finish (20 ticks = 1 second).").defineInRange("laserDecompositionTime", 200, 20, 72000);
            laserEnergyPerDecompositionTick = builder.comment("Energy (FE) consumed per tick in Decomposition mode.").defineInRange("laserEnergyPerDecompositionTick", 500, 1, Integer.MAX_VALUE);
            laserEnergyPerMiningTick = builder.comment("Energy (FE) consumed per mining operation in Wide-Beam or Resonance mode.").defineInRange("laserEnergyPerMiningTick", 100, 1, Integer.MAX_VALUE);
            builder.pop();

            builder.push("node_crafting");
            brassCoreSpeedRequirement = builder.comment("Minimum RPM required for crafting with a Brass Stabilizer Core.").defineInRange("brassCoreSpeedRequirement", 512, 32, 4096);
            steelCoreSpeedRequirement = builder.comment("Minimum RPM required for crafting with a Steel Stabilizer Core.").defineInRange("steelCoreSpeedRequirement", 1024, 32, 4096);
            netheriteCoreSpeedRequirement = builder.comment("Minimum RPM required for crafting with a Netherite Stabilizer Core.").defineInRange("netheriteCoreSpeedRequirement", 2048, 32, 4096);
            brassCoreFailurePenalty = builder.comment("Progress lost per tick on crafting failure with a Brass Stabilizer Core.").defineInRange("brassCoreFailurePenalty", 64, 0, 1024);
            steelCoreFailurePenalty = builder.comment("Progress lost per tick on crafting failure with a Steel Stabilizer Core.").defineInRange("steelCoreFailurePenalty", 256, 0, 1024);
            nodeFrameRequiredProgress = builder.comment("Total progress required to craft an Artificial Node in the Node Frame.").defineInRange("nodeFrameRequiredProgress", 240000, 1000, Integer.MAX_VALUE);
            builder.pop();

            builder.push("worldgen");
            minOreTypesPerNode = builder.comment("Minimum number of ore types a naturally generated node can have.").defineInRange("minOreTypesPerNode", 1, 1, 5);
            maxOreTypesPerNode = builder.comment("Maximum number of ore types a naturally generated node can have.").defineInRange("maxOreTypesPerNode", 3, 1, 10);
            allowedDimensions = builder
                    .comment(
                            "A list of dimension IDs where Ore Nodes are allowed to generate.",
                            "This is a general rule. Use dimension_profiles for more specific control."
                    )
                    .defineList(
                            List.of("allowedDimensions"),
                            () -> List.of("minecraft:overworld"),
                            () -> "",
                            (obj) -> obj instanceof String // 유효성 검사
                    );

            modBlacklist = builder
                    .comment("A list of mod IDs to completely exclude from Ore Node generation.")
                    .defineList(
                            List.of("modBlacklist"),
                            () -> List.of("examplemod1"),
                            () -> "",
                            obj -> obj instanceof String // 유효성 검사
                    );
            builder.push("quirks");
            builder.comment("Control all aspects of Artificial Node Quirks.");

            for (Quirk quirk : Quirk.values()) {
                builder.push(quirk.getId());

                quirkEnabled.put(quirk, builder.comment("Enable/disable this quirk from being generated.").define("enabled", true));

                quirkBlacklistedTiers.put(quirk, builder.comment("List of Stabilizer Core tiers (BRASS, STEEL, NETHERITE) that CANNOT generate this quirk.")
                        .defineList(List.of("blacklistedForTiers"),
                                () -> (quirk == Quirk.OVERLOAD_DISCHARGE) ? List.of("STEEL") : new ArrayList<>(),
                                () -> "",
                                obj -> obj instanceof String));

                switch (quirk) {
                    case BURIED_TREASURE -> {
                        quirkChance.put(quirk, builder.comment("Chance to drop an item from a structure's loot table.").defineInRange("chance", 0.05, 0.0, 1.0));
                        quirkLootTables.put(quirk, builder.comment("List of loot table IDs to pull items from.")
                                .defineList(List.of("lootTables"),
                                        () -> List.of(
                                                "minecraft:abandoned_mineshaft", "minecraft:desert_pyramid", "minecraft:jungle_temple",
                                                "minecraft:shipwreck_treasure", "minecraft:simple_dungeon"
                                        ),
                                        () -> "",
                                        obj -> obj instanceof String && ResourceLocation.tryParse((String)obj) != null));
                    }
                    case AQUIFER -> {
                        quirkMinFluidPercentage.put(quirk, builder.comment("Minimum fluid percentage (0.0-1.0) to activate hardness reduction.").defineInRange("minFluidPercentage", 0.6, 0.0, 1.0));
                        quirkMaxFluidPercentage.put(quirk, builder.comment("Maximum fluid percentage (0.0-1.0) to activate hardness reduction.").defineInRange("maxFluidPercentage", 0.7, 0.0, 1.0));
                        quirkHardnessMultiplier.put(quirk, builder.comment("Hardness multiplier when active (e.g., 0.8 for 20% reduction).").defineInRange("hardnessMultiplier", 0.8, 0.1, 1.0));
                    }
                    case OVERLOAD_DISCHARGE, STATIC_CHARGE, BONE_CHILL, BOTTLED_KNOWLEDGE, CHAOTIC_OUTPUT, TRACE_MINERALS ->
                            quirkChance.put(quirk, builder.comment("Chance for this quirk to activate on a valid event.").defineInRange("chance", 0.1, 0.0, 1.0));
                    case POLARITY_POSITIVE, POLARITY_NEGATIVE->
                        quirkValueMultiplier.put(quirk, builder.comment("Mining amount multiplier when the polarity bonus is active.").defineInRange("multiplier", 1.33, 1.0, 10.0));
                    case PURIFYING_RESONANCE->
                        quirkChance.put(quirk, builder.comment("Chance to force drop the most dominant ore in the node.").defineInRange("chance", 0.4, 0.0, 1.0));
                    default -> {}
                }
                builder.pop();
            }
            builder.pop();

            manualOreMappings = builder
                    .comment(
                            "Manually define the item that an ore block should be associated with.",
                            "This is useful for mods that don't use standard smelting recipes (e.g., Draconic Evolution).",
                            "Each entry needs a 'block_id' and the 'item_id' it should map to."
                    )
                    .defineList(
                            List.of("manualOreMappings"),
                            () -> List.of(
                                    createManualOreMapping("draconicevolution:overworld_draconium_ore", "draconicevolution:draconium_dust"),
                                    createManualOreMapping("draconicevolution:deepslate_draconium_ore","draconicevolution:draconium_dust")
                            ),
                            Config::inMemory,
                            obj -> obj instanceof Config // 유효성 검사
                    );

            dimensionProfiles = builder
                    .comment(
                            "Define specific generation rules for each dimension.",
                            "Each profile can contain a global 'ore_pool' for the dimension,",
                            "and a 'biome_overrides' list to apply specific rules to certain biomes or biome tags.",
                            "Biome identifiers starting with '#' are treated as tags (e.g., '#minecraft:is_forest')."
                    )
                    .defineList(
                            List.of("dimension_profiles"),
                            () -> List.of(createDefaultOverworldProfile()),
                            Config::inMemory,
                            obj -> obj instanceof Config // 유효성 검사
                    );

            nodeCombinationRecipes = builder
                    .comment(
                            "Define custom recipes for generating new resources inside a Cracked Ore Node.",
                            "Each recipe is triggered by chance when a cracked node is mined with the correct resources.",
                            "'required_fluid' is optional. Use 'minecraft:empty' or omit for no fluid.",
                            "'minimum_ratios' defines the minimum percentage (0.0 to 1.0) of each required item in the node's composition."
                    )
                    .defineList(
                            List.of("nodeCombinationRecipes"),
                            () -> List.of(
                                    // Steel
                                    createDefaultNodeRecipe(
                                            List.of("minecraft:raw_iron", "minecraft:coal"),
                                            "minecraft:empty",
                                            List.of(Map.of("item", "minecraft:raw_iron", "ratio", 0.1), Map.of("item", "minecraft:coal", "ratio", 0.1)),
                                            "adsdrill:raw_steel_chunk", 0.5, 2.0
                                    ),
                                    // Thunder Stone
                                    createDefaultNodeRecipe(
                                            List.of("minecraft:raw_copper", "minecraft:redstone"),
                                            "minecraft:water",
                                            List.of(Map.of("item", "minecraft:raw_copper", "ratio", 0.1), Map.of("item", "minecraft:redstone", "ratio", 0.1)),
                                            "adsdrill:thunder_stone", 0.05, 2.0
                                    ),
                                    // The Fossil
                                    createDefaultNodeRecipe(
                                            List.of("minecraft:granite", "minecraft:coal"),
                                            "minecraft:empty",
                                            List.of(Map.of("item", "minecraft:granite", "ratio", 0.1), Map.of("item", "minecraft:coal", "ratio", 0.1)),
                                            "adsdrill:fossil", 0.05, 2.0
                                    ),
                                    // Silky Jewel
                                    createDefaultNodeRecipe(
                                            List.of("minecraft:emerald", "minecraft:raw_gold"),
                                            "minecraft:empty",
                                            List.of(Map.of("item", "minecraft:emerald", "ratio", 0.1), Map.of("item", "minecraft:raw_gold", "ratio", 0.1)),
                                            "adsdrill:silky_jewel", 0.05, 2.0
                                    ),
                                    // Ultramarine
                                    createDefaultNodeRecipe(
                                            List.of("minecraft:lapis_lazuli", "minecraft:raw_iron"),
                                            "minecraft:water",
                                            List.of(Map.of("item", "minecraft:lapis_lazuli", "ratio", 0.1), Map.of("item", "minecraft:raw_iron", "ratio", 0.1)),
                                            "adsdrill:ultramarine", 0.05, 2.0
                                    ),
                                    // Rose Gold
                                    createDefaultNodeRecipe(
                                            List.of("minecraft:raw_gold", "minecraft:raw_copper"),
                                            "minecraft:empty",
                                            List.of(Map.of("item", "minecraft:raw_gold", "ratio", 0.1), Map.of("item", "minecraft:raw_copper", "ratio", 0.1)),
                                            "adsdrill:raw_rose_gold_chunk", 0.05, 2.0
                                    ),
                                    // Ivory Crystal
                                    createDefaultNodeRecipe(
                                            List.of("minecraft:andesite", "minecraft:diamond"),
                                            "minecraft:empty",
                                            List.of(Map.of("item", "minecraft:andesite", "ratio", 0.1), Map.of("item", "minecraft:diamond", "ratio", 0.1)),
                                            "adsdrill:ivory_crystal", 0.05, 2.0
                                    ),
                                    // Cinnabar
                                    createDefaultNodeRecipe(
                                            List.of("minecraft:redstone", "minecraft:coal"),
                                            "minecraft:lava",
                                            List.of(Map.of("item", "minecraft:redstone", "ratio", 0.1), Map.of("item", "minecraft:coal", "ratio", 0.1)),
                                            "adsdrill:cinnabar", 0.05, 2.0
                                    ),
                                    // Koh-i-Noor
                                    createDefaultNodeRecipe(
                                            List.of("minecraft:diamond", "minecraft:emerald"),
                                            "minecraft:lava",
                                            List.of(Map.of("item", "minecraft:diamond", "ratio", 0.1), Map.of("item", "minecraft:emerald", "ratio", 0.1)),
                                            "adsdrill:koh_i_noor", 0.05, 2.0
                                    ),
                                    // XOMV
                                    createDefaultNodeRecipe(
                                            List.of("minecraft:redstone", "create:raw_zinc"),
                                            "minecraft:water",
                                            List.of(Map.of("item", "minecraft:redstone", "ratio", 0.1), Map.of("item", "create:raw_zinc", "ratio", 0.1)),
                                            "adsdrill:xomv", 0.05, 2.0
                                    )
                            ),
                            Config::inMemory,
                            obj -> obj instanceof Config
                    );
            builder.pop();

            builder.pop();
        }

        public Optional<ResourceLocation> getManualMappingForItem(ResourceLocation blockId) {
            return Optional.ofNullable(manualOreMapCache.get(blockId));
        }

        // 프로필을 가져오는 헬퍼 메서드
        public Optional<DimensionGenerationProfile> getProfileForDimension(ResourceLocation dimensionId) {
            return Optional.ofNullable(dimensionProfileCache.get(dimensionId));
        }

        public static void onLoad() {
            dimensionProfileCache.clear();
            manualOreMapCache.clear();


            List<? extends Config> mappings = SERVER.manualOreMappings.get();
            for (Config mappingConfig : mappings) {
                try {
                    ResourceLocation blockId = ResourceLocation.parse(mappingConfig.get("block_id"));
                    ResourceLocation itemId = ResourceLocation.parse(mappingConfig.get("item_id"));
                    manualOreMapCache.put(blockId, itemId);
                } catch (Exception e) {
                    AdsDrillAddon.LOGGER.warn("Failed to parse a manual ore mapping from config: {}", e.getMessage());
                }
            }

            // 특성 설정 캐시 로드
            quirkConfigCache.clear();
            for (Quirk quirk : Quirk.values()) {
                try {
                    // [!!! 수정됨 !!!] get()만 사용하여 값을 가져옴. Optional/getOrDefault 제거
                    boolean enabled = SERVER.quirkEnabled.get(quirk).get();

                    double chance = SERVER.quirkChance.containsKey(quirk) ? SERVER.quirkChance.get(quirk).get() : 0.0;

                    List<? extends String> rawLootTables = SERVER.quirkLootTables.containsKey(quirk) ? SERVER.quirkLootTables.get(quirk).get() : List.of();
                    List<ResourceLocation> lootTables = new ArrayList<>(rawLootTables).stream()
                            .map(ResourceLocation::tryParse)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    List<? extends String> rawTiers = SERVER.quirkBlacklistedTiers.get(quirk).get();
                    List<String> blacklistedTiers = new ArrayList<>(rawTiers);

                    double minFluid = SERVER.quirkMinFluidPercentage.containsKey(quirk) ? SERVER.quirkMinFluidPercentage.get(quirk).get() : 0.6;
                    double maxFluid = SERVER.quirkMaxFluidPercentage.containsKey(quirk) ? SERVER.quirkMaxFluidPercentage.get(quirk).get() : 0.7;
                    double hardnessMult = SERVER.quirkHardnessMultiplier.containsKey(quirk) ? SERVER.quirkHardnessMultiplier.get(quirk).get() : 0.8;

                    double valueMultiplier = SERVER.quirkValueMultiplier.containsKey(quirk) ? SERVER.quirkValueMultiplier.get(quirk).get() : 1.33;

                    // QuirkConfig 레코드와 생성자에도 valueMultiplier 필드 추가 필요
                    QuirkConfig qc = new QuirkConfig(enabled, chance, lootTables, blacklistedTiers, minFluid, maxFluid, hardnessMult, valueMultiplier);
                    quirkConfigCache.put(quirk, qc);
                } catch (Exception e) {
                    AdsDrillAddon.LOGGER.warn("Failed to parse quirk config for '{}': {}", quirk.getId(), e.getMessage());
                }
            }
            AdsDrillAddon.LOGGER.info("Loaded {} Quirk configurations.", quirkConfigCache.size());

            List<? extends Config> profiles = SERVER.dimensionProfiles.get();
            for (Config profileConfig : profiles) {
                try {
                    ResourceLocation dimId = ResourceLocation.parse(profileConfig.get("dimension_id"));
                    Config statsConfig = profileConfig.get("stats");
                    DimensionGenerationProfile.Stats stats = new DimensionGenerationProfile.Stats(
                            statsConfig.get("min_yield"), statsConfig.get("max_yield"),
                            statsConfig.get("min_hardness"), statsConfig.get("max_hardness"),
                            statsConfig.get("min_richness"), statsConfig.get("max_richness"),
                            statsConfig.get("min_regeneration"), statsConfig.get("max_regeneration"),
                            statsConfig.get("fluid_chance"),
                            statsConfig.get("min_fluid_capacity"), statsConfig.get("max_fluid_capacity")
                    );

                    List<?> rawOrePool = profileConfig.getOrElse("ore_pool", List.of());
                    List<DimensionGenerationProfile.OrePoolEntry> globalOrePool = new ArrayList<>();
                    for (Object obj : rawOrePool) {
                        if (obj instanceof Config oreConfig) {
                            globalOrePool.add(new DimensionGenerationProfile.OrePoolEntry(
                                    oreConfig.get("item_id"),
                                    oreConfig.get("block_id"),
                                    oreConfig.getOrElse("weight_add", 0.0), // 없으면 0
                                    oreConfig.getOrElse("weight_multiplier", 1.0) // 없으면 1.0
                            ));
                        }
                    }

                    List<BiomeOverride> biomeOverrides = new ArrayList<>();
                    List<? extends Config> overrideConfigs = profileConfig.getOrElse("biome_overrides", List.of());
                    for (Config overrideConfig : overrideConfigs) {
                        List<String> biomes = overrideConfig.getOrElse("biomes", List.of());
                        List<DimensionGenerationProfile.OrePoolEntry> biomeOrePool = parseOrePool(overrideConfig.getOrElse("ore_pool", List.of()));
                        if (!biomes.isEmpty() && !biomeOrePool.isEmpty()) {
                            biomeOverrides.add(new BiomeOverride(biomes, biomeOrePool));
                        }
                    }


                    dimensionProfileCache.put(dimId, new DimensionGenerationProfile(dimId, stats, globalOrePool, biomeOverrides));
                } catch (Exception e) {
                    AdsDrillAddon.LOGGER.warn("Failed to parse a dimension profile from config: {}", e.getMessage());
                }
            }

            NodeRecipe.RECIPES.clear(); // 기존 레시피 초기화
            List<? extends Config> recipeConfigs = SERVER.nodeCombinationRecipes.get();

            for (Config recipeConfig : recipeConfigs) {
                try {
                    // 1. 필수 아이템 파싱
                    List<String> requiredItemIds = recipeConfig.get("required_items");
                    List<Item> requiredItems = new ArrayList<>();
                    for (String id : requiredItemIds) {
                        BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id))
                                .ifPresent(requiredItems::add);
                    }
                    if (requiredItems.size() != requiredItemIds.size()) {
                        AdsDrillAddon.LOGGER.warn("Skipping NodeRecipe due to missing required items: {}", requiredItemIds);
                        continue;
                    }

                    // 2. 필수 유체 파싱 (선택적)
                    String fluidId = recipeConfig.getOrElse("required_fluid", "minecraft:empty");
                    Fluid requiredFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidId));
                    if (requiredFluid == Fluids.EMPTY) {
                        requiredFluid = null;
                    }

                    // 3. 최소 비율 파싱
                    List<? extends Config> ratioConfigs = recipeConfig.get("minimum_ratios");
                    Map<Item, Float> minimumRatios = new HashMap<>();
                    boolean ratiosValid = true;
                    for (Config ratioConfig : ratioConfigs) {
                        Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(ratioConfig.get("item")));
                        if (itemOpt.isPresent()) {
                            minimumRatios.put(itemOpt.get(), ((Number) ratioConfig.get("ratio")).floatValue());
                        } else {
                            ratiosValid = false;
                            break;
                        }
                    }
                    if (!ratiosValid) {
                        AdsDrillAddon.LOGGER.warn("Skipping NodeRecipe due to missing ratio item.");
                        continue;
                    }

                    // 4. 결과물 및 기타 값 파싱
                    Optional<Item> outputItemOpt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(recipeConfig.get("output")));
                    if (outputItemOpt.isEmpty()) {
                        AdsDrillAddon.LOGGER.warn("Skipping NodeRecipe due to missing output item: {}", Optional.ofNullable(recipeConfig.get("output")));
                        continue;
                    }
                    ItemStack output = new ItemStack(outputItemOpt.get());
                    float chance = ((Number) recipeConfig.get("chance")).floatValue();
                    float consumptionMultiplier = ((Number) recipeConfig.get("consumption_multiplier")).floatValue();

                    // 5. 최종 레시피 객체 생성 및 추가
                    NodeRecipe.RECIPES.add(new NodeRecipe(
                            requiredItems,
                            requiredFluid,
                            minimumRatios,
                            output,
                            chance,
                            consumptionMultiplier
                    ));

                } catch (Exception e) {
                    AdsDrillAddon.LOGGER.warn("Failed to parse a NodeRecipe from config: {}", e.getMessage());
                }
            }
            AdsDrillAddon.LOGGER.info("Loaded {} Node Combination Recipes from config.", NodeRecipe.RECIPES.size());
        }
    }


    // 외부에서 특성 설정에 쉽게 접근할 수 있는 public 메서드
    public static QuirkConfig getQuirkConfig(Quirk quirk) {
        return quirkConfigCache.getOrDefault(quirk, new QuirkConfig()); // 설정 로드 실패 시 기본값 반환
    }


    // 수동 매핑 항목을 쉽게 만드는 헬퍼 메서드
    private static Config createManualOreMapping(String blockId, String itemId) {
        Config mapping = Config.inMemory();
        mapping.set("block_id", blockId);
        mapping.set("item_id", itemId);
        return mapping;
    }

    private static List<DimensionGenerationProfile.OrePoolEntry> parseOrePool(List<?> rawList) {
        List<DimensionGenerationProfile.OrePoolEntry> orePool = new ArrayList<>();
        for (Object obj : rawList) {
            if (obj instanceof Config oreConfig) {
                orePool.add(new DimensionGenerationProfile.OrePoolEntry(
                        oreConfig.get("item_id"),
                        oreConfig.get("block_id"),
                        oreConfig.getOrElse("weight_add", 0.0),
                        oreConfig.getOrElse("weight_multiplier", 1.0)
                ));
            }
        }
        return orePool;
    }

    private static Config createDefaultOverworldProfile() {
        Config profile = Config.inMemory();
        profile.set("dimension_id", "minecraft:overworld");

        Config stats = Config.inMemory();
        stats.set("min_yield", 1000);
        stats.set("max_yield", 3000);
        stats.set("min_hardness", 0.5);
        stats.set("max_hardness", 1.5);
        stats.set("min_richness", 0.8);
        stats.set("max_richness", 1.5);
        stats.set("min_regeneration", 0.0005);
        stats.set("max_regeneration", 0.0045);
        stats.set("fluid_chance", 0.4);
        stats.set("min_fluid_capacity", 5000);
        stats.set("max_fluid_capacity", 20000);
        profile.set("stats", stats);

        // 기본 광물 목록 설정 (오스뮴 포함)
        List<Config> orePool = new ArrayList<>();
        orePool.add(createOreEntry("minecraft:diamond", "minecraft:diamond_ore", 0.0, 0.5));
        orePool.add(createOreEntry("draconicevolution:draconium_dust","draconicevolution:overworld_draconium_ore",0.0,0.01));
        orePool.add(createOreEntry("draconicevolution:draconium_dust","draconicevolution:deepslate_draconium_ore",0.0,0.01));
        orePool.add(createOreEntry("mekanism:raw_osmium", "mekanism:osmium_ore", 15.0, 1.0));
        profile.set("ore_pool", orePool);
        List<Config> biomeOverrides = new ArrayList<>();

        Config desertOverride = Config.inMemory();
        desertOverride.set("biomes", List.of("minecraft:desert"));
        desertOverride.set("ore_pool", List.of(
                createOreEntry("minecraft:coal", "minecraft:coal_ore", 0.0, 1.0),
                createOreEntry("minecraft:lapis_lazuli", "minecraft:lapis_ore", 0.0, 1.0)
        ));
        biomeOverrides.add(desertOverride);

        Config jungleOverride = Config.inMemory();
        jungleOverride.set("biomes", List.of("#minecraft:is_jungle")); // '#'은 태그를 의미
        jungleOverride.set("ore_pool", List.of(
                createOreEntry("minecraft:raw_copper", "minecraft:copper_ore", 0.0, 1.0)
        ));
        biomeOverrides.add(jungleOverride);

        profile.set("biome_overrides", biomeOverrides);

        return profile;
    }
    // 광물 풀 항목을 쉽게 만드는 헬퍼 메서드
    private static Config createOreEntry(String itemId, String blockId, double add, double multiplier) {
        Config ore = Config.inMemory();
        ore.set("item_id", itemId);
        ore.set("block_id", blockId);
        ore.set("weight_add", add);
        ore.set("weight_multiplier", multiplier);
        return ore;
    }
    private static Config createDefaultNodeRecipe(List<String> requiredItems, String requiredFluid, List<Map<String, Object>> minimumRatios, String output, double chance, double consumptionMultiplier) {
        Config recipe = Config.inMemory();
        recipe.set("required_items", requiredItems);
        recipe.set("required_fluid", requiredFluid);

        List<Config> ratioConfigs = new ArrayList<>();
        for (Map<String, Object> ratioMap : minimumRatios) {
            Config ratioConfig = Config.inMemory();
            ratioConfig.set("item", ratioMap.get("item"));
            ratioConfig.set("ratio", ratioMap.get("ratio"));
            ratioConfigs.add(ratioConfig);
        }
        recipe.set("minimum_ratios", ratioConfigs);

        recipe.set("output", output);
        recipe.set("chance", chance);
        recipe.set("consumption_multiplier", consumptionMultiplier);
        return recipe;
    }


    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "adsdrill-server.toml");
    }

    public static void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            MyAddonServerConfig.onLoad();
        }
    }

    public static void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            MyAddonServerConfig.onLoad();
        }
    }
}