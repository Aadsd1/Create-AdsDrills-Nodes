package com.yourname.mycreateaddon;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import com.yourname.mycreateaddon.registry.MyAddonItems;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MyCreateAddon.MOD_ID)
public class MyCreateAddon {

    public static final String MOD_ID = "mycreateaddon";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    public MyCreateAddon(IEventBus modEventBus) {
        REGISTRATE.registerEventListeners(modEventBus);

        MyAddonBlocks.register();
        MyAddonItems.register();
        MyAddonBlockEntity.register();
    }


    public static CreateRegistrate registrate() {
        return REGISTRATE;
    }
}