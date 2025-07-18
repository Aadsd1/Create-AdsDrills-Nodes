package com.adsd.adsdrill.content.kinetics.drill.head;


import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.adsd.adsdrill.registry.AdsDrillBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class LaserDrillHeadBlock extends DirectionalKineticBlock implements IDrillHead, IBE<LaserDrillHeadBlockEntity>, IWrenchable {

    public LaserDrillHeadBlock(Properties properties) {
        super(properties);
    }


    @Override
    public void onDrillTick(Level level, BlockPos headPos, BlockState headState, DrillCoreBlockEntity core) {
        if (level.isClientSide) return;
        withBlockEntityDo(level, headPos, be -> be.onDrillTick(core));
    }

    @Override
    public float getHeatGeneration() {
        return 0.1f; // 레이저는 열이 조금 발생
    }

    @Override
    public float getCoolingRate() {
        return 0.1f;
    }

    @Override
    public float getStressImpact() {
        return 0; // 에너지를 사용하므로 스트레스 부하는 없음
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public Class<LaserDrillHeadBlockEntity> getBlockEntityClass() {
        return LaserDrillHeadBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LaserDrillHeadBlockEntity> getBlockEntityType() {
        return AdsDrillBlockEntity.LASER_DRILL_HEAD.get();
    }
}