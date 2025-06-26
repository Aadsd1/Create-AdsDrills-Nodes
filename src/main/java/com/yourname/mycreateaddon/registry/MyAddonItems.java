package com.yourname.mycreateaddon.registry;// ModItems.java (만약 아이템을 직접 등록한다면)
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.yourname.mycreateaddon.MyCreateAddon;
// ... 다른 import ...


public class MyAddonItems {

    private static final CreateRegistrate REGISTRATE = MyCreateAddon.registrate();

    // 예시: public static final ItemEntry<Item> MY_CUSTOM_ITEM = REGISTRATE.item("my_custom_item", Item::new).register();

    public static void register() {}
}