package com.adsd.adsdrill.content.item;


import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TooltipBlockItem extends BlockItem {

    private final String descriptionId;

    public TooltipBlockItem(Block block, Properties properties, String descriptionId) {
        super(block, properties);
        this.descriptionId = descriptionId;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(this.descriptionId).withStyle(ChatFormatting.GRAY));
    }
}