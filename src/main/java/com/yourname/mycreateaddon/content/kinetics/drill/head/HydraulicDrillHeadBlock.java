package com.yourname.mycreateaddon.content.kinetics.drill.head;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlock;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlockEntity;
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;


public class HydraulicDrillHeadBlock extends DirectionalKineticBlock implements IDrillHead, IBE<HydraulicDrillHeadBlockEntity>, IRotate {

    private static final int WATER_CONSUMPTION = 50; // mb per tick
    private static final float STRESS_IMPACT = 6.0f;
    private static final float HEAT_GENERATION = 0.1f;
    private static final ResourceLocation SLUICE_TARGET_TAG = ResourceLocation.fromNamespaceAndPath("c", "raw_materials");

    public HydraulicDrillHeadBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onDrillTick(Level level, BlockPos headPos, BlockState headState, DrillCoreBlockEntity core) {
        if (level.isClientSide || core.getFinalSpeed() == 0) return;

        FluidStack waterRequest = new FluidStack(net.minecraft.world.level.material.Fluids.WATER, WATER_CONSUMPTION);
        if (core.getInternalFluidBuffer().drain(waterRequest, IFluidHandler.FluidAction.SIMULATE).getAmount() < WATER_CONSUMPTION) {
            return; // 물 부족
        }

        Direction facing = headState.getValue(FACING);
        BlockPos nodePos = headPos.relative(facing);

        if (level.getBlockEntity(nodePos) instanceof OreNodeBlockEntity nodeBE) {
            Map<Item, Float> composition = nodeBE.getResourceComposition();
            if (composition.isEmpty()) return;

            core.getInternalFluidBuffer().drain(waterRequest, IFluidHandler.FluidAction.EXECUTE);

            int totalMiningAmount = (int) (Math.abs(core.getFinalSpeed()) / 16f);
            if (totalMiningAmount <= 0) return;

            for (Map.Entry<Item, Float> entry : composition.entrySet()) {
                Item item = entry.getKey();
                float ratio = entry.getValue();

                if (new ItemStack(item).is(ItemTags.create(SLUICE_TARGET_TAG))) {
                    int specificMiningAmount = (int) Math.ceil(totalMiningAmount * ratio);
                    if (specificMiningAmount > 0) {
                        // [핵심 수정] 행운/실크터치 값을 0과 false로 고정하여 전달
                        List<ItemStack> drops = core.mineSpecificNode(nodeBE, specificMiningAmount, 0, false, item);
                        for (ItemStack drop : drops) {
                            core.processMinedItem(drop);
                        }
                    }
                }
            }


            if (core.getTickCounter() % 5 == 0) {
                if (level instanceof ServerLevel serverLevel) {
                    // 파티클 생성 위치를 헤드 앞쪽으로 약간 이동시켜 더 자연스럽게 만듭니다.
                    double px = headPos.getX() + 0.5 + facing.getStepX() * 0.6;
                    double py = headPos.getY() + 0.5 + facing.getStepY() * 0.6;
                    double pz = headPos.getZ() + 0.5 + facing.getStepZ() * 0.6;

                    // 파티클이 분사되는 느낌을 주기 위해 속도를 조절합니다.
                    serverLevel.sendParticles(ParticleTypes.SPLASH, px, py, pz, 15, 0.2, 0.2, 0.2, 0.5);
                }
                level.playSound(null, headPos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.4f, 1.5f);
            }
        }
    }

    // --- 인터페이스 및 연결 로직 (이전과 동일, useItemOn 메서드는 제거됨) ---
    @Override
    public float getHeatGeneration() { return HEAT_GENERATION; }
    @Override
    public float getCoolingRate() { return 0.2f; }
    @Override
    public float getStressImpact() { return STRESS_IMPACT; }
    @Override
    public Direction.Axis getRotationAxis(BlockState state) { return state.getValue(FACING).getAxis(); }
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) { return face == state.getValue(FACING).getOpposite(); }
    @Override
    public Class<HydraulicDrillHeadBlockEntity> getBlockEntityClass() { return HydraulicDrillHeadBlockEntity.class; }
    @Override
    public BlockEntityType<? extends HydraulicDrillHeadBlockEntity> getBlockEntityType() { return MyAddonBlockEntity.HYDRAULIC_DRILL_HEAD.get(); }
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, HydraulicDrillHeadBlockEntity::updateCoreConnection);
        }
    }
    @Override
    protected void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide) return;
        withBlockEntityDo(level, pos, HydraulicDrillHeadBlockEntity::updateCoreConnection);
        Direction facing = state.getValue(FACING);
        BlockPos corePos = pos.relative(facing.getOpposite());
        if (level.getBlockState(corePos).getBlock() instanceof DrillCoreBlock) {
            level.neighborChanged(corePos, this, pos);
        }
    }
}