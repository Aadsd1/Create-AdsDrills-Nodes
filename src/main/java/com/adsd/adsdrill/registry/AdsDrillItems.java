package com.adsd.adsdrill.registry;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.content.item.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;


public class AdsDrillItems {

    private static final CreateRegistrate REGISTRATE = AdsDrillAddon.registrate();


    public static final ItemEntry<NodeLocatorItem> BRASS_NODE_LOCATOR = REGISTRATE
            .item("brass_node_locator", p -> new NodeLocatorItem(p, NodeLocatorItem.Tier.BRASS))
            .properties(p -> p.stacksTo(1))
            .lang("Brass Node Locator")
            .register();

    public static final ItemEntry<NodeLocatorItem> STEEL_NODE_LOCATOR = REGISTRATE
            .item("steel_node_locator", p -> new NodeLocatorItem(p, NodeLocatorItem.Tier.STEEL))
            .properties(p -> p.stacksTo(1))
            .lang("Steel Node Locator")
            .model((ctx, prov) -> {
                ItemModelBuilder builder = prov.withExistingParent(ctx.getName(), "item/generated")
                        .texture("layer0", prov.modLoc("item/steel_node_locator/steel_node_locator_base"));

                for (int i = 0; i < 8; i++) {
                    ModelFile needleModel = prov.getExistingFile(prov.modLoc("item/steel_node_locator_needle_" + i));

                    builder.override()
                            .predicate(prov.modLoc("angle_int"), (float) i)
                            .model(needleModel);
                }
            })
            .register();
    public static final ItemEntry<NodeLocatorItem> NETHERITE_NODE_LOCATOR = REGISTRATE
            .item("netherite_node_locator", p -> new NodeLocatorItem(p, NodeLocatorItem.Tier.NETHERITE))
            .properties(p -> p.stacksTo(1).fireResistant())
            .lang("Netherite Node Locator")
            .register();

