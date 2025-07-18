package com.yourname.mycreateaddon;


import com.yourname.mycreateaddon.crafting.ModuleUpgrades;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(modid = MyCreateAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 여기에 레시피 등록 메서드를 호출합니다.

            ModuleUpgrades.register();
            MyCreateAddon.LOGGER.info("Module Upgrade Recipes have been registered.");
        });
    }


}
