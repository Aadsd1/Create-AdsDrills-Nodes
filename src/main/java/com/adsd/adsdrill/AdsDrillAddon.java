package com.adsd.adsdrill;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.adsd.adsdrill.client.event.AnvilTuningEvents;
import com.adsd.adsdrill.client.event.CommandEvents;
import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.registry.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(AdsDrillAddon.MOD_ID)
public class AdsDrillAddon {

    public static final String MOD_ID = "adsdrill";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID).defaultCreativeTab((ResourceKey<CreativeModeTab>) null);



    public AdsDrillAddon(IEventBus modEventBus, ModContainer modContainer) {

        REGISTRATE.registerEventListeners(modEventBus);
        AdsDrillConfigs.register(modContainer);
        AdsDrillBlocks.register();
        AdsDrillBlockEntity.register();
        AdsDrillItems.register();
        AdsDrillCreativeTabs.register(modEventBus);
        AdsDrillFeatures.register();
        AdsDrillBiomeModifiers.register(modEventBus);

        modEventBus.addListener(AdsDrillConfigs::onConfigLoad);
        modEventBus.addListener(AdsDrillConfigs::onConfigReload);


        NeoForge.EVENT_BUS.register(AnvilTuningEvents.class);

        NeoForge.EVENT_BUS.register(CommandEvents.class);
    }

    public static CreateRegistrate registrate() {
        return REGISTRATE;
    }
}