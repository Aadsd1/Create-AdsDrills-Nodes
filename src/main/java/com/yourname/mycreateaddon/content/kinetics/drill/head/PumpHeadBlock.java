package com.yourname.mycreateaddon.content.kinetics.drill.head;

import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity;

public class PumpHeadBlock extends AbstractDrillHeadBlock {

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
            if (!nodeBE.getFluid().isEmpty()) {
                int amountToDrain = (int) (this.pumpRate * (Math.abs(core.getFinalSpeed()) / 64f));
                if (amountToDrain <= 0) return;
                FluidStack drained = nodeBE.drainFluid(amountToDrain);
                if (!drained.isEmpty()) {
                    core.getInternalFluidBuffer().fill(drained, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    @Override
    public float getHeatGeneration() { return 0.05f; }
    @Override
    public float getCoolingRate() { return 0.1f; }
    @Override
    public float getStressImpact() { return this.stressImpact; }



    @Override
    public BlockEntityType<? extends AbstractDrillHeadBlockEntity> getBlockEntityType() {
        return MyAddonBlockEntity.PUMP_HEAD.get();
    }
}