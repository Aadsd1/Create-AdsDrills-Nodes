package com.adsd.adsdrill.crafting;


import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.adsd.adsdrill.content.kinetics.node.ArtificialNodeBlockEntity;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlockEntity;
import com.adsd.adsdrill.registry.AdsDrillItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Supplier;


/**
 * 인공 광맥 노드의 특수 효과(Quirk)를 정의하는 열거형입니다.
 */
public enum Quirk {

    // Common Tier
    STEADY_HANDS("steady_hands", Tier.COMMON, AdsDrillItems.RAW_STEEL_CHUNK) {
        @Override
        public int onCalculateDrops(int originalCount, QuirkContext context) {
            return Math.max(2, originalCount);
        }
    },
    SIGNAL_AMPLIFICATION("signal_amplification", Tier.COMMON, AdsDrillItems.CINNABAR) {
        @Override
        public void onAfterDropsCalculated(List<ItemStack> drops, QuirkContext context) {
            if (context.level().hasNeighborSignal(context.nodePos())) {
                boolean applied = false;
                for (ItemStack stack : drops) {
                    int bonus = (int) Math.ceil(stack.getCount() * 0.1);
                    if (bonus > 0) {
                        stack.grow(bonus);
                        applied = true;
                    }
                }
                if (applied) {
                    context.playEffects((ParticleOptions) ParticleTypes.DUST, SoundEvents.NOTE_BLOCK_PLING.value(), 0.6f, 1.8f, 10, 0.1);
                }
            }
        }
    },

    WILD_MAGIC("wild_magic", Tier.COMMON, AdsDrillItems.XOMV) {},

    PETRIFIED_HEART("petrified_heart", Tier.COMMON, AdsDrillItems.SILKY_JEWEL) {
        @Override
        public float onCalculateRegeneration(float originalRegen, QuirkContext context) {
            return originalRegen + (originalRegen * (context.nodeBE().getHardness() * 0.25f));
        }
    },
    AQUIFER("aquifer", Tier.COMMON, AdsDrillItems.ULTRAMARINE) {
        @Override
        public float onCalculateHardness(float originalHardness, QuirkContext context) {
            var config = AdsDrillConfigs.getQuirkConfig(this);
            if (context.nodeBE().getFluid().getAmount() > 0) {
                float fluidPercentage = (float) context.nodeBE().getFluid().getAmount() / context.nodeBE().getMaxFluidCapacity();
                if (fluidPercentage >= config.minFluidPercentage() && fluidPercentage <= config.maxFluidPercentage()) {
                    if (context.level().getRandom().nextInt(10) == 0) {
                        context.playEffects(ParticleTypes.DRIPPING_DRIPSTONE_WATER, SoundEvents.AMBIENT_UNDERWATER_ENTER, 0.2f, 2.0f, 5, 0.05);
                    }
                    return originalHardness * (float) config.hardnessMultiplier();
                }
            }
            return originalHardness;
        }
    },

    TRACE_MINERALS("trace_minerals", Tier.COMMON, AdsDrillItems.CINNABAR) {
        @Override
        public void onAfterDropsCalculated(List<ItemStack> drops, QuirkContext context) {
            var config = AdsDrillConfigs.getQuirkConfig(this);
            if (config.isEnabled() && context.level().getRandom().nextFloat() < config.chance()) {
                context.playEffects(ParticleTypes.CRIT, SoundEvents.STONE_BREAK, 0.5f, 1.5f, 7, 0.1);
                drops.add(new ItemStack(Items.IRON_NUGGET));
            }
        }
    },

    // Rare Tier
    STATIC_CHARGE("static_charge", Tier.RARE, AdsDrillItems.THUNDER_STONE) {
        @Override
        public void onAfterDropsCalculated(List<ItemStack> drops, QuirkContext context) {
            var config = AdsDrillConfigs.getQuirkConfig(this);
            if (config.isEnabled() && context.level().isRaining() && context.level().getRandom().nextFloat() < config.chance()) {
                context.playEffects(ParticleTypes.ELECTRIC_SPARK, (SoundEvent) SoundEvents.TRIDENT_THUNDER, 0.2f, 1.7f, 20, 0.5);
                for (ItemStack stack : drops) {
                    stack.grow(stack.getCount());
                }
            }
        }
    },

