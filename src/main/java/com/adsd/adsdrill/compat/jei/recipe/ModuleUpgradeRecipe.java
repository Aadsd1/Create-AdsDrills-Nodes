package com.adsd.adsdrill.compat.jei.recipe;

import net.minecraft.world.item.ItemStack;

public record ModuleUpgradeRecipe(
        ItemStack inputFrame,
        ItemStack upgradeItem,
        ItemStack outputModule
) {}