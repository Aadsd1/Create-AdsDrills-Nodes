package com.yourname.mycreateaddon.registry;


import com.yourname.mycreateaddon.MyCreateAddon;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MyAddonCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MyCreateAddon.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BASE_TAB = CREATIVE_MODE_TABS.register("base_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> MyAddonBlocks.DRILL_CORE.asItem().getDefaultInstance())
                    .title(Component.translatable("creativetab.mycreateaddon.base_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(MyAddonBlocks.DRILL_CORE.get());
                        pOutput.accept(MyAddonBlocks.FRAME_MODULE.get());
                        pOutput.accept(MyAddonBlocks.SPEED_MODULE.get());
                        pOutput.accept(MyAddonBlocks.REINFORCEMENT_MODULE.get());
                        pOutput.accept(MyAddonBlocks.EFFICIENCY_MODULE.get());
                        pOutput.accept(MyAddonBlocks.ITEM_BUFFER_MODULE.get());
                        pOutput.accept(MyAddonBlocks.FLUID_BUFFER_MODULE.get());
                        pOutput.accept(MyAddonBlocks.REDSTONE_BRAKE_MODULE.get());
                        pOutput.accept(MyAddonBlocks.NODE_FRAME.get());
                        pOutput.accept(MyAddonBlocks.ARTIFICIAL_NODE.get());
                        pOutput.accept(MyAddonBlocks.IRON_ROTARY_DRILL_HEAD.get());
                        pOutput.accept(MyAddonBlocks.DIAMOND_ROTARY_DRILL_HEAD.get());
                        pOutput.accept(MyAddonBlocks.NETHERITE_ROTARY_DRILL_HEAD.get());
                        pOutput.accept(MyAddonBlocks.HYDRAULIC_DRILL_HEAD.get());
                        pOutput.accept(MyAddonBlocks.PUMP_HEAD.get());
                        pOutput.accept(MyAddonBlocks.EXPLOSIVE_DRILL_HEAD.get());
                        pOutput.accept(MyAddonBlocks.LASER_DRILL_HEAD.get());
                        pOutput.accept(MyAddonBlocks.FURNACE_MODULE.get());
                        pOutput.accept(MyAddonBlocks.BLAST_FURNACE_MODULE.get());
                        pOutput.accept(MyAddonBlocks.CRUSHER_MODULE.get());
                        pOutput.accept(MyAddonBlocks.WASHER_MODULE.get());
                        pOutput.accept(MyAddonBlocks.HEATSINK_MODULE.get());
                        pOutput.accept(MyAddonBlocks.COOLANT_MODULE.get());
                        pOutput.accept(MyAddonBlocks.COMPACTOR_MODULE.get());
                        pOutput.accept(MyAddonBlocks.FILTER_MODULE.get());
                        pOutput.accept(MyAddonBlocks.ENERGY_INPUT_MODULE.get());
                        pOutput.accept(MyAddonBlocks.ENERGY_BUFFER_MODULE.get());
                        pOutput.accept(MyAddonBlocks.KINETIC_DYNAMO_MODULE.get());
                        pOutput.accept(MyAddonBlocks.RESONATOR_MODULE.get());

                        pOutput.accept(MyAddonItems.CINNABAR);
                        pOutput.accept(MyAddonItems.THE_FOSSIL);
                        pOutput.accept(MyAddonItems.IVORY_CRYSTAL);
                        pOutput.accept(MyAddonItems.KOH_I_NOOR);
                        pOutput.accept(MyAddonItems.RAW_ROSE_GOLD_CHUNK);
                        pOutput.accept(MyAddonItems.ROSE_GOLD);
                        pOutput.accept(MyAddonItems.SILKY_JEWEL);
                        pOutput.accept(MyAddonItems.THUNDER_STONE);
                        pOutput.accept(MyAddonItems.ULTRAMARINE);
                        pOutput.accept(MyAddonItems.RAW_STEEL_CHUNK);
                        pOutput.accept(MyAddonItems.STEEL_INGOT);
                        pOutput.accept(MyAddonItems.XOMV);
                        pOutput.accept(MyAddonItems.BRASS_STABILIZER_CORE);
                        pOutput.accept(MyAddonItems.STEEL_STABILIZER_CORE);
                        pOutput.accept(MyAddonItems.NETHERITE_STABILIZER_CORE);
                        pOutput.accept(MyAddonItems.ORE_NODE_NEUTRALIZER);
                        pOutput.accept(MyAddonItems.UNFINISHED_NODE_DATA);
                        pOutput.accept(MyAddonItems.NODE_DESIGNATOR);
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}