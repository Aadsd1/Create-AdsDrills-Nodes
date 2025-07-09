package com.yourname.mycreateaddon.registry;// ModItems.java (만약 아이템을 직접 등록한다면)
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.item.NeutralizerItem;
import com.yourname.mycreateaddon.content.item.NodeDesignatorItem;
import com.yourname.mycreateaddon.content.item.UnfinishedNodeDataItem;
import net.minecraft.world.item.Item;
// ... 다른 import ...


public class MyAddonItems {

    private static final CreateRegistrate REGISTRATE = MyCreateAddon.registrate();

    public static final ItemEntry<Item> CINNABAR = REGISTRATE.item("cinnabar", Item::new)
            .register();
    public static final ItemEntry<Item> THE_FOSSIL = REGISTRATE.item("fossil", Item::new)
            .register();
    public static final ItemEntry<Item> IVORY_CRYSTAL = REGISTRATE.item("ivory_crystal", Item::new)
            .register();
    public static final ItemEntry<Item> KOH_I_NOOR = REGISTRATE.item("koh_i_noor", Item::new)
            .register();
    public static final ItemEntry<Item> RAW_ROSE_GOLD_CHUNK = REGISTRATE.item("raw_rose_gold_chunk", Item::new)
            .register();
    public static final ItemEntry<Item> ROSE_GOLD = REGISTRATE.item("rose_gold", Item::new)
            .register();
    public static final ItemEntry<Item> SILKY_JEWEL = REGISTRATE.item("silky_jewel", Item::new)
            .register();
    public static final ItemEntry<Item> THUNDER_STONE = REGISTRATE.item("thunder_stone", Item::new)
            .register();
    public static final ItemEntry<Item> ULTRAMARINE = REGISTRATE.item("ultramarine", Item::new)
            .register();
    public static final ItemEntry<Item> RAW_STEEL_CHUNK = REGISTRATE.item("raw_steel_chunk", Item::new)
            .register();
    public static final ItemEntry<Item> STEEL_INGOT = REGISTRATE.item("steel_ingot", Item::new)
            .register();
    public static final ItemEntry<Item> XOMV = REGISTRATE.item("xomv", Item::new)
            .register();


    public static final ItemEntry<NeutralizerItem> NEUTRALIZER = REGISTRATE.item("neutralizer", NeutralizerItem::new)
            .register();

    public static final ItemEntry<UnfinishedNodeDataItem> UNFINISHED_NODE_DATA = REGISTRATE.item("unfinished_node_data", UnfinishedNodeDataItem::new)
            .register();



    public static final ItemEntry<NodeDesignatorItem> NODE_DESIGNATOR = REGISTRATE.item("node_designator", NodeDesignatorItem::new)
            .register();

    public static void register() {}
}