package com.adsd.adsdrill.content.item;

import com.adsd.adsdrill.content.kinetics.node.OreNodeBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NodeLocatorItem extends Item {

    public enum Tier {
        BRASS(8, ChatFormatting.GOLD),
        STEEL(32, ChatFormatting.AQUA),
        NETHERITE(64, ChatFormatting.DARK_PURPLE);

        public final int scanRadius;
        public final ChatFormatting color;

        Tier(int scanRadius, ChatFormatting color) {
            this.scanRadius = scanRadius;
            this.color = color;
        }
    }

    private final Tier tier;

    public NodeLocatorItem(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 모든 로직은 서버에서만 실행
        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        // --- 1. 쉬프트 + 우클릭 로직: 기존 정보 다시 표시 ---
        if (player.isShiftKeyDown()) {
            CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

            if (nbt.contains("TargetPos")) {
                BlockPos targetPos = NbtUtils.readBlockPos(nbt, "TargetPos").orElse(null);

                // NBT에 좌표가 있지만 유효하지 않은 경우 (드문 경우)
                if (targetPos == null) {
                    player.displayClientMessage(Component.translatable("message.adsdrill.locator.no_target_stored").withStyle(ChatFormatting.YELLOW), true);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.5f, 0.7f);
                    return InteractionResultHolder.success(stack);
                }

                // 정보 재확인 사운드
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.5f, 1.2f);

                // 각 티어에 맞는 메시지를 다시 표시
                switch (tier) {
                    case BRASS:
                        player.displayClientMessage(Component.translatable("message.adsdrill.locator.found.brass").withStyle(ChatFormatting.GREEN), true);
                        break;
                    case STEEL:
                        // 강철 티어는 현재 위치와의 거리를 다시 계산해서 보여줌
                        double distance = Math.sqrt(player.blockPosition().distSqr(targetPos));
                        player.displayClientMessage(Component.translatable("message.adsdrill.locator.found.steel", String.format("%.1f", distance)).withStyle(ChatFormatting.GREEN), true);
                        break;
                    case NETHERITE:
                        player.displayClientMessage(Component.translatable("message.adsdrill.locator.found.netherite", targetPos.getX(), targetPos.getY(), targetPos.getZ()).withStyle(ChatFormatting.GREEN), true);
                        break;
                }
            } else {
                // 저장된 타겟이 없을 경우
                player.displayClientMessage(Component.translatable("message.adsdrill.locator.no_target_stored").withStyle(ChatFormatting.YELLOW), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.5f, 0.7f);
            }

        }
        // --- 2. 일반 우클릭 로직: 새로 탐색 ---
        else {
            ServerLevel serverLevel = (ServerLevel) level;
            BlockPos playerPos = player.blockPosition();
            player.getCooldowns().addCooldown(this, 40); // 스팸 방지 쿨다운

            Optional<BlockPos> nearestNodePos = findNearestNode(serverLevel, playerPos, stack);

            if (nearestNodePos.isPresent()) {
                BlockPos targetPos = nearestNodePos.get();
                double distance = Math.sqrt(playerPos.distSqr(targetPos));
                CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                nbt.put("TargetPos", NbtUtils.writeBlockPos(targetPos));
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.5f);

                // 각 티어에 맞는 메시지 표시
                switch (tier) {
                    case BRASS -> player.displayClientMessage(Component.translatable("message.adsdrill.locator.found.brass").withStyle(ChatFormatting.GREEN), true);
                    case STEEL -> player.displayClientMessage(Component.translatable("message.adsdrill.locator.found.steel", String.format("%.1f", distance)).withStyle(ChatFormatting.GREEN), true);
                    case NETHERITE -> player.displayClientMessage(Component.translatable("message.adsdrill.locator.found.netherite", targetPos.getX(), targetPos.getY(), targetPos.getZ()).withStyle(ChatFormatting.GREEN), true);
                }
            } else {
                // 탐색 실패 시 NBT에서 좌표 제거
                CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                if (nbt.contains("TargetPos")) {
                    nbt.remove("TargetPos");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.5f, 0.8f);
                player.displayClientMessage(Component.translatable("message.adsdrill.locator.not_found", tier.scanRadius).withStyle(ChatFormatting.RED), true);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    private Optional<BlockPos> findNearestNode(ServerLevel level, BlockPos center, ItemStack locatorStack) {
        CompoundTag nbt = locatorStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        Item targetOreItem = nbt.contains("TargetOre") ?
                BuiltInRegistries.ITEM.get(ResourceLocation.parse(nbt.getString("TargetOre"))) : Items.AIR;

        boolean hasTarget = (targetOreItem != Items.AIR);

        return BlockPos.betweenClosedStream(
                        center.offset(-tier.scanRadius, -tier.scanRadius, -tier.scanRadius),
                        center.offset(tier.scanRadius, tier.scanRadius, tier.scanRadius)
                )
                .filter(pos -> {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (!(be instanceof OreNodeBlockEntity nodeBE)) {
                        return false; // OreNode가 아니면 실패
                    }

                    // 타겟이 없으면 모든 노드를 통과
                    if (!hasTarget) {
                        return true;
                    }

                    // 타겟이 있으면, 노드의 구성물에 해당 아이템이 포함되어 있는지 확인
                    return nodeBE.getResourceComposition().containsKey(targetOreItem);
                })
                .map(BlockPos::immutable)
                .min(Comparator.comparingDouble(pos -> pos.distSqr(center)));
    }


    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.adsdrill.node_locator.tier", Component.translatable("tooltip.adsdrill.node_locator.tier." + tier.name().toLowerCase()).withStyle(tier.color)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.adsdrill.node_locator.radius", tier.scanRadius).withStyle(ChatFormatting.GRAY));

        CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (nbt.contains("TargetOre")) {
            Item targetItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(nbt.getString("TargetOre")));
            if (targetItem != Items.AIR) {
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("tooltip.adsdrill.node_locator.targeting_result", targetItem.getDescription().copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
                if (flag.isAdvanced()) {
                    tooltip.add(Component.literal("  (ID: " + nbt.getString("TargetOre") + ")").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        }

        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("tooltip.adsdrill.node_locator.usage").withStyle(ChatFormatting.DARK_GRAY));

        // 네더라이트 티어일 때만 모루 강화 툴팁 추가
        if (this.tier == Tier.NETHERITE) {
            tooltip.add(Component.translatable("tooltip.adsdrill.node_locator.tuning_info").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}