package com.yourname.mycreateaddon;


import com.yourname.mycreateaddon.crafting.ModuleUpgrades;
import com.yourname.mycreateaddon.crafting.NodeRecipe; // NodeRecipe 클래스 import
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import com.yourname.mycreateaddon.registry.MyAddonCreativeTabs;
import com.yourname.mycreateaddon.registry.MyAddonItems;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

// 이 어노테이션이 클래스를 이벤트 버스에 자동으로 등록해줍니다.
@EventBusSubscriber(modid = MyCreateAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 여기에 레시피 등록 메서드를 호출합니다.
            NodeRecipe.registerRecipes();
            MyCreateAddon.LOGGER.info("Node Combination Recipes have been registered.");

            ModuleUpgrades.register();
            MyCreateAddon.LOGGER.info("Module Upgrade Recipes have been registered.");
        });
    }


}
