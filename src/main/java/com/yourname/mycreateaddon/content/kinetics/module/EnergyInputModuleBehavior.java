package com.yourname.mycreateaddon.content.kinetics.module;


import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * 에너지 입력 모듈이 자신의 버퍼에서 코어 버퍼로 에너지를 옮기는 로직을 처리합니다.
 */
public class EnergyInputModuleBehavior implements IModuleBehavior {

    @Override
    public void onCoreTick(GenericModuleBlockEntity moduleBE, DrillCoreBlockEntity core) {
        IEnergyStorage moduleBuffer = moduleBE.getEnergyHandler();
        if (moduleBuffer != null) {
            int energyToTransfer = moduleBuffer.extractEnergy(1000, true);
            if (energyToTransfer > 0) {
                int accepted = core.getInternalEnergyBuffer().receiveEnergy(energyToTransfer, false);
                moduleBuffer.extractEnergy(accepted, false);
            }
        }
    }
}