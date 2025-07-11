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

        if (player.isShiftKeyDown() && stack.isEmpty()) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            frameBE.retrieveItem(player);
            return ItemInteractionResult.SUCCESS;
        }

        // [!!! 핵심 수정 !!!]
        // 클라이언트 측의 즉각적인 반응을 위해, isCatalystItem 체크를 여기에 추가합니다.
        if (level.isClientSide) {
            if (stack.getItem() instanceof UnfinishedNodeDataItem ||
                    stack.getItem() instanceof StabilizerCoreItem ||
                    frameBE.isCatalystItem(stack)) { // 이 부분이 추가되었습니다.
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 서버 측 로직 (이 부분은 이전 답변과 동일합니다)
        if (stack.getItem() instanceof UnfinishedNodeDataItem) {
            if (frameBE.addData(stack)) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.FAIL;
        }
        else if (stack.getItem() instanceof StabilizerCoreItem) {
            if (frameBE.addStabilizerCore(stack)) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.FAIL;
        }
        else if (frameBE.isCatalystItem(stack)) {
            if (frameBE.addCatalyst(stack)) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.FAIL;
        }

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