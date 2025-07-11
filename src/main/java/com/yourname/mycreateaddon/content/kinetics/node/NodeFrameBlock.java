package com.yourname.mycreateaddon.content.kinetics.node;


import com.simibubi.create.foundation.block.IBE;
import com.yourname.mycreateaddon.content.item.StabilizerCoreItem; // 나중에 만들 아이템 클래스
import com.yourname.mycreateaddon.content.item.UnfinishedNodeDataItem;
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class NodeFrameBlock extends Block implements IBE<NodeFrameBlockEntity> {

    public NodeFrameBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<NodeFrameBlockEntity> getBlockEntityClass() {
        return NodeFrameBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends NodeFrameBlockEntity> getBlockEntityType() {
        return MyAddonBlockEntity.NODE_FRAME.get();
    }

    @Override
    protected boolean isPathfindable(@NotNull BlockState state, @NotNull PathComputationType type) {
        return false;
    }


    @Override
    protected @NotNull ItemInteractionResult useItemOn(
            @NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level,
            @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand,
            @NotNull BlockHitResult hit) {

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof NodeFrameBlockEntity frameBE)) {
            return ItemInteractionResult.FAIL;
        }

        // [1단계 추가] 아이템 회수 로직
        if (player.isShiftKeyDown() && stack.isEmpty()) {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            // BE에서 아이템 회수 로직 호출
            frameBE.retrieveItem(player);
            return ItemInteractionResult.SUCCESS;
        }

        // 클라이언트에서는 항상 성공으로 처리하여 즉각적인 반응성을 보장
        if (level.isClientSide) {
            if (stack.getItem() instanceof UnfinishedNodeDataItem || stack.getItem() instanceof StabilizerCoreItem) {
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 서버 측 로직 (아이템 추가)
        // 1. 미완성 노드 데이터 아이템 처리
        if (stack.getItem() instanceof UnfinishedNodeDataItem) {
            if (frameBE.addData(stack)) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.FAIL;
        }

        // 2. 안정화 코어 아이템 처리
        else if (stack.getItem() instanceof StabilizerCoreItem) {
            if (frameBE.addStabilizerCore(stack)) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.FAIL;
        }

        // 3. 처리할 아이템이 아니면 기본 상호작용으로 넘김
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (state.hasBlockEntity() && (!state.is(newState.getBlock()) || !newState.hasBlockEntity())) {
            withBlockEntityDo(level, pos, NodeFrameBlockEntity::onBroken);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}