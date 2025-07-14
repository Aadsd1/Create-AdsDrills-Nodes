package com.yourname.mycreateaddon.config;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class MyAddonConfigs {

    public static final MyAddonServerConfig SERVER;
    private static final ModConfigSpec SERVER_SPEC;

    static {
        final ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();
        SERVER = new MyAddonServerConfig(serverBuilder);
        SERVER_SPEC = serverBuilder.build();
    }

    @SuppressWarnings("deprecation")
    public static class MyAddonServerConfig {
        // 기존 설정
        public final ModConfigSpec.ConfigValue<List<? extends String>> allowedDimensions;

        // [!!! 신규 !!!] 드릴 코어 설정
        public final ModConfigSpec.IntValue brassDrillMaxModules;
        public final ModConfigSpec.IntValue steelDrillMaxModules;
        public final ModConfigSpec.IntValue netheriteDrillMaxModules;
        public final ModConfigSpec.DoubleValue brassDrillSpeedBonus;
        public final ModConfigSpec.DoubleValue steelDrillSpeedBonus;
        public final ModConfigSpec.DoubleValue netheriteDrillSpeedBonus;
        public final ModConfigSpec.DoubleValue brassDrillBaseStress;
        public final ModConfigSpec.DoubleValue steelDrillBaseStress;
        public final ModConfigSpec.DoubleValue netheriteDrillBaseStress;

        // [!!! 신규 !!!] 열(Heat) 시스템 설정
        public final ModConfigSpec.DoubleValue heatBoostStartThreshold;
        public final ModConfigSpec.DoubleValue heatOverloadStartThreshold;
        public final ModConfigSpec.DoubleValue heatCooldownResetThreshold;

        // [!!! 신규 !!!] 레이저 드릴 헤드 설정
        public final ModConfigSpec.IntValue laserDecompositionTime;
        public final ModConfigSpec.IntValue laserEnergyPerDecompositionTick;
        public final ModConfigSpec.IntValue laserEnergyPerMiningTick;

        // [!!! 신규 !!!] 인공 노드 제작 설정
        public final ModConfigSpec.IntValue nodeFrameRequiredProgress;


        public MyAddonServerConfig(ModConfigSpec.Builder builder) {
            builder.comment("My Create Addon Server-Side Configurations").push("general");

            // --- 월드 생성 ---
            builder.push("worldgen");
            allowedDimensions = builder
                    .comment(
                            "A list of dimension IDs where Ore Nodes are allowed to generate.",
                            "Examples: [\"minecraft:overworld\", \"minecraft:the_nether\"]"
                    )
                    .defineList("allowedDimensions",
                            List.of("minecraft:overworld"),
                            (obj) -> obj instanceof String);
            builder.pop();

            // --- 드릴 코어 밸런스 ---
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

            // --- 열 시스템 밸런스 ---
            builder.push("heat_system");
            heatBoostStartThreshold = builder.comment("Heat percentage (%) to start the optimal speed boost.").defineInRange("heatBoostStartThreshold", 40.0, 0.0, 100.0);
            heatOverloadStartThreshold = builder.comment("Heat percentage (%) to start overloading (efficiency loss).").defineInRange("heatOverloadStartThreshold", 90.0, 0.0, 100.0);
            heatCooldownResetThreshold = builder.comment("Heat percentage (%) required to cool down to before restarting after an overheat.").defineInRange("heatCooldownResetThreshold", 30.0, 0.0, 100.0);
            builder.pop();

            // --- 레이저 드릴 밸런스 ---
            builder.push("laser_drill");
            laserDecompositionTime = builder.comment("Time in ticks for the laser's Decomposition mode to finish (20 ticks = 1 second).").defineInRange("laserDecompositionTime", 200, 20, 72000);
            laserEnergyPerDecompositionTick = builder.comment("Energy (FE) consumed per tick in Decomposition mode.").defineInRange("laserEnergyPerDecompositionTick", 500, 1, Integer.MAX_VALUE);
            laserEnergyPerMiningTick = builder.comment("Energy (FE) consumed per mining operation in Wide-Beam or Resonance mode.").defineInRange("laserEnergyPerMiningTick", 100, 1, Integer.MAX_VALUE);
            builder.pop();

            // --- 인공 노드 제작 밸런스 ---
            builder.push("node_crafting");
            nodeFrameRequiredProgress = builder.comment("Total progress required to craft an Artificial Node in the Node Frame.").defineInRange("nodeFrameRequiredProgress", 240000, 1000, Integer.MAX_VALUE);
            builder.pop();

            builder.pop();
        }
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "mycreateaddon-server.toml");
    }
}