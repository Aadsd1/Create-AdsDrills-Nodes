package com.adsd.adsdrill.content.kinetics.drill.core;


import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DrillStructureItemHandler implements IItemHandler {

    private final List<IItemHandler> handlers;
    private final int[] slotOffsets;
    private final int totalSlots;

    public DrillStructureItemHandler(List<IItemHandler> handlers) {
        this.handlers = handlers;
        this.slotOffsets = new int[handlers.size()];
        int currentOffset = 0;
        for (int i = 0; i < handlers.size(); i++) {
            this.slotOffsets[i] = currentOffset;
            currentOffset += handlers.get(i).getSlots();
        }
        this.totalSlots = currentOffset;
    }

    @Override
    public int getSlots() {
        return totalSlots;
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        for (int i = handlers.size() - 1; i >= 0; i--) {
            if (slot >= slotOffsets[i]) {
                return handlers.get(i).getStackInSlot(slot - slotOffsets[i]);
            }
        }
        return ItemStack.EMPTY;
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        // 직접 슬롯 지정 삽입은 복잡하므로, 가장 간단한 방식인 '순차적 삽입'을 먼저 구현합니다.
        // 이 메서드는 외부 자동화(파이프 등)가 특정 슬롯에 넣으려 할 때 호출됩니다.
        // 지금은 우선 순차 삽입만 지원하도록 insertItem(stack, simulate)로 넘깁니다.
        return insertItem(stack, simulate);
    }

    // 이 메서드는 Create의 휴대용 저장 인터페이스 등에서 주로 사용됩니다.
    public ItemStack insertItem(@NotNull ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (IItemHandler handler : handlers) {
            // [추천 수정안]
            // ItemHandlerHelper를 사용하여 각 핸들러에 아이템을 삽입합니다.
            // 이 메서드는 알아서 빈 슬롯이나 합칠 수 있는 슬롯을 찾아 아이템을 넣습니다.
            remaining = ItemHandlerHelper.insertItem(handler, remaining, simulate);

            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        for (int i = handlers.size() - 1; i >= 0; i--) {
            if (slot >= slotOffsets[i]) {
                return handlers.get(i).extractItem(slot - slotOffsets[i], amount, simulate);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        for (int i = handlers.size() - 1; i >= 0; i--) {
            if (slot >= slotOffsets[i]) {
                return handlers.get(i).getSlotLimit(slot - slotOffsets[i]);
            }
        }
        return 0;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        for (int i = handlers.size() - 1; i >= 0; i--) {
            if (slot >= slotOffsets[i]) {
                return handlers.get(i).isItemValid(slot - slotOffsets[i], stack);
            }
        }
        return false;
    }
}