package com.adsd.adsdrill.client.event;


import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.registry.AdsDrillItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

@EventBusSubscriber(modid = AdsDrillAddon.MOD_ID)
public class AnvilTuningEvents {
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack leftStack = event.getLeft();
        ItemStack rightStack = event.getRight();

        if (leftStack.getItem() != AdsDrillItems.NETHERITE_NODE_LOCATOR.get()) {
            return;
        }

        // 1. 광석으로 튜닝
        Block oreBlock = Block.byItem(rightStack.getItem());
        if (oreBlock != Blocks.AIR && rightStack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores")))) {
            Level level = event.getPlayer().level();

            RecipeManager recipeManager = level.getRecipeManager();
            RegistryAccess registryAccess = level.registryAccess();
            Item targetItem = AdsDrillConfigs.getOreItemFromBlock(oreBlock, recipeManager, registryAccess);

            if (targetItem != Items.AIR) {
                ItemStack output = leftStack.copy();
                CompoundTag nbt = output.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

                nbt.putString("TargetOre", BuiltInRegistries.ITEM.getKey(targetItem).toString());

                output.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

                event.setOutput(output);
                event.setCost(5);
                event.setMaterialCost(1);
            }
        }
        // 2. 유체로 튜닝
        else if (rightStack.getCapability(Capabilities.FluidHandler.ITEM) != null) {
            IFluidHandlerItem fluidHandler = rightStack.getCapability(Capabilities.FluidHandler.ITEM);
            if (fluidHandler != null && !fluidHandler.getFluidInTank(0).isEmpty()) {
                Fluid fluid = fluidHandler.getFluidInTank(0).getFluid();
                ItemStack output = leftStack.copy();
                CompoundTag nbt = output.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                nbt.putString("TargetFluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
                output.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                event.setOutput(output);
                event.setCost(5);
                event.setMaterialCost(1);
            }
        }
        // 3. 부싯돌로 모든 필터 초기화
        else if (rightStack.getItem() == Items.FLINT) {
            ItemStack output = leftStack.copy();
            CompoundTag nbt = output.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (nbt.contains("TargetOre") || nbt.contains("TargetFluid")) {
                nbt.remove("TargetOre");
                nbt.remove("TargetFluid");
                output.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                event.setOutput(output);
                event.setCost(1);
                event.setMaterialCost(1);
            }
        }
    }




}