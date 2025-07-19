package com.adsd.adsdrill.content.kinetics.drill.head;


import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlockEntity;
import com.adsd.adsdrill.registry.AdsDrillItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.adsd.adsdrill.registry.AdsDrillBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RotaryDrillHeadBlock extends AbstractDrillHeadBlock {

    private final float heatGeneration;
    private final float coolingRate;
    private final int miningLevel;
    private final float stressImpact;

    public RotaryDrillHeadBlock(Properties properties, float heatGeneration, float coolingRate, int miningLevel, float stressImpact) {
        super(properties);
        this.heatGeneration = heatGeneration;
        this.coolingRate = coolingRate;
        this.miningLevel = miningLevel;
        this.stressImpact = stressImpact;
    }

    @Override
    public void onDrillTick(Level level, BlockPos headPos, BlockState headState, DrillCoreBlockEntity core) {
        if (level.isClientSide) return;
        float finalSpeed = core.getFinalSpeed();
        if (finalSpeed == 0) return;

        Direction facing = headState.getValue(FACING);
        BlockPos nodePos = headPos.relative(facing);

        if (level.getBlockEntity(nodePos) instanceof OreNodeBlockEntity nodeBE) {
            if (nodeBE.isCracked() && this.miningLevel < 2) {
                return;
            }
            int miningAmount = (int) (Math.abs(finalSpeed) / AdsDrillConfigs.SERVER.rotarySpeedDivisor.get());
            if (miningAmount > 0) {
                int fortune = 0;
                boolean silkTouch = false;
                if (level.getBlockEntity(headPos) instanceof RotaryDrillHeadBlockEntity headBE) {
                    fortune = headBE.getFortuneLevel();
                    silkTouch = headBE.hasSilkTouch();
                }
                List<ItemStack> minedItems = core.mineNode(nodeBE, miningAmount, fortune, silkTouch);
                for (ItemStack minedItem : minedItems) {
                    if (!minedItem.isEmpty()) {
                        core.processMinedItem(minedItem);
                    }
                }
            }
        }
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        Item itemInHand = stack.getItem();
        if (itemInHand == AdsDrillItems.ROSE_GOLD.get() || itemInHand == AdsDrillItems.SILKY_JEWEL.get()) {
            if (!player.isShiftKeyDown()) {
                if (!level.isClientSide) {
                    withBlockEntityDo(level, pos, be -> {
                        if (be instanceof RotaryDrillHeadBlockEntity rotaryBE) {
                            rotaryBE.applyUpgrade(player, itemInHand);
                            if (!player.getAbilities().instabuild) {
                                stack.shrink(1);
                            }
                        }
                    });
                }
                return ItemInteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public float getHeatGeneration() { return this.heatGeneration; }
    @Override
    public float getCoolingRate() { return this.coolingRate; }
    @Override
    public float getStressImpact() { return this.stressImpact; }



    @Override
    public BlockEntityType<? extends AbstractDrillHeadBlockEntity> getBlockEntityType() {
        return AdsDrillBlockEntity.ROTARY_DRILL_HEAD.get();
    }
}