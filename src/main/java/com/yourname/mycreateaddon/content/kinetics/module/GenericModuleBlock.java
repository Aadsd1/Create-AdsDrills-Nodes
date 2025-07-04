package com.yourname.mycreateaddon.content.kinetics.module; // 또는 원하는 경로

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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

        // 처리 모듈일 경우에만 우선순위 변경 기능 작동
        if (type.getRecipeTypeSupplier() != null) {
            if (!level.isClientSide) {
                withBlockEntityDo(level, pos, be -> be.cyclePriority(context.getPlayer()));
            }
            return InteractionResult.SUCCESS;
        }

        // 처리 모듈이 아닌 경우, 블록을 회전시키는 기본 동작을 수행
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
