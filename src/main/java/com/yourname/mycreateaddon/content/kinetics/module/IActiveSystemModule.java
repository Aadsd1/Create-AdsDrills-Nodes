package com.yourname.mycreateaddon.content.kinetics.module;

import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;

/**
 * 드릴 코어의 시스템(열, 에너지 등)에 매 틱마다 직접 관여하는 모듈을 위한 인터페이스입니다.
 */
public interface IActiveSystemModule {

    /**
     * 드릴 코어의 매 틱마다 서버 측에서 호출됩니다.
     * @param core 이 모듈을 제어하는 코어 인스턴스
     */
    void onCoreTick(DrillCoreBlockEntity core);

}