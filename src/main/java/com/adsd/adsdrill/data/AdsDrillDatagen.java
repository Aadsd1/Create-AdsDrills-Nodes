package com.adsd.adsdrill.data;


import com.adsd.adsdrill.AdsDrillAddon;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.RegistrateDataProvider;
import com.adsd.adsdrill.content.worldgen.ConditionalFeatureAdditionModifier;
import com.adsd.adsdrill.data.recipe.AnyCraftRecipeGen;
import com.adsd.adsdrill.data.recipe.MixRecipeGen;
import com.adsd.adsdrill.data.recipe.SequencialRecipeGen;
import com.adsd.adsdrill.registry.AdsDrillFeatures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.adsd.adsdrill.AdsDrillAddon.MOD_ID;
import static com.adsd.adsdrill.AdsDrillAddon.REGISTRATE;

@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class AdsDrillDatagen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        addCustomLang(REGISTRATE);

        generator.addProvider(
                true,
                new RegistrateDataProvider(REGISTRATE, MOD_ID, event)
        );
        generator.addProvider(event.includeServer(), new AnyCraftRecipeGen(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), new MixRecipeGen(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), new SequencialRecipeGen(packOutput, lookupProvider));

        generator.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(
                packOutput, lookupProvider,
                new RegistrySetBuilder()
                        .add(Registries.CONFIGURED_FEATURE, AdsDrillFeatures::bootstrapConfiguredFeatures)
                        .add(Registries.PLACED_FEATURE, AdsDrillFeatures::bootstrapPlacedFeatures)
                         .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, AdsDrillDatagen::bootstrapBiomeModifiers),
                Set.of(AdsDrillAddon.MOD_ID)
        ));
    }

    public static void bootstrapBiomeModifiers(BootstrapContext<BiomeModifier> context) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);

        context.register(
                ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(AdsDrillAddon.MOD_ID, "add_ore_node_conditionally")),
                new ConditionalFeatureAdditionModifier(
                        placedFeatures.getOrThrow(AdsDrillFeatures.ORE_NODE_PLACED_FEATURE)
                )
        );
    }


    public static void addCustomLang(CreateRegistrate registrate) {
        registrate.addRawLang("mycreateaddon.jei.info.stabilizer_cores", "Stabilizer Cores are used in the Node Frame to begin the Artificial Node crafting process. The tier of the core heavily influences the final node's stats, potential Quirks, and the required crafting speed.");

        registrate.addRawLang("adsdrill.jei.category.node_assembly", "Node Assembly");
        registrate.addRawLang("adsdrill.jei.category.module_upgrading", "Module Upgrading");
        registrate.addRawLang("adsdrill.jei.category.drill_upgrading", "Drill Head Upgrading");
        registrate.addRawLang("adsdrill.jei.category.node_combination", "Node Combination");
        registrate.addRawLang("adsdrill.jei.category.laser_decomposition", "Laser Decomposition");

        // Node Frame 툴팁 및 텍스트
        registrate.addRawLang("adsdrill.jei.tooltip.node_data", "Insert 1 to 9 Unfinished Node Data items. More data increases the final node's yield.");
        registrate.addRawLang("adsdrill.jei.tooltip.stabilizer_core", "A Stabilizer Core is required. The tier of the core determines the potential stats and Quirks of the final node.");
        registrate.addRawLang("adsdrill.jei.tooltip.catalyst", "Optional. Catalysts increase the chance of specific Quirks appearing.");
        registrate.addRawLang("adsdrill.jei.requires_drill", "Requires a Rotary Drill Head above, powered by a Drill Core.");

        // 드릴 헤드 강화 텍스트
        registrate.addRawLang("adsdrill.jei.fortune_1", "Fortune I");
        registrate.addRawLang("adsdrill.jei.fortune_up_to_3", "Fortune (Up to III)");
        registrate.addRawLang("adsdrill.jei.silk_touch", "Silk Touch");

        // 노드 조합 텍스트
        registrate.addRawLang("adsdrill.jei.in_cracked_node", "In a Cracked Ore Node");

        // 모루 튜닝 정보
        registrate.addRawLang("adsdrill.jei.info.anvil_tuning", "The Netherite Node Locator can be tuned in an Anvil.\n\n- Combine with any Ore to make the locator target that specific resource.\n- Combine with Flint to clear any existing target.");

        registrate.addRawLang("adsdrill.jei.tooltip.chance", "%s%% Chance");

        registrate.addRawLang("goggle.adsdrill.drill_core.header", "Drill Assembly");
        registrate.addRawLang("goggle.adsdrill.drill_core.invalid", "Structure Invalid");
        registrate.addRawLang("goggle.adsdrill.drill_core.reason.loop_detected", "Error: Structural loop detected.");
        registrate.addRawLang("goggle.adsdrill.drill_core.reason.multiple_cores", "Error: Multiple cores in one assembly.");
        registrate.addRawLang("goggle.adsdrill.drill_core.reason.too_many_modules", "Error: Module limit exceeded (%s).");
        registrate.addRawLang("goggle.adsdrill.drill_core.reason.duplicate_processing_module", "Error: Duplicate processing module detected.");
        registrate.addRawLang("goggle.adsdrill.drill_core.speed_bonus", "Speed Bonus");
        registrate.addRawLang("goggle.adsdrill.drill_core.stress_impact", "Stress Impact"); // "Added Stress"에서 변경
        registrate.addRawLang("goggle.adsdrill.drill_core.heat_reduction", "Heat Reduction"); // [신규]
        registrate.addRawLang("goggle.adsdrill.drill_core.reason.head_missing", "Warning: Drill Head missing.");
        registrate.addRawLang("goggle.adsdrill.drill_core.heat_label", "Heat: ");
        registrate.addRawLang("goggle.adsdrill.drill_core.efficiency_label", "Efficiency: ");
        registrate.addRawLang("goggle.adsdrill.drill_core.effective_speed_label", "Effective Speed: ");
        registrate.addRawLang("goggle.adsdrill.drill_core.overheated", "CRITICAL OVERHEAT!");
        registrate.addRawLang("goggle.adsdrill.drill_core.cooling_down", "System shutdown, requires cooling to %s");
        registrate.addRawLang("goggle.adsdrill.ore_node.header", "Ore Node");
        registrate.addRawLang("goggle.adsdrill.ore_node.composition", "Composition:");
        registrate.addRawLang("goggle.adsdrill.ore_node.yield", "Yield: %s");// 단단함 등급
        registrate.addRawLang("goggle.adsdrill.ore_node.hardness", "Hardness");
        registrate.addRawLang("goggle.adsdrill.ore_node.richness", "Richness");
        registrate.addRawLang("goggle.adsdrill.ore_node.regeneration", "Regeneration");
        registrate.addRawLang("goggle.adsdrill.ore_node.hardness.brittle", "Brittle");   // 부서지기 쉬움
        registrate.addRawLang("goggle.adsdrill.ore_node.hardness.normal", "Normal");    // 보통
        registrate.addRawLang("goggle.adsdrill.ore_node.hardness.tough", "Tough");      // 질김
        registrate.addRawLang("goggle.adsdrill.ore_node.hardness.resilient", "Resilient"); // 매우 단단함
        // 풍부함 등급
        registrate.addRawLang("goggle.adsdrill.ore_node.richness.sparse", "Sparse");     // 희박함
        registrate.addRawLang("goggle.adsdrill.ore_node.richness.normal", "Normal");     // 보통
        registrate.addRawLang("goggle.adsdrill.ore_node.richness.rich", "Rich");         // 풍부함
        registrate.addRawLang("goggle.adsdrill.ore_node.richness.bountiful", "Bountiful"); // 풍요로움
        // 재생력 등급
        registrate.addRawLang("goggle.adsdrill.ore_node.regeneration.weak", "Weak");       // 약함
        registrate.addRawLang("goggle.adsdrill.ore_node.regeneration.strong", "Strong");   // 강함
        registrate.addRawLang("goggle.adsdrill.drill_core.storage_header", "Internal Storage");
        registrate.addRawLang("goggle.adsdrill.drill_core.storage.items", "Items");
        registrate.addRawLang("goggle.adsdrill.drill_core.storage.fluid", "Fluid");
        registrate.addRawLang("goggle.adsdrill.drill_core.storage.energy", "Energy"); // [신규]
        registrate.addRawLang("goggle.adsdrill.drill_core.storage.empty", "Empty");
        registrate.addRawLang("adsdrill.priority_changed", "Processing Priority set to: %s");
        registrate.addRawLang("goggle.adsdrill.sneak_for_details", "(Hold Sneak for details)");
        registrate.addRawLang("adsdrill.upgrade_fail.already_applied", "This upgrade is already applied.");
        registrate.addRawLang("adsdrill.upgrade_fail.conflict", "Cannot apply this upgrade while another is active.");
        registrate.addRawLang("adsdrill.upgrade_fail.max_level", "Fortune is already at maximum level.");
        registrate.addRawLang("adsdrill.upgrade_success.silktouch", "Silk Touch applied to Drill Head.");
        registrate.addRawLang("adsdrill.upgrade_success.fortune", "Fortune on Drill Head upgraded to level %s.");
        registrate.addRawLang("goggle.adsdrill.ore_node.fluid.empty", "None");
        registrate.addRawLang("goggle.adsdrill.module.processing_priority", "Processing Priority");
        registrate.addRawLang("adsdrill.laser_head.mode.wide_beam", "Mode: Wide-Beam");
        registrate.addRawLang("adsdrill.laser_head.mode.resonance", "Mode: Resonance");
        registrate.addRawLang("adsdrill.laser_head.mode.decomposition", "Mode: Decomposition");
        registrate.addRawLang("goggle.adsdrill.drill_core.energy_cost", "Energy Cost: %s FE/t"); // [신규]
        registrate.addRawLang("adsdrill.node_designator.linked", "Laser Head linked.");
        registrate.addRawLang("adsdrill.node_designator.not_linked", "Link to a Laser Head first! (Sneak + Right-Click)");
        registrate.addRawLang("adsdrill.node_designator.linked_to", "Linked to: %s, %s, %s");
        registrate.addRawLang("adsdrill.node_designator.tooltip", "Links to a Laser Head to designate targets.");
        registrate.addRawLang("adsdrill.node_designator.target_set", "Target set: %s");
        registrate.addRawLang("adsdrill.node_designator.target_removed", "Target removed: %s");
        registrate.addRawLang("adsdrill.node_designator.target_limit", "Target limit reached (4).");
        registrate.addRawLang("adsdrill.resonator.set", "Resonance frequency set to: %s");
        registrate.addRawLang("adsdrill.resonator.cleared", "Resonance frequency cleared.");
        registrate.addRawLang("goggle.adsdrill.laser_head.header", "Laser Drill Head Status");
        registrate.addRawLang("goggle.adsdrill.laser_head.resonance_target", "Resonance Target: ");
        registrate.addRawLang("goggle.adsdrill.laser_head.no_resonance_target", "None");
        registrate.addRawLang("goggle.adsdrill.laser_head.designated_targets", "Designated Targets:");
        registrate.addRawLang("goggle.adsdrill.laser_head.no_targets", "No designated targets.");
        registrate.addRawLang("goggle.adsdrill.drill_core.halted_by_redstone", "System Halted by Redstone Signal");
        registrate.addRawLang("tooltip.adsdrill.stabilizer_core.description", "Used in a Node Frame to determine the properties of an artificial node.");
        registrate.addRawLang("tooltip.adsdrill.stabilizer_core.brass.line1", "§7- Low speed requirement.");
        registrate.addRawLang("tooltip.adsdrill.stabilizer_core.brass.line2", "§7- Tolerant to crafting failures.");
        registrate.addRawLang("tooltip.adsdrill.stabilizer_core.steel.line1", "§7- High speed requirement.");
        registrate.addRawLang("tooltip.adsdrill.stabilizer_core.steel.line2", "§7- Progress decays quickly on failure.");
        registrate.addRawLang("tooltip.adsdrill.stabilizer_core.netherite.line1", "§7- Extreme speed requirement.");
        registrate.addRawLang("tooltip.adsdrill.stabilizer_core.netherite.line2", "§c§l- Progress resets instantly on failure!");
        registrate.addRawLang("goggle.adsdrill.drill_core.reason.no_energy_source", "Warning: No energy source detected for Laser Head.");
        registrate.addRawLang("goggle.adsdrill.drill_core.reason.insufficient_energy", "Warning: Insufficient energy for Laser Head operation.");
        registrate.addRawLang("goggle.adsdrill.drill_core.tier.brass", "Brass Drill Core");
        registrate.addRawLang("goggle.adsdrill.drill_core.tier.steel", "Steel Drill Core");
        registrate.addRawLang("goggle.adsdrill.drill_core.tier.netherite", "Netherite Drill Core");
        registrate.addRawLang("goggle.adsdrill.drill_core.valid", "Operational (%s / %s Modules)");
        registrate.addRawLang("adsdrill.stabilizer_core.header", "Stabilizer Core:");
        registrate.addRawLang("adsdrill.node_data.header", "%s Unfinished Node Data");
        registrate.addRawLang("goggle.adsdrill.node_frame.progress", "Progress");
        registrate.addRawLang("goggle.adsdrill.node_frame.speed_requirement", "Speed Requirement:");
        registrate.addRawLang("goggle.adsdrill.node_frame.none", "None");

        // Quirk 이름
        registrate.addRawLang("quirk.adsdrill.steady_hands", "Steady Hands");
        registrate.addRawLang("quirk.adsdrill.static_charge", "Static Charge");
        registrate.addRawLang("quirk.adsdrill.overload_discharge", "Overload Discharge");
        registrate.addRawLang("quirk.adsdrill.bone_chill", "Bone Chill");
        registrate.addRawLang("quirk.adsdrill.withering_echo", "Withering Echo");
        registrate.addRawLang("quirk.adsdrill.bottled_knowledge", "Bottled Knowledge");
        registrate.addRawLang("quirk.adsdrill.aura_of_vitality", "Aura of Vitality");
        registrate.addRawLang("quirk.adsdrill.purifying_resonance", "Purifying Resonance");
        registrate.addRawLang("quirk.adsdrill.polarity_positive", "Polarity: Positive (+)");
        registrate.addRawLang("quirk.adsdrill.polarity_negative", "Polarity: Negative (-)");
        registrate.addRawLang("quirk.adsdrill.signal_amplification", "Signal Amplification");
        registrate.addRawLang("quirk.adsdrill.gemstone_facets", "Gemstone Facets");
        registrate.addRawLang("quirk.adsdrill.chaotic_output", "Chaotic Output");
        registrate.addRawLang("quirk.adsdrill.wild_magic", "Wild Magic");
        registrate.addRawLang("quirk.adsdrill.petrified_heart", "Petrified Heart");
        registrate.addRawLang("quirk.adsdrill.aquifer", "Aquifer");
        registrate.addRawLang("quirk.adsdrill.trace_minerals", "Trace Minerals");
        registrate.addRawLang("quirk.adsdrill.buried_treasure", "Buried Treasure");
        registrate.addRawLang("quirk.adsdrill.volatile_fissures", "Volatile Fissures");

        // Quirk 설명
        registrate.addRawLang("quirk.adsdrill.steady_hands.description", "§7Guarantees at least two item drop per cycle.");
        registrate.addRawLang("quirk.adsdrill.static_charge.description", "§7Has a chance to double mining output during rain or snow.");
        registrate.addRawLang("quirk.adsdrill.overload_discharge.description", "§7When the drill core overheats, may cause a lightning strike on it.");
        registrate.addRawLang("quirk.adsdrill.bone_chill.description", "§7Has a chance to spawn a Skeleton nearby when mined.");
        registrate.addRawLang("quirk.adsdrill.withering_echo.description", "§7When the drill core overheats, spawns a Wither Skeleton.");
        registrate.addRawLang("quirk.adsdrill.bottled_knowledge.description", "§7Has a chance to drop a Bottle o' Enchanting alongside ores.");
        registrate.addRawLang("quirk.adsdrill.aura_of_vitality.description", "§7Slightly increases the regeneration rate of nearby ore nodes.");
        registrate.addRawLang("quirk.adsdrill.purifying_resonance.description", "§7Increases the drop chance of the most dominant ore.");
        registrate.addRawLang("quirk.adsdrill.polarity_positive.description", "§7Increases mining yield if a Negative Polarity node is nearby.");
        registrate.addRawLang("quirk.adsdrill.polarity_negative.description", "§7Increases mining yield if a Positive Polarity node is nearby.");
        registrate.addRawLang("quirk.adsdrill.signal_amplification.description", "§7Increases mining yield by 10% while receiving a redstone signal.");
        registrate.addRawLang("quirk.adsdrill.gemstone_facets.description", "§7Doubles the effectiveness of the Fortune enchantment on this node.");
        registrate.addRawLang("quirk.adsdrill.chaotic_output.description", "§7Has a 5% chance to yield a random ore from this node's composition, ignoring normal ratios.");
        registrate.addRawLang("quirk.adsdrill.wild_magic.description", "§7Sometimes creates random, harmless visual and sound effects.");
        registrate.addRawLang("quirk.adsdrill.petrified_heart.description", "§7This node's Regeneration is boosted based on its Hardness.");
        registrate.addRawLang("quirk.adsdrill.aquifer.description", "§7Reduces node Hardness while the internal fluid level is between 60% and 70% of its capacity.");
        registrate.addRawLang("quirk.adsdrill.trace_minerals.description", "§7Has a small chance to yield Iron Nuggets as a byproduct.");
        registrate.addRawLang("quirk.adsdrill.buried_treasure.description", "§7Has a 5% chance to yield a random item from ancient structures.");
        registrate.addRawLang("quirk.adsdrill.volatile_fissures.description", "§7Causes a small explosion for every 10 yield consumed, damaging nearby entities and briefly becoming Cracked.");

        registrate.addRawLang("adsdrill.quirk.header", "Quirks:");
        registrate.addRawLang("adsdrill.quirk_candidates.header", "Quirk Candidates:");
        registrate.addRawLang("adsdrill.fluid_content.header", "Fluid Content");
        registrate.addRawLang("adsdrill.catalyst.head","Catalyst");
        registrate.addRawLang("creativetab.adsdrill.base_tab", "My Create Addon");
        // 툴팁
        registrate.addRawLang("tooltip.adsdrill.node_locator.tier", "Tier: %s");
        registrate.addRawLang("tooltip.adsdrill.node_locator.tier.brass", "Brass");
        registrate.addRawLang("tooltip.adsdrill.node_locator.tier.steel", "Steel");
        registrate.addRawLang("tooltip.adsdrill.node_locator.tier.netherite", "Netherite");
        registrate.addRawLang("tooltip.adsdrill.node_locator.radius", "Scan Radius: %s blocks");
        registrate.addRawLang("tooltip.adsdrill.node_locator.usage", "Right-click to scan for nodes.");

        // 액션바 메시지
        registrate.addRawLang("message.adsdrill.locator.found.brass", "Node detected nearby!");
        registrate.addRawLang("message.adsdrill.locator.found.steel", "Node detected! Approx. %s blocks away.");
        registrate.addRawLang("message.adsdrill.locator.found.netherite", "Node locked at: %s, %s, %s");
        registrate.addRawLang("message.adsdrill.locator.no_target_stored", "No target stored in this locator.");
        registrate.addRawLang("message.adsdrill.locator.not_found", "No nodes found within %s blocks.");
        registrate.addRawLang("tooltip.adsdrill.node_locator.targeting_result", "Targeting Resource: %s");
        registrate.addRawLang("tooltip.adsdrill.node_locator.tuning_info", "Tune with an Ore or clear with Flint in an Anvil.");
    }
}
