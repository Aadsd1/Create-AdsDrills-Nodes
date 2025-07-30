package com.adsd.adsdrill.content.item;


import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 모든 안정화 코어 아이템의 부모 클래스입니다.
 * 코어의 등급(Tier) 정보를 가지고 있으며, 공통적인 툴팁을 제공합니다.
 */
public class StabilizerCoreItem extends Item {

    // 코어의 등급을 나타내는 열거형
    public enum Tier {
        BRASS(ChatFormatting.GOLD),
        STEEL(ChatFormatting.AQUA),
        NETHERITE(ChatFormatting.DARK_PURPLE);

        private final ChatFormatting color;

        Tier(ChatFormatting color) {
            this.color = color;
        }

        public ChatFormatting getColor() {
            return color;
        }
    }

    private final Tier tier;

    public StabilizerCoreItem(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    public Tier getTier() {
        return this.tier;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        // 코어의 역할을 설명하는 툴팁
        tooltip.add(Component.translatable("tooltip.adsdrill.stabilizer_core.description").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("")); // 빈 줄 추가

        // 등급별로 다른 설명 추가
        switch (this.tier) {
            case BRASS:
                tooltip.add(Component.translatable("tooltip.adsdrill.stabilizer_core.brass.line1").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.adsdrill.stabilizer_core.brass.line2").withStyle(ChatFormatting.DARK_GRAY));
                break;
            case STEEL:
                tooltip.add(Component.translatable("tooltip.adsdrill.stabilizer_core.steel.line1").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.adsdrill.stabilizer_core.steel.line2").withStyle(ChatFormatting.DARK_GRAY));
                break;
            case NETHERITE:
                tooltip.add(Component.translatable("tooltip.adsdrill.stabilizer_core.netherite.line1").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.adsdrill.stabilizer_core.netherite.line2").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD));
                break;
        }
    }
}