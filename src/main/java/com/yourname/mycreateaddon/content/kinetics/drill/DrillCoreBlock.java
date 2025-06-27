package com.yourname.mycreateaddon.content.kinetics.drill;


import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity; // BE 레지스트리 임포트
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType; // BlockEntityType 임포트
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.pathfinder.PathComputationType;



public class DrillCoreBlock extends DirectionalKineticBlock implements IBE<DrillCoreBlockEntity> {
    public DrillCoreBlock(Properties properties) {
        super(properties);
    }
    @Override
    public Class<DrillCoreBlockEntity> getBlockEntityClass() {
        return DrillCoreBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DrillCoreBlockEntity> getBlockEntityType() {
        return MyAddonBlockEntity.DRILL_CORE.get();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide()) {
            withBlockEntityDo(level, pos, DrillCoreBlockEntity::scheduleStructureCheck);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // 블록이 완전히 다른 블록으로 교체될 때(파괴될 때)만 로직을 실행합니다.
        // isMoving 플래그는 Create의 이동 장치(Contraption)에 의해 이동될 때 true가 됩니다.
        if (state.hasBlockEntity() && (!state.is(newState.getBlock()) || !newState.hasBlockEntity())) {
            withBlockEntityDo(level, pos, DrillCoreBlockEntity::onBroken);
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}