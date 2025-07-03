package com.yourname.mycreateaddon.content.kinetics.module;


import java.util.function.Supplier; // [수정] Supplier 임포트
import com.simibubi.create.AllRecipeTypes;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;

public enum ModuleType {
    // ModuleType(스트레스, 속도보너스, 아이템용량, 유체용량)
    FRAME(0.5f, 0.0f, 0, 0),
    SPEED(1.0f, 0.1f, 0, 0),

    // [신규] 버퍼 모듈 타입 추가
    ITEM_BUFFER(0.2f, 0.0f, 16 * 64, 0),       // 아이템 16 스택 분량
    FLUID_BUFFER(0.2f, 0.0f, 0, 16000),// 16 버킷 분량



    // [수정] 현장 처리 모듈 타입 (Supplier 사용)
    FURNACE(1.5f, 0.0f, 0, 0, () -> RecipeType.SMELTING),
    BLAST_FURNACE(2.5f, 0.0f, 0, 0, () -> RecipeType.BLASTING),
    CRUSHER(4.0f, 0.0f, 0, 0, AllRecipeTypes.CRUSHING::getType),
    WASHER(1.0f, 0.0f, 0, 0, AllRecipeTypes.SPLASHING::getType);// Create 세척

    private final float stressImpact;
    private final float speedBonus;
    private final int itemCapacity;
    private final int fluidCapacity;

    private final Supplier<RecipeType<?>> recipeTypeSupplier;


    ModuleType(float stressImpact, float speedBonus, int itemCapacity, int fluidCapacity,  @Nullable Supplier<RecipeType<?>> recipeTypeSupplier) {
        this.stressImpact = stressImpact;
        this.speedBonus = speedBonus;
        this.itemCapacity = itemCapacity;
        this.fluidCapacity = fluidCapacity;
        this.recipeTypeSupplier = recipeTypeSupplier;
    }

    // [1단계 추가] 기존 모듈 호환성을 위한 오버로딩
    ModuleType(float stressImpact, float speedBonus, int itemCapacity, int fluidCapacity) {
        this(stressImpact, speedBonus, itemCapacity, fluidCapacity, null);
    }

    public float getStressImpact() {
        return stressImpact;
    }

    public float getSpeedBonus() {
        return speedBonus;
    }
    public int getItemCapacity() { return itemCapacity; }
    public int getFluidCapacity() { return fluidCapacity; }
    // [수정] Getter 이름을 Supplier임을 명시하도록 변경
    @Nullable
    public Supplier<RecipeType<?>> getRecipeTypeSupplier() {
        return recipeTypeSupplier;
    }
}