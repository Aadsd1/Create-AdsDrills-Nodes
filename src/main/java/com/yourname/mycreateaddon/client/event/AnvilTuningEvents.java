package com.yourname.mycreateaddon.client.event;


import com.yourname.mycreateaddon.registry.MyAddonItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

@EventBusSubscriber(modid = com.yourname.mycreateaddon.MyCreateAddon.MOD_ID)
public class AnvilTuningEvents {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack leftStack = event.getLeft();
        ItemStack rightStack = event.getRight();

        if (leftStack.getItem() != MyAddonItems.NETHERITE_NODE_LOCATOR.get()) {
            return;
        }

        // [!!! 핵심 수정: 오른쪽 아이템이 광물인지 확인하고, '결과물' ID만 저장 !!!]
        if (rightStack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores")))) {
            Level level = event.getPlayer().level();
            Item targetItem = getSmeltedOrRawResult(rightStack, level);

            if (targetItem != Items.AIR) {
                ItemStack output = leftStack.copy();
                CompoundTag nbt = output.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

                // 'TargetOre' 라는 이름으로 결과물 아이템 ID만 저장합니다.
                nbt.putString("TargetOre", BuiltInRegistries.ITEM.getKey(targetItem).toString());
                // 원본 블록 정보는 더 이상 저장하지 않습니다.
                nbt.remove("TargetSourceItem");
                nbt.remove("TargetResultItem"); // 이전 이름 정리

                output.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

                event.setOutput(output);
                event.setCost(5);
                event.setMaterialCost(1);
            }
        }
        // 지정 해제 로직
        else if (rightStack.getItem() == Items.FLINT) {
            ItemStack output = leftStack.copy();
            CompoundTag nbt = output.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

            // 모든 관련 타겟 태그를 제거합니다.
            if (nbt.contains("TargetOre") || nbt.contains("TargetResultItem")) {
                nbt.remove("TargetOre");
                nbt.remove("TargetResultItem");
                nbt.remove("TargetSourceItem");
                output.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

                event.setOutput(output);
                event.setCost(1);
                event.setMaterialCost(1);
            }
        }
    }


    /**
     * 광석 블록 아이템을 받아, 제련 시 나오는 아이템(또는 그에 해당하는 원석)을 반환합니다.
     * OreNodeFeature의 로직과 동일한 결과를 보장합니다.
     */
    private static Item getSmeltedOrRawResult(ItemStack oreStack, Level level) {
        if (oreStack.isEmpty()) {
            return Items.AIR;
        }

        var recipeManager = level.getRecipeManager();

        // 제련 레시피를 찾아서 결과물을 확인
        for (var recipeHolder : recipeManager.getAllRecipesFor(RecipeType.SMELTING)) {
            SmeltingRecipe recipe = recipeHolder.value();

            if (recipe.getIngredients().getFirst().test(oreStack)) {
                ItemStack resultStack = recipe.getResultItem(level.registryAccess());
                Item resultItem = resultStack.getItem();

                ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(resultItem);

                // 결과물이 주괴(ingot)인 경우, 대응하는 'raw_' 아이템을 찾아서 반환
                if (resultId.getPath().endsWith("_ingot")) {
                    String rawMaterialName = resultId.getPath().replace("_ingot", "");
                    // 네이밍 컨벤션에 따라 네임스페이스를 유지하며 raw_ ID를 만듭니다.
                    ResourceLocation rawId = ResourceLocation.fromNamespaceAndPath(resultId.getNamespace(), "raw_" + rawMaterialName);

                    Item rawItem = BuiltInRegistries.ITEM.get(rawId);
                    // raw_ 아이템이 존재하면 그것을 반환
                    if (rawItem != Items.AIR) {
                        return rawItem;
                    }
                }
                // 주괴가 아니거나(예: 다이아몬드, 석탄) 대응하는 raw_ 아이템이 없으면, 제련 결과물 자체를 반환
                return resultItem;
            }
        }
        // 제련 레시피가 없으면, 광석 블록 아이템 자체를 반환 (예: 석탄 블록 -> 석탄)
        return oreStack.getItem();
    }

}