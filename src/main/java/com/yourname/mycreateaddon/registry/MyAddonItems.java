package com.yourname.mycreateaddon.registry;// ModItems.java (만약 아이템을 직접 등록한다면)
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.item.NeutralizerItem;
import com.yourname.mycreateaddon.content.item.NodeDesignatorItem;
import com.yourname.mycreateaddon.content.item.StabilizerCoreItem;
import com.yourname.mycreateaddon.content.item.UnfinishedNodeDataItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
// ... 다른 import ...


public class MyAddonItems {

    private static final CreateRegistrate REGISTRATE = MyCreateAddon.registrate();

    public static final ItemEntry<Item> CINNABAR = REGISTRATE.item("cinnabar", Item::new)
            .tag(MyAddonTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> THE_FOSSIL = REGISTRATE.item("fossil", Item::new)
            .tag(MyAddonTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> IVORY_CRYSTAL = REGISTRATE.item("ivory_crystal", Item::new)
            .tag(MyAddonTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> KOH_I_NOOR = REGISTRATE.item("koh_i_noor", Item::new)
            .tag(MyAddonTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> RAW_ROSE_GOLD_CHUNK = REGISTRATE.item("raw_rose_gold_chunk", Item::new)
            .register();
    public static final ItemEntry<Item> ROSE_GOLD = REGISTRATE.item("rose_gold", Item::new)
            .tag(MyAddonTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> SILKY_JEWEL = REGISTRATE.item("silky_jewel", Item::new)
            .tag(MyAddonTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> THUNDER_STONE = REGISTRATE.item("thunder_stone", Item::new)
            .tag(MyAddonTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> ULTRAMARINE = REGISTRATE.item("ultramarine", Item::new)
            .tag(MyAddonTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> RAW_STEEL_CHUNK = REGISTRATE.item("raw_steel_chunk", Item::new)
            .tag(MyAddonTags.CATALYSTS)
            .register();
    public static final ItemEntry<Item> STEEL_INGOT = REGISTRATE.item("steel_ingot", Item::new)
            .register();
    public static final ItemEntry<Item> XOMV = REGISTRATE.item("xomv", Item::new)
            .tag(MyAddonTags.CATALYSTS)
            .register();
    public static final ItemEntry<StabilizerCoreItem> BRASS_STABILIZER_CORE = REGISTRATE
            .item("brass_stabilizer_core", p -> new StabilizerCoreItem(p, StabilizerCoreItem.Tier.BRASS))
            .properties(p -> p.rarity(Rarity.COMMON))
            .lang("Brass Stabilizer Core")
            .register();

    public static final ItemEntry<StabilizerCoreItem> STEEL_STABILIZER_CORE = REGISTRATE
            .item("steel_stabilizer_core", p -> new StabilizerCoreItem(p, StabilizerCoreItem.Tier.STEEL))
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .lang("Steel Stabilizer Core")
            .register();

    public static final ItemEntry<StabilizerCoreItem> NETHERITE_STABILIZER_CORE = REGISTRATE
            .item("netherite_stabilizer_core", p -> new StabilizerCoreItem(p, StabilizerCoreItem.Tier.NETHERITE))
            .properties(p -> p.rarity(Rarity.RARE).fireResistant())
            .lang("Netherite Stabilizer Core")
            .register();

    public static final ItemEntry<NeutralizerItem> NEUTRALIZER = REGISTRATE.item("neutralizer", NeutralizerItem::new)
            .register();

    public static final ItemEntry<UnfinishedNodeDataItem> UNFINISHED_NODE_DATA = REGISTRATE.item("unfinished_node_data", UnfinishedNodeDataItem::new)
            .register();



    public static final ItemEntry<NodeDesignatorItem> NODE_DESIGNATOR = REGISTRATE.item("node_designator", NodeDesignatorItem::new)
            .register();

    public static void register() {
        // [3. 추가] MyAddonTags 클래스를 초기화합니다.
        MyAddonTags.init();
    }
}