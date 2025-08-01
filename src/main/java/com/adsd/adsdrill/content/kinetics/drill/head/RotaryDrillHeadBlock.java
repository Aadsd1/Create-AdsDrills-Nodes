package com.adsd.adsdrill.content.kinetics.drill.head;


import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.adsd.adsdrill.registry.AdsDrillBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RotaryDrillHeadBlock extends AbstractDrillHeadBlock {

    private final float heatGeneration;
    private final float coolingRate;
    private final int miningLevel;
    private final float stressImpact;

    public RotaryDrillHeadBlock(Properties properties, float heatGeneration, float coolingRate, int miningLevel, float stressImpact) {
        super(properties);
        this.heatGeneration = heatGeneration;
        this.coolingRate = coolingRate;
        this.miningLevel = miningLevel;
        this.stressImpact = stressImpact;
    }

    @Override
    public void onDrillTick(Level level, BlockPos headPos, BlockState headState, DrillCoreBlockEntity core) {
        if (level.isClientSide) return;

        float finalSpeed = core.getFinalSpeed();
        Direction facing = headState.getValue(FACING);
        BlockPos nodePos = headPos.relative(facing);
        BlockEntity be = level.getBlockEntity(nodePos);

        if (!(be instanceof OreNodeBlockEntity nodeBE)) {
            return; // 앞에 노드가 없으면 아무것도 하지 않음
        }

        // 드릴 속도가 0이거나, 균열된 노드를 캘 수 없는 등급이면 아무것도 하지 않음
        if (finalSpeed == 0) {
            return;
        }
        if (nodeBE.isCracked() && this.miningLevel < 2) {
            return;
        }

        int miningAmount = (int) (Math.abs(finalSpeed) / AdsDrillConfigs.SERVER.rotarySpeedDivisor.get());

        if (miningAmount > 0) {
            int fortune = 0;
            boolean silkTouch = false;
            if (level.getBlockEntity(headPos) instanceof RotaryDrillHeadBlockEntity headBE) {
                fortune = headBE.getFortuneLevel();
                silkTouch = headBE.hasSilkTouch();
            }
            List<ItemStack> minedItems = core.mineNode(nodeBE, miningAmount, fortune, silkTouch, headPos);
            for (ItemStack minedItem : minedItems) {
                if (!minedItem.isEmpty()) {
                    core.processMinedItem(minedItem);
                }
            }
        }
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        Item itemInHand = stack.getItem();

        // 설정 파일에서 아이템 리스트와 아이템을 가져옵니다.
        List<? extends String> fortuneItemIds = AdsDrillConfigs.SERVER.rotaryDrillFortuneItems.get();
        Item silkTouchItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(AdsDrillConfigs.SERVER.rotaryDrillSilkTouchItem.get()));

        // 손에 든 아이템이 실크터치 아이템인지 확인
        boolean isSilkTouchUpgrade = (itemInHand == silkTouchItem);

        // 손에 든 아이템이 행운 업그레이드 아이템 리스트에 포함되어 있는지 확인
        boolean isFortuneUpgrade = fortuneItemIds.stream()
                .map(id -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)))
                .anyMatch(item -> item == itemInHand);

        // 둘 중 하나라도 해당되면 업그레이드 로직을 시도
        if (isSilkTouchUpgrade || isFortuneUpgrade) {
            if (!player.isShiftKeyDown()) {
                if (!level.isClientSide) {
                    withBlockEntityDo(level, pos, be -> {
                        if (be instanceof RotaryDrillHeadBlockEntity rotaryBE) {
                            rotaryBE.applyUpgrade(player, itemInHand);
                        }
                    });
                }
                return ItemInteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public float getHeatGeneration() { return this.heatGeneration; }
    @Override
    public float getCoolingRate() { return this.coolingRate; }
    @Override
    public float getStressImpact() { return this.stressImpact; }



    @Override
    public BlockEntityType<? extends AbstractDrillHeadBlockEntity> getBlockEntityType() {
        return AdsDrillBlockEntity.ROTARY_DRILL_HEAD.get();
    }
}