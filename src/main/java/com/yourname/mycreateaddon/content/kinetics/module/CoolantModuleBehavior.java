package com.yourname.mycreateaddon.content.kinetics.module;


import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 냉각수 모듈의 열 관리 로직을 처리하는 행동 클래스입니다.
 */
public class CoolantModuleBehavior implements IModuleBehavior {

    private static final float COOLANT_ACTIVATION_HEAT = 5.0f;
    private static final int WATER_CONSUMPTION_PER_TICK = 5;
    private static final float HEAT_REDUCTION_PER_TICK = 0.4f;

    @Override
    public void onCoreTick(GenericModuleBlockEntity moduleBE, DrillCoreBlockEntity core) {
        Level level = moduleBE.getLevel();
        if (level == null) return;

        if (core.getHeat() > COOLANT_ACTIVATION_HEAT) {
            FluidStack waterToConsume = new FluidStack(Fluids.WATER, WATER_CONSUMPTION_PER_TICK);
            if (core.consumeFluid(waterToConsume, true).getAmount() == WATER_CONSUMPTION_PER_TICK) {
                core.consumeFluid(waterToConsume, false);
                core.addHeat(-HEAT_REDUCTION_PER_TICK);
                if (!level.isClientSide && level.getRandom().nextFloat() < 0.2f) {
                    level.playSound(null, moduleBE.getBlockPos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.5F + level.getRandom().nextFloat() * 0.5f);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.CLOUD, moduleBE.getBlockPos().getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8, moduleBE.getBlockPos().getY() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8, moduleBE.getBlockPos().getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8, 1, 0, 0.1, 0, 0.0);
                    }
                }
            }
        }
    }
}