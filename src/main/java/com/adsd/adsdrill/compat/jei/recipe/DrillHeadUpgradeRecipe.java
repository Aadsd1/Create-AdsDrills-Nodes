package com.adsd.adsdrill.compat.jei.recipe;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
public record DrillHeadUpgradeRecipe(
        ItemStack inputDrill,
        ItemStack upgradeItem,
        Component outputDescription
) {}