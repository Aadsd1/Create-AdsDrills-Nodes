package com.yourname.mycreateaddon.content.kinetics.module;


import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import net.minecraft.core.BlockPos; // 추가
import net.minecraft.world.level.Level; // 추가

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
    void playEffects(Level level, BlockPos modulePos);

    default List<ItemStack> processItem(ItemStack stack, DrillCoreBlockEntity core) {

        Level level = core.getLevel();
        BlockPos modulePos = ((GenericModuleBlockEntity)this).getBlockPos();
        if (level == null || getRecipeType() == null) return Collections.singletonList(stack);

        // [핵심 수정] 1. 레시피를 가장 먼저 찾는다.
        Optional<RecipeHolder<?>> recipeHolderOpt = findRecipe(stack, level);

        // 레시피가 존재할 경우에만 다음 단계를 진행
        if (recipeHolderOpt.isPresent()) {
            // 2. 레시피가 존재한다면, 그 다음에 작동 전제 조건을 확인한다.
            if (checkProcessingPreconditions(core)) {
                // 3. 모든 조건이 맞으면, 그제서야 자원을 소모하고 효과를 재생한다.
                consumeResources(core);
                playEffects(level, modulePos);

                Recipe<?> recipe = recipeHolderOpt.get().value();
                List<ItemStack> results = new ArrayList<>();

                // ... (이하 결과물 처리 로직은 이전과 동일) ...
                if (recipe instanceof ProcessingRecipe<?> processingRecipe) {
                    if (!processingRecipe.getResultItem(level.registryAccess()).isEmpty()) {
                        results.add(processingRecipe.getResultItem(level.registryAccess()).copy());
                    }
                    for (ProcessingOutput output : processingRecipe.getRollableResults()) {
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
        }

        // 레시피가 없거나, 조건이 맞지 않으면 원본 아이템을 그대로 반환
        return Collections.singletonList(stack);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Optional<RecipeHolder<?>> findRecipe(ItemStack stack, Level level) {
        RecipeType<?> type = getRecipeType();
        if (type == null) {
            return Optional.empty();
        }

        // 1. RecipeFinder를 사용하여, 해당 레시피 타입(예: Crushing)에 속하는 모든 레시피를 가져옵니다.
        //    이때, 캐시 키를 사용하여 매번 모든 레시피를 스캔하는 것을 방지합니다.
        Object cacheKey = "my_addon_drill_processing_" + type.toString();
        List<RecipeHolder<?>> allRecipesOfType = RecipeFinder.get(cacheKey, level,
                holder -> holder.value().getType() == type
        );

        // 2. 가져온 레시피 리스트를 순회하며, 입력된 아이템(stack)과 재료가 일치하는 레시피를 찾습니다.
        for (RecipeHolder<?> recipeHolder : allRecipesOfType) {
            Recipe<?> recipe = recipeHolder.value();
            // 레시피의 모든 재료를 확인합니다.
            for (Ingredient ingredient : recipe.getIngredients()) {
                // 재료가 입력된 아이템과 일치하는지 확인합니다.
                if (ingredient.test(stack)) {
                    // 일치하는 레시피를 찾았으면 즉시 반환합니다.
                    return (Optional) Optional.of(recipeHolder);
                }
            }
        }

        // 일치하는 레시피를 찾지 못했으면 빈 Optional을 반환합니다.
        return Optional.empty();
    }
}