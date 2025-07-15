package com.yourname.mycreateaddon.content.kinetics.module;


import com.simibubi.create.AllRecipeTypes;
import net.minecraft.world.item.crafting.RecipeType;


public enum ModuleType {
    // 기본 프레임
    FRAME(0.0f, 0.0f, 0.0f, 0, 0, 0, new FrameBehavior()),

    // 성능 강화 모듈
    SPEED(0.25f, 0.08f, 0.05f, 0, 0, 0, new FrameBehavior()),
    REINFORCEMENT(-0.1f, 0.0f, 0.0f, 0, 0, 0, new FrameBehavior()),
    EFFICIENCY(-0.15f, 0.01f, -0.2f, 0, 0, 0, new FrameBehavior()),

    // 처리 모듈
    FURNACE(0.1f, 0.0f, 0.0f, 0, 0, 0, new ProcessingModuleBehavior(() -> RecipeType.SMELTING)),
    BLAST_FURNACE(0.2f, 0.0f, 0.0f, 0, 0, 0, new ProcessingModuleBehavior(() -> RecipeType.BLASTING)),
    CRUSHER(0.4f, 0.0f, 0.0f, 0, 0, 0, new ProcessingModuleBehavior(AllRecipeTypes.CRUSHING::getType)),
    WASHER(0.05f, 0.0f, 0.0f, 0, 0, 0, new ProcessingModuleBehavior(AllRecipeTypes.SPLASHING::getType)),
    COMPACTOR(0.5f, 0.0f, 0.0f, 0, 0, 0, new CompactorModuleBehavior()),

    // 시스템 및 유틸리티 모듈
    COOLANT(0.05f, 0.0f, 0.0f, 0, 0, 0, new CoolantModuleBehavior()),
    HEATSINK(0.0f, 0.0f, -0.15f, 0, 0, 0, new FrameBehavior()),
    FILTER(0.0f, 0.0f, 0.0f, 1, 0, 0, new FilterModuleBehavior()),
    RESONATOR(0.0f, 0.0f, 0.0f, 0, 0, 0, new FrameBehavior()),
    REDSTONE_BRAKE(0.05f, 0.0f, 0.0f, 0, 0, 0, new FrameBehavior()),

    // 버퍼 모듈
    ITEM_BUFFER(0.01f, 0.0f, 0.0f, 16 * 64, 0, 0, new FrameBehavior()),
    FLUID_BUFFER(0.01f, 0.0f, 0.0f, 0, 16000, 0, new FrameBehavior()),

    // 에너지 관련 모듈
    ENERGY_INPUT(0.0f, 0.0f, 0.0f, 0, 0, 0, new EnergyInputModuleBehavior()),
    ENERGY_BUFFER(0.01f, 0.0f, 0.0f, 0, 0, 100000, new FrameBehavior()),
    KINETIC_DYNAMO(2.0f, 0.0f, 0.1f, 0, 0, 0, new KineticDynamoModuleBehavior());

    private final float stressImpact;
    private final float speedBonus;
    private final float heatModifier;
    private final int itemCapacity;
    private final int fluidCapacity;
    private final int energyCapacity;
    private final IModuleBehavior behavior;


    ModuleType(float stressImpact, float speedBonus, float heatModifier, int itemCapacity, int fluidCapacity, int energyCapacity, IModuleBehavior behavior) {
        this.stressImpact = stressImpact;
        this.speedBonus = speedBonus;
        this.heatModifier = heatModifier;
        this.itemCapacity = itemCapacity;
        this.fluidCapacity = fluidCapacity;
        this.energyCapacity = energyCapacity;
        this.behavior = behavior;
    }

    // --- Getter ---
    public float getStressImpact() { return stressImpact; }
    public float getSpeedBonus() { return speedBonus; }
    public float getHeatModifier() { return heatModifier; }
    public int getItemCapacity() { return itemCapacity; }
    public int getFluidCapacity() { return fluidCapacity; }
    public int getEnergyCapacity() { return energyCapacity; }
    public IModuleBehavior getBehavior() { return behavior; }

}