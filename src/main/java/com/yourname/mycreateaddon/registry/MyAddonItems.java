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


    public static final ItemEntry<Item> CRACKED_IRON_CHUNK = REGISTRATE.item("cracked_iron_chunk", Item::new)
            .register();
    // [신규]
    public static final ItemEntry<NeutralizerItem> NEUTRALIZER = REGISTRATE.item("neutralizer", NeutralizerItem::new)
            .register();

    public static final ItemEntry<UnfinishedNodeDataItem> UNFINISHED_NODE_DATA = REGISTRATE.item("unfinished_node_data", UnfinishedNodeDataItem::new)
            .register();



    public static final ItemEntry<NodeDesignatorItem> NODE_DESIGNATOR = REGISTRATE.item("node_designator", NodeDesignatorItem::new)
            .register();

    public static void register() {}
}