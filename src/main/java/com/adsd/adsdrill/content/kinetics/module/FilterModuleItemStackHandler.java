package com.adsd.adsdrill.content.kinetics.module;


import com.simibubi.create.content.logistics.filter.FilterItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 필터 모듈 전용으로 사용되는 아이템 핸들러입니다.
 * 오직 필터 아이템만 슬롯에 들어갈 수 있도록 강제합니다.
 */
public class FilterModuleItemStackHandler extends ItemStackHandler {

    private final GenericModuleBlockEntity blockEntity;

    public FilterModuleItemStackHandler(int size, GenericModuleBlockEntity be) {
        super(size);
        this.blockEntity = be;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return stack.getItem() instanceof FilterItem && super.isItemValid(slot, stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        blockEntity.setChanged();
    }
}
