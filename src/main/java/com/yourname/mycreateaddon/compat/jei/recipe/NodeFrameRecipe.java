package com.yourname.mycreateaddon.compat.jei.recipe;


import com.yourname.mycreateaddon.content.item.StabilizerCoreItem;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import com.yourname.mycreateaddon.registry.MyAddonItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * JEI에 노드 프레임 레시피를 표시하기 위한 데이터 클래스입니다.
 */
public record NodeFrameRecipe(
        ItemStack stabilizerCore,
        ItemStack catalyst1,
        ItemStack catalyst2,
        List<ItemStack> dataInputs,
        ItemStack output
) {
    public static NodeFrameRecipe create(StabilizerCoreItem.Tier tier, Item catalyst1, Item catalyst2) {
        ItemStack core;
        switch (tier) {
            case BRASS -> core = new ItemStack(MyAddonItems.BRASS_STABILIZER_CORE.get());
            case STEEL -> core = new ItemStack(MyAddonItems.STEEL_STABILIZER_CORE.get());
            case NETHERITE -> core = new ItemStack(MyAddonItems.NETHERITE_STABILIZER_CORE.get());
            default -> throw new IllegalArgumentException("Unknown tier");
        }

        return new NodeFrameRecipe(
                core,
                new ItemStack(catalyst1),
                new ItemStack(catalyst2),
                List.of(new ItemStack(MyAddonItems.UNFINISHED_NODE_DATA.get())),
                new ItemStack(MyAddonBlocks.ARTIFICIAL_NODE.get())
        );
    }
}