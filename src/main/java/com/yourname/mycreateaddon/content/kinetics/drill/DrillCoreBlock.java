package com.yourname.mycreateaddon.content.kinetics.drill;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock; // Create 상속 확인
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity; // BE 레지스트리 임포트
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape; // RenderShape 임포트
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType; // BlockEntityType 임포트
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;


// EntityBlock 인터페이스 구현 명시 (KineticBlock 에 이미 있을 수 있지만 명확성을 위해)
public class DrillCoreBlock extends RotatedPillarKineticBlock implements IBE<DrillCoreBlockEntity> {

    protected static final VoxelShape SHAPE = Shapes.box(0.0625, 0.0625, 0.0625, 0.9375, 0.9375, 0.9375); // 1/16 ~ 15/16

    public DrillCoreBlock(Properties properties) {
        super(properties);
        // 기본 BlockState 설정
    }
    // --- IBE 인터페이스 구현 메서드 ---
    @Override
    public Class<DrillCoreBlockEntity> getBlockEntityClass() {
        return DrillCoreBlockEntity.class; // 실제 BE 클래스 반환
    }


    @Override
    public BlockEntityType<? extends DrillCoreBlockEntity> getBlockEntityType() {
        // 등록된 BE 타입 반환
        return MyAddonBlockEntity.DRILL_CORE.get(); // <<< 이 메서드 다시 추가!
    }


    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS) && face.getAxisDirection() == Direction.AxisDirection.NEGATIVE;
    }

    // --- 상속된 메서드 ---
    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    // 블록의 충돌 및 렌더링 모양 정의
    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }


    // --- 주변 블록 변경 감지 ---
    @Override
    protected void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        super.neighborChanged(pState, pLevel, pPos, pBlock, pFromPos, pIsMoving);
        if (!pLevel.isClientSide()) {
            withBlockEntityDo(pLevel, pPos, DrillCoreBlockEntity::scheduleStructureCheck); // IBE의 헬퍼 메서드 활용 가능
        }
    }

    // --- 블록 제거 시 처리 ---
    // RotatedPillarKineticBlock 또는 상위 클래스에서 이미 onRemove 처리를 할 수 있으므로,
    // 특별히 추가할 로직이 없다면 오버라이드하지 않아도 될 수 있습니다.
    // BE의 자원 해제 등은 BE의 setRemoved() 또는 destroy() 에서 처리하는 것이 일반적입니다.
    // @Override
    // public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
    //     if (pState.hasBlockEntity() && !pState.is(pNewState.getBlock())) {
    //         // BE 관련 로직 (예: 인벤토리 드롭)은 여기서 하기보다 BE 내부에서 처리 권장
    //         pLevel.removeBlockEntity(pPos); // 필요시 명시적 제거
    //     }
    //     super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    // }
}