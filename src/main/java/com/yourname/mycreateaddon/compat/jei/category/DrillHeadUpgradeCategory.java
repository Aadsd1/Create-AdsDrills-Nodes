package com.yourname.mycreateaddon.compat.jei.category;


import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.compat.jei.recipe.DrillHeadUpgradeRecipe;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DrillHeadUpgradeCategory implements IRecipeCategory<DrillHeadUpgradeRecipe> {

    public static final RecipeType<DrillHeadUpgradeRecipe> TYPE = RecipeType.create(MyCreateAddon.MOD_ID, "drill_head_upgrading", DrillHeadUpgradeRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable plus;
    private final IDrawable arrow;

    public DrillHeadUpgradeCategory(IGuiHelper helper) {
        int textureWidth = 256;
        int textureHeight = 256;

        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "textures/gui/jei/module_upgrade.png");
        background = helper.drawableBuilder(location, 0, 0, 120, 50).setTextureSize(textureWidth,textureHeight).build();
        icon = helper.createDrawableItemStack(new ItemStack(MyAddonBlocks.IRON_ROTARY_DRILL_HEAD.get()));
        plus = helper.drawableBuilder(location, 128, 0, 16, 16).setTextureSize(textureWidth, textureHeight).build();
        arrow = helper.drawableBuilder(location, 128, 16, 24, 16).setTextureSize(textureWidth, textureHeight).build();
    }

    @Override public @NotNull RecipeType<DrillHeadUpgradeRecipe> getRecipeType() { return TYPE; }
    @Override public @NotNull Component getTitle() { return Component.translatable("mycreateaddon.jei.category.drill_upgrading"); }
    @Override public IDrawable getIcon() { return icon; }

    @Override public int getWidth() { return 120; }
    @Override public int getHeight() { return 50; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DrillHeadUpgradeRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 12, 17).addItemStack(recipe.inputDrill());
        builder.addSlot(RecipeIngredientRole.INPUT, 48, 17).addItemStack(recipe.upgradeItem());
        // 결과물은 아이템이 아닌 텍스트이므로 슬롯을 추가하지 않습니다.
    }

    @Override
    public void draw(DrillHeadUpgradeRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics, 0, 0);
        plus.draw(guiGraphics, 31, 17);
        arrow.draw(guiGraphics, 66, 17);
        guiGraphics.drawString(Minecraft.getInstance().font, recipe.outputDescription(), 90, 19, 0x404040, false);
    }

    public static List<DrillHeadUpgradeRecipe> getRecipes() {
        return List.of(
                new DrillHeadUpgradeRecipe(
                        new ItemStack(MyAddonBlocks.IRON_ROTARY_DRILL_HEAD.get()),
                        new ItemStack(MyAddonItems.ROSE_GOLD.get()),
                        Component.translatable("mycreateaddon.jei.fortune_1")
                ),
                new DrillHeadUpgradeRecipe(
                        new ItemStack(MyAddonBlocks.DIAMOND_ROTARY_DRILL_HEAD.get()),
                        new ItemStack(MyAddonItems.ROSE_GOLD.get()),
                        Component.translatable("mycreateaddon.jei.fortune_up_to_3")
                ),
                new DrillHeadUpgradeRecipe(
                        new ItemStack(MyAddonBlocks.NETHERITE_ROTARY_DRILL_HEAD.get()),
                        new ItemStack(MyAddonItems.SILKY_JEWEL.get()),
                        Component.translatable("mycreateaddon.jei.silk_touch")
                )
        );
    }
}