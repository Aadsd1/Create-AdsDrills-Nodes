package com.yourname.mycreateaddon;


import com.yourname.mycreateaddon.crafting.ModuleUpgrades;
import com.yourname.mycreateaddon.crafting.NodeRecipe; // NodeRecipe 클래스 import
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

// 이 어노테이션이 클래스를 이벤트 버스에 자동으로 등록해줍니다.
@EventBusSubscriber(modid = MyCreateAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        // FMLCommonSetupEvent가 발생했을 때 이 메서드가 호출됩니다.
        // 이 이벤트는 여러 스레드에서 동시에 실행될 수 있으므로,
        // 리스트 수정과 같이 스레드에 안전하지 않은 작업은 enqueueWork로 감싸는 것이 좋습니다.
        event.enqueueWork(() -> {
            // 여기에 레시피 등록 메서드를 호출합니다.
            NodeRecipe.registerRecipes();
            MyCreateAddon.LOGGER.info("Node Combination Recipes have been registered.");

            ModuleUpgrades.register();
            MyCreateAddon.LOGGER.info("Module Upgrade Recipes have been registered.");
        });
    }
}