package com.yourname.mycreateaddon.content.kinetics.module;


public enum ModuleType {
    // ModuleType(스트레스, 속도보너스, 아이템용량, 유체용량)
    FRAME(0.5f, 0.0f, 0, 0),
    SPEED(1.0f, 0.1f, 0, 0),

    // [신규] 버퍼 모듈 타입 추가
    ITEM_BUFFER(0.2f, 0.0f, 16 * 64, 0),       // 아이템 16 스택 분량
    FLUID_BUFFER(0.2f, 0.0f, 0, 16000); // 16 버킷 분량

    private final float stressImpact;
    private final float speedBonus;
    private final int itemCapacity;
    private final int fluidCapacity;

    ModuleType(float stressImpact, float speedBonus, int itemCapacity, int fluidCapacity) {
        this.stressImpact = stressImpact;
        this.speedBonus = speedBonus;
        this.itemCapacity = itemCapacity;
        this.fluidCapacity = fluidCapacity;
    }

    public float getStressImpact() {
        return stressImpact;
    }

    public float getSpeedBonus() {
        return speedBonus;
    }
    public int getItemCapacity() { return itemCapacity; }
    public int getFluidCapacity() { return fluidCapacity; }

}