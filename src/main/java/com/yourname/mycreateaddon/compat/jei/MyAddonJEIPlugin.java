package com.yourname.mycreateaddon.compat.jei;


import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.compat.jei.category.*;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import com.yourname.mycreateaddon.registry.MyAddonItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;


@JeiPlugin
public class MyAddonJEIPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new NodeFrameCategory(helper),
                new ModuleUpgradeCategory(helper),
                new DrillHeadUpgradeCategory(helper),
                new NodeCombinationCategory(helper),
                new LaserDecompositionCategory(helper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(NodeFrameCategory.TYPE, NodeFrameCategory.getRecipes());
        registration.addRecipes(ModuleUpgradeCategory.TYPE, ModuleUpgradeCategory.getRecipes());
        registration.addRecipes(DrillHeadUpgradeCategory.TYPE, DrillHeadUpgradeCategory.getRecipes());
        registration.addRecipes(NodeCombinationCategory.TYPE, NodeCombinationCategory.getRecipes());
        registration.addRecipes(LaserDecompositionCategory.TYPE, LaserDecompositionCategory.getRecipes());

        addAnvilTuningRecipes(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(MyAddonBlocks.NODE_FRAME.get()), NodeFrameCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(MyAddonBlocks.FRAME_MODULE.get()), ModuleUpgradeCategory.TYPE);

        Stream.of(MyAddonBlocks.IRON_ROTARY_DRILL_HEAD, MyAddonBlocks.DIAMOND_ROTARY_DRILL_HEAD, MyAddonBlocks.NETHERITE_ROTARY_DRILL_HEAD)
                .forEach(drill -> registration.addRecipeCatalyst(new ItemStack(drill.get()), DrillHeadUpgradeCategory.TYPE));

        registration.addRecipeCatalyst(new ItemStack(MyAddonBlocks.EXPLOSIVE_DRILL_HEAD.get()), NodeCombinationCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(MyAddonBlocks.LASER_DRILL_HEAD.get()), LaserDecompositionCategory.TYPE);
    }

    private void addAnvilTuningRecipes(IRecipeRegistration registration) {
        ItemStack locator = new ItemStack(MyAddonItems.NETHERITE_NODE_LOCATOR.get());
        ItemStack ironOre = new ItemStack(Items.IRON_ORE);
        ItemStack tunedWithIron = locator.copy();
        CompoundTag nbt = new CompoundTag();
        nbt.putString("TargetOre", "minecraft:raw_iron");
        tunedWithIron.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

        // [CORRECTED] Wrap recipe in RecipeHolder
        RecipeHolder<SmithingRecipe> ironRecipe = new RecipeHolder<>(
                ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "jei/anvil_tuning/ore_tuning"),
                new SmithingTransformRecipe(Ingredient.EMPTY, Ingredient.of(locator), Ingredient.of(ironOre), tunedWithIron)
        );

        ItemStack flint = new ItemStack(Items.FLINT);
        ItemStack clearedLocator = locator.copy();

        RecipeHolder<SmithingRecipe> clearRecipe = new RecipeHolder<>(
                ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "jei/anvil_tuning/clear_tuning"),
                new SmithingTransformRecipe(Ingredient.EMPTY, Ingredient.of(tunedWithIron), Ingredient.of(flint), clearedLocator)
        );

        registration.addRecipes(RecipeTypes.SMITHING, List.of(ironRecipe, clearRecipe));

        registration.addIngredientInfo(
                new ItemStack(MyAddonItems.NETHERITE_NODE_LOCATOR.get()),
                mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                Component.translatable("mycreateaddon.jei.info.anvil_tuning")
        );
    }
}