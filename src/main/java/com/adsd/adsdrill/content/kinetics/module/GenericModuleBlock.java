package com.adsd.adsdrill.content.kinetics.module; // 또는 원하는 경로

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.adsd.adsdrill.crafting.ModuleUpgrades;
import com.adsd.adsdrill.registry.AdsDrillBlocks;
import com.adsd.adsdrill.registry.AdsDrillItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.*;

import com.simibubi.create.foundation.block.IBE;
import com.adsd.adsdrill.registry.AdsDrillBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;


public class GenericModuleBlock extends Block implements IBE<GenericModuleBlockEntity>, IWrenchable {

    private final ModuleType moduleType;

    protected static final VoxelShape SHAPE = Shapes.box(0.125, 0.125, 0.125, 0.875, 0.875, 0.875);

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState pState, net.minecraft.world.level.@NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        return SHAPE;
    }

    public GenericModuleBlock(Properties properties, ModuleType moduleType) {
        super(properties);
        this.moduleType = moduleType;
    }
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ModuleType type = getModuleType();

        if (type.getBehavior().getRecipeType() != null || type == ModuleType.FILTER) {
            if (!level.isClientSide) {
                withBlockEntityDo(level, pos, be -> be.cyclePriority(context.getPlayer()));
            }
            return InteractionResult.SUCCESS;
        }

        return IWrenchable.super.onWrenched(state, context);
    }
    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        return IWrenchable.super.onSneakWrenched(state, context);
    }
    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide)
            return;

        if (level.getBlockEntity(pos) instanceof GenericModuleBlockEntity be) {
            CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);

            if (blockEntityData != null) {
                // 3. CustomData에서 실제 CompoundTag를 추출하여 BE에 로드합니다.
                be.loadWithComponents(blockEntityData.copyTag(), level.registryAccess());
            }
        }
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        // 렌치 사용 시에는 기존 로직을 따르도록 먼저 확인
        if (stack.getItem() instanceof WrenchItem) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }

        if (stack.getItem() == AdsDrillItems.MODULE_UPGRADE_REMOVER.get()) {
            // 현재 모듈이 프레임이 아닐 경우에만 작동
            if (getModuleType() != ModuleType.FRAME) {
                if (!level.isClientSide) {
                    // 블록을 교체하기 전에 내용물을 먼저 드롭
                    if (level.getBlockEntity(pos) instanceof GenericModuleBlockEntity be) {
                        be.dropContents();
                    }

                    // 프레임 모듈로 교체
                    level.setBlock(pos, AdsDrillBlocks.FRAME_MODULE.get().defaultBlockState(), 3);

                    // 효과 재생 및 아이템 소모
                    level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8f, 1.5f);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.1);
                    }
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                return ItemInteractionResult.SUCCESS;
            }
            // 이미 프레임이면 아무것도 하지 않음
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }


        // --- 프레임 모듈을 업그레이드하는 로직 ---
        if (getModuleType() == ModuleType.FRAME) {
            BlockEntry<? extends GenericModuleBlock> targetModuleEntry = ModuleUpgrades.getUpgradeResult(stack.getItem());
            if (targetModuleEntry != null) {
                if (!level.isClientSide) {
                    BlockState newState = targetModuleEntry.get().defaultBlockState();
                    level.setBlock(pos, newState, 3);
                    level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.2f);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0.1);
                    }
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                return ItemInteractionResult.SUCCESS;
            }
        }

        // --- 프레임이 아닌 모듈의 고유 상호작용 로직 ---
        return handleNonFrameModuleInteraction(stack, state, level, pos, player, hand, hit);
    }

    // useItemOn 메서드를 오버라이드하여 필터 상호작용 추가
    private ItemInteractionResult handleNonFrameModuleInteraction(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (getModuleType() == ModuleType.RESONATOR) {
            if (!level.isClientSide) {
                withBlockEntityDo(level, pos, be -> be.setResonatorFilter(stack, player));
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (getModuleType() == ModuleType.FILTER) {
            if (!level.isClientSide) {
                withBlockEntityDo(level, pos, be -> {
                    ItemStack filterInSlot = be.getFilter();
                    ItemStack heldItem = player.getItemInHand(hand);
                    if (heldItem.getItem() instanceof FilterItem && filterInSlot.isEmpty()) {
                        be.setFilter(heldItem.split(1));
                        player.getInventory().setChanged();
                    } else if (heldItem.isEmpty() && player.isShiftKeyDown() && !filterInSlot.isEmpty()) {
                        player.getInventory().placeItemBackInInventory(filterInSlot);
                        be.setFilter(ItemStack.EMPTY);
                    }
                });
            }
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
    // --- [수정] 데이터 보존을 위한 드롭 로직 ---
    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder params) {
        // [수정] getOptional 대신 get 사용
        BlockEntity be = params.getParameter(LootContextParams.BLOCK_ENTITY);

        if (be instanceof GenericModuleBlockEntity) {
            ItemStack dropStack = new ItemStack(this);
            // BlockEntity의 내장 메서드를 사용하여 안전하게 NBT 저장
            be.saveToItem(dropStack, Objects.requireNonNull(be.getLevel()).registryAccess());
            return Collections.singletonList(dropStack);
        }
        // BE가 없으면 기본 드롭 로직을 따름 (이 경우엔 자신을 드롭)
        return super.getDrops(state, params);
    }
    public ModuleType getModuleType() {
        return moduleType;
    }

    @Override
    protected void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide()) {
            return;
        }

        findAndNotifyCore(level, pos);

        // 자신의 BE에 에너지 연결 상태를 업데이트하도록 알림
        withBlockEntityDo(level, pos, GenericModuleBlockEntity::updateEnergyConnections);
    }

    public static void findAndNotifyCore(Level level, BlockPos startPos) {
        Set<BlockPos> visited = new HashSet<>();
        searchForCore(level, startPos, visited, 0);
    }

    private static void searchForCore(Level level, BlockPos currentPos, Set<BlockPos> visited, int depth) {
        if (depth > 64 || !visited.add(currentPos)) {
            return;
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = currentPos.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            BlockState neighborState = level.getBlockState(neighborPos);
            Block neighborBlock = neighborState.getBlock();

            if (level.getBlockEntity(neighborPos) instanceof DrillCoreBlockEntity core) {
                core.scheduleStructureCheck();
            }
            else if (neighborBlock instanceof GenericModuleBlock) {
                searchForCore(level, neighborPos, visited, depth + 1);
            }
        }
    }

    @Override
    public Class<GenericModuleBlockEntity> getBlockEntityClass() {
        return GenericModuleBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GenericModuleBlockEntity> getBlockEntityType() {
        return AdsDrillBlockEntity.GENERIC_MODULE.get();
    }
}
