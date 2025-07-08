package com.yourname.mycreateaddon.data;


import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.RegistrateDataProvider;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.worldgen.ConditionalFeatureAdditionModifier;
import com.yourname.mycreateaddon.registry.MyAddonFeatures;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.yourname.mycreateaddon.MyCreateAddon.MOD_ID;
import static com.yourname.mycreateaddon.MyCreateAddon.REGISTRATE;

@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class MyAddonDatagen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event){
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        addCustomLang(REGISTRATE);

        generator.addProvider(
                true,
                new RegistrateDataProvider(REGISTRATE,MOD_ID, event)
        );// --- 아래 내용을 추가해주세요 ---
        generator.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(
                packOutput, lookupProvider,
                new RegistrySetBuilder()
                        .add(Registries.CONFIGURED_FEATURE, MyAddonFeatures::bootstrapConfiguredFeatures)
                        .add(Registries.PLACED_FEATURE, MyAddonFeatures::bootstrapPlacedFeatures)
                        //.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, MyAddonDatagen::bootstrapBiomeModifiers),
                        .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, MyAddonDatagen::bootstrapBiomeModifiers),
                Set.of(MyCreateAddon.MOD_ID)
        ));
    }

    public static void bootstrapBiomeModifiers(BootstrapContext<BiomeModifier> context) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);

        context.register(
                ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "add_ore_node_conditionally")),
                // [핵심 수정] HolderSet 관련 로직을 모두 제거하고, feature만 인자로 전달
                new ConditionalFeatureAdditionModifier(
                        placedFeatures.getOrThrow(MyAddonFeatures.ORE_NODE_PLACED_FEATURE)
                )
        );
    }
//    // --- 아래 내용을 추가해주세요 ---
//    public static void bootstrapBiomeModifiers(BootstrapContext<BiomeModifier> context) {
//        context.register(
//                ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "add_ore_node")),
//                new BiomeModifiers.AddFeaturesBiomeModifier(
//                        context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
//                        HolderSet.direct(context.lookup(Registries.PLACED_FEATURE).getOrThrow(MyAddonFeatures.ORE_NODE_PLACED_FEATURE)),
//                        GenerationStep.Decoration.UNDERGROUND_ORES
//                )
//        );
//    }


    public static void addCustomLang(CreateRegistrate registrate) {
        registrate.addRawLang("goggle.mycreateaddon.drill_core.header", "Drill Assembly");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.valid", "Operational (%s Modules)");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.invalid", "Structure Invalid");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.reason.loop_detected", "Error: Structural loop detected.");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.reason.multiple_cores", "Error: Multiple cores in one assembly.");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.reason.too_many_modules", "Error: Module limit exceeded (%s).");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.reason.duplicate_processing_module", "Error: Duplicate processing module detected.");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.speed_bonus", "Speed Bonus");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.stress_impact", "Stress Impact"); // "Added Stress"에서 변경
        registrate.addRawLang("goggle.mycreateaddon.drill_core.heat_reduction", "Heat Reduction"); // [신규]
        registrate.addRawLang("goggle.mycreateaddon.drill_core.reason.head_missing", "Warning: Drill Head missing.");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.heat_label", "Heat: ");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.efficiency_label", "Efficiency: ");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.effective_speed_label", "Effective Speed: ");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.overheated", "CRITICAL OVERHEAT!");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.cooling_down", "System shutdown, requires cooling to %s");
        registrate.addRawLang("goggle.mycreateaddon.ore_node.header", "Ore Node");
        registrate.addRawLang("goggle.mycreateaddon.ore_node.composition", "Composition:");
        registrate.addRawLang("goggle.mycreateaddon.ore_node.yield", "Yield: %s");// 단단함 등급
        registrate.addRawLang("goggle.mycreateaddon.ore_node.hardness", "Hardness");
        registrate.addRawLang("goggle.mycreateaddon.ore_node.richness", "Richness");
        registrate.addRawLang("goggle.mycreateaddon.ore_node.regeneration", "Regeneration");
        registrate.addRawLang("goggle.mycreateaddon.ore_node.hardness.brittle", "Brittle");   // 부서지기 쉬움
        registrate.addRawLang("goggle.mycreateaddon.ore_node.hardness.normal", "Normal");    // 보통
        registrate.addRawLang("goggle.mycreateaddon.ore_node.hardness.tough", "Tough");      // 질김
        registrate.addRawLang("goggle.mycreateaddon.ore_node.hardness.resilient", "Resilient"); // 매우 단단함
        // 풍부함 등급
        registrate.addRawLang("goggle.mycreateaddon.ore_node.richness.sparse", "Sparse");     // 희박함
        registrate.addRawLang("goggle.mycreateaddon.ore_node.richness.normal", "Normal");     // 보통
        registrate.addRawLang("goggle.mycreateaddon.ore_node.richness.rich", "Rich");         // 풍부함
        registrate.addRawLang("goggle.mycreateaddon.ore_node.richness.bountiful", "Bountiful"); // 풍요로움
        // 재생력 등급
        registrate.addRawLang("goggle.mycreateaddon.ore_node.regeneration.weak", "Weak");       // 약함
        registrate.addRawLang("goggle.mycreateaddon.ore_node.regeneration.strong", "Strong");   // 강함
        registrate.addRawLang("goggle.mycreateaddon.drill_core.storage_header", "Internal Storage");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.storage.items", "Items");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.storage.fluid", "Fluid");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.storage.energy", "Energy"); // [신규]
        registrate.addRawLang("goggle.mycreateaddon.drill_core.storage.empty", "Empty");
        registrate.addRawLang("mycreateaddon.priority_changed", "Processing Priority set to: %s");
        registrate.addRawLang("goggle.mycreateaddon.sneak_for_details", "(Hold Sneak for details)");
        registrate.addRawLang("mycreateaddon.upgrade_fail.already_applied", "This upgrade is already applied.");
        registrate.addRawLang("mycreateaddon.upgrade_fail.conflict", "Cannot apply this upgrade while another is active.");
        registrate.addRawLang("mycreateaddon.upgrade_fail.max_level", "Fortune is already at maximum level.");
        registrate.addRawLang("mycreateaddon.upgrade_success.silktouch", "Silk Touch applied to Drill Head.");
        registrate.addRawLang("mycreateaddon.upgrade_success.fortune", "Fortune on Drill Head upgraded to level %s.");
        registrate.addRawLang("goggle.mycreateaddon.ore_node.fluid.empty", "None");
        registrate.addRawLang("goggle.mycreateaddon.module.processing_priority", "Processing Priority");
    }
}
