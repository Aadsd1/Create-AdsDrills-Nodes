package com.adsd.adsdrill.content.kinetics.module;


import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * 에너지 입력 모듈이 자신의 버퍼에서 코어 버퍼로 에너지를 옮기는 로직을 처리합니다.
 */
public class EnergyInputModuleBehavior implements IModuleBehavior {

    private static final int MAX_ENERGY_PULL_PER_TICK = 4096; // 틱당 최대로 가져올 에너지 양 (설정 가능)

    @Override
    public void onCoreTick(GenericModuleBlockEntity moduleBE, DrillCoreBlockEntity core) {
        Level level = moduleBE.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        IEnergyStorage moduleBuffer = moduleBE.getEnergyHandler();
        if (moduleBuffer == null) {
            return;
        }

        // 모듈 버퍼에 아직 공간이 있다면 외부에서 에너지를 가져옵니다.
        if (moduleBuffer.getEnergyStored() < moduleBuffer.getMaxEnergyStored()) {
            for (Direction dir : moduleBE.getEnergyConnections()) {
                // 이웃 블록의 에너지 저장고(Capability)를 찾습니다.
                IEnergyStorage source = level.getCapability(Capabilities.EnergyStorage.BLOCK, moduleBE.getBlockPos().relative(dir), dir.getOpposite());

                // 소스가 존재하고, 에너지를 추출할 수 있는지 확인합니다.
                if (source != null && source.canExtract()) {
                    // 1. 모듈이 받을 수 있는 최대 에너지 양을 계산합니다.
                    int maxToReceive = Math.min(MAX_ENERGY_PULL_PER_TICK, moduleBuffer.getMaxEnergyStored() - moduleBuffer.getEnergyStored());

                    // 2. 소스에서 실제로 얼마나 에너지를 뺄 수 있는지 시뮬레이션합니다.
                    int energyAvailable = source.extractEnergy(maxToReceive, true);

                    if (energyAvailable > 0) {
                        // 3. 모듈 버퍼로 에너지를 실제로 받습니다.
                        int acceptedByModule = moduleBuffer.receiveEnergy(energyAvailable, false);

                        // 4. 실제로 받은 양만큼 소스에서 에너지를 추출합니다.
                        source.extractEnergy(acceptedByModule, false);
                    }
                }
            }
        }

        // 모듈 버퍼에 저장된 에너지를 코어로 전달합니다.
        int energyToTransfer = moduleBuffer.extractEnergy(MAX_ENERGY_PULL_PER_TICK, true);
        if (energyToTransfer > 0) {
            int acceptedByCore = core.getInternalEnergyBuffer().receiveEnergy(energyToTransfer, false);
            moduleBuffer.extractEnergy(acceptedByCore, false);
        }
    }
}