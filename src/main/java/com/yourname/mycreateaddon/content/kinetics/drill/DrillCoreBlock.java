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
    // --- IBE 인터페이스 구현 메서드 ---
    @Override
    public Class<DrillCoreBlockEntity> getBlockEntityClass() {
        return DrillCoreBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DrillCoreBlockEntity> getBlockEntityType() {
        return MyAddonBlockEntity.DRILL_CORE.get();
    }
//    @Override
//    public BlockState getStateForPlacement(BlockPlaceContext context) {
//        // 플레이어가 바라보는 6방향 중 가장 가까운 방향으로 블록을 설치합니다.
//        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
//    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        // 블록이 향하는 방향(FACING)의 축을 회전축으로 사용합니다.
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        // 블록의 '뒤쪽' 면에서만 동력을 받도록 합니다.
        return face == state.getValue(FACING);
    }

    // --- 주변 블록 변경 감지 (수정 필요 없음) ---
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
}