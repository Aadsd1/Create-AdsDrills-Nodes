package com.yourname.mycreateaddon.content.kinetics.module; // 또는 원하는 경로

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes; // Shapes 임포트
import net.minecraft.world.phys.shapes.VoxelShape; // VoxelShape 임포트

public class FrameModuleBlock extends Block {

    // 프레임 형태를 위한 VoxelShape 정의 (예시: 6x6x6 크기의 중앙 빔)
    // 필요에 따라 더 정교하게 만들 수 있습니다.
    protected static final VoxelShape SHAPE = Shapes.box(0.1875, 0.1875, 0.1875, 0.8125, 0.8125, 0.8125); // 3/16 ~ 13/16

    public FrameModuleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    // 블록의 충돌 및 렌더링 모양 정의
    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    // 빛이 통과하도록 설정 (선택 사항)
    @Override
    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation") // useShapeForLightOcclusion 은 deprecated 될 수 있음
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1.0F; // 최대 밝기 (그림자 최소화)
    }

    // 렌더링 레이어 설정 (선택 사항, 반투명 등 필요시)
    // 최신 버전에서는 ItemBlockRenderTypes 또는 유사한 클래스를 사용하여 클라이언트 측에서 설정할 수 있음
    // 예: ClientSetup 클래스에서 ItemBlockRenderTypes.setRenderLayer(ModBlocks.FRAME_MODULE.get(), RenderType.cutout());
}