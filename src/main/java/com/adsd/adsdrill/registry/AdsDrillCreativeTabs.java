package com.adsd.adsdrill.registry;


import com.adsd.adsdrill.AdsDrillAddon;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AdsDrillCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AdsDrillAddon.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BASE_TAB = CREATIVE_MODE_TABS.register("base_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> AdsDrillBlocks.DRILL_CORE.asItem().getDefaultInstance())
                    .title(Component.translatable("creativetab.adsdrill.base_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(AdsDrillBlocks.DRILL_CORE.get());
                        pOutput.accept(AdsDrillBlocks.FRAME_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.SPEED_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.REINFORCEMENT_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.EFFICIENCY_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.ITEM_BUFFER_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.FLUID_BUFFER_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.REDSTONE_BRAKE_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.NODE_FRAME.get());
                        pOutput.accept(AdsDrillBlocks.ARTIFICIAL_NODE.get());
                        pOutput.accept(AdsDrillBlocks.IRON_ROTARY_DRILL_HEAD.get());
                        pOutput.accept(AdsDrillBlocks.DIAMOND_ROTARY_DRILL_HEAD.get());
                        pOutput.accept(AdsDrillBlocks.NETHERITE_ROTARY_DRILL_HEAD.get());
                        pOutput.accept(AdsDrillBlocks.HYDRAULIC_DRILL_HEAD.get());
                        pOutput.accept(AdsDrillBlocks.PUMP_HEAD.get());
                        pOutput.accept(AdsDrillBlocks.EXPLOSIVE_DRILL_HEAD.get());
                        pOutput.accept(AdsDrillBlocks.LASER_DRILL_HEAD.get());
                        pOutput.accept(AdsDrillBlocks.FURNACE_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.BLAST_FURNACE_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.CRUSHER_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.WASHER_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.HEATSINK_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.COOLANT_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.COMPACTOR_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.FILTER_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.ENERGY_INPUT_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.ENERGY_BUFFER_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.KINETIC_DYNAMO_MODULE.get());
                        pOutput.accept(AdsDrillBlocks.RESONATOR_MODULE.get());

                        pOutput.accept(AdsDrillItems.DRILL_CORE_NETHERITE_UPGRADE);
                        pOutput.accept(AdsDrillItems.DRILL_CORE_STEEL_UPGRADE);
                        pOutput.accept(AdsDrillItems.BRASS_NODE_LOCATOR.get());
                        pOutput.accept(AdsDrillItems.STEEL_NODE_LOCATOR.get());
                        pOutput.accept(AdsDrillItems.NETHERITE_NODE_LOCATOR.get());

                        pOutput.accept(AdsDrillItems.MODULE_UPGRADE_REMOVER);
                        pOutput.accept(AdsDrillItems.MODULE_R_BRAKE_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_SPEED_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_REIN_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_HEATSINK_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_EFFI_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_COOL_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_RESO_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_COMP_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_FURNACE_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_BF_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_CRUSH_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_WASH_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_FILTER_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_F_BUFFER_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_I_BUFFER_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_E_BUFFER_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_E_GEN_UPGRADE);
                        pOutput.accept(AdsDrillItems.MODULE_E_INPUT_UPGRADE);

                        pOutput.accept(AdsDrillItems.CINNABAR);
                        pOutput.accept(AdsDrillItems.THE_FOSSIL);
                        pOutput.accept(AdsDrillItems.IVORY_CRYSTAL);
                        pOutput.accept(AdsDrillItems.KOH_I_NOOR);
                        pOutput.accept(AdsDrillItems.RAW_ROSE_GOLD_CHUNK);
                        pOutput.accept(AdsDrillItems.ROSE_GOLD);
                        pOutput.accept(AdsDrillItems.SILKY_JEWEL);
                        pOutput.accept(AdsDrillItems.THUNDER_STONE);
                        pOutput.accept(AdsDrillItems.ULTRAMARINE);
                        pOutput.accept(AdsDrillItems.RAW_STEEL_CHUNK);
                        pOutput.accept(AdsDrillItems.STEEL_INGOT);
                        pOutput.accept(AdsDrillItems.XOMV);
                        pOutput.accept(AdsDrillItems.BRASS_STABILIZER_CORE);
                        pOutput.accept(AdsDrillItems.STEEL_STABILIZER_CORE);
                        pOutput.accept(AdsDrillItems.NETHERITE_STABILIZER_CORE);
                        pOutput.accept(AdsDrillItems.ORE_NODE_NEUTRALIZER);
                        pOutput.accept(AdsDrillItems.ORE_CAKE);
                        pOutput.accept(AdsDrillItems.UNFINISHED_NODE_DATA);
                        pOutput.accept(AdsDrillItems.LASER_DESIGNATOR);
                        pOutput.accept(AdsDrillItems.NODE_DEBUGGER);

                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}