    BONE_CHILL("bone_chill", Tier.RARE, AdsDrillItems.THE_FOSSIL) {
        @Override
        public void onAfterDropsCalculated(List<ItemStack> drops, QuirkContext context) {
            var config = AdsDrillConfigs.getQuirkConfig(this);
            if (config.isEnabled() && context.level().getRandom().nextFloat() < config.chance()) {
                context.playEffects(ParticleTypes.SOUL, SoundEvents.SKELETON_AMBIENT, 0.7f, 0.8f, 15, 0.3);
                Skeleton skeleton = new Skeleton(EntityType.SKELETON, context.level());
                skeleton.moveTo(context.nodePos().getX() + 0.5, context.nodePos().getY() + 1, context.nodePos().getZ() + 0.5, context.level().getRandom().nextFloat() * 360, 0);
                context.level().addFreshEntity(skeleton);
            }
        }
    },

    BOTTLED_KNOWLEDGE("bottled_knowledge", Tier.RARE, AdsDrillItems.ULTRAMARINE) {
        @Override
        public void onAfterDropsCalculated(List<ItemStack> drops, QuirkContext context) {
            var config = AdsDrillConfigs.getQuirkConfig(this);
            if (config.isEnabled() && context.level().getRandom().nextFloat() < config.chance()) {
                context.playEffects(ParticleTypes.ENCHANT, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f, 10, 0.2);
                drops.add(new ItemStack(Items.EXPERIENCE_BOTTLE));
            }
        }
    },

