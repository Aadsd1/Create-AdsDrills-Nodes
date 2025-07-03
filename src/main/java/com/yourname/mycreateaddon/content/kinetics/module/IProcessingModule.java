package com.yourname.mycreateaddon.content.kinetics.module;


import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 드릴 코어에 의해 제어되는 현장 처리 모듈의 동작을 정의합니다.
 */

public interface IProcessingModule {

    RecipeType<?> getRecipeType();
    boolean checkProcessingPreconditions(DrillCoreBlockEntity core);
    void consumeResources(DrillCoreBlockEntity core);

    default List<ItemStack> processItem(ItemStack stack, DrillCoreBlockEntity core) {
        Level level = core.getLevel();
        if (level == null || getRecipeType() == null) return Collections.singletonList(stack);

        Optional<RecipeHolder<?>> recipeHolderOpt = findRecipe(stack, level);

        if (recipeHolderOpt.isEmpty()) {
            return Collections.singletonList(stack);
        }

        Recipe<?> recipe = recipeHolderOpt.get().value();

        if (checkProcessingPreconditions(core)) {
            consumeResources(core);

            List<ItemStack> results = new ArrayList<>();

            if (recipe instanceof ProcessingRecipe<?> processingRecipe) {
                if (!processingRecipe.getResultItem(level.registryAccess()).isEmpty()) {
                    results.add(processingRecipe.getResultItem(level.registryAccess()).copy());
                }
                List<ProcessingOutput> rollableResults = processingRecipe.getRollableResults();
                for (ProcessingOutput output : rollableResults) {
                    results.add(output.getStack().copy());
                }
            } else {
                results.add(recipe.getResultItem(level.registryAccess()).copy());
            }

            results.removeIf(ItemStack::isEmpty);

            if (!results.isEmpty()) {
                return results;
            }
        }

        return Collections.singletonList(stack);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Optional<RecipeHolder<?>> findRecipe(ItemStack stack, Level level) {
        RecipeType<?> type = getRecipeType();
        Object cacheKey = "my_addon_drill_processing_" + type.toString();
        if (type == RecipeType.SMELTING || type == RecipeType.BLASTING || type == RecipeType.SMOKING) {
            return level.getRecipeManager().getRecipeFor((RecipeType) type, new SingleRecipeInput(stack), level);
        } else {
            return RecipeFinder.get(cacheKey, level, holder -> {
                Recipe<?> recipe = holder.value();
                if (recipe.getIngredients().isEmpty() || recipe.getType() != type) return false;
                return recipe.getIngredients().getFirst().test(stack);
            }).stream().findFirst();
        }
    }
}