package com.yourname.mycreateaddon.registry;



import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.module.ModuleType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;


@EventBusSubscriber(modid = MyCreateAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class MyAddonCapabilities {

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, // 아이템 핸들러 Capability
                MyAddonBlockEntity.GENERIC_MODULE.get(), // GENERIC_MODULE 타입의 BE에 대해
                (moduleBE, context) -> {
                    // 모듈 타입이 아이템 버퍼일 경우에만 핸들러를 반환
                    if (moduleBE.getModuleType() == ModuleType.ITEM_BUFFER) {
                        return moduleBE.getItemHandler();
                    }
                    // I/O 모듈을 나중에 추가한다면 여기에 로직 추가
                    // if (moduleBE.getModuleType() == ModuleType.ITEM_INPUT || moduleBE.getModuleType() == ModuleType.ITEM_OUTPUT) {
                    //     // 코어의 가상 핸들러를 반환하는 로직
                    // }
                    return null; // 그 외 타입은 노출 안 함
                }
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK, // 유체 핸들러 Capability
                MyAddonBlockEntity.GENERIC_MODULE.get(), // GENERIC_MODULE 타입의 BE에 대해
                (moduleBE, context) -> {
                    // 모듈 타입이 유체 버퍼일 경우에만 핸들러를 반환
                    if (moduleBE.getModuleType() == ModuleType.FLUID_BUFFER) {
                        return moduleBE.getFluidHandler();
                    }
                    // I/O 모듈 로직...
                    return null; // 그 외 타입은 노출 안 함
                }
        );

    }
}