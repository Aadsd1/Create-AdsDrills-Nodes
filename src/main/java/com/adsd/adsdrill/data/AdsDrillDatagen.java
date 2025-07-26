package com.adsd.adsdrill.data;


import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.crafting.Quirk;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.RegistrateDataProvider;
import com.adsd.adsdrill.content.worldgen.ConditionalFeatureAdditionModifier;
import com.adsd.adsdrill.data.recipe.GeneralRecipeGen;
import com.adsd.adsdrill.data.recipe.MixRecipeGen;
import com.adsd.adsdrill.data.recipe.SequencialRecipeGen;
import com.adsd.adsdrill.registry.AdsDrillFeatures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

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
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        addCustomLang(REGISTRATE);

        generator.addProvider(
                true,
                new RegistrateDataProvider(REGISTRATE, MOD_ID, event)
        );
        generator.addProvider(event.includeServer(), new GeneralRecipeGen(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), new MixRecipeGen(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), new SequencialRecipeGen(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), new AdvancementProvider(packOutput));


        BlockTagsProvider blockTagsProvider = new BlockTagsProvider(packOutput, lookupProvider, AdsDrillAddon.MOD_ID, existingFileHelper) {
            @Override
            protected void addTags(HolderLookup.@NotNull Provider p_256380_) {}
        };
        generator.addProvider(event.includeServer(), blockTagsProvider);
        generator.addProvider(event.includeServer(), new AdsDrillTagsProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), existingFileHelper));

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

        registrate.addRawLang("goggle.adsdrill.drill_core.header", "Drill Assembly");
        registrate.addRawLang("goggle.adsdrill.drill_core.invalid", "Structure Invalid");
        registrate.addRawLang("goggle.adsdrill.drill_core.reason.loop_detected", "Error: Structural loop detected.");
        registrate.addRawLang("goggle.adsdrill.drill_core.reason.multiple_cores", "Error: Multiple cores in one assembly.");
        registrate.addRawLang("goggle.adsdrill.drill_core.reason.too_many_modules", "Error: Module limit exceeded (%s).");
        registrate.addRawLang("goggle.adsdrill.drill_core.reason.duplicate_processing_module", "Error: Duplicate processing module detected.");
        registrate.addRawLang("goggle.adsdrill.drill_core.speed_bonus", "Speed Bonus");
        registrate.addRawLang("goggle.adsdrill.drill_core.stress_impact", "Stress Impact");
        registrate.addRawLang("goggle.adsdrill.drill_core.heat_reduction", "Heat Reduction");
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
        registrate.addRawLang("goggle.adsdrill.drill_core.storage.energy", "Energy");
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
        registrate.addRawLang("goggle.adsdrill.drill_core.energy_cost", "Energy Cost: %s FE/t");
        registrate.addRawLang("tooltip.adsdrill.laser_designator.mode_change", "Right-click a Laser Head to cycle its mode.");
        registrate.addRawLang("adsdrill.laser_designator.linked", "Laser Head linked.");
        registrate.addRawLang("adsdrill.laser_designator.not_linked", "Link to a Laser Head first! (Sneak + Right-Click)");
        registrate.addRawLang("adsdrill.laser_designator.linked_to", "Linked to: %s, %s, %s");
        registrate.addRawLang("adsdrill.laser_designator.tooltip", "Links to a Laser Head to designate targets.");
        registrate.addRawLang("adsdrill.laser_designator.target_set", "Target set: %s");
        registrate.addRawLang("adsdrill.laser_designator.target_removed", "Target removed: %s");
        registrate.addRawLang("adsdrill.laser_designator.target_limit", "Target limit reached (4).");
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
        registrate.addRawLang("creativetab.adsdrill.base_tab", "AdsDrills & Nodes");
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
        registrate.addRawLang("tooltip.adsdrill.node_locator.tuning_info.combined", "Tune with an Ore or Fluid Container in an Anvil. Can be combined for compound filtering. Clear with Flint.");

        //Advancements Root-Main
        registrate.addRawLang("advancements.adsdrill.root.title", "Advanced Drilling");
        registrate.addRawLang("advancements.adsdrill.root.description", "Craft a Drill Core, the heart of a modular mining drill.");

        registrate.addRawLang("advancements.adsdrill.main.steel_core.title", "Heart of Steel");
        registrate.addRawLang("advancements.adsdrill.main.steel_core.description", "Upgrade the Brass Drill Core to Steel to attach more modules and increase its efficiency.");

        registrate.addRawLang("advancements.adsdrill.main.netherite_core.title", "The Ultimate Core");
        registrate.addRawLang("advancements.adsdrill.main.netherite_core.description", "Perform the final upgrade on your Drill Core with Netherite to unleash its maximum potential.");
        // ==================================================
        //               Advancements - Drill Heads
        // ==================================================
        registrate.addRawLang("advancements.adsdrill.heads.basic_rotary_drill.title", "Time to Dig");
        registrate.addRawLang("advancements.adsdrill.heads.basic_rotary_drill.description", "Craft the most basic Rotary Drill Head to complete your drill assembly.");

        registrate.addRawLang("advancements.adsdrill.heads.diamond_rotary_drill.title", "Deeper and Harder");
        registrate.addRawLang("advancements.adsdrill.heads.diamond_rotary_drill.description", "Strengthen your Rotary Drill Head with diamonds.");

        registrate.addRawLang("advancements.adsdrill.heads.netherite_rotary_drill.title","Nether-Infused Drill");
        registrate.addRawLang("advancements.adsdrill.heads.netherite_rotary_drill.description","Make your End tier Rotary Drill Head");

        registrate.addRawLang("advancements.adsdrill.heads.hydraulic_drill.title", "Hydraulic Breakthrough");
        registrate.addRawLang("advancements.adsdrill.heads.hydraulic_drill.description", "Craft a Hydraulic Drill Head to selectively mine specific resources using the power of water.");

        registrate.addRawLang("advancements.adsdrill.heads.laser_drill.title", "Pew Pew!");
        registrate.addRawLang("advancements.adsdrill.heads.laser_drill.description", "Craft the pinnacle of mining technology, the Laser Drill Head.");

        registrate.addRawLang("advancements.adsdrill.heads.applied_enchanting.title", "Applied Enchanting");
        registrate.addRawLang("advancements.adsdrill.heads.applied_enchanting.description", "Apply a Fortune or Silk Touch upgrade directly to a Rotary Drill Head.");

        registrate.addRawLang("advancements.adsdrill.heads.unstable_mining.title", "Unstable Mining");
        registrate.addRawLang("advancements.adsdrill.heads.unstable_mining.description", "Craft an Explosive Drill Head. It seems to react... interestingly to overheating.");
        // ==================================================
        //               Advancements - Module System
        // ==================================================
        registrate.addRawLang("advancements.adsdrill.modules.frame.title", "Sturdy Foundation");
        registrate.addRawLang("advancements.adsdrill.modules.frame.description", "Craft a Frame Module, the foundation for all other modules.");

        registrate.addRawLang("advancements.adsdrill.modules.specialized.title", "Specialized Support");
        registrate.addRawLang("advancements.adsdrill.modules.specialized.description", "Upgrade a Frame Module into a Speed or Buffer Module to enhance your drill's performance.");

        registrate.addRawLang("advancements.adsdrill.modules.processing.title", "Automated Factory");
        registrate.addRawLang("advancements.adsdrill.modules.processing.description", "Add a processing module, like a Furnace or Crusher, to handle everything from mining to smelting.");

        registrate.addRawLang("advancements.adsdrill.modules.power_management.title", "Unlimited Power!");
        registrate.addRawLang("advancements.adsdrill.modules.power_management.description", "Construct an energy module to generate, store, or receive FE for advanced drill heads.");

        registrate.addRawLang("advancements.adsdrill.modules.redstone_control.title", "Red Light, Green Light");
        registrate.addRawLang("advancements.adsdrill.modules.redstone_control.description", "Add a Redstone Brake Module to your assembly for remote control capabilities.");
        // ==================================================
        //            Advancements - Artificial Node
        // ==================================================
        registrate.addRawLang("advancements.adsdrill.artificial_node.frame.title", "Geologist? No, Creator!");
        registrate.addRawLang("advancements.adsdrill.artificial_node.frame.description", "Craft the Node Frame, the foundation for creating artificial ore nodes.");

        registrate.addRawLang("advancements.adsdrill.artificial_node.stabilizer.title", "The Core of Stability");
        registrate.addRawLang("advancements.adsdrill.artificial_node.stabilizer.description", "Craft a Stabilizer Core, which determines the properties of an artificial node.");

        registrate.addRawLang("advancements.adsdrill.artificial_node.complete.title", "Stonemason's Alchemy");
        registrate.addRawLang("advancements.adsdrill.artificial_node.complete.description", "With the power of a Node Frame and a Drill, create something from almost nothing. Obtain your first Artificial Ore Node!");

        registrate.addRawLang("advancements.adsdrill.artificial_node.deconstructionist.title", "Deconstructionist");
        registrate.addRawLang("advancements.adsdrill.artificial_node.deconstructionist.description", "Use a Laser Drill in Decomposition mode to break down a natural ore node into raw data.");
        // ==================================================
        //               Advancements - Tools
        // ==================================================
        registrate.addRawLang("advancements.adsdrill.tools.locator.title", "Nodal Navigator");
        registrate.addRawLang("advancements.adsdrill.tools.locator.description", "Craft a Node Locator to search for hidden ore nodes beneath the surface.");

        registrate.addRawLang("advancements.adsdrill.tools.advanced_locator.title", "Precision Engineering");
        registrate.addRawLang("advancements.adsdrill.tools.advanced_locator.description", "Upgrade your Node Locator for a wider range and more precise targeting capabilities.");

        registrate.addRawLang("advancements.adsdrill.tools.laser_designator.title", "Point and Shoot");
        registrate.addRawLang("advancements.adsdrill.tools.laser_designator.description", "Craft a Laser Designator to manually assign targets to your Laser Drill Head.");

        // ==================================================
        //            Advancements - Hidden Challenges
        // ==================================================
        registrate.addRawLang("advancements.adsdrill.hidden.catalyst_collector.title", "Master of Quirks");
        registrate.addRawLang("advancements.adsdrill.hidden.catalyst_collector.description", "You have collected every known catalyst capable of influencing the very nature of artificial nodes. A true alchemist!");

        registrate.addRawLang("advancements.adsdrill.hidden.fine_tuning.title", "Fine-Tuning");
        registrate.addRawLang("advancements.adsdrill.hidden.fine_tuning.description", "Tune a Netherite Node Locator in an Anvil to seek out a specific resource.");
        // --- Item Tooltips ---

        // Drill Cores
        registrate.addRawLang("tooltip.adsdrill.drill_core.description", "The heart of a modular drill assembly. Right-click with an Upgrade Kit to enhance.");
        registrate.addRawLang("tooltip.adsdrill.drill_core.stats_header", "Base Stats:");
        registrate.addRawLang("tooltip.adsdrill.drill_core.stats.max_modules", "Max Modules: %s");
        registrate.addRawLang("tooltip.adsdrill.drill_core.stats.speed_bonus", "Speed Bonus: %s");
        registrate.addRawLang("tooltip.adsdrill.drill_core.stats.base_stress", "Base Stress: %s SU");

        // Drill Heads
        registrate.addRawLang("tooltip.adsdrill.rotary_drill_head.description", "A standard drill head for mining Ore Nodes. Can be enchanted by right-clicking with specific items.");
        registrate.addRawLang("tooltip.adsdrill.rotary_drill_head.stats", "Stress: %s SU, Heat: +%s/t, Cooling: +%s/t");
        registrate.addRawLang("tooltip.adsdrill.hydraulic_drill_head.description", "Consumes water to selectively mine raw materials (ores, gems, etc.).");
        registrate.addRawLang("tooltip.adsdrill.hydraulic_drill_head.stats", "Water Consumption: %d mB/t");
        registrate.addRawLang("tooltip.adsdrill.pump_head.description", "Directly extracts fluids from the internal tank of an Ore Node.");
        registrate.addRawLang("tooltip.adsdrill.pump_head.stats", "Base Pumping Rate: %d mB/t at 64 RPM");
        registrate.addRawLang("tooltip.adsdrill.explosive_drill_head.description", "Consumes an item on overheat to fracture an Ore Node, enabling special combination recipes.");
        registrate.addRawLang("tooltip.adsdrill.explosive_drill_head.consumable", "Consumable: %s");
        registrate.addRawLang("tooltip.adsdrill.laser_drill_head.description", "An advanced, FE-powered drill head. Controlled with the Laser Designator.");
        registrate.addRawLang("tooltip.adsdrill.laser_drill_head.stats", "Energy Cost: %s FE (Mining), %s FE/t (Decomposition)");

        // Modules
        registrate.addRawLang("tooltip.adsdrill.frame_module.description", "The basic chassis for all functional modules.");
        registrate.addRawLang("tooltip.adsdrill.speed_module.description", "Increases drill speed, but also raises Stress Impact and Heat Generation.");
        registrate.addRawLang("tooltip.adsdrill.speed_module.stats", "Speed: %s%%, Stress: %s%%, Heat: %s%%");
        registrate.addRawLang("tooltip.adsdrill.reinforcement_module.description", "Reduces Stress Impact, allowing more modules to be attached.");
        registrate.addRawLang("tooltip.adsdrill.reinforcement_module.stats", "Stress: %s%%");
        registrate.addRawLang("tooltip.adsdrill.efficiency_module.description", "Improves heat efficiency, mitigating overheating and optimizing performance.");
        registrate.addRawLang("tooltip.adsdrill.efficiency_module.stats", "Speed: %s%%, Stress: %s%%, Heat: %s%%");
        registrate.addRawLang("tooltip.adsdrill.item_buffer_module.description", "Adds an internal item storage buffer of 16 slots to the drill.");
        registrate.addRawLang("tooltip.adsdrill.fluid_buffer_module.description", "Adds an internal fluid storage buffer of 16,000 mB to the drill.");
        registrate.addRawLang("tooltip.adsdrill.redstone_brake_module.description", "Halts all drill operations when receiving a redstone signal.");
        registrate.addRawLang("tooltip.adsdrill.furnace_module.description", "Automatically smelts mined items. Consumes the core's heat.");
        registrate.addRawLang("tooltip.adsdrill.blast_furnace_module.description", "Smelts mined items faster than a Furnace Module. Consumes more heat.");
        registrate.addRawLang("tooltip.adsdrill.crusher_module.description", "Automatically crushes mined items into dusts.");
        registrate.addRawLang("tooltip.adsdrill.washer_module.description", "Automatically washes mined items, consuming water from the drill's buffer.");
        registrate.addRawLang("tooltip.adsdrill.compactor_module.description", "Automatically compacts items from the internal buffer using 3x3 recipes.");
        registrate.addRawLang("tooltip.adsdrill.heatsink_module.description", "Increases the drill's base heat dissipation rate.");
        registrate.addRawLang("tooltip.adsdrill.heatsink_module.stats", "Heat Dissipation Bonus: +%s%%");
        registrate.addRawLang("tooltip.adsdrill.coolant_module.description", "Rapidly cools the drill by consuming water from the internal buffer.");
        registrate.addRawLang("tooltip.adsdrill.coolant_module.stats", "Heat Reduction: %s/t, Water Cost: %s mB/t");
        registrate.addRawLang("tooltip.adsdrill.coolant_module.after_cooling", "When receiving a Redstone signal, it activates after an overheat.");
        registrate.addRawLang("tooltip.adsdrill.filter_module.description", "Controls the item processing flow by filtering items.");
        registrate.addRawLang("tooltip.adsdrill.resonator_module.description", "Guides the Laser Drill to mine only a specific resource.");
        registrate.addRawLang("tooltip.adsdrill.energy_input_module.description", "Receives FE from external sources and supplies it to the drill.");
        registrate.addRawLang("tooltip.adsdrill.energy_buffer_module.description", "Adds an internal FE energy storage buffer to the drill.");
        registrate.addRawLang("tooltip.adsdrill.kinetic_dynamo_module.description", "Generates FE directly from the drill's rotational force.");
        registrate.addRawLang("tooltip.adsdrill.kinetic_dynamo_module.stats", "Energy Generation: 1 FE per %s RPM");
        registrate.addRawLang("tooltip.adsdrill.module_upgrade_remover.description", "Right-click on a module to revert it to a Frame Module.");

        // Node Related
        registrate.addRawLang("tooltip.adsdrill.artificial_node.description", "An artificial Ore Node crafted in the Node Frame. Can be retrieved with a Wrench.");
        registrate.addRawLang("tooltip.adsdrill.ore_node_neutralizer.description", "Use on a natural Ore Node to permanently remove it.");

        // ==================================================
        //            JEI Categories & Basic UI Text
        // ==================================================
        registrate.addRawLang("adsdrill.jei.category.node_assembly", "Node Assembly");
        registrate.addRawLang("adsdrill.jei.category.module_upgrading", "Module Upgrading");
        registrate.addRawLang("adsdrill.jei.category.drill_upgrading", "Drill Head Upgrading");
        registrate.addRawLang("adsdrill.jei.category.drill_core_upgrading", "Drill Core Upgrading");
        registrate.addRawLang("adsdrill.jei.category.node_combination", "Node Combination");
        registrate.addRawLang("adsdrill.jei.category.laser_decomposition", "Laser Decomposition");

        registrate.addRawLang("adsdrill.jei.tooltip.node_data", "Insert 1 to 9 Unfinished Node Data items. More data increases the final node's yield.");
        registrate.addRawLang("adsdrill.jei.tooltip.stabilizer_core", "A Stabilizer Core is required. The tier of the core determines the potential stats and Quirks of the final node.");
        registrate.addRawLang("adsdrill.jei.tooltip.catalyst", "Optional. Catalysts increase the chance of specific Quirks appearing.");
        registrate.addRawLang("adsdrill.jei.requires_drill", "Requires a Rotary Drill Head above, powered by a Drill Core.");
        registrate.addRawLang("adsdrill.jei.in_cracked_node", "In a Cracked Ore Node");
        registrate.addRawLang("adsdrill.jei.tooltip.minimum_ratio", "Min. Ratio: %s");
        registrate.addRawLang("adsdrill.jei.info.anvil_tuning", "The Netherite Node Locator can be tuned in an Anvil.\n\n- Combine with an Ore or Fluid Container to set a filter.\n- Combine an already tuned locator with the other type for combined filtering.\n- Combine with Flint to clear all filters.");
        registrate.addRawLang("adsdrill.jei.tooltip.chance", "%s%% Chance");

        // 드릴 헤드 강화 텍스트
        registrate.addRawLang("adsdrill.jei.fortune_1", "Fortune I");
        registrate.addRawLang("adsdrill.jei.fortune_up_to_3", "Fortune (Up to III)");
        registrate.addRawLang("adsdrill.jei.silk_touch", "Silk Touch");

        // ==================================================
        //               JEI Information Pages
        // ==================================================

        // --- Page 1: Introduction ---
        registrate.addRawLang("adsdrill.jei.info.introduction.title", "§lIntroduction to Advanced Drilling");
        registrate.addRawLang("adsdrill.jei.info.introduction.1",
                "AdsDrill adds a modular, multi-block drilling system based on the Create mod. This guide will help you build, optimize, and unleash the full potential of your drills.");
        registrate.addRawLang("adsdrill.jei.info.introduction.2",
                "The core component is the 'Drill Core'. Attach a 'Drill Head' to begin mining, and connect various 'Modules' to its sides to enhance performance or add new functionality.");
        registrate.addRawLang("adsdrill.jei.info.introduction.3",
                "§6§lEngineer's Goggles§r are an essential tool for viewing real-time information about your drill's status, ore node statistics, and more.");
        registrate.addRawLang("adsdrill.jei.info.introduction.4",
                "§8Most detailed settings can be adjusted in the `adsdrill-server.toml` config file. The values in this guide are based on the default configuration.");

        // --- Page 2: Drill Assembly ---
        registrate.addRawLang("adsdrill.jei.info.structure.title", "§lDrill Assembly Basics");
        registrate.addRawLang("adsdrill.jei.info.structure.1",
                "A valid drill structure consists of one 'Drill Core', one 'Drill Head', and multiple 'Modules'. Wearing §6Goggles§r while looking at the core allows you to instantly check the structure's validity and its current stats.");
        registrate.addRawLang("adsdrill.jei.info.structure.2",
                "§71. Power Input:§r Rotational force from Create must be supplied to the 'back' of the Drill Core (the face with the arrow).");
        registrate.addRawLang("adsdrill.jei.info.structure.3",
                "§72. Head Attachment:§r The Drill Head must be placed on the 'front' of the Drill Core (opposite the back). The head's orientation must also face away from the core.");
        registrate.addRawLang("adsdrill.jei.info.structure.4",
                "§73. Module Connection:§r Modules can be attached to the 'sides' of the Drill Core (the four faces that are not the front or back), either directly or by chaining them off of other modules.");
        registrate.addRawLang("adsdrill.jei.info.structure.5",
                "§7Common reasons for an invalid structure include:§r");
        registrate.addRawLang("adsdrill.jei.info.structure.error.loop",
                "  - §cLoop Detected:§r Modules are connected in a way that forms a closed loop.");
        registrate.addRawLang("adsdrill.jei.info.structure.error.multiple_cores",
                "  - §cMultiple Cores:§r A single drill assembly can only have one Drill Core.");
        registrate.addRawLang("adsdrill.jei.info.structure.error.limit",
                "  - §cModule Limit Exceeded:§r The core's tier determines the maximum number of modules. (Config: brassDrillMaxModules, etc.)");
        registrate.addRawLang("adsdrill.jei.info.structure.error.duplicate_processing",
                "  - §cDuplicate Processing Module:§r Only one of each type of processing module (e.g., Furnace, Crusher) can be installed.");

        // --- Page 3: Heat Management System ---
        registrate.addRawLang("adsdrill.jei.info.heat.title", "§lHeat Management System");
        registrate.addRawLang("adsdrill.jei.info.heat.1",
                "Drills generate heat during operation, which directly impacts their efficiency. You can monitor the current heat, efficiency, and overheat status in real-time with §6Goggles§r.");
        registrate.addRawLang("adsdrill.jei.info.heat.2",
                "§7Heat Generation & Cooling:§r Heat increases based on the Drill Head type and rotational speed. When idle, the drill cools down. 'Heatsink' or 'Coolant' modules can accelerate cooling.");
        registrate.addRawLang("adsdrill.jei.info.heat.3",
                "§7Efficiency Ranges (Default Config):§r");
        registrate.addRawLang("adsdrill.jei.info.heat.range.normal",
                "  - §7Normal (0 - %s%%):§r Operates at 100%% efficiency.");
        registrate.addRawLang("adsdrill.jei.info.heat.range.boost",
                "  - §6Optimal Boost (%s%% - %s%%):§r Efficiency gradually increases up to a maximum of §e%sx§r.");
        registrate.addRawLang("adsdrill.jei.info.heat.range.overload",
                "  - §cOverloading (%s%% - 99.9%%):§r Efficiency begins to drop off sharply.");
        registrate.addRawLang("adsdrill.jei.info.heat.range.overheated",
                "  - §4Overheated (100%%):§r The drill shuts down immediately. It will not restart until the heat drops below §9%s%%§r.");

        // --- Page 4: Mining Logic Explained ---
        registrate.addRawLang("adsdrill.jei.info.mining.title", "§lMining Logic Explained");
        registrate.addRawLang("adsdrill.jei.info.mining.1",
                "Mining occurs whenever the drill's 'Mining Power' overcomes a node's 'Mining Resistance'. This is called a 'Mining Cycle'.");
        registrate.addRawLang("adsdrill.jei.info.mining.step1_power",
                "§7Step 1: Calculate Base Mining Power§r");
        registrate.addRawLang("adsdrill.jei.info.mining.step1_detail",
                "  - For rotary heads, this is the drill's final speed (RPM) divided by a config value (`rotarySpeedDivisor`: %s). Laser Heads have a fixed power (`laserMiningAmount`: %d).");
        registrate.addRawLang("adsdrill.jei.info.mining.step2_effective_power",
                "§7Step 2: Calculate Effective Mining Power§r");
        registrate.addRawLang("adsdrill.jei.info.mining.step2_detail",
                "  - The node's §bHardness§r acts as a divisor. Effective Mining Power is `Base Mining Power / Hardness`. A node with 2.0 Hardness will halve your mining power.");
        registrate.addRawLang("adsdrill.jei.info.mining.step3_progress",
                "§7Step 3: Accumulate Mining Progress§r");
        registrate.addRawLang("adsdrill.jei.info.mining.step3_detail",
                "  - Each tick (1/20s), 'Effective Mining Power' is added to 'Mining Progress'. Every node has a fixed 'Mining Resistance' of 1000.");
        registrate.addRawLang("adsdrill.jei.info.mining.step4_cycle",
                "§7Step 4: Cycle Completion & Rewards§r");
        registrate.addRawLang("adsdrill.jei.info.mining.step4_detail",
                "  - When 'Mining Progress' exceeds 'Mining Resistance' (1000), one §aMining Cycle§r is completed. The node's 'Yield' is consumed by 1, and an ore item is dropped. Any leftover progress carries over to the next cycle.");

        // --- Page 5: Module System & Priority ---
        registrate.addRawLang("adsdrill.jei.info.modules.title", "§lModule System & Priority");
        registrate.addRawLang("adsdrill.jei.info.modules.1",
                "Modules alter the drill's performance or add automation. Mined items pass through a chain of 'Processing Modules' in a specific order.");
        registrate.addRawLang("adsdrill.jei.info.modules.2",
                "§7Processing Order:§r Items are processed by modules with a lower priority number first. You can view the established processing order by looking at the core with §6Goggles§r.");
        registrate.addRawLang("adsdrill.jei.info.modules.3",
                "§7Setting Priority:§r Use a Wrench on a processing module (Furnace, Crusher, Filter, etc.) to cycle its priority from 1 to 10.");
        registrate.addRawLang("adsdrill.jei.info.modules.4",
                "§7System & Compactor Modules:§r Modules like the Coolant and Kinetic Dynamo affect the core directly each tick, while the Compactor acts after all other processing, so they do not have a set priority.");

        // --- Page 6: Artificial Nodes & Quirks ---
        registrate.addRawLang("adsdrill.jei.info.quirks.title", "§lArtificial Nodes & Quirks");
        registrate.addRawLang("adsdrill.jei.info.quirks.1",
                "Artificial Nodes, crafted in a Node Frame, can possess unique 'Quirks' that grant special effects, altering how they are mined or how they interact with the environment.");
        registrate.addRawLang("adsdrill.jei.info.quirks.list_header",
                "§7Known Quirks:§r");
        for (Quirk quirk : Quirk.values()) {
            String key = "adsdrill.jei.info.quirk." + quirk.getId();
            String tierColor = quirk.getTier().getColor().toString();
            String tierName = quirk.getTier().getName();
            String quirkName = Component.translatable("quirk.adsdrill." + quirk.getId()).getString();
            String description = Component.translatable("quirk.adsdrill." + quirk.getId() + ".description").getString();
            registrate.addRawLang(key + ".base", "  - " + tierColor + "[" + tierName + "] " + quirkName + ":§7 " + description);
            registrate.addRawLang(key + ".with_chance", "  - " + tierColor + "[" + tierName + "] " + quirkName + ":§7 " + description + " (Default Chance: %s%%)");
        }

        // --- Page 7: Advanced Node Crafting ---
        registrate.addRawLang("adsdrill.jei.info.crafting.title", "§lAdvanced Node Crafting");
        registrate.addRawLang("adsdrill.jei.info.crafting.1",
                "The final properties of an Artificial Node are determined by the combination of 'Unfinished Node Data', a 'Stabilizer Core', and 'Catalysts'.");
        registrate.addRawLang("adsdrill.jei.info.crafting.2",
                "§7Ore Composition & Yield:§r");
        registrate.addRawLang("adsdrill.jei.info.crafting.sub.composition",
                "  - §bComposition:§r The final ore ratios are a weighted average based on the composition and yield of all input 'Node Data'.");
        registrate.addRawLang("adsdrill.jei.info.crafting.sub.yield",
                "  - §bYield:§r The final max yield is the sum of all input 'Node Data' yields, multiplied by a factor from the 'Stabilizer Core' (Brass: 0.8x, Steel: 1.0x, Netherite: 1.25x).");
        registrate.addRawLang("adsdrill.jei.info.crafting.3",
                "§7Base Stats (Hardness, Richness, Regeneration):§r");
        registrate.addRawLang("adsdrill.jei.info.crafting.sub.stats",
                "  - These are determined §drandomly§r within a range set by the 'Stabilizer Core' tier and are §cnot§r affected by the input data. Higher tier cores result in better average stats.");
        registrate.addRawLang("adsdrill.jei.info.crafting.4",
                "§7Fluid Content:§r");
        registrate.addRawLang("adsdrill.jei.info.crafting.sub.fluid",
                "  - The fluid with the highest total capacity among all input 'Node Data' will be chosen as the final fluid.");
        registrate.addRawLang("adsdrill.jei.info.crafting.5",
                "§7Quirks:§r");
        registrate.addRawLang("adsdrill.jei.info.crafting.sub.quirk_core",
                "  - §bStabilizer Core:§r Determines the §dnumber§r and §dtier§r (Common/Rare/Epic) of Quirks that can be generated.");
        registrate.addRawLang("adsdrill.jei.info.crafting.sub.quirk_catalyst",
                "  - §bCatalyst:§r Greatly increases the §dprobability§r of a specific, related Quirk appearing.");
        registrate.addRawLang("adsdrill.jei.info.crafting.6",
                "§lSummary:§r Use §eData§r to design your ore blend, a §eCore§r to set the overall power level, and §eCatalysts§r to target specific Quirks.");

        // --- Page 8: Natural Ore Nodes ---
        registrate.addRawLang("adsdrill.jei.info.natural_nodes.title", "§lNatural Ore Nodes");
        registrate.addRawLang("adsdrill.jei.info.natural_nodes.1",
                "Natural Ore Nodes are massive, indestructible ore deposits found deep underground. They can only be harvested by the AdsDrill system.");
        registrate.addRawLang("adsdrill.jei.info.natural_nodes.2",
                "§7Exploration:§r As these nodes do not generate exposed to air, a 'Node Locator' is required to find them.");
        registrate.addRawLang("adsdrill.jei.info.natural_nodes.3",
                "§7Composition:§r A node's ore content is influenced by the biome and dimension it spawns in. These rules can be configured in `dimension_profiles`.");
        registrate.addRawLang("adsdrill.jei.info.natural_nodes.4",
                "§7Information:§r Wearing §6Engineer's Goggles§r and looking at a node will display its detailed statistics, including yield, hardness, richness, regeneration, and fluid content.");
        registrate.addRawLang("adsdrill.jei.info.natural_nodes.5",
                "§7Utilization:§r You can either mine resources directly or use a 'Laser Drill Head' in Decomposition mode to break the node down into 'Unfinished Node Data' for crafting artificial nodes.");
        registrate.addRawLang("adsdrill.jei.info.natural_nodes.6",
                "§7Removal:§r The 'Ore Node Neutralizer' can be used to permanently destroy a natural ore node. This item will not work on artificial nodes.");

        // --- 페이지 9: 광맥 노드 능력치 심화 ---
        registrate.addRawLang("adsdrill.jei.info.stats.title", "§lUnderstanding Node Stats");
        registrate.addRawLang("adsdrill.jei.info.stats.1", "Ore Nodes have three base stats that determine their performance. You can view these base stats at any time using Engineer's Goggles.");
        registrate.addRawLang("adsdrill.jei.info.stats.hardness", "  - §bHardness (H):§r Acts as a divisor for mining power. Higher hardness means slower mining.");
        registrate.addRawLang("adsdrill.jei.info.stats.richness", "  - §bRichness (R):§r Increases the chance of getting extra drops per mining cycle and significantly boosts regeneration speed.");
        registrate.addRawLang("adsdrill.jei.info.stats.regeneration", "  - §bRegeneration (G):§r The base rate at which the node's Yield replenishes per second (Yield/s).");
        registrate.addRawLang("adsdrill.jei.info.stats.2", "§7Effective Stats & Formulas:§r");
        registrate.addRawLang("adsdrill.jei.info.stats.3", "Quirks can modify these base stats. The goggle tooltip will show the final 'effective' values in the modifier section. The formulas are as follows:");
        registrate.addRawLang("adsdrill.jei.info.stats.formula_hardness", "  - §eEffective Hardness§r = `Base H * Quirk Multipliers` (e.g., Aquifer reduces this value).");
        registrate.addRawLang("adsdrill.jei.info.stats.formula_regeneration", "  - §eEffective G (Yield/s)§r = `(Base G * 20 * Quirk Bonuses) * (0.8 + (Richness * 0.8))`");
        registrate.addRawLang("adsdrill.jei.info.stats.4", "When sneaking, the goggle tooltip shows the base `(H, R, G)` values for quick reference, without these formulas applied.");
    }
}
