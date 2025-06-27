package com.yourname.mycreateaddon.content.kinetics.module.Frame; // 또는 원하는 경로

import com.yourname.mycreateaddon.content.kinetics.drill.DrillCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes; // Shapes 임포트
import net.minecraft.world.phys.shapes.VoxelShape; // VoxelShape 임포트

import java.util.HashSet;
import java.util.Set;
import com.simibubi.create.foundation.block.IBE; // IBE 임포트
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity; // BE 레지스트리 임포트
import net.minecraft.world.level.block.entity.BlockEntityType;



public class FrameModuleBlock extends Block implements IBE<FrameModuleBlockEntity> {

    protected static final VoxelShape SHAPE = Shapes.box(0.1875, 0.1875, 0.1875, 0.8125, 0.8125, 0.8125);

    public FrameModuleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    // --- 주변 블록 변경 감지 로직 추가 ---
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide()) {
            return;
        }
        // 이 블록에 연결된 코어를 찾아 구조 재검사를 요청합니다.
        findAndNotifyCore(level, pos);
    }

    /**
     * 이 블록에 연결된 코어를 찾아 구조 재검사를 요청하는 헬퍼 메서드.
     * @param level 월드
     * @param startPos 이 프레임 블록의 위치
     */
    public static void findAndNotifyCore(Level level, BlockPos startPos) {
        Set<BlockPos> visited = new HashSet<>();
        searchForCore(level, startPos, visited, 0);
    }

    private static void searchForCore(Level level, BlockPos currentPos, Set<BlockPos> visited, int depth) {
        // 탐색 범위 제한 및 무한 루프 방지
        if (depth > 64 || !visited.add(currentPos)) {
            return;
        }

        // 모든 방향으로 탐색
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = currentPos.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            BlockState neighborState = level.getBlockState(neighborPos);
            Block neighborBlock = neighborState.getBlock();

            // 만약 코어를 찾았다면, 구조 재검사를 요청하고 탐색을 종료합니다.
            if (level.getBlockEntity(neighborPos) instanceof DrillCoreBlockEntity core) {
                core.scheduleStructureCheck();
                // 코어를 여러 개 찾을 필요는 없으므로 여기서 멈춰도 되지만,
                // 만약을 위해 계속 탐색하여 연결된 모든 코어에 알릴 수 있습니다.
            }
            // 만약 다른 프레임이나 모듈 블록이라면, 계속해서 탐색을 이어갑니다.
            else if (neighborBlock instanceof FrameModuleBlock) { // TODO: 나중에 다른 모듈 블록도 추가
                searchForCore(level, neighborPos, visited, depth + 1);
            }
        }
    }
    // --- IBE 인터페이스 구현 메서드 추가 ---
    @Override
    public Class<FrameModuleBlockEntity> getBlockEntityClass() {
        return FrameModuleBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FrameModuleBlockEntity> getBlockEntityType() {
        return MyAddonBlockEntity.FRAME_MODULE.get();
    }

    // --- 나머지 메서드는 그대로 유지 ---
    @Override
    protected VoxelShape getShape(BlockState pState, net.minecraft.world.level.BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

}