    PURIFYING_RESONANCE("purifying_resonance", Tier.RARE, AdsDrillItems.IVORY_CRYSTAL) {
        @Override
        public Optional<Item> onSelectItemToDrop(QuirkContext context) {
            var config = AdsDrillConfigs.getQuirkConfig(this);
            if (config.isEnabled() && context.level().getRandom().nextFloat() < config.chance()) {
                context.playEffects(ParticleTypes.END_ROD, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 2.0f, 15, 0.05);
                return context.nodeBE().getResourceComposition().entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey);
            }
            return Optional.empty();
        }
    },

    POLARITY_POSITIVE("polarity_positive", Tier.RARE, AdsDrillItems.CINNABAR) {
        @Override
        public void onPeriodicTick(QuirkContext context) {
            boolean wasActive = context.nodeBE().isPolarityBonusActive();
            checkForOppositePolarity(Quirk.POLARITY_NEGATIVE, context);
            boolean isActive = context.nodeBE().isPolarityBonusActive();
            if (!wasActive && isActive) {
                context.playEffects(ParticleTypes.HAPPY_VILLAGER, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.5f, 1.5f, 10, 0.1);
            }
        }

        @Override
        public double onCalculateMiningAmount(double originalAmount, QuirkContext context) {
            if (context.nodeBE().isPolarityBonusActive()) {
                return originalAmount * AdsDrillConfigs.getQuirkConfig(this).valueMultiplier();
            }
            return originalAmount;
        }
    },
    POLARITY_NEGATIVE("polarity_negative", Tier.RARE, AdsDrillItems.CINNABAR) {
        @Override
        public void onPeriodicTick(QuirkContext context) {
            boolean wasActive = context.nodeBE().isPolarityBonusActive();
            checkForOppositePolarity(Quirk.POLARITY_POSITIVE, context);
            boolean isActive = context.nodeBE().isPolarityBonusActive();
            if (!wasActive && isActive) {
                context.playEffects(ParticleTypes.HAPPY_VILLAGER, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.5f, 1.6f, 10, 0.1);
            }
        }
        @Override
        public double onCalculateMiningAmount(double originalAmount, QuirkContext context) {
            if (context.nodeBE().isPolarityBonusActive()) {
                return originalAmount * AdsDrillConfigs.getQuirkConfig(this).valueMultiplier();
            }
            return originalAmount;
        }
    },
    BURIED_TREASURE("buried_treasure", Tier.RARE, AdsDrillItems.SILKY_JEWEL) {
        @Override
        public void onAfterDropsCalculated(List<ItemStack> drops, QuirkContext context) {
            var config = AdsDrillConfigs.getQuirkConfig(this);
            if (config.isEnabled() && context.level().getRandom().nextFloat() < config.chance()) {
                generateBuriedTreasure(context, config.lootTables()).ifPresent(itemStack -> {
                    context.playEffects(ParticleTypes.TOTEM_OF_UNDYING, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.2f, 20, 0.2);
                    drops.add(itemStack);
                });
            }
        }

        /**
         * 이 특성 전용 헬퍼 메서드. 설정된 루트 테이블 목록에서 아이템을 생성합니다.
         */
        private Optional<ItemStack> generateBuriedTreasure(QuirkContext context, List<ResourceLocation> lootTableLocations) {
            if (lootTableLocations.isEmpty()) {
                return Optional.empty();
            }

            ServerLevel serverLevel = context.level();

            // 설정 파일에서 읽어온 목록을 ResourceKey로 변환
            List<ResourceKey<LootTable>> lootTables = lootTableLocations.stream()
                    .map(loc -> ResourceKey.create(Registries.LOOT_TABLE, loc))
                    .toList();

            ResourceKey<LootTable> chosenTableKey = lootTables.get(serverLevel.getRandom().nextInt(lootTables.size()));

            LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(chosenTableKey);

            LootParams lootParams = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(context.nodePos()))
                    .create(LootContextParamSets.CHEST);

            List<ItemStack> generatedLoot = lootTable.getRandomItems(lootParams);

            if (!generatedLoot.isEmpty()) {
                // 생성된 아이템 중 비어있지 않은 것만 필터링하여 하나를 무작위로 선택
                List<ItemStack> nonEmptyLoot = generatedLoot.stream().filter(s -> !s.isEmpty()).toList();
                if (!nonEmptyLoot.isEmpty()) {
                    return Optional.of(nonEmptyLoot.get(serverLevel.getRandom().nextInt(nonEmptyLoot.size())).copy());
                }
            }

            return Optional.empty();
        }
    },

    // Epic Tier
    OVERLOAD_DISCHARGE("overload_discharge", Tier.EPIC, AdsDrillItems.THUNDER_STONE) {
        @Override
        public void onDrillCoreOverheat(QuirkContext context) {
            var config = AdsDrillConfigs.getQuirkConfig(this);
            if (config.isEnabled() && context.level().getRandom().nextFloat() < config.chance()) {
                context.playEffectsAt(context.coreBE().getBlockPos(), ParticleTypes.FLASH, SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 1.0f, 1, 0.0);
                LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, context.level());
                lightning.setPos(Vec3.atCenterOf(context.coreBE().getBlockPos()));
                context.level().addFreshEntity(lightning);
            }
        }
    },

    WITHERING_ECHO("withering_echo", Tier.EPIC, AdsDrillItems.THE_FOSSIL) {
        @Override
        public void onDrillCoreOverheat(QuirkContext context) {
            context.playEffects(ParticleTypes.SOUL_FIRE_FLAME, SoundEvents.WITHER_SKELETON_HURT, 1.0f, 0.5f, 30, 0.4);
            WitherSkeleton witherSkeleton = new WitherSkeleton(EntityType.WITHER_SKELETON, context.level());
            witherSkeleton.moveTo(context.nodePos().getX() + 0.5, context.nodePos().getY() + 1, context.nodePos().getZ() + 0.5, context.level().getRandom().nextFloat() * 360, 0);
            context.level().addFreshEntity(witherSkeleton);
        }
    },
    GEMSTONE_FACETS("gemstone_facets", Tier.EPIC, AdsDrillItems.KOH_I_NOOR) {
        @Override
        public int onCalculateFortune(int originalFortune) {
            return originalFortune * 2;
        }
    },
    AURA_OF_VITALITY("aura_of_vitality", Tier.EPIC, AdsDrillItems.ROSE_GOLD) {

        @Override
        public void onPeriodicTick(QuirkContext context) {
            // --- 1. 플레이어 버프 로직 ---
            AABB playerScanArea = new AABB(context.nodePos()).inflate(5);
            List<Player> players = context.level().getEntitiesOfClass(Player.class, playerScanArea);

            if (!players.isEmpty()) {
                if (context.level().getRandom().nextInt(5) == 0) {
                    context.playEffects(ParticleTypes.HEART, SoundEvents.NOTE_BLOCK_BELL.value(), 0.3f, 1.8f, 2, 0.05);
                }
            }
            for (Player player : players) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 45, 0, true, false));
            }

            // --- 2. AOV 시너지 이펙트 로직 ---
            // 80틱(4초)마다 한 번씩, 서버 부하 분산을 위해 노드마다 다른 타이밍에 체크
            if (context.level().getGameTime() % 80 != (context.nodePos().hashCode() & 79)) {
                return;
            }

            final int scanRadius = 3;

            // 주변에서 가장 가까운 다른 AOV 노드를 찾습니다.
            Optional<BlockPos> closestAovNodeOpt = BlockPos.betweenClosedStream(
                            context.nodePos().offset(-scanRadius, -scanRadius, -scanRadius),
                            context.nodePos().offset(scanRadius, scanRadius, scanRadius)
                    )
                    .filter(pos -> !pos.equals(context.nodePos()))
                    .filter(pos -> {
                        BlockEntity be = context.level().getBlockEntity(pos);
                        return be instanceof ArtificialNodeBlockEntity otherNode && otherNode.hasQuirk(Quirk.AURA_OF_VITALITY);
                    })
                    .map(BlockPos::immutable)
                    .min(Comparator.comparingDouble(pos -> pos.distSqr(context.nodePos())));

            // 가장 가까운 AOV 노드를 찾았다면, 이펙트를 재생합니다.
            if (closestAovNodeOpt.isPresent()) {
                BlockPos targetPos = closestAovNodeOpt.get();
                Vec3 startVec = Vec3.atCenterOf(context.nodePos());
                Vec3 endVec = Vec3.atCenterOf(targetPos);

                // 두 노드 사이를 잇는 마법 파티클을 생성합니다.
                Vec3 direction = endVec.subtract(startVec);
                double distance = direction.length();
                if (distance < 1.0) return; // 바로 붙어있으면 이펙트 생략

                Vec3 step = direction.normalize().scale(0.4); // 파티클 간격
                int particleCount = (int)(distance / 0.4);

                for (int i = 0; i < particleCount; i++) {
                    Vec3 particlePos = startVec.add(step.scale(i));
                    double offsetX = (context.level().getRandom().nextDouble() - 0.5) * 0.2;
                    double offsetY = (context.level().getRandom().nextDouble() - 0.5) * 0.2;
                    double offsetZ = (context.level().getRandom().nextDouble() - 0.5) * 0.2;
                    context.level().sendParticles(ParticleTypes.ENCHANT, particlePos.x() + offsetX, particlePos.y() + offsetY, particlePos.z() + offsetZ, 1, 0, 0, 0, 0);
                }

                // 시작 노드에서만 소리를 재생하여 중복을 피합니다.
                context.level().playSound(null, context.nodePos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.5f, 2.0f);
            }
        }

        @Override
        public float onCalculateRegeneration(float originalRegen, QuirkContext context) {
            final int scanRadius = 3;

            List<OreNodeBlockEntity> nearbyNodes = new ArrayList<>();
            BlockPos.betweenClosedStream(
                            context.nodePos().offset(-scanRadius, -scanRadius, -scanRadius),
                            context.nodePos().offset(scanRadius, scanRadius, scanRadius)
                    )
                    .filter(pos -> !pos.equals(context.nodePos()))
                    .forEach(pos -> {
                        BlockEntity be = context.level().getBlockEntity(pos);
                        if (be instanceof OreNodeBlockEntity node) {
                            nearbyNodes.add(node);
                        }
                    });

            float bonusMultiplier = (float) (nearbyNodes.size() * 0.10);

            long synergyCount = nearbyNodes.stream()
                    .filter(node -> node instanceof ArtificialNodeBlockEntity artificialNode && artificialNode.hasQuirk(Quirk.AURA_OF_VITALITY))
                    .count();

            bonusMultiplier += (float) (synergyCount * 0.25);

            return originalRegen * (1.0f + bonusMultiplier);
        }
    },

    CHAOTIC_OUTPUT("chaotic_output", Tier.EPIC, AdsDrillItems.XOMV) {
        @Override
        public Optional<Item> onSelectItemToDrop(QuirkContext context) {
            var config = AdsDrillConfigs.getQuirkConfig(this);
            if (config.isEnabled() && context.level().getRandom().nextFloat() < config.chance()) {
                List<Item> allOres = context.nodeBE().getResourceComposition().keySet().stream().toList();
                if (!allOres.isEmpty()) {
                    context.playEffects(ParticleTypes.POOF, SoundEvents.ENDERMAN_TELEPORT, 0.5f, 1.7f, 15, 0.1);
                    return Optional.of(allOres.get(context.level().getRandom().nextInt(allOres.size())));
                }
            }
            return Optional.empty();
        }
    },
    VOLATILE_FISSURES("volatile_fissures", Tier.EPIC, AdsDrillItems.ROSE_GOLD) {
        @Override
        public void onYieldConsumed(QuirkContext context) {
            if (context.nodeBE() instanceof ArtificialNodeBlockEntity artificialNode) {
                artificialNode.incrementVolatileFissureCounter();
            }
        }
    };

    public enum Tier {
        COMMON("Common", ChatFormatting.WHITE),
        RARE("Rare", ChatFormatting.BLUE),
        EPIC("Epic", ChatFormatting.LIGHT_PURPLE);

        private final String name;
        private final ChatFormatting color;

        Tier(String name, ChatFormatting color) {
            this.name = name;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public ChatFormatting getColor() {
            return color;
        }
    }

    private final String id;
    private final Tier tier;
    private final Supplier<Item> catalyst; // 이 특성을 부여하는 주요 촉매 아이템


    Quirk(String id, Tier tier, Supplier<Item> catalyst) {
        this.id = id;
        this.tier = tier;
        this.catalyst = catalyst;
    }


    /**
     * @return 번역 키에 사용될 고유 ID (예: "quirk.adsdrill.steady_hands")
     */
    public String getId() {
        return id;
    }

    public Tier getTier() {
        return tier;
    }

    /**
     * @return 이 특성을 부여하는 주된 촉매 아이템 (Supplier)
     */
    public Supplier<Item> getCatalyst() {
        return catalyst;
    }

    /**
     * @return 번역된 특성 이름 (예: "굳건한 기반")
     */
    public Component getDisplayName() {
        return Component.translatable("quirk.adsdrill." + this.id);
    }

    /**
     * @return 번역된 특성 설명
     */
    public Component getDescription() {
        return Component.translatable("quirk.adsdrill." + this.id + ".description");
    }

    public void onAfterDropsCalculated(List<ItemStack> drops, QuirkContext context) {}
    public int onCalculateDrops(int originalCount, QuirkContext context) { return originalCount; }
    public int onCalculateFortune(int originalFortune) { return originalFortune; }
    public float onCalculateHardness(float originalHardness, QuirkContext context) { return originalHardness; }
    public float onCalculateRegeneration(float originalRegen, QuirkContext context) { return originalRegen; }
    public void onDrillCoreOverheat(QuirkContext context) {}

    public Optional<Item> onSelectItemToDrop(QuirkContext context) { return Optional.empty(); }
    public double onCalculateMiningAmount(double originalAmount, QuirkContext context) { return originalAmount; }
    public void onPeriodicTick(QuirkContext context) {}
    public void onYieldConsumed(QuirkContext context) {}
    private static void checkForOppositePolarity(Quirk oppositeQuirk, QuirkContext context) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = context.nodePos().relative(dir);
            BlockEntity be = context.level().getBlockEntity(neighborPos);
            if (be instanceof ArtificialNodeBlockEntity otherNode && otherNode.hasQuirk(oppositeQuirk)) {
                context.nodeBE().setPolarityBonusActive(true);
                return; // 하나라도 찾으면 즉시 종료
            }
        }
    }


    // --- 로직에 필요한 정보를 담는 컨텍스트 레코드 ---
    public record QuirkContext(ServerLevel level, BlockPos nodePos, OreNodeBlockEntity nodeBE, DrillCoreBlockEntity coreBE) {
        /**
         * 노드 위치에 파티클과 사운드 이펙트를 재생합니다.
         */
        public void playEffects(ParticleOptions particle, net.minecraft.sounds.SoundEvent sound, float volume, float pitch, int particleCount, double particleSpeed) {
            playEffectsAt(nodePos, particle, sound, volume, pitch, particleCount, particleSpeed);
        }

        /**
         * 지정된 위치에 파티클과 사운드 이펙트를 재생합니다.
         */
        public void playEffectsAt(BlockPos pos, ParticleOptions particle, net.minecraft.sounds.SoundEvent sound, float volume, float pitch, int particleCount, double particleSpeed) {
            Vec3 center = Vec3.atCenterOf(pos);
            level.sendParticles(particle, center.x(), center.y(), center.z(), particleCount, 0.5, 0.5, 0.5, particleSpeed);
            level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
        }
    }
}