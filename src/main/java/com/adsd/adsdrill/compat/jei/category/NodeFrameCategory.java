package com.adsd.adsdrill.compat.jei.category;


import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.compat.jei.recipe.NodeFrameRecipe;
import com.adsd.adsdrill.content.item.StabilizerCoreItem;
import com.adsd.adsdrill.registry.AdsDrillBlocks;
import com.adsd.adsdrill.registry.AdsDrillItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class NodeFrameCategory implements IRecipeCategory<NodeFrameRecipe> {

    public static final RecipeType<NodeFrameRecipe> TYPE = RecipeType.create(AdsDrillAddon.MOD_ID, "node_assembly", NodeFrameRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable drill;
    private final IDrawable arrow;

    public NodeFrameCategory(IGuiHelper helper) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(AdsDrillAddon.MOD_ID, "textures/gui/jei/node_frame_recipe.png");

        int textureWidth = 256;
        int textureHeight = 256;

        background = helper.drawableBuilder(location, 0, 0, 170, 120) // Part of the texture to draw
                .setTextureSize(textureWidth, textureHeight) // Actual size of the PNG file
                .build();

        icon = helper.createDrawableItemStack(new ItemStack(AdsDrillBlocks.NODE_FRAME.get()));

        drill = helper.drawableBuilder(location, 176, 0, 32, 32)
                .setTextureSize(textureWidth, textureHeight)
                .build();

        arrow = helper.drawableBuilder(location, 176, 32, 24, 16)
                .setTextureSize(textureWidth, textureHeight)
                .build();
    }

    @Override
    public @NotNull RecipeType<NodeFrameRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("adsdrill.jei.category.node_assembly");
    }

    @Override
    public int getWidth() {
        return 170;
    }

    @Override
    public int getHeight() {
        return 120;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull NodeFrameRecipe recipe, @NotNull IFocusGroup focuses) {
        // Data input slots (3x3 grid)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                builder.addSlot(RecipeIngredientRole.INPUT, 18 + col * 18, 50 + row * 18)
                        .addItemStack(new ItemStack(AdsDrillItems.UNFINISHED_NODE_DATA.get()))
                        .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("adsdrill.jei.tooltip.node_data")));
            }
        }

        // Stabilizer core slot
        builder.addSlot(RecipeIngredientRole.INPUT, 80, 28)
                .addItemStack(recipe.stabilizerCore())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("adsdrill.jei.tooltip.stabilizer_core")));

        // Catalyst slots
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 28)
                .addItemStack(recipe.catalyst1())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("adsdrill.jei.tooltip.catalyst")));
        builder.addSlot(RecipeIngredientRole.INPUT, 142, 28)
                .addItemStack(recipe.catalyst2())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("adsdrill.jei.tooltip.catalyst")));

        // Output slot
        builder.addSlot(RecipeIngredientRole.OUTPUT, 138, 77)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(@NotNull NodeFrameRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics, 0, 0);

        drill.draw(guiGraphics, 77, 0);
        arrow.draw(guiGraphics, 98, 81);

        Font font = Minecraft.getInstance().font;
        Component text = Component.translatable("adsdrill.jei.requires_drill");
        int x = 87;
        int y = 45;
        int maxWidth = 80;
        int color = 0x909090;

        guiGraphics.drawWordWrap(font, text, x, y, maxWidth, color);
    }

    public static List<NodeFrameRecipe> getRecipes() {
        List<NodeFrameRecipe> recipes = new ArrayList<>();
        recipes.add(NodeFrameRecipe.create(StabilizerCoreItem.Tier.BRASS, AdsDrillItems.CINNABAR.get(), AdsDrillItems.ULTRAMARINE.get()));
        recipes.add(NodeFrameRecipe.create(StabilizerCoreItem.Tier.STEEL, AdsDrillItems.THE_FOSSIL.get(), AdsDrillItems.IVORY_CRYSTAL.get()));
        recipes.add(NodeFrameRecipe.create(StabilizerCoreItem.Tier.NETHERITE, AdsDrillItems.KOH_I_NOOR.get(), AdsDrillItems.THUNDER_STONE.get()));
        return recipes;
    }
}