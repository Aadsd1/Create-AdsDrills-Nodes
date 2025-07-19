package com.adsd.adsdrill.content.kinetics.drill.head;


import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 모든 기계식 드릴 헤드 Block의 공통 로직을 담는 추상 클래스입니다.
 */
public abstract class AbstractDrillHeadBlock extends DirectionalKineticBlock implements IDrillHead, IRotate, IBE<AbstractDrillHeadBlockEntity> {
    public AbstractDrillHeadBlock(Properties properties) {
        super(properties);
    }

    private void updateConnection(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof AbstractDrillHeadBlockEntity be) {
                be.updateCoreConnection();
            }
        }
    }
    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder builder) {
        BlockEntity be = builder.getParameter(LootContextParams.BLOCK_ENTITY);

        if (be instanceof AbstractDrillHeadBlockEntity drillHeadBE) {
            ItemStack dropStack = new ItemStack(this);
            // BlockEntity의 데이터를 아이템 스택에 저장합니다.
            var level = builder.getLevel();
            drillHeadBE.saveToItem(dropStack, level.registryAccess());
            return Collections.singletonList(dropStack);
        }

        return super.getDrops(state, builder);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        ItemStack stack = new ItemStack(this);
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof AbstractDrillHeadBlockEntity drillHeadBE) {
            if (drillHeadBE.hasLevel()) {
                drillHeadBE.saveToItem(stack, Objects.requireNonNull(drillHeadBE.getLevel()).registryAccess());
            }
        }
        return stack;
    }
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        updateConnection(level, pos);
    }

    @Override
    protected void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        updateConnection(level, pos);

        // 코어 블록에게도 변경 사항을 알려 구조 재검사를 유도
        Direction facing = state.getValue(FACING);
        BlockPos corePos = pos.relative(facing.getOpposite());
        if (level.getBlockState(corePos).getBlock() instanceof DrillCoreBlock) {
            level.neighborChanged(corePos, this, pos);
        }
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }
    //자식 클래스가 자신의 BE 타입을 반환하도록 강제하는 추상 메서드
    @Override
    public abstract BlockEntityType<? extends AbstractDrillHeadBlockEntity> getBlockEntityType();

    //공통 클래스 타입을 반환
    @Override
    public Class<AbstractDrillHeadBlockEntity> getBlockEntityClass() {
        return AbstractDrillHeadBlockEntity.class;
    }
}