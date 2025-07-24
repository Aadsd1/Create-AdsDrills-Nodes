package com.adsd.adsdrill.content.kinetics.node;

import com.simibubi.create.foundation.block.IBE;
import com.adsd.adsdrill.registry.AdsDrillBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
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
        return AdsDrillBlockEntity.ORE_NODE.get();
    }

    // 이 블록은 어떤 아이템도 드롭하지 않습니다.
    @Override
    protected @NotNull List<net.minecraft.world.item.ItemStack> getDrops(@NotNull BlockState pState, LootParams.@NotNull Builder pParams) {
        return Collections.emptyList();
    }
    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof OreNodeBlockEntity nodeBE) {
            float maxYield = nodeBE.getMaxYield();
            if (maxYield == 0) return 0;
            float currentYield = nodeBE.getCurrentYield();
            float ratio = currentYield / maxYield;
            // 내용물이 조금이라도 있으면 최소 1, 가득 차면 15를 반환하는 바닐라 표준 계산식
            return Mth.floor(ratio * 14.0F) + (currentYield > 0 ? 1 : 0);
        }
        return 0;
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
