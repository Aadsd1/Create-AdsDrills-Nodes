package com.yourname.mycreateaddon.compat.jei.category;


import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.compat.jei.recipe.LaserDecompositionRecipe;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import com.yourname.mycreateaddon.registry.MyAddonItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LaserDecompositionCategory implements IRecipeCategory<LaserDecompositionRecipe> {

    public static final RecipeType<LaserDecompositionRecipe> TYPE = RecipeType.create(MyCreateAddon.MOD_ID, "laser_decomposition", LaserDecompositionRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public LaserDecompositionCategory(IGuiHelper helper) {
        int textureWidth = 256;
        int textureHeight = 256;
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "textures/gui/jei/module_upgrade.png");
        background = helper.drawableBuilder(location, 0, 0, 120, 50).setTextureSize(textureWidth, textureHeight).build();
        icon = helper.createDrawableItemStack(new ItemStack(MyAddonBlocks.LASER_DRILL_HEAD.get()));
        arrow = helper.drawableBuilder(location, 128, 16, 24, 16).setTextureSize(textureWidth, textureHeight).build();
    }

    @Override public @NotNull RecipeType<LaserDecompositionRecipe> getRecipeType() { return TYPE; }
    @Override public @NotNull Component getTitle() { return Component.translatable("mycreateaddon.jei.category.laser_decomposition"); }
    @Override public IDrawable getIcon() { return icon; }

    @Override public int getWidth() { return 120; }
    @Override public int getHeight() { return 50; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, LaserDecompositionRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 48, 2).addItemStack(new ItemStack(MyAddonBlocks.LASER_DRILL_HEAD.get()));
        builder.addSlot(RecipeIngredientRole.INPUT, 24, 26).addItemStack(recipe.inputNode());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 80, 26).addItemStack(recipe.outputData());
    }

    @Override
    public void draw(@NotNull LaserDecompositionRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {

        background.draw(guiGraphics, 0, 0);
        arrow.draw(guiGraphics, 50, 26);
    }

    public static List<LaserDecompositionRecipe> getRecipes() {
        return List.of(new LaserDecompositionRecipe(
                new ItemStack(MyAddonBlocks.ORE_NODE.get()),
                new ItemStack(MyAddonItems.UNFINISHED_NODE_DATA.get())
        ));
    }
}