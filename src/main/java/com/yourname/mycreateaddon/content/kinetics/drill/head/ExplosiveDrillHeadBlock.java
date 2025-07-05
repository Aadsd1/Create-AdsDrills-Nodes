package com.yourname.mycreateaddon.content.kinetics.drill.head;


import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.yourname.mycreateaddon.MyCreateAddon;
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
        if (level.isClientSide) return;

        Direction facing = headState.getValue(FACING);
        BlockPos nodePos = headPos.relative(facing);

        if (level.getBlockEntity(nodePos) instanceof OreNodeBlockEntity nodeBE) {
            if (nodeBE.isCracked()) return;
            if (level.getGameTime() % 20 != 0) return;

            ItemStack tntRequest = new ItemStack(Items.TNT, 1);

            // [핵심 수정] consumeItems의 반환 값을 직접 확인
            // 이 메서드는 '소모하고 남은 아이템'을 반환합니다.
            // 우리가 1개를 요청했으니, 성공했다면 남은 것은 0개, 즉 반환 스택은 비어있어야 합니다.
            if (core.consumeItems(tntRequest, false).isEmpty()) {
                nodeBE.setCracked(true);
                level.playSound(null, nodePos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0f, 1.2f);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.explode(null, nodePos.getX() + 0.5, nodePos.getY() + 0.5, nodePos.getZ() + 0.5, 2.0f, Level.ExplosionInteraction.NONE);
                }
            }
        }
    }

    // 이 헤드는 회전하지 않으며, 열도 거의 발생시키지 않는다고 가정
    @Override public float getHeatGeneration() { return 0.05f; }
    @Override public float getCoolingRate() { return 0.05f; }
    @Override public Direction.Axis getRotationAxis(BlockState state) { return null; } // 회전 안 함
}