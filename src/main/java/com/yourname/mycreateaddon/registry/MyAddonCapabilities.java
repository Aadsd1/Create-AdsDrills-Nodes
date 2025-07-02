package com.yourname.mycreateaddon.registry;



import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
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
                Capabilities.ItemHandler.BLOCK,
                MyAddonBlockEntity.DRILL_CORE.get(),
                (core, context) -> {
                    Direction side = context != null ? (Direction) context : null;
                    if (side == null || side.getAxis() != core.getBlockState().getValue(BlockStateProperties.FACING).getAxis()) {
                        return core.getInternalItemBuffer();
                    }
                    return null;
                }
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                MyAddonBlockEntity.DRILL_CORE.get(),
                (core, context) -> {
                    Direction side = context != null ? (Direction) context : null;
                    if (side == null || side.getAxis() != core.getBlockState().getValue(BlockStateProperties.FACING).getAxis()) {
                        return core.getInternalFluidBuffer();
                    }
                    return null;
                }
        );
    }
}