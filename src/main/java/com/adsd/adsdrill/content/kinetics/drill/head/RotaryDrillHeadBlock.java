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
import net.minecraft.world.level.block.entity.BlockEntity;
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
        Direction facing = headState.getValue(FACING);
        BlockPos nodePos = headPos.relative(facing);
        BlockEntity be = level.getBlockEntity(nodePos);

        if (!(be instanceof OreNodeBlockEntity nodeBE)) {
            // 노드가 없다면, 이전에 채굴 중이던 노드가 있을 경우를 대비해 채굴 중지 신호를 보냅니다.
            BlockEntity lastBE = level.getBlockEntity(nodePos);
            if (lastBE instanceof OreNodeBlockEntity lastNodeBE) {
                lastNodeBE.stopMining();
            }
            return;
        }

        // 드릴 속도가 0이면 채굴 중지
        if (finalSpeed == 0) {
            nodeBE.stopMining();
            return;
        }

        // 채굴 시작을 노드에 알립니다.
        nodeBE.setMiningDrill(headPos);

        if (nodeBE.isCracked() && this.miningLevel < 2) {
            nodeBE.stopMining(); // 조건 안맞으면 채굴 중지
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
            List<ItemStack> minedItems = core.mineNode(nodeBE, miningAmount, fortune, silkTouch, headPos);
            for (ItemStack minedItem : minedItems) {
                if (!minedItem.isEmpty()) {
                    core.processMinedItem(minedItem);
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