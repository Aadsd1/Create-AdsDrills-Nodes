package com.yourname.mycreateaddon.content.kinetics.drill.head;


import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlock;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.foundation.block.IBE;
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class RotaryDrillHeadBlock extends DirectionalKineticBlock implements IDrillHead, IBE<RotaryDrillHeadBlockEntity>, IRotate {


    // [수정] 성능 값을 저장할 필드 추가
    private final float heatGeneration;
    private final float coolingRate;
    private final int miningLevel;

    private final float stressImpact; // [신규] 스트레스 필드 추가


    public RotaryDrillHeadBlock(Properties properties, float heatGeneration, float coolingRate, int miningLevel, float stressImpact) {
        super(properties);
        this.heatGeneration = heatGeneration;
        this.coolingRate = coolingRate;
        this.miningLevel = miningLevel;
        this.stressImpact = stressImpact; // [신규]
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

            int miningAmount = (int) (Math.abs(finalSpeed) / 20f);

            if (miningAmount > 0) {
                List<ItemStack> minedItems = core.mineNode(nodeBE, miningAmount);
                for (ItemStack minedItem : minedItems) {
                    if (!minedItem.isEmpty()) {
                        core.processMinedItem(minedItem);
                    }
                }

            }
        }
    }

    // [수정] IDrillHead의 메서드들이 필드 값을 반환하도록 변경
    @Override
    public float getHeatGeneration() {
        return this.heatGeneration;
    }

    @Override
    public float getCoolingRate() {
        return this.coolingRate;
    }

    // [신규] getStressImpact() 구현
    @Override
    public float getStressImpact() {
        return this.stressImpact;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, RotaryDrillHeadBlockEntity::updateCoreConnection);
        }
    }

    @Override
    protected void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide) return;

        withBlockEntityDo(level, pos, RotaryDrillHeadBlockEntity::updateCoreConnection);

        Direction facing = state.getValue(FACING);
        BlockPos corePos = pos.relative(facing.getOpposite());
        if (level.getBlockState(corePos).getBlock() instanceof DrillCoreBlock) {
            level.neighborChanged(corePos, this, pos);
        }
    }

    @Override
    public Class<RotaryDrillHeadBlockEntity> getBlockEntityClass() {
        return RotaryDrillHeadBlockEntity.class;
    }

    // [핵심 수정] 반환 타입을 ItemInteractionResult로 변경
    @Override
    protected @NotNull ItemInteractionResult useItemOn(
            @NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
            @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit
    ) {
        Item itemInHand = stack.getItem();

        // 손에 든 아이템이 금 주괴 또는 에메랄드일 때만 로직 실행
        if (itemInHand == Items.GOLD_INGOT || itemInHand == Items.EMERALD) {
            // 쉬프트 클릭이 아닐 때만 작동
            if (!player.isShiftKeyDown()) {
                if (!level.isClientSide) {
                    withBlockEntityDo(level, pos, be -> {
                        be.applyUpgrade(player, itemInHand);
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                    });
                }
                // [핵심 수정] 성공 시 반환 값 변경
                return ItemInteractionResult.SUCCESS;
            }
        }

        // 그 외의 경우는 상위 클래스의 동작에 맡김
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }


    @Override
    public BlockEntityType<? extends RotaryDrillHeadBlockEntity> getBlockEntityType() {
        return MyAddonBlockEntity.ROTARY_DRILL_HEAD.get();
    }
}