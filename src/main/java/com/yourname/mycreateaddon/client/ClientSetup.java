package com.yourname.mycreateaddon.client;

import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.etc.MyAddonPartialModels;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import com.yourname.mycreateaddon.content.kinetics.node.OreNodeModelLoader; // 새로 만들 파일

@EventBusSubscriber(modid = MyCreateAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    // FMLClientSetupEvent에서 PartialModel을 초기화합니다.
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(MyAddonPartialModels::init);
    }

    // 모델 등록 이벤트 핸들러는 그대로 유지합니다.
    @SubscribeEvent
    public static void onModelRegister(ModelEvent.RegisterAdditional event) {
        event.register(new ModelResourceLocation(MyAddonPartialModels.SHAFT_FOR_DRILL_LOCATION, "standalone"));
        event.register(new ModelResourceLocation(MyAddonPartialModels.SHAFT_FOR_MODULE_LOCATION, "standalone"));
        event.register(new ModelResourceLocation(MyAddonPartialModels.ROTARY_DRILL_HEAD_LOCATION,"standalone"));
    }


    @SubscribeEvent
    public static void onModelRegistry(ModelEvent.RegisterGeometryLoaders event) {
        event.register(
                ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "ore_node_loader"),
                new OreNodeModelLoader()
        );
    }

}