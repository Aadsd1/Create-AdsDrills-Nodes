package com.adsd.adsdrill.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public record NodeRecipe(
        List<Item> requiredItems,
        @Nullable Fluid requiredFluid,
        Map<Item, Float> minimumRatios,
        ItemStack output,
        float chance,
        float consumptionMultiplier
) {
    // 이제 RECIPES 리스트는 설정 파일에서 로드될 것이므로, 여기서 초기화합니다.
    public static final List<NodeRecipe> RECIPES = new ArrayList<>();

}