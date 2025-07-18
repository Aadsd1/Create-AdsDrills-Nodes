package com.yourname.mycreateaddon.compat.jei.category;


import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.compat.jei.recipe.ModuleUpgradeRecipe;
import com.yourname.mycreateaddon.crafting.ModuleUpgrades;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
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
import java.util.stream.Collectors;

public class ModuleUpgradeCategory implements IRecipeCategory<ModuleUpgradeRecipe> {

    public static final RecipeType<ModuleUpgradeRecipe> TYPE = RecipeType.create(MyCreateAddon.MOD_ID, "module_upgrading", ModuleUpgradeRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable plus;
    private final IDrawable arrow;

    public ModuleUpgradeCategory(IGuiHelper helper) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "textures/gui/jei/module_upgrade.png");
        int textureWidth = 256;
        int textureHeight = 256;

        background = helper.drawableBuilder(location, 0, 0, 120, 50)
                .setTextureSize(textureWidth, textureHeight)
                .build();

        icon = helper.createDrawableItemStack(new ItemStack(MyAddonBlocks.FRAME_MODULE.get()));

        plus = helper.drawableBuilder(location, 128, 0, 16, 16)
                .setTextureSize(textureWidth, textureHeight)
                .build();

        arrow = helper.drawableBuilder(location, 128, 16, 24, 16)
                .setTextureSize(textureWidth, textureHeight)
                .build();
    }

    @Override public int getWidth() { return 120; }
    @Override public int getHeight() { return 50; }
    @Override public @NotNull RecipeType<ModuleUpgradeRecipe> getRecipeType() { return TYPE; }
    @Override public @NotNull Component getTitle() { return Component.translatable("mycreateaddon.jei.category.module_upgrading"); }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ModuleUpgradeRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 12, 17).addItemStack(recipe.inputFrame());
        builder.addSlot(RecipeIngredientRole.INPUT, 48, 17).addItemStack(recipe.upgradeItem());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 17).addItemStack(recipe.outputModule());
    }

    @Override
    public void draw(@NotNull ModuleUpgradeRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics, 0, 0);
        plus.draw(guiGraphics, 31, 17);
        arrow.draw(guiGraphics, 66, 17);
    }
    public static List<ModuleUpgradeRecipe> getRecipes() {
        return ModuleUpgrades.getUpgradeResultMap().entrySet().stream()
                .map(entry -> new ModuleUpgradeRecipe(
                        new ItemStack(MyAddonBlocks.FRAME_MODULE.get()),
                        new ItemStack(entry.getKey()),
                        new ItemStack(entry.getValue().get())
                ))
                .collect(Collectors.toList());
    }
}