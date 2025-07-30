package com.adsd.adsdrill.content.kinetics.module;


import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.simibubi.create.AllRecipeTypes;
import net.minecraft.world.item.crafting.RecipeType;



public enum ModuleType {
    // 기본 프레임
    FRAME(0.0, 0.0, 0.0, 0, 0, 0, new FrameBehavior(), false),

    // 성능 강화 모듈
    SPEED(0.25, 0.08, 0.05, 0, 0, 0, new FrameBehavior(), true),
    REINFORCEMENT(-0.1, 0.0, 0.0, 0, 0, 0, new FrameBehavior(), true),
    EFFICIENCY(-0.15, 0.01, -0.2, 0, 0, 0, new FrameBehavior(), true),

    // 처리 모듈
    FURNACE(0.1, 0.0, 0.0, 0, 0, 0, new ProcessingModuleBehavior(() -> RecipeType.SMELTING), true),
    BLAST_FURNACE(0.2, 0.0, 0.0, 0, 0, 0, new ProcessingModuleBehavior(() -> RecipeType.BLASTING), true),
    CRUSHER(0.4, 0.0, 0.0, 0, 0, 0, new ProcessingModuleBehavior(AllRecipeTypes.CRUSHING::getType), true),
    WASHER(0.05, 0.0, 0.0, 0, 0, 0, new ProcessingModuleBehavior(AllRecipeTypes.SPLASHING::getType), true),
    COMPACTOR(0.5, 0.0, 0.0, 0, 0, 0, new CompactorModuleBehavior(), true),

    // 시스템 및 유틸리티 모듈
    COOLANT(0.05, 0.0, 0.0, 0, 0, 0, new CoolantModuleBehavior(), false),
    HEATSINK(0.0, 0.0, -0.15, 0, 0, 0, new FrameBehavior(), true),
    FILTER(0.0, 0.0, 0.0, 1, 0, 0, new FilterModuleBehavior(), false),
    RESONATOR(0.0, 0.0, 0.0, 0, 0, 0, new FrameBehavior(), false),
    REDSTONE_BRAKE(0.05, 0.0, 0.0, 0, 0, 0, new FrameBehavior(), false),

    // 버퍼 모듈
    ITEM_BUFFER(0.01, 0.0, 0.0, 16 * 64, 0, 0, new FrameBehavior(), true),
    FLUID_BUFFER(0.01, 0.0, 0.0, 0, 16000, 0, new FrameBehavior(), true),

    // 에너지 관련 모듈
    ENERGY_INPUT(0.0, 0.0, 0.0, 0, 0, 10000, new EnergyInputModuleBehavior(), false),
    ENERGY_BUFFER(0.01, 0.0, 0.0, 0, 0, 100000, new FrameBehavior(), true),
    KINETIC_DYNAMO(2.0, 0.0, 0.1, 0, 0, 0, new KineticDynamoModuleBehavior(), true);

    private final double defaultStressImpact;
    private final double defaultSpeedBonus;
    private final double defaultHeatModifier;

    private final int itemCapacity;
    private final int fluidCapacity;
    private final int energyCapacity;
    private final IModuleBehavior behavior;
    private final boolean isPerformanceModule;


    ModuleType(double stressImpact, double speedBonus, double heatModifier, int itemCapacity, int fluidCapacity, int energyCapacity, IModuleBehavior behavior, boolean isPerformanceModule) {
        this.defaultStressImpact = stressImpact;
        this.defaultSpeedBonus = speedBonus;
        this.defaultHeatModifier = heatModifier;
        this.itemCapacity = itemCapacity;
        this.fluidCapacity = fluidCapacity;
        this.energyCapacity = energyCapacity;
        this.behavior = behavior;
        this.isPerformanceModule = isPerformanceModule;
    }


    public double getStressImpact() { return AdsDrillConfigs.getModuleConfig(this).stressImpact(); }
    public double getSpeedBonus() { return AdsDrillConfigs.getModuleConfig(this).speedBonus(); }
    public double getHeatModifier() { return AdsDrillConfigs.getModuleConfig(this).heatModifier(); }

    public double getDefaultStressImpact() { return defaultStressImpact; }
    public double getDefaultSpeedBonus() { return defaultSpeedBonus; }
    public double getDefaultHeatModifier() { return defaultHeatModifier; }

    public int getItemCapacity() { return itemCapacity; }
    public int getFluidCapacity() { return fluidCapacity; }
    public int getEnergyCapacity() { return energyCapacity; }
    public IModuleBehavior getBehavior() { return behavior; }
    public String getSerializedName() { return this.name().toLowerCase(); }

    public boolean isPerformanceModule() { return isPerformanceModule; }
}