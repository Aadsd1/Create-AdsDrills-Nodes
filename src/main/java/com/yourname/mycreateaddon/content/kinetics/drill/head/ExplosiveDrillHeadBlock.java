package com.yourname.mycreateaddon.content.kinetics.drill.head;


import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlockEntity;

public class ExplosiveDrillHeadBlock extends DirectionalKineticBlock implements IDrillHead{

    public ExplosiveDrillHeadBlock(Properties properties) {
        super(properties);
    }
    // [추가] 블록이 설치될 때 올바른 방향을 설정하도록 함
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 플레이어가 바라보는 방향의 반대쪽을 블록의 facing으로 설정 (드릴 코어를 바라보도록)
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    // 이 블록은 다른 기계와 동력을 주고받지 않으므로, 항상 false
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return false;
    }

    @Override
    public void onDrillTick(Level level, BlockPos headPos, BlockState headState, DrillCoreBlockEntity core) {
        // 이 헤드는 일반적인 틱에서는 아무 작업도 하지 않습니다.
    }

    // [핵심] 과열 이벤트가 발생했을 때만 작동합니다.

    @Override
    public boolean onOverheat(Level level, BlockPos headPos, DrillCoreBlockEntity core) {
        if (level.isClientSide) return false;

        ItemStack tntRequest = new ItemStack(Items.TNT, 1);
        if (core.consumeItems(tntRequest, false).isEmpty()) {

            // [핵심 수정] this.getBlockState(headPos) -> level.getBlockState(headPos)
            BlockState headState = level.getBlockState(headPos);
            BlockPos nodePos = headPos.relative(headState.getValue(FACING));

            if (level.getBlockEntity(nodePos) instanceof OreNodeBlockEntity nodeBE && !nodeBE.isCracked()) {
                core.consumeItems(tntRequest, true);
                nodeBE.setCracked(true);

                level.playSound(null, nodePos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0f, 1.2f);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.explode(null, nodePos.getX() + 0.5, nodePos.getY() + 0.5, nodePos.getZ() + 0.5, 2.0f, Level.ExplosionInteraction.NONE);
                }

            }
        }

        return false;
    }

    // 이 헤드는 회전하지 않으며, 열도 거의 발생시키지 않는다고 가정
    @Override public float getHeatGeneration() { return 0.05f; }
    @Override public float getCoolingRate() { return 0.05f; }
    // [신규] getStressImpact() 구현
    @Override public float getStressImpact() { return 16.0f; } // 폭발형 헤드는 2.0 SU 요구
    @Override public Direction.Axis getRotationAxis(BlockState state) { return null; } // 회전 안 함
}