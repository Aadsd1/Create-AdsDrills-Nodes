package com.yourname.mycreateaddon.content.kinetics.drill.head;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlock;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity;

public class PumpHeadBlock extends DirectionalKineticBlock implements IDrillHead, IBE<PumpHeadBlockEntity>, IRotate {

    private final int pumpRate;
    private final float stressImpact;

    public PumpHeadBlock(Properties properties, int pumpRate, float stressImpact) {
        super(properties);
        this.pumpRate = pumpRate;
        this.stressImpact = stressImpact;
    }

    @Override
    public void onDrillTick(Level level, BlockPos headPos, BlockState headState, DrillCoreBlockEntity core) {
        if (level.isClientSide || core.getFinalSpeed() == 0) return;

        Direction facing = headState.getValue(FACING);
        BlockPos nodePos = headPos.relative(facing);

        if (level.getBlockEntity(nodePos) instanceof OreNodeBlockEntity nodeBE) {
            // 노드에 추출할 유체가 있는지 확인
            if (!nodeBE.getFluid().isEmpty()) {
                // 1. 노드에서 유체를 추출 시도
                // 펌프 성능과 드릴 속도에 비례하여 추출량 결정
                int amountToDrain = (int) (this.pumpRate * (Math.abs(core.getFinalSpeed()) / 64f));
                if (amountToDrain <= 0) return;

                FluidStack drained = nodeBE.drainFluid(amountToDrain);

                if (!drained.isEmpty()) {
                    // 2. 코어의 내부 버퍼로 유체를 옮김
                    IFluidHandler coreBuffer = core.getInternalFluidBuffer();
                    coreBuffer.fill(drained, IFluidHandler.FluidAction.EXECUTE);

                    // TODO: (선택적) 펌핑 효과음 및 파티클 재생
                }
            }
        }
    }

    @Override
    public float getHeatGeneration() {
        return 0.05f; // 펌프는 열을 거의 발생시키지 않음
    }

    @Override
    public float getCoolingRate() {
        return 0.1f;
    }

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
            withBlockEntityDo(level, pos, PumpHeadBlockEntity::updateCoreConnection);
        }
    }

    @Override
    protected void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide) return;

        withBlockEntityDo(level, pos, PumpHeadBlockEntity::updateCoreConnection);

        Direction facing = state.getValue(FACING);
        BlockPos corePos = pos.relative(facing.getOpposite());
        if (level.getBlockState(corePos).getBlock() instanceof DrillCoreBlock) {
            level.neighborChanged(corePos, this, pos);
        }
    }

    @Override
    public Class<PumpHeadBlockEntity> getBlockEntityClass() {
        return PumpHeadBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PumpHeadBlockEntity> getBlockEntityType() {
        return MyAddonBlockEntity.PUMP_HEAD.get(); // 나중에 등록할 BE 타입
    }
}
