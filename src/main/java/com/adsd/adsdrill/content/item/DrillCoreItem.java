package com.adsd.adsdrill.content.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlock;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlockEntity.Tier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DrillCoreItem extends BlockItem {
    public DrillCoreItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected BlockState getPlacementState(@NotNull BlockPlaceContext context) {
        BlockState stateToPlace = super.getPlacementState(context);
        if (stateToPlace == null) return null;

        ItemStack stack = context.getItemInHand();
        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);

        if (blockEntityData != null) {
            CompoundTag nbt = blockEntityData.copyTag();
            if (nbt.contains("CoreTier")) {
                try {
                    Tier tier = Tier.valueOf(nbt.getString("CoreTier"));
                    return stateToPlace.setValue(DrillCoreBlock.TIER, tier);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return stateToPlace;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.adsdrill.drill_core.description").withStyle(ChatFormatting.GRAY));

        Tier tier = Tier.BRASS; // 기본값
        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            CompoundTag nbt = blockEntityData.copyTag();
            if (nbt.contains("CoreTier")) {
                try {
                    tier = Tier.valueOf(nbt.getString("CoreTier"));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("tooltip.adsdrill.drill_core.stats_header").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.adsdrill.drill_core.stats.max_modules", tier.getMaxModules()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.adsdrill.drill_core.stats.speed_bonus", String.format("%.0f%%", (tier.getSpeedBonus() - 1.0f) * 100)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.adsdrill.drill_core.stats.base_stress", tier.getBaseStress()).withStyle(ChatFormatting.GOLD));
    }
}