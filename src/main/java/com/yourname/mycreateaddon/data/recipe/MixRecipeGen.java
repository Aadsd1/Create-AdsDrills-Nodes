package com.yourname.mycreateaddon.data.recipe;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.data.recipe.MixingRecipeGen;
import com.yourname.mycreateaddon.registry.MyAddonItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class MixRecipeGen extends MixingRecipeGen {

    GeneratedRecipe

    ORE_NODE_NEUTRALIZER=create(ResourceLocation.parse("ore_node_neutralizer"), b->b.require(Items.REDSTONE)
            .require(Items.SUGAR)
            .require(AllItems.WHEAT_FLOUR)
            .output(MyAddonItems.ORE_NODE_NEUTRALIZER));


    public MixRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }
}
