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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;
import java.util.stream.Collectors;

public class HydraulicDrillHeadBlock extends DirectionalKineticBlock implements IDrillHead, IBE<HydraulicDrillHeadBlockEntity>, IRotate {

    private static final int WATER_CONSUMPTION = 100; // mb per operation
    private static final float STRESS_IMPACT = 6.0f;
    private static final float HEAT_GENERATION = 0.1f;

    public HydraulicDrillHeadBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onDrillTick(Level level, BlockPos headPos, BlockState headState, DrillCoreBlockEntity core) {
        if (level.isClientSide || core.getFinalSpeed() == 0) return;

        // 1. 물 소모 확인
        FluidStack waterRequest = new FluidStack(net.minecraft.world.level.material.Fluids.WATER, WATER_CONSUMPTION);
        if (core.getInternalFluidBuffer().drain(waterRequest, IFluidHandler.FluidAction.SIMULATE).getAmount() < WATER_CONSUMPTION) {
            return; // 물이 부족하면 작동 중지
        }

        Direction facing = headState.getValue(FACING);
        BlockPos nodePos = headPos.relative(facing);

        if (level.getBlockEntity(nodePos) instanceof OreNodeBlockEntity nodeBE) {
            // 2. 실제 물 소모
            core.getInternalFluidBuffer().drain(waterRequest, IFluidHandler.FluidAction.EXECUTE);

            // 3. 채굴량 계산
            int miningAmount = (int) (Math.abs(core.getFinalSpeed()) / 16f); // 다른 헤드보다 효율이 약간 더 좋게 설정
            if (miningAmount <= 0) return;

            // 4. 업그레이드 정보 가져오기 (코어에 연결된 로터리 헤드 BE에서 가져와야 함)
            // 이 로직은 RotaryDrillHeadBlockEntity에 의존하므로, 해당 BE의 getter가 필요합니다.
            // 우선은 하드코딩된 값으로 진행하고, 나중에 연동합니다.
            // -> RotaryDrillHeadBlockEntity의 mineNode를 수정하여 이 문제를 해결합니다.
            int fortune = 0;
            boolean silkTouch = false;

            if(level.getBlockEntity(headPos) instanceof HydraulicDrillHeadBlockEntity headBE) {
                fortune = headBE.getFortuneLevel();
                silkTouch = headBE.hasSilkTouch();
            }

            // 5. 노드 채굴
            List<ItemStack> minedItems = core.mineNode(nodeBE, miningAmount, fortune, silkTouch);

            // 6. 결과물 필터링 (핵심 로직)
            List<ItemStack> filteredDrops = minedItems.stream()
                    // c:raw_materials 태그를 가진 아이템만 통과시킵니다. (예: 철 원석, 구리 원석 등)
                    // 실크터치로 나온 광석 블록도 통과시켜야 합니다.
                    .filter(stack -> stack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "raw_materials"))) || stack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores"))))
                    .collect(Collectors.toList());

            // 7. 필터링된 아이템 처리
            for (ItemStack finalItem : filteredDrops) {
                if (!finalItem.isEmpty()) {
                    core.processMinedItem(finalItem);
                }
            }

            // 8. 시각 및 청각 효과
            if (level instanceof ServerLevel serverLevel && serverLevel.getRandom().nextFloat() < 0.3f) {
                serverLevel.sendParticles(ParticleTypes.SPLASH, headPos.getX() + 0.5, headPos.getY() + 0.5, headPos.getZ() + 0.5, 5, facing.getStepX() * 0.5, facing.getStepY() * 0.5, facing.getStepZ() * 0.5, 0.1);
                level.playSound(null, headPos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5f, 1.5f + level.random.nextFloat() * 0.5f);
            }
        }
    }

    // --- 인터페이스 메서드 구현 ---
    @Override
    public float getHeatGeneration() { return HEAT_GENERATION; }
    @Override
    public float getCoolingRate() { return 0.2f; } // 물을 사용하므로 냉각 성능이 좋음
    @Override
    public float getStressImpact() { return STRESS_IMPACT; }
    @Override
    public Direction.Axis getRotationAxis(BlockState state) { return state.getValue(FACING).getAxis(); }
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) { return face == state.getValue(FACING).getOpposite(); }

    // --- IBE 구현 ---
    @Override
    public Class<HydraulicDrillHeadBlockEntity> getBlockEntityClass() { return HydraulicDrillHeadBlockEntity.class; }
    @Override
    public BlockEntityType<? extends HydraulicDrillHeadBlockEntity> getBlockEntityType() { return MyAddonBlockEntity.HYDRAULIC_DRILL_HEAD.get(); }

    // --- 연결 로직 ---
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, HydraulicDrillHeadBlockEntity::updateCoreConnection);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
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
