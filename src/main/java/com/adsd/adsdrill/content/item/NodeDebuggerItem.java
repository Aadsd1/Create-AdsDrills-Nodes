package com.adsd.adsdrill.content.item;

import com.adsd.adsdrill.content.kinetics.node.NodeFrameBlock;
import com.adsd.adsdrill.content.kinetics.node.NodeFrameBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class NodeDebuggerItem extends Item {
    public NodeDebuggerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockPos pos = pContext.getClickedPos();

        // 서버 사이드에서만 로직 실행
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 클릭한 블록이 노드 프레임인지 확인
        if (level.getBlockState(pos).getBlock() instanceof NodeFrameBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            // 블록 엔티티가 노드 프레임 엔티티인지 확인
            if (be instanceof NodeFrameBlockEntity frameBE) {
                // 노드 프레임의 제작 완료 메서드를 강제로 호출
                frameBE.completeCrafting();

                if (pContext.getPlayer() != null) {
                    pContext.getPlayer().displayClientMessage(Component.literal("Node Frame crafting forced!").withStyle(ChatFormatting.YELLOW), true);
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }
}
