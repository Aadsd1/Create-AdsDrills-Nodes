package com.adsd.adsdrill.content.kinetics.module;


import com.adsd.adsdrill.content.kinetics.base.IResourceAccessor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * 압축 모듈의 3x3 조합 레시피 일괄 처리 로직을 담당하는 행동 클래스입니다.
 */
public class CompactorModuleBehavior implements IModuleBehavior {

    @Override
    public boolean processBulk(GenericModuleBlockEntity moduleBE, IResourceAccessor coreResources) {
        Level level = moduleBE.getLevel();
        if (level == null) return false;

        RecipeManager recipeManager = level.getRecipeManager();

        for (RecipeHolder<CraftingRecipe> recipeHolder : recipeManager.getAllRecipesFor(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = recipeHolder.value();
            if (recipe.getIngredients().size() == 9 && !recipe.getResultItem(level.registryAccess()).isEmpty()) {
                Ingredient firstIngredient = recipe.getIngredients().getFirst();
                if (firstIngredient.isEmpty() || firstIngredient.getItems().length == 0) continue;

                boolean allSame = true;
                for (int i = 1; i < 9; i++) {
                    ItemStack[] items = recipe.getIngredients().get(i).getItems();
                    if (items.length == 0 || !firstIngredient.test(items[0])) {
                        allSame = false;
                        break;
                    }
                }

                if (allSame) {
                    ItemStack ingredientStack = firstIngredient.getItems()[0].copy();
                    ingredientStack.setCount(9);

                    if (coreResources.consumeItems(ingredientStack, true).isEmpty()) {
                        coreResources.consumeItems(ingredientStack, false);
                        ItemStack resultStack = recipe.getResultItem(level.registryAccess()).copy();
                        ItemHandlerHelper.insertItem(coreResources.getInternalItemBuffer(), resultStack, false);
                        playCompactingEffects(moduleBE);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void playCompactingEffects(GenericModuleBlockEntity moduleBE) {
        Level level = moduleBE.getLevel();
        if (level != null && !level.isClientSide) {
            level.playSound(null, moduleBE.getBlockPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.2F, 1.2f);
            if (level instanceof ServerLevel serverLevel) {
                double px = moduleBE.getBlockPos().getX() + 0.5;
                double py = moduleBE.getBlockPos().getY() + 0.5;
                double pz = moduleBE.getBlockPos().getZ() + 0.5;
                serverLevel.sendParticles(ParticleTypes.CRIT, px, py, pz, 15, 0.4, 0.4, 0.4, 0.1);
            }
        }
    }
}