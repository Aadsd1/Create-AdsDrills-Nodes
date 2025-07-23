package com.adsd.adsdrill.content.kinetics.module;


import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.simibubi.create.AllRecipeTypes;
import net.minecraft.world.item.crafting.RecipeType;



public enum ModuleType {
    // 기본 프레임
    FRAME(0.0f, 0.0f, 0.0f, 0, 0, 0, new FrameBehavior(), false),

    // 성능 강화 모듈
    SPEED(0.25f, 0.08f, 0.05f, 0, 0, 0, new FrameBehavior(), true),
    REINFORCEMENT(-0.1f, 0.0f, 0.0f, 0, 0, 0, new FrameBehavior(), true),
    EFFICIENCY(-0.15f, 0.01f, -0.2f, 0, 0, 0, new FrameBehavior(), true),

    // 처리 모듈
    FURNACE(0.1f, 0.0f, 0.0f, 0, 0, 0, new ProcessingModuleBehavior(() -> RecipeType.SMELTING), true),
    BLAST_FURNACE(0.2f, 0.0f, 0.0f, 0, 0, 0, new ProcessingModuleBehavior(() -> RecipeType.BLASTING), true),
    CRUSHER(0.4f, 0.0f, 0.0f, 0, 0, 0, new ProcessingModuleBehavior(AllRecipeTypes.CRUSHING::getType), true),
    WASHER(0.05f, 0.0f, 0.0f, 0, 0, 0, new ProcessingModuleBehavior(AllRecipeTypes.SPLASHING::getType), true),
    COMPACTOR(0.5f, 0.0f, 0.0f, 0, 0, 0, new CompactorModuleBehavior(), true),

    // 시스템 및 유틸리티 모듈
    COOLANT(0.05f, 0.0f, 0.0f, 0, 0, 0, new CoolantModuleBehavior(), false),
    HEATSINK(0.0f, 0.0f, -0.15f, 0, 0, 0, new FrameBehavior(), true),
    FILTER(0.0f, 0.0f, 0.0f, 1, 0, 0, new FilterModuleBehavior(), false),
    RESONATOR(0.0f, 0.0f, 0.0f, 0, 0, 0, new FrameBehavior(), false),
    REDSTONE_BRAKE(0.05f, 0.0f, 0.0f, 0, 0, 0, new FrameBehavior(), false),

    // 버퍼 모듈
    ITEM_BUFFER(0.01f, 0.0f, 0.0f, 16 * 64, 0, 0, new FrameBehavior(), true),
    FLUID_BUFFER(0.01f, 0.0f, 0.0f, 0, 16000, 0, new FrameBehavior(), true),

    // 에너지 관련 모듈
    ENERGY_INPUT(0.0f, 0.0f, 0.0f, 0, 0, 10000, new EnergyInputModuleBehavior(), false),
    ENERGY_BUFFER(0.01f, 0.0f, 0.0f, 0, 0, 100000, new FrameBehavior(), true),
    KINETIC_DYNAMO(2.0f, 0.0f, 0.1f, 0, 0, 0, new KineticDynamoModuleBehavior(), true);

    private final float defaultStressImpact;
    private final float defaultSpeedBonus;
    private final float defaultHeatModifier;

    private final int itemCapacity;
    private final int fluidCapacity;
    private final int energyCapacity;
    private final IModuleBehavior behavior;
    private final boolean isPerformanceModule;


    ModuleType(float stressImpact, float speedBonus, float heatModifier, int itemCapacity, int fluidCapacity, int energyCapacity, IModuleBehavior behavior, boolean isPerformanceModule) {
        this.defaultStressImpact = stressImpact;
        this.defaultSpeedBonus = speedBonus;
        this.defaultHeatModifier = heatModifier;
        this.itemCapacity = itemCapacity;
        this.fluidCapacity = fluidCapacity;
        this.energyCapacity = energyCapacity;
        this.behavior = behavior;
        this.isPerformanceModule = isPerformanceModule;
    }

    public float getStressImpact() { return (float) AdsDrillConfigs.getModuleConfig(this).stressImpact(); }
    public float getSpeedBonus() { return (float) AdsDrillConfigs.getModuleConfig(this).speedBonus(); }
    public float getHeatModifier() { return (float) AdsDrillConfigs.getModuleConfig(this).heatModifier(); }

    public float getDefaultStressImpact() { return defaultStressImpact; }
    public float getDefaultSpeedBonus() { return defaultSpeedBonus; }
    public float getDefaultHeatModifier() { return defaultHeatModifier; }

    public int getItemCapacity() { return itemCapacity; }
    public int getFluidCapacity() { return fluidCapacity; }
    public int getEnergyCapacity() { return energyCapacity; }
    public IModuleBehavior getBehavior() { return behavior; }
    public String getSerializedName() { return this.name().toLowerCase(); }

    public boolean isPerformanceModule() { return isPerformanceModule; }
}