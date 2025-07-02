package com.yourname.mycreateaddon.data;


import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.RegistrateDataProvider;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.registry.MyAddonFeatures;
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
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
                        .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, MyAddonDatagen::bootstrapBiomeModifiers),
                Set.of(MyCreateAddon.MOD_ID)
        ));
    }

    // --- 아래 내용을 추가해주세요 ---
    public static void bootstrapBiomeModifiers(BootstrapContext<BiomeModifier> context) {
        context.register(
                ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "add_ore_node")),
                new BiomeModifiers.AddFeaturesBiomeModifier(
                        context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
                        HolderSet.direct(context.lookup(Registries.PLACED_FEATURE).getOrThrow(MyAddonFeatures.ORE_NODE_PLACED_FEATURE)),
                        GenerationStep.Decoration.UNDERGROUND_ORES
                )
        );
    }


    public static void addCustomLang(CreateRegistrate registrate) {
        registrate.addRawLang("goggle.mycreateaddon.drill_core.header", "Drill Assembly");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.valid", "Operational (%s Modules)");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.invalid", "Structure Invalid");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.reason.loop_detected", "Error: Structural loop detected.");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.reason.multiple_cores", "Error: Multiple cores in one assembly.");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.reason.too_many_modules", "Error: Module limit exceeded (%s).");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.speed_bonus", "Speed Bonus");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.stress_impact", "Added Stress");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.reason.head_missing", "Warning: Drill Head missing.");
        registrate.addRawLang("goggle.mycreateaddon.ore_node.header", "Ore Vein");
        registrate.addRawLang("goggle.mycreateaddon.ore_node.yield", "Remaining Yield: %s");
        registrate.addRawLang("goggle.mycreateaddon.ore_node.composition", "Composition:");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.heat_label", "Heat: ");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.efficiency_label", "Efficiency: ");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.effective_speed_label", "Effective Speed: ");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.overheated", "CRITICAL OVERHEAT!");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.cooling_down", "System shutdown, requires cooling to %s");
    }
}
