package com.yourname.mycreateaddon.content.kinetics.drill.head;


import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class LaserDrillHeadBlock extends DirectionalKineticBlock implements IDrillHead, IBE<LaserDrillHeadBlockEntity>, IWrenchable {

    public LaserDrillHeadBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            withBlockEntityDo(context.getLevel(), context.getClickedPos(), be -> {
                be.cycleMode();
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(be.getMode().getDisplayName(), true);
                }
            });
        }
        return InteractionResult.SUCCESS;
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
    public Class<LaserDrillHeadBlockEntity> getBlockEntityClass() {
        return LaserDrillHeadBlockEntity.class;
    }
    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }
    @Override
    public BlockEntityType<? extends LaserDrillHeadBlockEntity> getBlockEntityType() {
        return MyAddonBlockEntity.LASER_DRILL_HEAD.get();
    }
}