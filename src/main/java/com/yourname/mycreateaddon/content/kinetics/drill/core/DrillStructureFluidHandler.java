package com.yourname.mycreateaddon.content.kinetics.drill.core;


import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DrillStructureFluidHandler implements IFluidHandler {

    private final List<IFluidHandler> handlers;

    public DrillStructureFluidHandler(List<IFluidHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public int getTanks() {
        int count = 0;
        for (IFluidHandler handler : handlers) {
            count += handler.getTanks();
        }
        return count;
    }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank) {
        int tankOffset = 0;
        for (IFluidHandler handler : handlers) {
            if (tank < tankOffset + handler.getTanks()) {
                return handler.getFluidInTank(tank - tankOffset);
            }
            tankOffset += handler.getTanks();
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        int tankOffset = 0;
        for (IFluidHandler handler : handlers) {
            if (tank < tankOffset + handler.getTanks()) {
                return handler.getTankCapacity(tank - tankOffset);
            }
            tankOffset += handler.getTanks();
        }
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        int tankOffset = 0;
        for (IFluidHandler handler : handlers) {
            if (tank < tankOffset + handler.getTanks()) {
                return handler.isFluidValid(tank - tankOffset, stack);
            }
            tankOffset += handler.getTanks();
        }
        return false;
    }

    @Override
    public int fill(FluidStack resource, @NotNull FluidAction action) {
        FluidStack remaining = resource.copy();
        int filledAmount = 0;

        for (IFluidHandler handler : handlers) {
            int filled = handler.fill(remaining, action);
            filledAmount += filled;
            remaining.shrink(filled);
            if (remaining.isEmpty()) {
                break;
            }
        }
        return filledAmount;
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, @NotNull FluidAction action) {
        if (resource.isEmpty() || handlers.isEmpty()) {
            return FluidStack.EMPTY;
        }

        FluidStack drainedStack = FluidStack.EMPTY;
        for (IFluidHandler handler : handlers) {
            FluidStack drained = handler.drain(resource, action);
            if (!drained.isEmpty()) {
                if (drainedStack.isEmpty()) {
                    drainedStack = drained;
                } else {
                    drainedStack.grow(drained.getAmount());
                }
                resource.shrink(drained.getAmount());
                if (resource.isEmpty()) {
                    break;
                }
            }
        }
        return drainedStack;
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, @NotNull FluidAction action) {
        if (maxDrain <= 0 || handlers.isEmpty()) {
            return FluidStack.EMPTY;
        }

        FluidStack drainedStack = FluidStack.EMPTY;
        for (IFluidHandler handler : handlers) {
            if (drainedStack.isEmpty()) {
                // 첫 번째로 드레인되는 유체를 기준으로 삼음
                drainedStack = handler.drain(maxDrain, action);
                if (!drainedStack.isEmpty()) {
                    maxDrain -= drainedStack.getAmount();
                }
            } else {
                // 이미 드레인된 유체와 동일한 종류만 추가로 드레인
                FluidStack toDrain = drainedStack.copy();
                toDrain.setAmount(maxDrain);
                FluidStack additionallyDrained = handler.drain(toDrain, action);
                if (!additionallyDrained.isEmpty()) {
                    drainedStack.grow(additionallyDrained.getAmount());
                    maxDrain -= additionallyDrained.getAmount();
                }
            }

            if (maxDrain <= 0) {
                break;
            }
        }
        return drainedStack;
    }
}