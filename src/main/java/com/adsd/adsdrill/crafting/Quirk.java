package com.adsd.adsdrill.crafting;


import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.adsd.adsdrill.content.kinetics.node.ArtificialNodeBlockEntity;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlockEntity;
import com.adsd.adsdrill.registry.AdsDrillItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                for (ItemStack stack : drops) {
                    int bonus = (int) Math.ceil(stack.getCount() * 0.1); // 10% 보너스
                    stack.grow(bonus);
                }
            }
        }
    },
    WILD_MAGIC("wild_magic", Tier.COMMON, AdsDrillItems.XOMV), // 효과가 클라이언트 사이드라 제외
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
                drops.add(new ItemStack(Items.EXPERIENCE_BOTTLE));
            }
        }
    },
    PURIFYING_RESONANCE("purifying_resonance", Tier.RARE, AdsDrillItems.IVORY_CRYSTAL) {
        @Override
        public Optional<Item> onSelectItemToDrop(QuirkContext context) {
            var config = AdsDrillConfigs.getQuirkConfig(this);
            if (config.isEnabled() && context.level().getRandom().nextFloat() < config.chance()) {
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
            checkForOppositePolarity(Quirk.POLARITY_NEGATIVE, context);
        }

        @Override
        public double onCalculateMiningAmount(double originalAmount, QuirkContext context) {
            // [!!! 신규: 배수 적용 로직 !!!]
            if (context.nodeBE().isPolarityBonusActive()) {
                return originalAmount * AdsDrillConfigs.getQuirkConfig(this).valueMultiplier();
            }
            return originalAmount;
        }
    },
    POLARITY_NEGATIVE("polarity_negative", Tier.RARE, AdsDrillItems.CINNABAR) {
        @Override
        public void onPeriodicTick(QuirkContext context) {
            checkForOppositePolarity(Quirk.POLARITY_POSITIVE, context);
        }

        @Override
        public double onCalculateMiningAmount(double originalAmount, QuirkContext context) {
            // [!!! 신규: 배수 적용 로직 !!!]
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
                // [!!! 신규: 로직을 Quirk enum 내부로 이전 !!!]
                generateBuriedTreasure(context, config.lootTables()).ifPresent(drops::add);
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
                LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, context.level());
                lightning.setPos(Vec3.atCenterOf(context.coreBE().getBlockPos()));
                context.level().addFreshEntity(lightning);
            }
        }
    },
    WITHERING_ECHO("withering_echo", Tier.EPIC, AdsDrillItems.THE_FOSSIL) {
        @Override
        public void onDrillCoreOverheat(QuirkContext context) {
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
            // 주변 4블록 내의 다른 노드에 재생력 부스트 부여
            BlockPos.betweenClosedStream(context.nodePos().offset(-4, -4, -4), context.nodePos().offset(4, 4, 4))
                    .forEach(pos -> {
                        if (pos.equals(context.nodePos())) return;
                        BlockEntity be = context.level().getBlockEntity(pos);
                        if (be instanceof OreNodeBlockEntity otherNode) {
                            otherNode.receiveRegenBoost();
                        }
                    });
        }
    },
    CHAOTIC_OUTPUT("chaotic_output", Tier.EPIC, AdsDrillItems.XOMV) {
        @Override
        public Optional<Item> onSelectItemToDrop(QuirkContext context) {
            var config = AdsDrillConfigs.getQuirkConfig(this);
            if (config.isEnabled() && context.level().getRandom().nextFloat() < config.chance()) {
                List<Item> allOres = context.nodeBE().getResourceComposition().keySet().stream().toList();
                if (!allOres.isEmpty()) {
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
    public record QuirkContext(ServerLevel level, BlockPos nodePos, OreNodeBlockEntity nodeBE, DrillCoreBlockEntity coreBE) {}
}