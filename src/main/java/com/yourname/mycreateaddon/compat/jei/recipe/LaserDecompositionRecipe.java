package com.yourname.mycreateaddon.compat.jei.recipe;

import net.minecraft.world.item.ItemStack;

public record LaserDecompositionRecipe(
        ItemStack inputNode,
        ItemStack outputData
) {}