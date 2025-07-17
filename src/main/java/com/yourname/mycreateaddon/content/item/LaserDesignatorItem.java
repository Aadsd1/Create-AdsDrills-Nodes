package com.yourname.mycreateaddon.content.item;



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
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LaserDesignatorItem extends Item {
    public LaserDesignatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        var player = context.getPlayer();

        if (player == null) return InteractionResult.PASS;

        // 먼저, 클릭한 블록이 레이저 헤드인지 확인합니다.
        if (level.getBlockEntity(clickedPos) instanceof LaserDrillHeadBlockEntity laserBE) {

            // 1. Shift + 우클릭: 지정기를 이 헤드에 연결합니다.
            if (player.isShiftKeyDown()) {
                CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                nbt.put("HeadPos", NbtUtils.writeBlockPos(clickedPos));
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("mycreateaddon.node_designator.linked").withStyle(ChatFormatting.GREEN), true);
                }
            }
            // 2. 그냥 우클릭: 이 헤드의 모드를 변경합니다.
            else {
                if (!level.isClientSide) {
                    laserBE.cycleMode();
                    // 변경된 모드 이름을 플레이어에게 알려줍니다.
                    player.displayClientMessage(laserBE.getMode().getDisplayName(), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 클릭한 블록이 광물 노드인 경우 (기존 타겟 지정 로직)
        if (level.getBlockState(clickedPos).getBlock() instanceof OreNodeBlock) {
            CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (!nbt.contains("HeadPos")) {
                if (!level.isClientSide) player.displayClientMessage(Component.translatable("mycreateaddon.node_designator.not_linked").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            BlockPos headPos = NbtUtils.readBlockPos(nbt, "HeadPos").orElse(BlockPos.ZERO);
            if (level.getBlockEntity(headPos) instanceof LaserDrillHeadBlockEntity linkedLaserBE) {
                if (!level.isClientSide) {
                    linkedLaserBE.toggleTarget(clickedPos, player);
                }
                return InteractionResult.SUCCESS;
            }
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
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