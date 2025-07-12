package com.yourname.mycreateaddon.content.kinetics.node;


import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

// OreNodeBlock을 상속받고 IWrenchable를 구현합니다.
public class ArtificialNodeBlock extends OreNodeBlock implements IWrenchable {

    public ArtificialNodeBlock(Properties properties) {
        super(properties);
    }

    // BE 타입은 ArtificialNodeBlockEntity를 사용하도록 오버라이드합니다.
    @Override
    public BlockEntityType<? extends OreNodeBlockEntity> getBlockEntityType() {
        return MyAddonBlockEntity.ARTIFICIAL_NODE.get();
    }
    // [핵심 수정] getDrops를 오버라이드하여 NBT 데이터가 담긴 아이템을 드롭하도록 합니다.
    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder params) {
        BlockEntity be = params.getParameter(LootContextParams.BLOCK_ENTITY);

        // BE가 존재하고, 타입이 OreNodeBlockEntity(또는 그 자식)가 맞는지 확인합니다.
        if (be instanceof OreNodeBlockEntity nodeBE) {
            ItemStack dropStack = new ItemStack(this);

            // BE의 데이터를 아이템 스택에 저장합니다.
            var level = params.getLevel();
            nodeBE.saveToItem(dropStack, level.registryAccess());

            return Collections.singletonList(dropStack);
        }

        // BE가 없거나 타입이 다를 경우, 부모 클래스의 드롭 로직을 따릅니다.
        // (이 경우 부모가 빈 리스트를 반환하므로 아무것도 드롭되지 않습니다)
        return super.getDrops(state, params);
    }
    // 렌치로 쉬프트+우클릭 시 블록을 아이템화하고 드롭하는 로직
    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {

        return IWrenchable.super.onSneakWrenched(state, context);
    }
}