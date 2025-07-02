package com.yourname.mycreateaddon.content.kinetics.node;

import com.simibubi.create.foundation.block.IBE;
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;

import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;


public class OreNodeBlock extends Block implements IBE<OreNodeBlockEntity> {

    // 나중에 동적 텍스처 구현 시, 채굴 진행도에 따라 텍스처를 변화시키는 데 사용할 수 있습니다.
    public static final BooleanProperty MINED = BooleanProperty.create("mined");

    public OreNodeBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(MINED, false));
    }

    @Override
    public Class<OreNodeBlockEntity> getBlockEntityClass() {
        return OreNodeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends OreNodeBlockEntity> getBlockEntityType() {
        return MyAddonBlockEntity.ORE_NODE.get();
    }

    // 이 블록은 어떤 아이템도 드롭하지 않습니다.
    @Override
    protected @NotNull List<net.minecraft.world.item.ItemStack> getDrops(@NotNull BlockState pState, LootParams.@NotNull Builder pParams) {
        return Collections.emptyList();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(MINED);
        super.createBlockStateDefinition(pBuilder);
    }

    // 경로 탐색에서 이 블록을 통과할 수 없도록 설정합니다.
    @Override
    protected boolean isPathfindable(@NotNull BlockState pState, @NotNull PathComputationType pPathComputationType) {
        return false;
    }
}
