package com.yourname.mycreateaddon.content.kinetics.module;


public enum ModuleType {
    // 모듈 종류를 여기에 정의합니다.
    // ModuleType(스트레스 영향, 속도 보너스 배율)
    FRAME(0.5f, 0.0f),
    SPEED(1.0f, 0.1f); // 스피드 모듈은 개당 속도를 10% 증가시킴

    private final float stressImpact;
    private final float speedBonus;

    ModuleType(float stressImpact, float speedBonus) {
        this.stressImpact = stressImpact;
        this.speedBonus = speedBonus;
    }

    public float getStressImpact() {
        return stressImpact;
    }

    public float getSpeedBonus() {
        return speedBonus;
    }
}