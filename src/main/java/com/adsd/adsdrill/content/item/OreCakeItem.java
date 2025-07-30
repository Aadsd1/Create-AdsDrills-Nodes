package com.adsd.adsdrill.content.item;

import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlock;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OreCakeItem extends Item {

    public OreCakeItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        // 서버에서만 로직 실행
        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }

        // 클릭한 블록이 OreNodeBlock 또는 그 자식 클래스인지 확인
        if (level.getBlockState(pos).getBlock() instanceof OreNodeBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof OreNodeBlockEntity nodeBE) {

                // BE의 매장량 회복 메서드를 호출하고, 성공 여부를 반환받음
                if (nodeBE.restoreYield(AdsDrillConfigs.SERVER.nodeRestorativeYieldAmount.get())) {
                    // 성공 시 효과 재생
                    level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 1.0f, 1.5f);
                    if (level instanceof ServerLevel serverLevel) {
                        BlockState particleState = be.getBlockState();
                        BlockParticleOption particleOption = new BlockParticleOption(ParticleTypes.BLOCK, particleState);
                        serverLevel.sendParticles(particleOption,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                20, 0.5, 0.5, 0.5, 0.1);
                    }

                    // 아이템 소모
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                } else {
                    // 실패 시 (이미 가득 참) 플레이어에게 메시지 표시
                    player.displayClientMessage(Component.translatable("message.adsdrill.restorative.full").withStyle(ChatFormatting.YELLOW), true);
                    return InteractionResult.FAIL;
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.adsdrill.node_restorative.description").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("tooltip.adsdrill.node_restorative.effect",
                        String.format("%,d", AdsDrillConfigs.SERVER.nodeRestorativeYieldAmount.get()))
                .withStyle(ChatFormatting.GOLD));
    }
}
