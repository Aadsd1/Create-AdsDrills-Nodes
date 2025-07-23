package com.adsd.adsdrill.compat.jei.recipe;


import net.minecraft.world.item.ItemStack;

public record DrillCoreUpgradeRecipe(
        ItemStack inputCore,
        ItemStack upgradeKit,
        ItemStack outputCore
) {}