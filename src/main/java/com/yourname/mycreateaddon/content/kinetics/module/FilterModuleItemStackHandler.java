package com.yourname.mycreateaddon.content.kinetics.module;


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
        // 이 슬롯에 들어올 아이템이 유효한지 검사합니다.
        // 1. 아이템이 FilterItem의 인스턴스인지 확인합니다.
        // 2. 부모 클래스의 유효성 검사도 통과하는지 확인합니다.
        return stack.getItem() instanceof FilterItem && super.isItemValid(slot, stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        // 핸들러의 내용물이 바뀔 때마다 BlockEntity에 변경 사항을 알립니다.
        // 이는 월드 저장 및 클라이언트 동기화에 매우 중요합니다.
        blockEntity.setChanged();
    }
}