    public static final ItemEntry<Item> MODULE_BF_UPGRADE = REGISTRATE.item("module_bfurnace_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_COMP_UPGRADE = REGISTRATE.item("module_compactor_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_COOL_UPGRADE = REGISTRATE.item("module_coolant_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_CRUSH_UPGRADE = REGISTRATE.item("module_crusher_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_E_BUFFER_UPGRADE = REGISTRATE.item("module_ebuffer_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_EFFI_UPGRADE = REGISTRATE.item("module_efficiency_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_E_GEN_UPGRADE = REGISTRATE.item("module_egenerator_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_E_INPUT_UPGRADE = REGISTRATE.item("module_einput_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_F_BUFFER_UPGRADE = REGISTRATE.item("module_fbuffer_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_FURNACE_UPGRADE = REGISTRATE.item("module_furnace_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_HEATSINK_UPGRADE = REGISTRATE.item("module_heatsink_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_I_BUFFER_UPGRADE = REGISTRATE.item("module_ibuffer_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_R_BRAKE_UPGRADE = REGISTRATE.item("module_rbrake_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_REIN_UPGRADE = REGISTRATE.item("module_reinforcement_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_RESO_UPGRADE = REGISTRATE.item("module_resonator_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_SPEED_UPGRADE = REGISTRATE.item("module_speed_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_WASH_UPGRADE = REGISTRATE.item("module_washer_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_FILTER_UPGRADE = REGISTRATE.item("module_filter_upgrade", Item::new)
            .register();
    public static final ItemEntry<Item> MODULE_UPGRADE_REMOVER=REGISTRATE.item("module_upgrade_remover",Item::new)
            .register();

    public static final ItemEntry<Item> DRILL_CORE_STEEL_UPGRADE =REGISTRATE.item("core_steel_upgrade",Item::new)
            .register();
    public static final ItemEntry<Item> DRILL_CORE_NETHERITE_UPGRADE =REGISTRATE.item("core_netherite_upgrade",Item::new)
            .register();
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_DRILL_CORE_STEEL_UPGRADE=REGISTRATE.item("incomplete_core_steel_upgrade", SequencedAssemblyItem::new)
            .register();
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_DRILL_CORE_NETHERITE_UPGRADE=REGISTRATE.item("incomplete_core_netherite_upgrade", SequencedAssemblyItem::new)
            .register();
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_DRILL_CORE=REGISTRATE.item("incomplete_drill_core", SequencedAssemblyItem::new)
            .register();
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_LASER_DRILL_HEAD=REGISTRATE.item("incomplete_laser_drill_head", SequencedAssemblyItem::new)
            .register();

    public static final ItemEntry<NodeDebuggerItem> NODE_DEBUGGER = REGISTRATE
            .item("node_debugger", NodeDebuggerItem::new)
            .properties(p -> p.rarity(Rarity.EPIC)) // 눈에 띄게 에픽 등급으로 설정
            .lang("Node Debugger")
            .register();

    public static final ItemEntry<Item> CINNABAR = REGISTRATE.item("cinnabar", Item::new)
            .tag(AdsDrillTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> THE_FOSSIL = REGISTRATE.item("fossil", Item::new)
            .tag(AdsDrillTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> IVORY_CRYSTAL = REGISTRATE.item("ivory_crystal", Item::new)
            .tag(AdsDrillTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> KOH_I_NOOR = REGISTRATE.item("koh_i_noor", Item::new)
            .tag(AdsDrillTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> RAW_ROSE_GOLD_CHUNK = REGISTRATE.item("raw_rose_gold_chunk", Item::new)
            .register();
    public static final ItemEntry<Item> ROSE_GOLD = REGISTRATE.item("rose_gold", Item::new)
            .tag(AdsDrillTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> SILKY_JEWEL = REGISTRATE.item("silky_jewel", Item::new)
            .tag(AdsDrillTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> THUNDER_STONE = REGISTRATE.item("thunder_stone", Item::new)
            .tag(AdsDrillTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> ULTRAMARINE = REGISTRATE.item("ultramarine", Item::new)
            .tag(AdsDrillTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> RAW_STEEL_CHUNK = REGISTRATE.item("raw_steel_chunk", Item::new)
            .tag(AdsDrillTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> STEEL_INGOT = REGISTRATE.item("steel_ingot", Item::new)
            .register();
    public static final ItemEntry<Item> XOMV = REGISTRATE.item("xomv", Item::new)
            .tag(AdsDrillTags.CATALYSTS)
            .register();
    public static final ItemEntry<StabilizerCoreItem> BRASS_STABILIZER_CORE = REGISTRATE
            .item("brass_stabilizer_core", p -> new StabilizerCoreItem(p, StabilizerCoreItem.Tier.BRASS))
            .properties(p -> p.rarity(Rarity.COMMON))
            .register();

    public static final ItemEntry<StabilizerCoreItem> STEEL_STABILIZER_CORE = REGISTRATE
            .item("steel_stabilizer_core", p -> new StabilizerCoreItem(p, StabilizerCoreItem.Tier.STEEL))
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .register();

    public static final ItemEntry<StabilizerCoreItem> NETHERITE_STABILIZER_CORE = REGISTRATE
            .item("netherite_stabilizer_core", p -> new StabilizerCoreItem(p, StabilizerCoreItem.Tier.NETHERITE))
            .properties(p -> p.rarity(Rarity.RARE).fireResistant())
            .register();

    public static final ItemEntry<NeutralizerItem> ORE_NODE_NEUTRALIZER = REGISTRATE.item("ore_node_neutralizer", NeutralizerItem::new)
            .register();

    public static final ItemEntry<UnfinishedNodeDataItem> UNFINISHED_NODE_DATA = REGISTRATE.item("unfinished_node_data", UnfinishedNodeDataItem::new)
            .register();


    public static final ItemEntry<LaserDesignatorItem> LASER_DESIGNATOR = REGISTRATE.item("laser_designator", LaserDesignatorItem::new)
            .register();

    public static void register() {
        AdsDrillTags.init();
    }
}