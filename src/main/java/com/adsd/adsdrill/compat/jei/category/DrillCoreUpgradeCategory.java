package com.adsd.adsdrill.compat.jei.category;


import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.compat.jei.recipe.DrillCoreUpgradeRecipe;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlockEntity;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DrillCoreUpgradeCategory implements IRecipeCategory<DrillCoreUpgradeRecipe> {

    public static final RecipeType<DrillCoreUpgradeRecipe> TYPE = RecipeType.create(AdsDrillAddon.MOD_ID, "drill_core_upgrading", DrillCoreUpgradeRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable plus;
    private final IDrawable arrow;

    public DrillCoreUpgradeCategory(IGuiHelper helper) {
        int textureWidth = 256;
        int textureHeight = 256;

        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(AdsDrillAddon.MOD_ID, "textures/gui/jei/module_upgrade.png");
        background = helper.drawableBuilder(location, 0, 0, 120, 50).setTextureSize(textureWidth,textureHeight).build();
        icon = helper.createDrawableItemStack(new ItemStack(AdsDrillBlocks.DRILL_CORE.get()));
        plus = helper.drawableBuilder(location, 128, 0, 16, 16).setTextureSize(textureWidth, textureHeight).build();
        arrow = helper.drawableBuilder(location, 128, 16, 24, 16).setTextureSize(textureWidth, textureHeight).build();
    }

    @Override public @NotNull RecipeType<DrillCoreUpgradeRecipe> getRecipeType() { return TYPE; }
    @Override public @NotNull Component getTitle() { return Component.translatable("adsdrill.jei.category.drill_upgrading"); }
    @Override public IDrawable getIcon() { return icon; }

    @Override public int getWidth() { return 120; }
    @Override public int getHeight() { return 50; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DrillCoreUpgradeRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 12, 17).addItemStack(recipe.inputCore());
        builder.addSlot(RecipeIngredientRole.INPUT, 48, 17).addItemStack(recipe.upgradeKit());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 17).addItemStack(recipe.outputCore());
    }

    @Override
    public void draw(@NotNull DrillCoreUpgradeRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics, 0, 0);
        plus.draw(guiGraphics, 31, 17);
        arrow.draw(guiGraphics, 66, 17);
    }

    public static List<DrillCoreUpgradeRecipe> getRecipes() {
        return List.of(
                new DrillCoreUpgradeRecipe(
                        getTieredCore(DrillCoreBlockEntity.Tier.BRASS),
                        new ItemStack(AdsDrillItems.DRILL_CORE_STEEL_UPGRADE.get()),
                        getTieredCore(DrillCoreBlockEntity.Tier.STEEL)
                ),
                new DrillCoreUpgradeRecipe(
                        getTieredCore(DrillCoreBlockEntity.Tier.STEEL),
                        new ItemStack(AdsDrillItems.DRILL_CORE_NETHERITE_UPGRADE.get()),
                        getTieredCore(DrillCoreBlockEntity.Tier.NETHERITE)
                )
        );
    }

    private static ItemStack getTieredCore(DrillCoreBlockEntity.Tier tier) {
        ItemStack stack = new ItemStack(AdsDrillBlocks.DRILL_CORE.get());
        CompoundTag nbt = new CompoundTag();
        nbt.putString("CoreTier", tier.name());
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(nbt));
        return stack;
    }
}
