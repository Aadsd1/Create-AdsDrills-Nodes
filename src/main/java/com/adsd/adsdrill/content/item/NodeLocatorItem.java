package com.adsd.adsdrill.content.item;

import com.adsd.adsdrill.content.kinetics.node.ArtificialNodeBlockEntity;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final Map<UUID, List<BlockPos>> IGNORED_NODES_MAP = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> IGNORE_TIMESTAMP_MAP = new ConcurrentHashMap<>();
    private static final long IGNORE_DURATION_MS = 10000; // 10초

    private final Tier tier;

    public NodeLocatorItem(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        UUID playerUUID = player.getUUID();
        long currentTime = System.currentTimeMillis();

        if (player.isShiftKeyDown()) {
            IGNORED_NODES_MAP.remove(playerUUID);
            IGNORE_TIMESTAMP_MAP.remove(playerUUID);

            CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (nbt.contains("TargetPos")) {
                BlockPos targetPos = NbtUtils.readBlockPos(nbt, "TargetPos").orElse(null);
                if (targetPos == null) {
                    player.displayClientMessage(Component.translatable("message.adsdrill.locator.no_target_stored").withStyle(ChatFormatting.YELLOW), true);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.5f, 0.7f);
                    return InteractionResultHolder.success(stack);
                }

                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.5f, 1.2f);

                switch (tier) {
                    case BRASS:
                        player.displayClientMessage(Component.translatable("message.adsdrill.locator.found.brass").withStyle(ChatFormatting.GREEN), true);
                        break;
                    case STEEL:
                        double distance = Math.sqrt(player.blockPosition().distSqr(targetPos));
                        player.displayClientMessage(Component.translatable("message.adsdrill.locator.found.steel", String.format("%.1f", distance)).withStyle(ChatFormatting.GREEN), true);
                        break;
                    case NETHERITE:
                        player.displayClientMessage(Component.translatable("message.adsdrill.locator.found.netherite", targetPos.getX(), targetPos.getY(), targetPos.getZ()).withStyle(ChatFormatting.GREEN), true);
                        break;
                }
            } else {
                player.displayClientMessage(Component.translatable("message.adsdrill.locator.no_target_stored").withStyle(ChatFormatting.YELLOW), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.5f, 0.7f);
            }

        }
        else {
            if (currentTime > IGNORE_TIMESTAMP_MAP.getOrDefault(playerUUID, 0L)) {
                IGNORED_NODES_MAP.remove(playerUUID);
            }

            ServerLevel serverLevel = (ServerLevel) level;
            BlockPos playerPos = player.blockPosition();
            player.getCooldowns().addCooldown(this, 40);

            Optional<BlockPos> nearestNodePos = findNearestNode(serverLevel, playerPos, stack, playerUUID);

            if (nearestNodePos.isPresent()) {
                BlockPos targetPos = nearestNodePos.get();
                IGNORED_NODES_MAP.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(targetPos);
                IGNORE_TIMESTAMP_MAP.put(playerUUID, currentTime + IGNORE_DURATION_MS);

                double distance = Math.sqrt(playerPos.distSqr(targetPos));
                CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                nbt.put("TargetPos", NbtUtils.writeBlockPos(targetPos));
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.5f);

                switch (tier) {
                    case BRASS -> player.displayClientMessage(Component.translatable("message.adsdrill.locator.found.brass").withStyle(ChatFormatting.GREEN), true);
                    case STEEL -> player.displayClientMessage(Component.translatable("message.adsdrill.locator.found.steel", String.format("%.1f", distance)).withStyle(ChatFormatting.GREEN), true);
                    case NETHERITE -> player.displayClientMessage(Component.translatable("message.adsdrill.locator.found.netherite", targetPos.getX(), targetPos.getY(), targetPos.getZ()).withStyle(ChatFormatting.GREEN), true);
                }
            } else {
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
    private Optional<BlockPos> findNearestNode(ServerLevel level, BlockPos center, ItemStack locatorStack, UUID playerUUID) {
        CompoundTag nbt = locatorStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        List<BlockPos> ignored = IGNORED_NODES_MAP.getOrDefault(playerUUID, Collections.emptyList());

        // 아이템 목록을 읽어옵니다.
        List<Item> targetOres = new ArrayList<>();
        if (nbt.contains("TargetOres", 9)) { // 9 = ListTag
            ListTag oreListNBT = nbt.getList("TargetOres", 8); // 8 = StringTag
            oreListNBT.forEach(tag -> {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(tag.getAsString()));
                if (item != Items.AIR) {
                    targetOres.add(item);
                }
            });
        }

        Fluid targetFluid = nbt.contains("TargetFluid") ?
                BuiltInRegistries.FLUID.get(ResourceLocation.parse(nbt.getString("TargetFluid"))) : Fluids.EMPTY;

        boolean hasOreTarget = !targetOres.isEmpty();
        boolean hasFluidTarget = (targetFluid != Fluids.EMPTY);

        return BlockPos.betweenClosedStream(
                        center.offset(-tier.scanRadius, -tier.scanRadius, -tier.scanRadius),
                        center.offset(tier.scanRadius, tier.scanRadius, tier.scanRadius)
                )
                .filter(pos -> !ignored.contains(pos))
                .filter(pos -> {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (!(be instanceof OreNodeBlockEntity nodeBE) || be instanceof ArtificialNodeBlockEntity) {
                        return false;
                    }

                    // 노드가 모든 타겟 광물을 포함하는지 확인합니다.
                    boolean oreMatch = true;
                    if (hasOreTarget) {
                        for (Item targetOre : targetOres) {
                            if (!nodeBE.getResourceComposition().containsKey(targetOre)) {
                                oreMatch = false; // 하나라도 없으면 즉시 실패
                                break;
                            }
                        }
                    }

                    boolean fluidMatch = !hasFluidTarget || nodeBE.getFluid().getFluid() == targetFluid;

                    return oreMatch && fluidMatch;
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
        boolean hasTarget = false;

        // 단일 태그 대신 목록을 읽고, 모든 항목을 툴팁에 표시합니다.
        if (nbt.contains("TargetOres", 9)) {
            ListTag oreListNBT = nbt.getList("TargetOres", 8);
            if (!oreListNBT.isEmpty()) {
                tooltip.add(Component.literal(""));
                hasTarget = true;
            }
            oreListNBT.forEach(tag -> {
                Item targetItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(tag.getAsString()));
                if (targetItem != Items.AIR) {
                    tooltip.add(Component.translatable("tooltip.adsdrill.node_locator.targeting_result", targetItem.getDescription().copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
                }
            });
        }

        if (nbt.contains("TargetFluid")) {
            Fluid targetFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(nbt.getString("TargetFluid")));
            if (targetFluid != Fluids.EMPTY) {
                if (!hasTarget) tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("tooltip.adsdrill.node_locator.targeting_result", targetFluid.getFluidType().getDescription().copy().withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
            }
        }

        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("tooltip.adsdrill.node_locator.usage").withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.translatable("tooltip.adsdrill.node_locator.usage.next").withStyle(ChatFormatting.DARK_GRAY));

        if (this.tier == Tier.NETHERITE) {
            tooltip.add(Component.translatable("tooltip.adsdrill.node_locator.tuning_info.combined").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}