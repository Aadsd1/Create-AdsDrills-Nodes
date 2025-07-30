package com.adsd.adsdrill.content.item;


import com.adsd.adsdrill.content.kinetics.node.ArtificialNodeBlock;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NeutralizerItem extends Item {

    public NeutralizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockState(pos).getBlock() instanceof ArtificialNodeBlock) {
            return InteractionResult.FAIL;
        }
        if (level.getBlockState(pos).getBlock() instanceof OreNodeBlock) {
            if (!level.isClientSide) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 1.0f);

                RandomSource random = level.getRandom();
                for (int i = 0; i < 20; i++) {
                    level.addParticle(ParticleTypes.SMOKE,
                            pos.getX() + random.nextDouble(),
                            pos.getY() + random.nextDouble(),
                            pos.getZ() + random.nextDouble(),
                            0, 0, 0);
                }

                if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.adsdrill.ore_node_neutralizer.description").withStyle(ChatFormatting.GRAY));
    }
}