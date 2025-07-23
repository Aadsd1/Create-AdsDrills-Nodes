package com.adsd.adsdrill.content.kinetics.drill.head;


import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlockEntity;
import com.adsd.adsdrill.registry.AdsDrillBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;
import java.util.Map;

public class HydraulicDrillHeadBlock extends AbstractDrillHeadBlock {

    private static final int WATER_CONSUMPTION = 50;
    private static final float STRESS_IMPACT = 6.0f;
    private static final float HEAT_GENERATION = 0.1f;
    private static final ResourceLocation SLUICE_TARGET_TAG = ResourceLocation.fromNamespaceAndPath("c", "raw_materials");

    public HydraulicDrillHeadBlock(Properties properties) {
        super(properties);
    }


    @Override
    public void onDrillTick(Level level, BlockPos headPos, BlockState headState, DrillCoreBlockEntity core) {
        if (level.isClientSide || core.getFinalSpeed() == 0) return;

        int waterConsumption = AdsDrillConfigs.SERVER.hydraulicWaterConsumption.get();
        FluidStack waterRequest = new FluidStack(net.minecraft.world.level.material.Fluids.WATER, waterConsumption);
        if (core.getInternalFluidBuffer().drain(waterRequest, IFluidHandler.FluidAction.SIMULATE).getAmount() < waterConsumption) {
            return;
        }

        Direction facing = headState.getValue(FACING);
        BlockPos nodePos = headPos.relative(facing);

        if (level.getBlockEntity(nodePos) instanceof OreNodeBlockEntity nodeBE) {
            Map<Item, Float> composition = nodeBE.getResourceComposition();
            if (composition.isEmpty()) return;

            core.getInternalFluidBuffer().drain(waterRequest, IFluidHandler.FluidAction.EXECUTE);

            int totalMiningAmount = (int) (Math.abs(core.getFinalSpeed()) / 16f); // 이 값도 설정으로 뺄 수 있습니다.
            if (totalMiningAmount <= 0) return;

            List<? extends String> bonusTagsStr = AdsDrillConfigs.SERVER.hydraulicBonusTags.get();
            List<TagKey<Item>> bonusTags = bonusTagsStr.stream()
                    .map(s -> TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse(s)))
                    .toList();
            double bonusMultiplier = AdsDrillConfigs.SERVER.hydraulicBonusMultiplier.get();

            for (Map.Entry<Item, Float> entry : composition.entrySet()) {
                Item item = entry.getKey();
                ItemStack itemStack = new ItemStack(item);

                // 이 아이템이 '원자재' 태그를 가지고 있는지 확인 (기존 로직)
                if (itemStack.is(ItemTags.create(SLUICE_TARGET_TAG))) {
                    int specificMiningAmount = (int) Math.ceil(totalMiningAmount * entry.getValue());

                    // 이 아이템이 보너스 태그 목록 중 하나에 속하는지 확인
                    boolean hasBonus = bonusTags.stream().anyMatch(itemStack::is);
                    if (hasBonus) {
                        specificMiningAmount *= (int) bonusMultiplier;
                    }

                    if (specificMiningAmount > 0) {
                        List<ItemStack> drops = core.mineSpecificNode(nodeBE, specificMiningAmount, 0, false, item);
                        for (ItemStack drop : drops) {
                            core.processMinedItem(drop);
                        }
                    }
                }
            }

            if (core.getTickCounter() % 5 == 0) {
                if (level instanceof ServerLevel serverLevel) {
                    double px = headPos.getX() + 0.5 + facing.getStepX() * 0.6;
                    double py = headPos.getY() + 0.5 + facing.getStepY() * 0.6;
                    double pz = headPos.getZ() + 0.5 + facing.getStepZ() * 0.6;
                    serverLevel.sendParticles(ParticleTypes.SPLASH, px, py, pz, 15, 0.2, 0.2, 0.2, 0.5);
                }
                level.playSound(null, headPos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.4f, 1.5f);
            }
        }
    }

    @Override public float getHeatGeneration() { return HEAT_GENERATION; }
    @Override public float getCoolingRate() { return 0.2f; }
    @Override public float getStressImpact() { return STRESS_IMPACT; }


    @Override
    public BlockEntityType<? extends AbstractDrillHeadBlockEntity> getBlockEntityType() {
        return AdsDrillBlockEntity.HYDRAULIC_DRILL_HEAD.get();
    }
}