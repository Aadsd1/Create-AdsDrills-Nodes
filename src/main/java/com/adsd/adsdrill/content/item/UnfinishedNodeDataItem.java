package com.adsd.adsdrill.content.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

// 2단계에서 NBT 관련 로직, 툴팁 등을 추가할 예정
public class UnfinishedNodeDataItem extends Item {
    public UnfinishedNodeDataItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        // [핵심 수정] stack.get()의 결과를 null 체크로 처리합니다.
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return; // 커스텀 데이터가 없으면 툴팁을 추가하지 않고 종료
        }

        CompoundTag tag = customData.copyTag(); // 실제 NBT 태그를 가져옵니다.

        if (tag.contains("Yield")) {
            tooltip.add(Component.translatable("goggle.adsdrill.ore_node.yield", String.format("%,.0f", tag.getFloat("Yield")))
                    .withStyle(ChatFormatting.GRAY));
        }

        if (tag.contains("Composition", 9)) { // 9 = ListTag
            tooltip.add(Component.literal(""));
            tooltip.add(Component.translatable("goggle.adsdrill.ore_node.composition").withStyle(ChatFormatting.DARK_GRAY));
            ListTag compositionList = tag.getList("Composition", 10); // 10 = CompoundTag
            for (int i = 0; i < compositionList.size(); i++) {
                CompoundTag entryTag = compositionList.getCompound(i);
                BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(entryTag.getString("Item")))
                        .ifPresent(item -> {
                            float ratio = entryTag.getFloat("Ratio");
                            tooltip.add(Component.literal("  - ")
                                    .append(item.getDescription().copy().withStyle(ChatFormatting.AQUA))
                                    .append(Component.literal(String.format(": %.1f%%", ratio * 100)).withStyle(ChatFormatting.DARK_AQUA)));
                        });
            }
        }
        if (tag.contains("FluidContent")) {
            FluidStack fluid = FluidStack.parse(Objects.requireNonNull(context.registries()), tag.getCompound("FluidContent")).orElse(FluidStack.EMPTY);
            if (!fluid.isEmpty()) {
                int capacity = tag.getInt("MaxFluidCapacity");
                tooltip.add(Component.literal("")); // 구분선
                tooltip.add(Component.translatable("adsdrill.fluid_content.header").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(fluid.getHoverName())
                        .append(Component.literal(String.format(" (%,d mb)", capacity)).withStyle(ChatFormatting.DARK_GRAY))
                );
            }
        }
    }
}