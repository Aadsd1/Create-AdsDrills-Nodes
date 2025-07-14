package com.yourname.mycreateaddon;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.AbstractRegistrate;
import com.yourname.mycreateaddon.config.MyAddonConfigs;
import com.yourname.mycreateaddon.registry.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

@Mod(MyCreateAddon.MOD_ID)
public class MyCreateAddon {

    public static final String MOD_ID = "mycreateaddon";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID).defaultCreativeTab((ResourceKey<CreativeModeTab>) null);



    public MyCreateAddon(IEventBus modEventBus, ModContainer modContainer) {

        REGISTRATE.registerEventListeners(modEventBus);
        MyAddonConfigs.register(modContainer);
        MyAddonBlocks.register();
        MyAddonBlockEntity.register();
        MyAddonItems.register();
        MyAddonCreativeTabs.register(modEventBus);
        MyAddonFeatures.register();
        MyAddonBiomeModifiers.register(modEventBus);

    }

    public static CreateRegistrate registrate() {
        return REGISTRATE;
    }
}