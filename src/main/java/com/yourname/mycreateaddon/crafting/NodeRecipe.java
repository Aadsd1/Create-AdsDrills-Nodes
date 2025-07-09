package com.yourname.mycreateaddon.crafting;

import com.simibubi.create.AllItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import com.yourname.mycreateaddon.registry.MyAddonItems;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record NodeRecipe(
        List<Supplier<Item>> requiredItems,
        @Nullable Fluid requiredFluid,
        Map<Supplier<Item>, Float> minimumRatios,
        ItemStack output,
        float chance,
        float consumptionMultiplier
) {
    // 여기에 레시피 매니저를 만들 수 있습니다.
    public static final List<NodeRecipe> RECIPES = new ArrayList<>();

    public static void registerRecipes() {
        // 예시 1: 강철 레시피
        RECIPES.add(new NodeRecipe(
                List.of(()->Items.RAW_IRON, ()->Items.COAL),
                null, // 유체 필요 없음
                Map.of(()->Items.RAW_IRON, 0.1f, ()->Items.COAL, 0.1f),
                new ItemStack(MyAddonItems.RAW_STEEL_CHUNK.get()),
                0.05f, // 5% 확률
                2.0f   // 매장량 2배 소모
        ));

        RECIPES.add(new NodeRecipe(
                List.of(()->Items.RAW_COPPER, ()->Items.REDSTONE),
                Fluids.WATER, // 물이 반드시 있어야 함
                Map.of(()->Items.RAW_COPPER, 0.1f, ()->Items.REDSTONE, 0.1f),
                new ItemStack(MyAddonItems.THUNDER_STONE.get()),
                0.05f, // 8% 확률
                2.0f
        ));

        RECIPES.add(new NodeRecipe(
                List.of(()->Items.QUARTZ, ()->Items.COAL),
                null,
                Map.of(()->Items.QUARTZ, 0.1f,()-> Items.COAL, 0.1f),
                new ItemStack(MyAddonItems.THE_FOSSIL.get()),
                0.05f,
                2.0f
        ));

        RECIPES.add(new NodeRecipe(
                List.of(()->Items.EMERALD, ()->Items.RAW_GOLD),
                null,
                Map.of(()->Items.EMERALD, 0.1f,()-> Items.RAW_GOLD, 0.1f),
                new ItemStack(MyAddonItems.SILKY_JEWEL.get()),
                0.05f,
                2.0f
        ));

        RECIPES.add(new NodeRecipe(
                List.of(()->Items.LAPIS_LAZULI, ()->Items.RAW_IRON),
                Fluids.WATER,
                Map.of(()->Items.LAPIS_LAZULI, 0.1f, ()->Items.RAW_IRON, 0.1f),
                new ItemStack(MyAddonItems.ULTRAMARINE.get()),
                0.05f,
                2.0f
        ));

        RECIPES.add(new NodeRecipe(
                List.of(()->Items.RAW_GOLD, ()->Items.RAW_COPPER),
                null,
                Map.of(()->Items.RAW_GOLD, 0.1f, ()->Items.RAW_COPPER, 0.1f),
                new ItemStack(MyAddonItems.RAW_ROSE_GOLD_CHUNK.get()),
                0.05f,
                2.0f
        ));

        RECIPES.add(new NodeRecipe(
                List.of(()->Items.QUARTZ, ()->Items.DIAMOND),
                null,
                Map.of(()->Items.QUARTZ, 0.1f, ()->Items.DIAMOND, 0.1f),
                new ItemStack(MyAddonItems.IVORY_CRYSTAL.get()),
                0.05f,
                2.0f
        ));

        RECIPES.add(new NodeRecipe(
                List.of(()->Items.REDSTONE, ()->Items.QUARTZ),
                Fluids.LAVA,
                Map.of(()->Items.REDSTONE, 0.1f, ()->Items.QUARTZ, 0.1f),
                new ItemStack(MyAddonItems.CINNABAR.get()),
                0.05f,
                2.0f
        ));

        RECIPES.add(new NodeRecipe(
                List.of(()->Items.DIAMOND, ()->Items.EMERALD),
                Fluids.LAVA,
                Map.of(()->Items.DIAMOND, 0.1f, ()->Items.EMERALD, 0.1f),
                new ItemStack(MyAddonItems.KOH_I_NOOR.get()),
                0.05f,
                2.0f
        ));

        RECIPES.add(new NodeRecipe(
                List.of(()->Items.REDSTONE, AllItems.RAW_ZINC),
                Fluids.WATER,
                Map.of(()->Items.REDSTONE, 0.1f, AllItems.RAW_ZINC, 0.1f),
                new ItemStack(MyAddonItems.XOMV.get()),
                0.05f,
                2.0f
        ));

    }
}