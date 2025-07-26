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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;
import java.util.Map;

public class HydraulicDrillHeadBlock extends AbstractDrillHeadBlock {

    private static final float STRESS_IMPACT = 6.0f;
    private static final float HEAT_GENERATION = 0.1f;
    private static final ResourceLocation SLUICE_TARGET_TAG = ResourceLocation.fromNamespaceAndPath("c", "raw_materials");

    public HydraulicDrillHeadBlock(Properties properties) {
        super(properties);
    }


    @Override
    public void onDrillTick(Level level, BlockPos headPos, BlockState headState, DrillCoreBlockEntity core) {
        if (level.isClientSide) return;

        float finalSpeed = core.getFinalSpeed();
        Direction facing = headState.getValue(FACING);
        BlockPos nodePos = headPos.relative(facing);

        if (!(level.getBlockEntity(nodePos) instanceof OreNodeBlockEntity nodeBE)) {
            return;
        }

        int waterConsumption = AdsDrillConfigs.SERVER.hydraulicWaterConsumption.get();
        FluidStack waterRequest = new FluidStack(net.minecraft.world.level.material.Fluids.WATER, waterConsumption);
        boolean hasEnoughWater = core.getInternalFluidBuffer().drain(waterRequest, IFluidHandler.FluidAction.SIMULATE).getAmount() >= waterConsumption;

        // 채굴 중지 조건: 속도가 0이거나 물이 부족하면 아무것도 하지 않음
        if (finalSpeed == 0 || !hasEnoughWater) {
            return;
        }

        Map<Item, Float> composition = nodeBE.getResourceComposition();
        if (composition.isEmpty()) return;

        // 조건이 충족되었으므로, 물을 실제로 소모하고 채굴을 진행
        core.getInternalFluidBuffer().drain(waterRequest, IFluidHandler.FluidAction.EXECUTE);

        int totalMiningAmount = (int) (Math.abs(core.getFinalSpeed()) / 16f);
        if (totalMiningAmount <= 0) return;

        List<? extends String> bonusTagsStr = AdsDrillConfigs.SERVER.hydraulicBonusTags.get();
        List<TagKey<Item>> bonusTags = bonusTagsStr.stream()
                .map(s -> TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse(s)))
                .toList();
        double bonusMultiplier = AdsDrillConfigs.SERVER.hydraulicBonusMultiplier.get();

        for (Map.Entry<Item, Float> entry : composition.entrySet()) {
            Item item = entry.getKey();
            ItemStack itemStack = new ItemStack(item);

            if (itemStack.is(ItemTags.create(SLUICE_TARGET_TAG))) {
                int specificMiningAmount = (int) Math.ceil(totalMiningAmount * entry.getValue());

                boolean hasBonus = bonusTags.stream().anyMatch(itemStack::is);
                if (hasBonus) {
                    specificMiningAmount *= (int) bonusMultiplier;
                }

                if (specificMiningAmount > 0) {
                    List<ItemStack> drops = core.mineSpecificNode(nodeBE, specificMiningAmount, 0, false, item, headPos);
                    for (ItemStack drop : drops) {
                        core.processMinedItem(drop);
                    }
                }
            }
        }

        if (core.getTickCounter() % 4 == 0) {
            if (level instanceof ServerLevel serverLevel) {
                Vec3 startPos = Vec3.atCenterOf(headPos).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.6));
                Vec3 endPos = Vec3.atCenterOf(nodePos);
                Vec3 direction = endPos.subtract(startPos).normalize();
                double speed = 0.6;
                double velX = direction.x * speed;
                double velY = direction.y * speed;
                double velZ = direction.z * speed;

                for (int i = 0; i < 8; i++) {
                    double px = startPos.x + (level.random.nextDouble() - 0.5) * 0.2;
                    double py = startPos.y + (level.random.nextDouble() - 0.5) * 0.2;
                    double pz = startPos.z + (level.random.nextDouble() - 0.5) * 0.2;
                    serverLevel.sendParticles(ParticleTypes.SPIT, px, py, pz, 1, velX, velY, velZ, 0.0);
                }

                Vec3 impactPos = Vec3.atCenterOf(nodePos).subtract(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.5));
                serverLevel.sendParticles(ParticleTypes.SPLASH, impactPos.x, impactPos.y, impactPos.z, 10, 0.3, 0.3, 0.3, 0.1);
            }

            level.playSound(null, headPos, SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.BLOCKS, 0.3f, 2.0f);
            level.playSound(null, nodePos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.2f, 1.8f);
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