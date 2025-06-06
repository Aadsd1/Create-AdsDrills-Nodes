package com.yourname.mycreateaddon.client; // 예시 패키지

import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.DrillCoreRenderer;
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent; // FMLClientSetupEvent

// 클래스 레벨에 어노테이션 추가

@EventBusSubscriber(modid = MyCreateAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    // FMLClientSetupEvent: 일반적인 클라이언트 초기화
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MyCreateAddon.LOGGER.info("ClientSetup: FMLClientSetupEvent for " + MyCreateAddon.MOD_ID);
        event.enqueueWork(() -> {
            // 예: RenderType 설정 (필요하다면)
            // ItemBlockRenderTypes.setRenderLayer(ModBlocks.FRAME_MODULE.get(), RenderType.cutoutMipped());
            // AllPartialModels.init(); // 만약 PartialModel 초기화 로직이 있다면
        });
    }

    // EntityRenderersEvent.RegisterRenderers: 렌더러 등록
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        MyCreateAddon.LOGGER.info("ClientSetup: Registering Block Entity Renderers for " + MyCreateAddon.MOD_ID);
        event.registerBlockEntityRenderer(MyAddonBlockEntity.DRILL_CORE.get(), DrillCoreRenderer::new);
        // 다른 BE 렌더러 등록
    }

    // ModelEvent.RegisterAdditional: PartialModel 등 추가 모델 등록 (필요시)
    // @SubscribeEvent
    // public static void onModelRegister(ModelEvent.RegisterAdditional event) {
    //     MyCreateAddon.LOGGER.info("ClientSetup: Registering Additional Models for " + MyCreateAddon.MOD_ID);
    //     // AllPartialModels.YOUR_PARTIAL_MODEL = new PartialModel(new ResourceLocation(MyCreateAddon.MOD_ID, "block/your_partial"));
    //     // event.register(AllPartialModels.YOUR_PARTIAL_MODEL.getLocation());
    // }
}