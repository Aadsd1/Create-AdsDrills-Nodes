package com.adsd.adsdrill.content.item;

import com.adsd.adsdrill.crafting.Quirk;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

    public class ArtificialNodeBlockItem extends BlockItem {
        public ArtificialNodeBlockItem(Block pBlock, Properties pProperties) {
            super(pBlock, pProperties);
        }

        @Override
        public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
            super.appendHoverText(stack, context, tooltip, flag);

            // 설명 키 변경
            tooltip.add(Component.translatable("tooltip.adsdrill.artificial_node.description").withStyle(ChatFormatting.GRAY));

            CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (data == null) {
                return;
            }
            CompoundTag nbt = data.copyTag();

            tooltip.add(Component.literal(""));

            // 매장량 (goggle 키 재사용)
            if (nbt.contains("CurrentYield")) {
                tooltip.add(Component.translatable("goggle.adsdrill.ore_node.yield", String.format("%,.0f", nbt.getFloat("CurrentYield")))
                        .withStyle(ChatFormatting.GRAY));
            }

            // 구성 (goggle 키 재사용)
            if (nbt.contains("Composition", 9)) {
                tooltip.add(Component.translatable("goggle.adsdrill.ore_node.composition").withStyle(ChatFormatting.DARK_GRAY));
                ListTag compositionList = nbt.getList("Composition", 10);
                for (int i = 0; i < Math.min(compositionList.size(), 3); i++) {
                    CompoundTag entryTag = compositionList.getCompound(i);
                    BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(entryTag.getString("Item")))
                            .ifPresent(item -> {
                                float ratio = entryTag.getFloat("Ratio");
                                tooltip.add(Component.literal("  - ")
                                        .append(item.getDescription().copy().withStyle(ChatFormatting.AQUA))
                                        .append(Component.literal(String.format(": %.1f%%", ratio * 100)).withStyle(ChatFormatting.DARK_AQUA)));
                            });
                }
                if (compositionList.size() > 3) {
                    tooltip.add(Component.literal("  ...").withStyle(ChatFormatting.DARK_GRAY));
                }
            }

            // 특성 (quirk.header 키 사용)
            if (nbt.contains("Quirks", 9)) {
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("adsdrill.quirk.header").withStyle(ChatFormatting.LIGHT_PURPLE));
                ListTag quirkList = nbt.getList("Quirks", 10);
                for (int i = 0; i < quirkList.size(); i++) {
                    try {
                        String quirkId = quirkList.getCompound(i).getString("id");
                        Quirk quirk = Quirk.valueOf(quirkId);
                        tooltip.add(Component.literal("  - ").append(quirk.getDisplayName().copy().withStyle(quirk.getTier().getColor())));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }