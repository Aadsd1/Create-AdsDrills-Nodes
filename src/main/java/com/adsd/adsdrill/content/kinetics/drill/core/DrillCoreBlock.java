package com.adsd.adsdrill.content.kinetics.drill.core;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.adsd.adsdrill.registry.AdsDrillBlockEntity;
import com.adsd.adsdrill.registry.AdsDrillItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlockEntity.Tier;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class DrillCoreBlock extends DirectionalKineticBlock implements IBE<DrillCoreBlockEntity> {

    public static final EnumProperty<Tier> TIER = EnumProperty.create("tier", Tier.class);
    protected static final VoxelShape SHAPE = Shapes.box(0.0625, 0.0625, 0.0625, 0.9375, 0.9375, 0.9375);

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState pState, net.minecraft.world.level.@NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        return SHAPE;
    }

    public DrillCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(TIER, Tier.BRASS));
    }

    @Override
    public Class<DrillCoreBlockEntity> getBlockEntityClass() {
        return DrillCoreBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DrillCoreBlockEntity> getBlockEntityType() {
        return AdsDrillBlockEntity.DRILL_CORE.get();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }
    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder builder) {
        // 이 시점의 getParameter는 null을 반환할 수 있습니다.
        BlockEntity be = builder.getParameter(LootContextParams.BLOCK_ENTITY);

        // BlockEntity가 존재하고, 타입이 DrillCoreBlockEntity가 맞는지 확인합니다.
        if (be instanceof DrillCoreBlockEntity coreBE) {
            ItemStack dropStack = new ItemStack(this);

            // BE의 데이터를 아이템 스택에 저장합니다.
            // level이 null일 수 있으므로 be.getLevel() 대신 builder에서 가져옵니다.
            var level = builder.getLevel();
            coreBE.saveToItem(dropStack, level.registryAccess());

            return Collections.singletonList(dropStack);
        }

        // BE가 없거나 타입이 다를 경우, 기본 드롭 로직(자기 자신 드롭)을 따릅니다.
        return super.getDrops(state, builder);
    }

    // [핵심] IForgeBlock 인터페이스의 정확한 시그니처로 오버라이드
    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        // 새 아이템 스택을 생성
        ItemStack stack = new ItemStack(this);

        // BlockEntity를 가져옴
        BlockEntity be = level.getBlockEntity(pos);

        // BE가 존재하고 DrillCoreBlockEntity 타입이 맞는지 확인
        if (be instanceof DrillCoreBlockEntity coreBE) {
            // BE의 데이터를 아이템 스택에 저장
            // coreBE.getLevel()은 Level을 반환하므로 registryAccess를 안전하게 가져올 수 있음
            if (coreBE.hasLevel()) {
                coreBE.saveToItem(stack, Objects.requireNonNull(coreBE.getLevel()).registryAccess());
            }
        }

        // 데이터가 포함된 아이템 스택을 반환
        return stack;
    }
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(TIER));
    }
    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;

        // 아이템 스택에 저장된 BE 데이터가 있는지 확인
        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            // BE를 가져와서 데이터를 로드
            withBlockEntityDo(level, pos, be -> {
                // copyTag()로 불변성을 유지하며 안전하게 로드
                be.loadWithComponents(blockEntityData.copyTag(), level.registryAccess());

                // 로드 후, BE의 데이터에 따라 BlockState도 동기화
                BlockState currentState = level.getBlockState(pos);
                BlockState newState = currentState.setValue(DrillCoreBlock.TIER, be.getCoreTier());
                if(currentState != newState) {
                    level.setBlock(pos, newState, 2);
                }
            });
        }
    }

    @Override
    protected void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, DrillCoreBlockEntity::scheduleStructureCheck);
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }
    // 업그레이드를 위한 상호작용 로직
    @Override
    protected @NotNull ItemInteractionResult useItemOn(
            @NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level,
            @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand,
            @NotNull BlockHitResult hit) {

        if (player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Item itemInHand = stack.getItem();

        // 업그레이드 재료 확인
        if (itemInHand == AdsDrillItems.DRILL_CORE_STEEL_UPGRADE.get() || itemInHand == AdsDrillItems.DRILL_CORE_NETHERITE_UPGRADE.get()) {
            if (level.isClientSide) {
                // 클라이언트에서는 항상 성공으로 처리하여 즉각적인 반응성을 보여줌
                return ItemInteractionResult.SUCCESS;
            }

            // withBlockEntityDo 대신 직접 BlockEntity를 가져옵니다.
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DrillCoreBlockEntity coreBE) {
                // BlockEntity의 tryUpgrade 메서드를 호출하고, 그 boolean 결과를 받습니다.
                boolean success = coreBE.tryUpgrade(itemInHand);

                if (success) {
                    // 성공했을 때만 아이템을 소모하고 소리를 재생합니다.
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.5f);
                    // 플레이어에게 성공 메시지를 보낼 수도 있습니다.
                    // player.displayClientMessage(Component.translatable("adsdrill.upgrade.success"), true);
                    return ItemInteractionResult.SUCCESS;
                }
            }

            // 업그레이드에 실패했거나(예: 티어가 맞지 않음), BE가 없는 경우
            return ItemInteractionResult.FAIL;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
    @Override
    protected boolean isPathfindable(@NotNull BlockState state, @NotNull PathComputationType pathComputationType) {
        return false;
    }
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.hasBlockEntity() && (!state.is(newState.getBlock()) || !newState.hasBlockEntity())) {
            withBlockEntityDo(level, pos, DrillCoreBlockEntity::onBroken);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}