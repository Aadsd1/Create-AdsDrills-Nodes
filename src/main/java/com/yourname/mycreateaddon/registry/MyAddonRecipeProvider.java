package com.yourname.mycreateaddon.registry;


import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.foundation.data.recipe.CreateRecipeProvider;
import com.simibubi.create.foundation.data.recipe.MechanicalCraftingRecipeBuilder;
import com.simibubi.create.foundation.data.recipe.MechanicalCraftingRecipeGen;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import com.yourname.mycreateaddon.registry.MyAddonItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.core.HolderLookup;
import org.jetbrains.annotations.NotNull;
import com.simibubi.create.foundation.data.recipe.SequencedAssemblyRecipeGen; //참고용
import java.util.concurrent.CompletableFuture;

public class MyAddonRecipeProvider extends RecipeProvider {

    public MyAddonRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

}