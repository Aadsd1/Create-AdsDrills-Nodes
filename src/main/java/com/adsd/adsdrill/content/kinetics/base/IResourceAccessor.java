package com.adsd.adsdrill.content.kinetics.base;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;


/**
 * 드릴 구조 내의 자원(아이템, 유체)에 대한 접근을 제공하는 인터페이스.
 * DrillCoreBlockEntity가 구현하며, 헤드나 다른 모듈에 이 인스턴스를 전달하여 사용합니다.
 */
public interface IResourceAccessor {

    /**
     * 드릴의 내부 아이템 버퍼에 직접 접근합니다.
     * @return 내부 아이템 핸들러
     */
    IItemHandler getInternalItemBuffer();

    /**
     * 드릴의 내부 유체 버퍼에 직접 접근합니다.
     * @return 내부 유체 핸들러
     */
    IFluidHandler getInternalFluidBuffer();

    /**
     * [신규] 내부 버퍼에서 에너지를 지정된 양만큼 소모합니다.
     * @param amount 소모할 에너지의 양 (FE)
     * @param simulate 시뮬레이션 여부 (true이면 실제로 소모하지 않음)
     * @return 실제로 소모된 에너지 양
     */
    int consumeEnergy(int amount, boolean simulate);
    /**
     * 내부 버퍼에서 특정 종류의 아이템을 지정된 양만큼 소모합니다.
     * @param stackToConsume 소모할 아이템의 종류와 개수
     * @param simulate 시뮬레이션 여부 (true이면 실제로 소모하지 않음)
     * @return 실제로 소모된 아이템 스택 (요청보다 적을 수 있음)
     */
    ItemStack consumeItems(ItemStack stackToConsume, boolean simulate);

    /**
     * 내부 버퍼에서 특정 종류의 유체를 지정된 양만큼 소모합니다.
     * @param fluidToConsume 소모할 유체의 종류와 양
     * @param simulate 시뮬레이션 여부 (true이면 실제로 소모하지 않음)
     * @return 실제로 소모된 유체 스택 (요청보다 적을 수 있음)
     */
    FluidStack consumeFluid(FluidStack fluidToConsume, boolean simulate);

}