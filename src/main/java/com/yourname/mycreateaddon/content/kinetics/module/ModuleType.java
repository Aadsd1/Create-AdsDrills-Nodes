package com.yourname.mycreateaddon.content.kinetics.module;


import java.util.function.Supplier; // [수정] Supplier 임포트
import com.simibubi.create.AllRecipeTypes;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;

public enum ModuleType {
    // ModuleType(스트레스, 속도보너스, 열 효율 보너스, 아이템용량, 유체용량, 레시피)
    FRAME(0.5f, 0.0f, 0.0f, 0, 0),
    SPEED(1.0f, 0.1f, 0.0f, 0, 0),

    // [신규] 효율 모듈: 스트레스는 약간, 속도는 그대로, 열 발생량 20% 감소
    EFFICIENCY(0.7f, 0.0f, -0.2f, 0, 0),
    // [신규] 강화 모듈: 스트레스 -2.0 감소, 속도/열은 그대로
    REINFORCEMENT(-2.0f, 0.0f, 0.0f, 0, 0),

    // [신규] 압축 모듈: 스트레스 높음, 다른 보너스 없음.
    COMPACTOR(5.0f, 0.0f, 0.0f, 0, 0),

    // [신규] 냉각 모듈: 스트레스는 약간, 다른 보너스는 없음.
    COOLANT(1.2f, 0.0f, 0.0f, 0, 0),
    // [신규] 방열판 모듈: 스트레스는 프레임보다 약간 높고, 열 발생량 15% 감소
    HEATSINK(0.8f, 0.0f, -0.15f, 0, 0),

    ITEM_BUFFER(0.2f, 0.0f, 0.0f, 16 * 64, 0),
    FLUID_BUFFER(0.2f, 0.0f, 0.0f, 0, 16000),

    FURNACE(1.5f, 0.0f, 0.0f, 0, 0, () -> RecipeType.SMELTING),
    BLAST_FURNACE(2.5f, 0.0f, 0.0f, 0, 0, () -> RecipeType.BLASTING),
    CRUSHER(4.0f, 0.0f, 0.0f, 0, 0, AllRecipeTypes.CRUSHING::getType),
    WASHER(1.0f, 0.0f, 0.0f, 0, 0, AllRecipeTypes.SPLASHING::getType);

    private final float stressImpact;
    private final float speedBonus;
    private final float heatModifier; // [신규] 열 효율 보너스 필드
    private final int itemCapacity;
    private final int fluidCapacity;
    private final Supplier<RecipeType<?>> recipeTypeSupplier;

    // [수정] 생성자 및 오버로딩
    ModuleType(float stressImpact, float speedBonus, float heatModifier, int itemCapacity, int fluidCapacity, @Nullable Supplier<RecipeType<?>> recipeTypeSupplier) {
        this.stressImpact = stressImpact;
        this.speedBonus = speedBonus;
        this.heatModifier = heatModifier; // [신규]
        this.itemCapacity = itemCapacity;
        this.fluidCapacity = fluidCapacity;
        this.recipeTypeSupplier = recipeTypeSupplier;
    }

    ModuleType(float stressImpact, float speedBonus, float heatModifier, int itemCapacity, int fluidCapacity) {
        this(stressImpact, speedBonus, heatModifier, itemCapacity, fluidCapacity, null);
    }


    public float getStressImpact() {
        return stressImpact;
    }

    public float getSpeedBonus() {
        return speedBonus;
    }
    public int getItemCapacity() { return itemCapacity; }
    // [신규] Getter
    public float getHeatModifier() {
        return heatModifier;
    }
    public int getFluidCapacity() { return fluidCapacity; }
    // [수정] Getter 이름을 Supplier임을 명시하도록 변경
    @Nullable
    public Supplier<RecipeType<?>> getRecipeTypeSupplier() {
        return recipeTypeSupplier;
    }
}