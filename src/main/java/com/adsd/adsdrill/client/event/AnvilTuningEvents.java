package com.adsd.adsdrill.client.event;


import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.registry.AdsDrillItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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

import java.util.HashSet;
import java.util.Set;

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

                // NBT에서 'TargetOres' 목록을 가져오거나, 없으면 새로 만듭니다. (9 = ListTag, 8 = StringTag)
                ListTag oreList = nbt.contains("TargetOres", 9) ? nbt.getList("TargetOres", 8) : new ListTag();

                // 중복 추가를 방지하기 위해 현재 목록을 Set으로 변환합니다.
                Set<String> existingOres = new HashSet<>();
                oreList.forEach(tag -> existingOres.add(tag.getAsString()));

                String newOreId = BuiltInRegistries.ITEM.getKey(targetItem).toString();

                // 이미 목록에 없는 광물일 경우에만 추가합니다.
                if (!existingOres.contains(newOreId)) {
                    oreList.add(StringTag.valueOf(newOreId));
                    nbt.put("TargetOres", oreList);
                    output.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

                    event.setOutput(output);
                    event.setCost(5);
                    event.setMaterialCost(1);
                }
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
            if (nbt.contains("TargetOres") || nbt.contains("TargetFluid")) {
                nbt.remove("TargetOres");
                nbt.remove("TargetFluid");
                output.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                event.setOutput(output);
                event.setCost(1);
                event.setMaterialCost(1);
            }
        }
    }
}