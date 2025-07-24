package com.adsd.adsdrill.content.kinetics.module;


import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlockEntity;
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


    @Override
    public void onCoreTick(GenericModuleBlockEntity moduleBE, DrillCoreBlockEntity core) {
        Level level = moduleBE.getLevel();
        if (level == null || level.isClientSide) return;

        boolean shouldCool = false;

        // 1. 레드스톤 신호가 있는지 확인하여 작동 모드를 결정합니다.
        if (level.hasNeighborSignal(moduleBE.getBlockPos())) {
            // 레드스톤 모드: 코어가 과열되었을 때만 냉각합니다.
            if (core.isOverheated()) {
                shouldCool = true;
            }
        } else {
            // 기본 모드: 열이 활성화 수치를 넘으면 항상 냉각합니다.
            if (core.getHeat() > AdsDrillConfigs.SERVER.coolantActivationThreshold.get().floatValue()) {
                shouldCool = true;
            }
        }

        // 2. 냉각이 필요하다고 판단되면, 공통 냉각 로직을 실행합니다.
        if (shouldCool) {
            performCooling(moduleBE, core, level);
        }
    }

    /**
     * 실제 냉각 로직을 수행하는 헬퍼 메서드입니다.
     */
    private void performCooling(GenericModuleBlockEntity moduleBE, DrillCoreBlockEntity core, Level level) {
        // 설정 파일에서 틱당 물 소모량과 열 감소량을 가져옵니다.
        int waterConsumption = AdsDrillConfigs.SERVER.coolantWaterConsumption.get();
        float heatReduction = AdsDrillConfigs.SERVER.coolantHeatReduction.get().floatValue();

        FluidStack waterToConsume = new FluidStack(Fluids.WATER, waterConsumption);

        // 물이 충분한지 확인하고, 충분하다면 소모 및 냉각을 진행합니다.
        if (core.consumeFluid(waterToConsume, true).getAmount() == waterConsumption) {
            core.consumeFluid(waterToConsume, false); // 실제 물 소모
            core.addHeat(-heatReduction); // 열 감소

            // 파티클 및 사운드 효과
            if (level.getRandom().nextFloat() < 0.2f) {
                level.playSound(null, moduleBE.getBlockPos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.5F + level.getRandom().nextFloat() * 0.5f);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CLOUD, moduleBE.getBlockPos().getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8, moduleBE.getBlockPos().getY() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8, moduleBE.getBlockPos().getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8, 1, 0, 0.1, 0, 0.0);
                }
            }
        }
    }
}