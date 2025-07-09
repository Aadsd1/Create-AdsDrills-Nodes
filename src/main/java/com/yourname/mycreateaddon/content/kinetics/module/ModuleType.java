package com.yourname.mycreateaddon.content.kinetics.module;


import java.util.function.Supplier; // [수정] Supplier 임포트
import com.simibubi.create.AllRecipeTypes;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;

public enum ModuleType {
    // ModuleType(스트레스 배율, 속도 배율, 열 배율, 아이템, 유체, 에너지, 레시피)
    // 배율은 (1.0 + 수치) 형태로 계산됨을 염두에 둘 것.

    // 기본 프레임. 효과 없음.
    FRAME(0.0f, 0.0f, 0.0f, 0, 0, 0),

    // 성능 강화 모듈
    SPEED(0.25f, 0.08f, 0.05f, 0, 0, 0),          // 스트레스 +25%, 속도 +8%, 열 +5%
    EFFICIENCY(0.0f, 0.0f, -0.2f, 0, 0, 0),       // 열 -20%
    REINFORCEMENT(-0.1f, 0.0f, 0.0f, 0, 0, 0),    // 스트레스 -10%

    // 처리 모듈 (처리 모듈은 기본적으로 스트레스를 더 많이 소모하도록 설정)
    FURNACE(0.1f, 0.0f, 0.0f, 0, 0, 0, () -> RecipeType.SMELTING),         // 스트레스 +10%
    BLAST_FURNACE(0.2f, 0.0f, 0.0f, 0, 0, 0, () -> RecipeType.BLASTING), // 스트레스 +20%
    CRUSHER(0.4f, 0.0f, 0.0f, 0, 0, 0, AllRecipeTypes.CRUSHING::getType), // 스트레스 +40%
    WASHER(0.05f, 0.0f, 0.0f, 0, 0, 0, AllRecipeTypes.SPLASHING::getType),// 스트레스 +5%
    COMPACTOR(0.5f, 0.0f, 0.0f, 0, 0, 0),                                  // 스트레스 +50%

    // 시스템 및 유틸리티 모듈
    COOLANT(0.05f, 0.0f, 0.0f, 0, 0, 0),         // 스트레스 +5% (냉각 펌프 작동 부하)
    HEATSINK(0.0f, 0.0f, -0.15f, 0, 0, 0),       // 열 -15%
    FILTER(0.0f, 0.0f, 0.0f, 1, 0, 0),           // 효과 없음
    RESONATOR(0.0f, 0.0f, 0.0f, 0, 0, 0),        // 효과 없음
    REDSTONE_BRAKE(0.05f, 0.0f, 0.0f, 0, 0, 0),   // 스트레스 +5% (제동 장치 부하)

    // 버퍼 모듈 (무게로 인한 약간의 스트레스 증가)
    ITEM_BUFFER(0.01f, 0.0f, 0.0f, 16 * 64, 0, 0), // 스트레스 +1%
    FLUID_BUFFER(0.01f, 0.0f, 0.0f, 0, 16000, 0),  // 스트레스 +1%

    // 에너지 관련 모듈
    ENERGY_INPUT(0.0f, 0.0f, 0.0f, 0, 0, 0),      // 효과 없음
    ENERGY_BUFFER(0.01f, 0.0f, 0.0f, 0, 0, 100000),// 스트레스 +1%
    KINETIC_DYNAMO(2.0f, 0.0f, 0.1f, 0, 0, 0);     // 스트레스 +200% (3배), 열 +10%

    private final float stressImpact;
    private final float speedBonus;
    private final float heatModifier;
    private final int itemCapacity;
    private final int fluidCapacity;
    private final int energyCapacity;
    private final Supplier<RecipeType<?>> recipeTypeSupplier;

    ModuleType(float stressImpact, float speedBonus, float heatModifier, int itemCapacity, int fluidCapacity, int energyCapacity, @Nullable Supplier<RecipeType<?>> recipeTypeSupplier) {
        this.stressImpact = stressImpact;
        this.speedBonus = speedBonus;
        this.heatModifier = heatModifier;
        this.itemCapacity = itemCapacity;
        this.fluidCapacity = fluidCapacity;
        this.energyCapacity = energyCapacity;
        this.recipeTypeSupplier = recipeTypeSupplier;
    }

    ModuleType(float stressImpact, float speedBonus, float heatModifier, int itemCapacity, int fluidCapacity, int energyCapacity) {
        this(stressImpact, speedBonus, heatModifier, itemCapacity, fluidCapacity, energyCapacity, null);
    }


    public float getStressImpact() {
        return stressImpact;
    }

    public float getSpeedBonus() {
        return speedBonus;
    }
    public int getItemCapacity() { return itemCapacity; }
    public float getHeatModifier() {
        return heatModifier;
    }
    public int getFluidCapacity() { return fluidCapacity; }
    public int getEnergyCapacity() {
        return energyCapacity;
    }
    @Nullable
    public Supplier<RecipeType<?>> getRecipeTypeSupplier() {
        return recipeTypeSupplier;
    }
}