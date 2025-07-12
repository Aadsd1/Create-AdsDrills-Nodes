package com.yourname.mycreateaddon.content.kinetics.module;

import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;

/**
 * 운동에너지 발전기 모듈이 회전력으로 에너지를 생성하는 로직을 처리합니다.
 */
public class KineticDynamoModuleBehavior implements IModuleBehavior {
    @Override
    public void onCoreTick(GenericModuleBlockEntity moduleBE, DrillCoreBlockEntity core) {
        float currentSpeed = Math.abs(moduleBE.getVisualSpeed());
        if (currentSpeed > 0) {
            int energyGenerated = (int) (currentSpeed / 4f); // 밸런스 조절 필요
            core.getInternalEnergyBuffer().receiveEnergy(energyGenerated, false);
        }
    }
}