package com.yourname.mycreateaddon.config;


import com.yourname.mycreateaddon.MyCreateAddon;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import com.electronwill.nightconfig.core.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MyAddonConfigs {

    public static final MyAddonServerConfig SERVER;
    private static final ModConfigSpec SERVER_SPEC;

    private static final Map<ResourceLocation, DimensionGenerationProfile> dimensionProfileCache = new ConcurrentHashMap<>();

    static {
        final ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();
        SERVER = new MyAddonServerConfig(serverBuilder);
        SERVER_SPEC = serverBuilder.build();
    }

    // 설정 데이터를 저장할 record
    public record DimensionGenerationProfile(
            ResourceLocation dimensionId,
            Stats stats,
            List<OrePoolEntry> orePool
    ) {
        public record Stats(
                int minYield, int maxYield,
                double minHardness, double maxHardness,
                double minRichness, double maxRichness,
                double minRegeneration, double maxRegeneration,
                double fluidChance,
                int minFluidCapacity, int maxFluidCapacity
        ) {}
        public record OrePoolEntry(
                String itemId,
                String blockId,
                double weight_add,
                double weight_multiplier
        ) {}
    }


    public static class MyAddonServerConfig {
        public final ModConfigSpec.ConfigValue<List<? extends String>> allowedDimensions;

        // 차원 프로필 설정
        public final ModConfigSpec.ConfigValue<List<? extends Config>> dimensionProfiles;


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



        public MyAddonServerConfig(ModConfigSpec.Builder builder) {
            builder.comment("My Create Addon Server-Side Configurations").push("general");

            builder.push("worldgen");

            allowedDimensions = builder
                    .comment(
                            "A list of dimension IDs where Ore Nodes are allowed to generate.",
                            "This is a general rule. Use dimension_profiles for more specific control."
                    )
                    .defineList("allowedDimensions",
                            List.of("minecraft:overworld"),
                            () -> "", // 새 항목 추가 시 기본값
                            (obj) -> obj instanceof String);

            dimensionProfiles = builder
                    .comment(
                            "Define specific generation rules for each dimension.",
                            "Each profile can add weight (weight_add) or multiply existing biome weight (weight_multiplier).",
                            "multiplier defaults to 1.0 (no change) if omitted."
                    )
                    .defineList("dimension_profiles",
                            List.of(createDefaultOverworldProfile()),
                            Config::inMemory,
                            obj -> obj instanceof Config);

            builder.pop();

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

            builder.push("heat_system");
            heatOverloadStartThreshold = builder.comment("Heat percentage (%) to start overloading (efficiency loss).").defineInRange("heatOverloadStartThreshold", 90.0, 0.0, 100.0);
            heatBoostStartThreshold = builder.comment("Heat percentage (%) to start the optimal speed boost.").defineInRange("heatBoostStartThreshold", 40.0, 0.0, 100.0);
            heatCooldownResetThreshold = builder.comment("Heat percentage (%) required to cool down to before restarting after an overheat.").defineInRange("heatCooldownResetThreshold", 30.0, 0.0, 100.0);
            builder.pop();

            builder.push("laser_drill");
            laserDecompositionTime = builder.comment("Time in ticks for the laser's Decomposition mode to finish (20 ticks = 1 second).").defineInRange("laserDecompositionTime", 200, 20, 72000);
            laserEnergyPerDecompositionTick = builder.comment("Energy (FE) consumed per tick in Decomposition mode.").defineInRange("laserEnergyPerDecompositionTick", 500, 1, Integer.MAX_VALUE);
            laserEnergyPerMiningTick = builder.comment("Energy (FE) consumed per mining operation in Wide-Beam or Resonance mode.").defineInRange("laserEnergyPerMiningTick", 100, 1, Integer.MAX_VALUE);
            builder.pop();

            builder.push("node_crafting");
            nodeFrameRequiredProgress = builder.comment("Total progress required to craft an Artificial Node in the Node Frame.").defineInRange("nodeFrameRequiredProgress", 240000, 1000, Integer.MAX_VALUE);
            builder.pop();

            builder.pop();
        }

        // 프로필을 가져오는 헬퍼 메서드
        public Optional<DimensionGenerationProfile> getProfileForDimension(ResourceLocation dimensionId) {
            return Optional.ofNullable(dimensionProfileCache.get(dimensionId));
        }

        public static void onLoad() {
            dimensionProfileCache.clear();
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
                    List<DimensionGenerationProfile.OrePoolEntry> orePool = new ArrayList<>();
                    for (Object obj : rawOrePool) {
                        if (obj instanceof Config oreConfig) {
                            orePool.add(new DimensionGenerationProfile.OrePoolEntry(
                                    oreConfig.get("item_id"),
                                    oreConfig.get("block_id"),
                                    oreConfig.getOrElse("weight_add", 0.0), // 없으면 0
                                    oreConfig.getOrElse("weight_multiplier", 1.0) // 없으면 1.0
                            ));
                        }
                    }

                    dimensionProfileCache.put(dimId, new DimensionGenerationProfile(dimId, stats, orePool));
                } catch (Exception e) {
                    MyCreateAddon.LOGGER.warn("Failed to parse a dimension profile from config: {}", e.getMessage());
                }
            }
        }
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
  //      orePool.add(createOreEntry("minecraft:raw_copper", "minecraft:copper_ore", 25.0, 1.0));
    //    orePool.add(createOreEntry("minecraft:coal", "minecraft:coal_ore", 40.0, 1.0));
      //  orePool.add(createOreEntry("create:raw_zinc", "create:zinc_ore", 20.0, 1.0)); // --- 메카니즘 오스뮴 추가 ---
        orePool.add(createOreEntry("mekanism:raw_osmium", "mekanism:osmium_ore", 15.0, 1.0));
        profile.set("ore_pool", orePool);

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


    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "mycreateaddon-server.toml");
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