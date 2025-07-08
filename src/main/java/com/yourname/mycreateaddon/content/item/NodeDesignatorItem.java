package com.yourname.mycreateaddon.content.item;



import com.yourname.mycreateaddon.content.kinetics.drill.head.LaserDrillHeadBlock;
import com.yourname.mycreateaddon.content.kinetics.drill.head.LaserDrillHeadBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NodeDesignatorItem extends Item {
    public NodeDesignatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        var player = context.getPlayer();

        if (player == null) return InteractionResult.PASS;

        // 1. 레이저 헤드를 쉬프트+우클릭: 헤드 연결
        if (player.isShiftKeyDown() && level.getBlockState(clickedPos).getBlock() instanceof LaserDrillHeadBlock) {
            CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            nbt.put("HeadPos", NbtUtils.writeBlockPos(clickedPos));
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("mycreateaddon.node_designator.linked").withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResult.SUCCESS;
        }

        // 2. 광물 노드를 우클릭: 타겟 지정/해제
        if (level.getBlockState(clickedPos).getBlock() instanceof OreNodeBlock) {
            CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (!nbt.contains("HeadPos")) {
                if (!level.isClientSide) player.displayClientMessage(Component.translatable("mycreateaddon.node_designator.not_linked").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            BlockPos headPos = NbtUtils.readBlockPos(nbt, "HeadPos").orElse(BlockPos.ZERO);
            if (level.getBlockEntity(headPos) instanceof LaserDrillHeadBlockEntity laserBE) {
                if (!level.isClientSide) {
                    laserBE.toggleTarget(clickedPos, player);
                }
                return InteractionResult.SUCCESS;
            }
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (nbt.contains("HeadPos")) {
            BlockPos headPos = NbtUtils.readBlockPos(nbt, "HeadPos").orElse(BlockPos.ZERO);
            tooltip.add(Component.translatable("mycreateaddon.node_designator.linked_to", headPos.getX(), headPos.getY(), headPos.getZ()).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("mycreateaddon.node_designator.tooltip").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}