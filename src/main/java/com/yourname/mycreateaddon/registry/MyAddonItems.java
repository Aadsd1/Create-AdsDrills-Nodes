package com.yourname.mycreateaddon.registry;// ModItems.java (만약 아이템을 직접 등록한다면)
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.yourname.mycreateaddon.MyCreateAddon;
import net.minecraft.world.item.Item;
// ... 다른 import ...


public class MyAddonItems {

    private static final CreateRegistrate REGISTRATE = MyCreateAddon.registrate();


    public static final ItemEntry<Item> CRACKED_IRON_CHUNK = REGISTRATE.item("cracked_iron_chunk", Item::new)
            .register();

    public static void register() {}
}