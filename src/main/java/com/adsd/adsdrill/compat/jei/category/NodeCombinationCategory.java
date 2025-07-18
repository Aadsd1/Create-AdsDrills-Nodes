package com.adsd.adsdrill.compat.jei.category;


import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.crafting.NodeRecipe;
import com.adsd.adsdrill.registry.AdsDrillBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class NodeCombinationCategory implements IRecipeCategory<NodeRecipe> {

    public static final RecipeType<NodeRecipe> TYPE = RecipeType.create(AdsDrillAddon.MOD_ID, "node_combination", NodeRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public NodeCombinationCategory(IGuiHelper helper) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(AdsDrillAddon.MOD_ID, "textures/gui/jei/node_combination.png");
        background = helper.drawableBuilder(location, 0, 0, 170, 70).setTextureSize(256, 256).build();
        icon = helper.createDrawableItemStack(new ItemStack(AdsDrillBlocks.EXPLOSIVE_DRILL_HEAD.get()));
        arrow = helper.drawableBuilder(location, 176, 0, 24, 16).setTextureSize(256, 256).build();
    }

    @Override public @NotNull RecipeType<NodeRecipe> getRecipeType() { return TYPE; }
    @Override public @NotNull Component getTitle() { return Component.translatable("adsdrill.jei.category.node_combination"); }
    @Override public int getWidth() { return 170; }
    @Override public int getHeight() { return 70; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, NodeRecipe recipe, @NotNull IFocusGroup focuses) {
        for (int i = 0; i < Math.min(4, recipe.requiredItems().size()); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 8 + i * 18, 8)
                    .addItemStack(new ItemStack(recipe.requiredItems().get(i)));
        }

        if (recipe.requiredFluid() != null && recipe.requiredFluid() != Fluids.EMPTY) {
            builder.addSlot(RecipeIngredientRole.INPUT, 8, 35)
                    .addIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(recipe.requiredFluid(), 1000))
                    .setFluidRenderer(1000, true, 16, 16);
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 140, 26)
                .addItemStack(recipe.output())
                .addRichTooltipCallback((view, tooltip) -> {
                    if (recipe.chance() < 1.0) {
                        tooltip.add(Component.translatable("adsdrill.jei.tooltip.chance", (int)(recipe.chance() * 100))
                                .withStyle(ChatFormatting.GOLD));
                    }
                });
    }

    @Override
    public void draw(@NotNull NodeRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics, 0, 0);
        arrow.draw(guiGraphics, 100, 26);
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("adsdrill.jei.in_cracked_node"),
                85, 55, 0x808080, false);
    }

    public static List<NodeRecipe> getRecipes() {
        return NodeRecipe.RECIPES;
    }
}