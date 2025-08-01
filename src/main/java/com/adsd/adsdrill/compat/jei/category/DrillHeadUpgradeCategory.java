package com.adsd.adsdrill.compat.jei.category;


import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.compat.jei.recipe.DrillHeadUpgradeRecipe;
import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.registry.AdsDrillBlocks;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DrillHeadUpgradeCategory implements IRecipeCategory<DrillHeadUpgradeRecipe> {

    public static final RecipeType<DrillHeadUpgradeRecipe> TYPE = RecipeType.create(AdsDrillAddon.MOD_ID, "drill_head_upgrading", DrillHeadUpgradeRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable plus;
    private final IDrawable arrow;

    public DrillHeadUpgradeCategory(IGuiHelper helper) {
        int textureWidth = 256;
        int textureHeight = 256;

        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(AdsDrillAddon.MOD_ID, "textures/gui/jei/module_upgrade.png");
        background = helper.drawableBuilder(location, 0, 0, 120, 50).setTextureSize(textureWidth,textureHeight).build();
        icon = helper.createDrawableItemStack(new ItemStack(AdsDrillBlocks.IRON_ROTARY_DRILL_HEAD.get()));
        plus = helper.drawableBuilder(location, 128, 0, 16, 16).setTextureSize(textureWidth, textureHeight).build();
        arrow = helper.drawableBuilder(location, 128, 16, 24, 16).setTextureSize(textureWidth, textureHeight).build();
    }

    @Override public @NotNull RecipeType<DrillHeadUpgradeRecipe> getRecipeType() { return TYPE; }
    @Override public @NotNull Component getTitle() { return Component.translatable("adsdrill.jei.category.drill_upgrading"); }
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
        // 최종적으로 반환할 레시피 리스트를 생성합니다.
        List<DrillHeadUpgradeRecipe> recipes = new ArrayList<>();

        // --- 1. 행운(Fortune) 업그레이드 레시피 생성 ---

        // 설정 파일에서 행운 업그레이드 아이템 ID 목록을 가져옵니다.
        List<? extends String> fortuneItemIds = AdsDrillConfigs.SERVER.rotaryDrillFortuneItems.get();

        // 행운 레벨 I (보통 철 드릴 헤드를 예시로 사용)
        if (!fortuneItemIds.isEmpty()) {
            Item fortune1Item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(fortuneItemIds.getFirst()));
            if (fortune1Item != Items.AIR) {
                recipes.add(new DrillHeadUpgradeRecipe(
                        new ItemStack(AdsDrillBlocks.IRON_ROTARY_DRILL_HEAD.get()),
                        new ItemStack(fortune1Item),
                        Component.translatable("adsdrill.jei.fortune_1")
                ));
            }
        }

        // 행운 레벨 II & III (보통 다이아몬드 드릴 헤드를 예시로 사용)
        // 설정 파일에 2개 이상의 아이템이 정의되어 있으면, "최대 3단계까지"라는 텍스트로 묶어서 보여줍니다.
        if (fortuneItemIds.size() >= 2) {
            // 두 번째 아이템(행운 II)을 대표 아이콘으로 사용합니다.
            Item fortune2Item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(fortuneItemIds.get(1)));
            if (fortune2Item != Items.AIR) {
                recipes.add(new DrillHeadUpgradeRecipe(
                        new ItemStack(AdsDrillBlocks.DIAMOND_ROTARY_DRILL_HEAD.get()),
                        new ItemStack(fortune2Item),
                        Component.translatable("adsdrill.jei.fortune_up_to_3")
                ));
            }
        }

        // --- 2. 섬세한 손길(Silk Touch) 업그레이드 레시피 생성 ---

        // 설정 파일에서 섬세한 손길 업그레이드 아이템 ID를 가져옵니다.
        String silkTouchItemId = AdsDrillConfigs.SERVER.rotaryDrillSilkTouchItem.get();
        Item silkTouchItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(silkTouchItemId));

        if (silkTouchItem != Items.AIR) {
            // 보통 네더라이트 드릴 헤드를 예시로 사용합니다.
            recipes.add(new DrillHeadUpgradeRecipe(
                    new ItemStack(AdsDrillBlocks.NETHERITE_ROTARY_DRILL_HEAD.get()),
                    new ItemStack(silkTouchItem),
                    Component.translatable("adsdrill.jei.silk_touch")
            ));
        }

        return recipes;
    }
}