package com.yourname.mycreateaddon.content.kinetics.module;


import java.util.function.Supplier; // [수정] Supplier 임포트
import com.simibubi.create.AllRecipeTypes;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;


public enum ModuleType {
    // ModuleType(스트레스, 속도, 열, 아이템, 유체, 에너지, 레시피)
    FRAME(0.5f, 0.0f, 0.0f, 0, 0, 0),
    SPEED(1.0f, 0.1f, 0.0f, 0, 0, 0),
    EFFICIENCY(0.7f, 0.0f, -0.2f, 0, 0, 0),
    REINFORCEMENT(-2.0f, 0.0f, 0.0f, 0, 0, 0),
    COMPACTOR(5.0f, 0.0f, 0.0f, 0, 0, 0),
    FILTER(1.0f, 0.0f, 0.0f, 1, 0, 0),
    COOLANT(1.2f, 0.0f, 0.0f, 0, 0, 0),
    HEATSINK(0.8f, 0.0f, -0.15f, 0, 0, 0),
    ITEM_BUFFER(0.2f, 0.0f, 0.0f, 16 * 64, 0, 0),
    FLUID_BUFFER(0.2f, 0.0f, 0.0f, 0, 16000, 0),

    // [신규] 에너지 관련 모듈
    ENERGY_INPUT(0.5f, 0.0f, 0.0f, 0, 0, 0), // 외부 에너지를 받는 포트
    ENERGY_BUFFER(0.2f, 0.0f, 0.0f, 0, 0, 100000), // 에너지 저장 용량 100kFE 증가
    KINETIC_DYNAMO(16.0f, 0.0f, 0.1f, 0, 0, 0), // 회전력을 FE로 변환 (높은 스트레스, 약간의 열 발생)
    // [신규] 공명기 모듈: 필터처럼 1칸의 인벤토리를 가짐
    RESONATOR(1.0f, 0.0f, 0.0f, 1, 0, 0),

    FURNACE(1.5f, 0.0f, 0.0f, 0, 0, 0, () -> RecipeType.SMELTING),
    BLAST_FURNACE(2.5f, 0.0f, 0.0f, 0, 0, 0, () -> RecipeType.BLASTING),
    CRUSHER(4.0f, 0.0f, 0.0f, 0, 0, 0, AllRecipeTypes.CRUSHING::getType),
    WASHER(1.0f, 0.0f, 0.0f, 0, 0, 0, AllRecipeTypes.SPLASHING::getType);

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