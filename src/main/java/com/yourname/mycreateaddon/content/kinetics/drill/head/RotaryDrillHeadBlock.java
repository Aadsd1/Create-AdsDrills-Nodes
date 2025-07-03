package com.yourname.mycreateaddon.content.kinetics.drill.head;


import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlock;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.foundation.block.IBE;
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;


public class RotaryDrillHeadBlock extends DirectionalKineticBlock implements IDrillHead, IBE<RotaryDrillHeadBlockEntity>, IRotate {

    public RotaryDrillHeadBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onDrillTick(Level level, BlockPos headPos, BlockState headState, DrillCoreBlockEntity core) {
        if (level.isClientSide) return;

        float finalSpeed = core.getFinalSpeed();
        if (finalSpeed == 0) return;

        Direction facing = headState.getValue(FACING);
        BlockPos nodePos = headPos.relative(facing);

        if (level.getBlockEntity(nodePos) instanceof OreNodeBlockEntity nodeBE) {
            int miningAmount = (int) (Math.abs(finalSpeed) / 20f);

            if (miningAmount > 0) {
                ItemStack minedItem = nodeBE.applyMiningTick(miningAmount);

                // 받은 아이템이 비어있지 않다면,
                if (!minedItem.isEmpty()) {
                    // [올바른 로직]
                    // 채굴된 아이템을 버퍼에 직접 넣는 것이 아니라,
                    // 코어의 처리 시스템으로 즉시 보냅니다.
                    core.processMinedItem(minedItem);
                }
            }
        }
    }

    // --- [추가] IDrillHead의 새 메서드 구현 ---
    @Override
    public float getHeatGeneration() {
        // 기본 회전형 헤드는 틱당 0.25의 열을 발생시킵니다.
        return 0.25f;
    }

    @Override
    public float getCoolingRate() {
        // 자체적으로 틱당 0.05의 열을 식힙니다.
        return 0.05f;
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

    @Override
    public BlockEntityType<? extends RotaryDrillHeadBlockEntity> getBlockEntityType() {
        return MyAddonBlockEntity.ROTARY_DRILL_HEAD.get();
    }
}