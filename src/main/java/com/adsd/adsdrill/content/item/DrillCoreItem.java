package com.adsd.adsdrill.content.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlock;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlockEntity.Tier;
import org.jetbrains.annotations.NotNull;

public class DrillCoreItem extends BlockItem {
    public DrillCoreItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected BlockState getPlacementState(@NotNull BlockPlaceContext context) {
        BlockState stateToPlace = super.getPlacementState(context);
        if (stateToPlace == null) return null;

        ItemStack stack = context.getItemInHand();
        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);

        if (blockEntityData != null) {
            CompoundTag nbt = blockEntityData.copyTag();
            if (nbt.contains("CoreTier")) {
                try {
                    Tier tier = Tier.valueOf(nbt.getString("CoreTier"));
                    return stateToPlace.setValue(DrillCoreBlock.TIER, tier);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        // NBT가 없으면 기본 상태(황동)로 설치
        return stateToPlace;
    }
}