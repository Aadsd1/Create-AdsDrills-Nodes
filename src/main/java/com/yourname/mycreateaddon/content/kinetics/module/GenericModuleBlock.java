package com.yourname.mycreateaddon.content.kinetics.module; // 또는 원하는 경로

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
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

import com.simibubi.create.foundation.block.IBE; // IBE 임포트
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity; // BE 레지스트리 임포트
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;


public class GenericModuleBlock extends Block implements IBE<GenericModuleBlockEntity>, IWrenchable {

    private final ModuleType moduleType;

    public GenericModuleBlock(Properties properties, ModuleType moduleType) {
        super(properties);
        this.moduleType = moduleType;
    }
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ModuleType type = getModuleType();

        // [핵심 수정] 조건을 확장하여, 일반 처리 모듈 또는 필터 모듈일 경우에 우선순위 변경 기능이 작동하도록 함
        if (type.getBehavior().getRecipeType() != null || type == ModuleType.FILTER) {
            if (!level.isClientSide) {
                withBlockEntityDo(level, pos, be -> be.cyclePriority(context.getPlayer()));
            }
            return InteractionResult.SUCCESS;
        }

        // 위 조건에 해당하지 않는 모듈은 블록을 회전시키는 기본 동작을 수행
        return IWrenchable.super.onWrenched(state, context);
    }
    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        // 쉬프트 클릭 시에는 항상 블록을 분해(아이템화)하는 기본 동작을 수행
        return IWrenchable.super.onSneakWrenched(state, context);
    }
    // --- [수정] 데이터 복원을 위한 설치 로직 (오류 해결) ---
    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide)
            return;

        if (level.getBlockEntity(pos) instanceof GenericModuleBlockEntity be) {
            // 1. 아이템 스택에서 BLOCK_ENTITY_DATA 컴포넌트를 직접 가져옵니다. (반환 타입: CustomData 또는 null)
            CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);

            // 2. null이 아닌지 확인합니다.
            if (blockEntityData != null) {
                // 3. CustomData에서 실제 CompoundTag를 추출하여 BE에 로드합니다.
                be.loadWithComponents(blockEntityData.copyTag(), level.registryAccess());
            }
        }
    }
    // [수정] useItemOn 메서드를 오버라이드하여 필터 상호작용 추가
    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        // [핵심 수정] 손에 든 아이템이 렌치일 경우, 이 메서드가 반응하지 않도록 함
        if (stack.getItem() instanceof WrenchItem) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }

        if (getModuleType() == ModuleType.RESONATOR) {
            if (!level.isClientSide) {
                withBlockEntityDo(level, pos, be -> be.setResonatorFilter(stack, player));
            }
            return ItemInteractionResult.SUCCESS;
        }
        // 필터 모듈에 대해서만 필터 장착/해제 로직을 실행
        if (getModuleType() == ModuleType.FILTER) {
            if (!level.isClientSide) {
                withBlockEntityDo(level, pos, be -> {
                    ItemStack filterInSlot = be.getFilter();
                    ItemStack heldItem = player.getItemInHand(hand);

                    // 손에 필터를 들고 있고, 모듈의 필터 슬롯이 비어있을 때
                    if (heldItem.getItem() instanceof FilterItem && filterInSlot.isEmpty()) {
                        be.setFilter(heldItem.split(1));
                        player.getInventory().setChanged();
                    }
                    // 손이 비어있고(쉬프트 클릭), 모듈에 필터가 있을 때
                    else if (heldItem.isEmpty() && player.isShiftKeyDown() && !filterInSlot.isEmpty()) {
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
        // 코어에 구조 재검사를 알림
        findAndNotifyCore(level, pos);

        // [신규] 자신의 BE에 에너지 연결 상태를 업데이트하도록 알림
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
        return MyAddonBlockEntity.GENERIC_MODULE.get();
    }
}
