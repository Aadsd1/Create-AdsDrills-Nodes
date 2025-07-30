package com.adsd.adsdrill.content.kinetics.base;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * 용량을 동적으로 변경하고, 변경 시 콜백을 실행하는 기능을 추가한 커스텀 EnergyStorage 입니다.
 */
public class DrillEnergyStorage extends EnergyStorage {

    private final Runnable onEnergyChanged;

    public DrillEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onEnergyChanged) {
        super(capacity, maxReceive, maxExtract);
        this.onEnergyChanged = onEnergyChanged;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (received > 0 && !simulate) {
            onEnergyChanged.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (extracted > 0 && !simulate) {
            onEnergyChanged.run();
        }
        return extracted;
    }

    /**
     * 에너지 저장소의 최대 용량을 설정합니다.
     * 현재 에너지량이 새 용량을 초과하면, 현재 에너지량을 최대 용량으로 맞춥니다.
     */
    public void setCapacity(int newCapacity) {
        this.capacity = newCapacity;
        if (this.energy > this.capacity) {
            this.energy = this.capacity;
        }
        onEnergyChanged.run();
    }

    /**
     * 저장된 에너지 양을 강제로 설정합니다. NBT 로드 시 사용됩니다.
     */
    public void setEnergy(int energy) {
        this.energy = Mth.clamp(energy, 0, this.capacity);
        onEnergyChanged.run();
    }
}