package com.yourname.mycreateaddon.content.kinetics.module;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 일반적인 아이템 가공(제련, 분쇄, 세척 등)을 처리하는 행동 클래스입니다.
 */
public class ProcessingModuleBehavior implements IModuleBehavior {

    private final Supplier<RecipeType<?>> recipeTypeSupplier;

    public ProcessingModuleBehavior(Supplier<RecipeType<?>> recipeTypeSupplier) {
        this.recipeTypeSupplier = recipeTypeSupplier;
    }

    @Override
    public RecipeType<?> getRecipeType() {
        return recipeTypeSupplier.get();
    }

    @Override
    public List<ItemStack> processItem(GenericModuleBlockEntity moduleBE, ItemStack stack, DrillCoreBlockEntity core) {
        Level level = core.getLevel();
        if (level == null || getRecipeType() == null) return Collections.singletonList(stack);

        Optional<RecipeHolder<?>> recipeHolderOpt = findRecipe(stack, level);

        if (recipeHolderOpt.isPresent()) {
            if (checkProcessingPreconditions(moduleBE, core)) {
                consumeResources(moduleBE, core);
                playEffects(moduleBE, level, moduleBE.getBlockPos());

                Recipe<?> recipe = recipeHolderOpt.get().value();
                List<ItemStack> results = new ArrayList<>();

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
        return Collections.singletonList(stack);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Optional<RecipeHolder<?>> findRecipe(ItemStack stack, Level level) {
        RecipeType<?> type = getRecipeType();
        if (type == null) return Optional.empty();

        Object cacheKey = "my_addon_drill_processing_" + type.toString();
        List<RecipeHolder<?>> allRecipesOfType = RecipeFinder.get(cacheKey, level, holder -> holder.value().getType() == type);

        for (RecipeHolder<?> recipeHolder : allRecipesOfType) {
            Recipe<?> recipe = recipeHolder.value();
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.test(stack)) {
                    return (Optional) Optional.of(recipeHolder);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean checkProcessingPreconditions(GenericModuleBlockEntity moduleBE, DrillCoreBlockEntity core) {
        return switch (moduleBE.getModuleType()) {
            case WASHER -> {
                FluidStack waterToSimulate = new FluidStack(Fluids.WATER, 100);
                yield core.getInternalFluidBuffer().drain(waterToSimulate, IFluidHandler.FluidAction.SIMULATE).getAmount() >= 100;
            }
            case FURNACE -> core.getHeat() >= 50f;
            case BLAST_FURNACE -> core.getHeat() >= 90f;
            case CRUSHER -> true;
            default -> false;
        };
    }

    @Override
    public void consumeResources(GenericModuleBlockEntity moduleBE, DrillCoreBlockEntity core) {
        float efficiency = core.getHeatEfficiency();
        switch (moduleBE.getModuleType()) {
            case FURNACE -> {
                float heatToConsume = 10.1f / (efficiency + 0.1f);
                core.addHeat(-heatToConsume);
            }
            case BLAST_FURNACE -> {
                float heatToConsume = 20.1f / (efficiency + 0.1f);
                core.addHeat(-heatToConsume);
            }
            case WASHER -> {
                FluidStack waterRequest = new FluidStack(Fluids.WATER, 100);
                core.getInternalFluidBuffer().drain(waterRequest, IFluidHandler.FluidAction.EXECUTE);
            }
            default -> {}
        }
    }

    @Override
    public void playEffects(GenericModuleBlockEntity moduleBE, Level level, BlockPos modulePos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        switch (moduleBE.getModuleType()) {
            case FURNACE, BLAST_FURNACE -> {
                serverLevel.playSound(null, modulePos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.7F, 1.5F);
                serverLevel.sendParticles(ParticleTypes.LAVA, modulePos.getX() + 0.5, modulePos.getY() + 1.0, modulePos.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.0);
            }
            case CRUSHER -> {
                serverLevel.playSound(null, modulePos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.7F, 1.5F);
                serverLevel.sendParticles(ParticleTypes.CRIT, modulePos.getX() + 0.5, modulePos.getY() + 0.5, modulePos.getZ() + 0.5, 7, 0.3, 0.3, 0.3, 0.1);
            }
            case WASHER -> {
                serverLevel.playSound(null, modulePos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.2F);
                serverLevel.sendParticles(ParticleTypes.SPLASH, modulePos.getX() + 0.5, modulePos.getY() + 1.1, modulePos.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.0);
            }
            default -> {}
        }
    }
}