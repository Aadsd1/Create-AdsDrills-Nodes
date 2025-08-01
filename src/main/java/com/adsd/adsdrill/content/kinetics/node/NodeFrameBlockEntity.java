package com.adsd.adsdrill.content.kinetics.node;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.content.item.StabilizerCoreItem;
import com.adsd.adsdrill.content.kinetics.drill.head.RotaryDrillHeadBlockEntity;
import com.adsd.adsdrill.crafting.Quirk;
import com.adsd.adsdrill.registry.AdsDrillBlocks;
import com.adsd.adsdrill.registry.AdsDrillTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.*;
import java.util.stream.Collectors;

public class NodeFrameBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    protected ItemStackHandler inventory = createInventory();
    private int progress = 0;
    private static final int REQUIRED_PROGRESS = AdsDrillConfigs.SERVER.nodeFrameRequiredProgress.get();
    private boolean isCompleting = false;

    // 인벤토리 슬롯 상수 정의
    private static final int DATA_SLOT_START = 0;
    private static final int DATA_SLOT_COUNT = 9;
    private static final int CORE_SLOT = 9;
    private static final int CATALYST_SLOT_1 = 10;
    private static final int CATALYST_SLOT_2 = 11;
    private static final int INVENTORY_SIZE = DATA_SLOT_COUNT + 1 + 2; // 데이터 + 코어 + 촉매

    private transient Map<Quirk, Float> clientQuirkCandidates = new HashMap<>();

    public NodeFrameBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    private ItemStackHandler createInventory() {
        return new ItemStackHandler(INVENTORY_SIZE) {
            @Override
            protected void onContentsChanged(int slot) {
                if (level != null && !level.isClientSide) {
                    updateQuirkCandidates();
                }
                setChanged();
                sendData();
            }
        };
    }
    // 특수 효과 후보군과 가중치를 계산하는 메서드
    private void updateQuirkCandidates() {
        if (level == null) return;

        Map<Quirk, Float> candidates = new HashMap<>();
        ItemStack coreStack = inventory.getStackInSlot(CORE_SLOT);
        StabilizerCoreItem.Tier tier = (coreStack.getItem() instanceof StabilizerCoreItem sci) ? sci.getTier() : StabilizerCoreItem.Tier.BRASS;

        // 실제 생성 로직과 동일하게, 코어 등급에 따라 가능한 특성 Tier를 먼저 결정합니다.
        List<Quirk.Tier> possibleTiers = new ArrayList<>();
        switch (tier) {
            case BRASS -> {
                possibleTiers.add(Quirk.Tier.COMMON);
                possibleTiers.add(Quirk.Tier.RARE);
            }
            case STEEL, NETHERITE -> {
                possibleTiers.add(Quirk.Tier.COMMON);
                possibleTiers.add(Quirk.Tier.RARE);
                possibleTiers.add(Quirk.Tier.EPIC);
            }
        }

        // 가능한 Tier에 속하는 Quirk들만 초기 후보 목록에 추가합니다.
        for (Quirk quirk : Quirk.values()) {
            if (possibleTiers.contains(quirk.getTier())) {
                candidates.put(quirk, 3.0f);
            }
        }

        // 촉매 보너스 및 Steel 등급의 특수 제외 처리
        ItemStack catalyst1 = inventory.getStackInSlot(CATALYST_SLOT_1);
        ItemStack catalyst2 = inventory.getStackInSlot(CATALYST_SLOT_2);
        final float CATALYST_BONUS = 15.0f;

        if (!catalyst1.isEmpty()) {
            candidates.replaceAll((quirk, weight) ->
                    quirk.getCatalyst().get() == catalyst1.getItem() ? weight + CATALYST_BONUS : weight
            );
        }
        if (!catalyst2.isEmpty()) {
            candidates.replaceAll((quirk, weight) ->
                    quirk.getCatalyst().get() == catalyst2.getItem() ? weight + CATALYST_BONUS : weight
            );
        }

        if (tier == StabilizerCoreItem.Tier.STEEL) {
            candidates.remove(Quirk.OVERLOAD_DISCHARGE);
            candidates.remove(Quirk.BONE_CHILL);
            candidates.remove(Quirk.WITHERING_ECHO);
            candidates.remove(Quirk.WILD_MAGIC);
        }

        // 계산된 결과를 클라이언트 동기화용 필드에 저장
        this.clientQuirkCandidates = candidates;
    }

    public boolean isCatalystItem(ItemStack stack) {
        return stack.is(AdsDrillTags.CATALYSTS);
    }

    public boolean addData(ItemStack dataStack) {
        for (int i = DATA_SLOT_START; i < DATA_SLOT_START + DATA_SLOT_COUNT; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, dataStack.split(1));
                playSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 1.0f);
                return true;
            }
        }
        playSound(SoundEvents.VILLAGER_NO, 1.0f);
        return false;
    }


    public boolean addStabilizerCore(ItemStack coreStack) {
        if (inventory.getStackInSlot(CORE_SLOT).isEmpty()) {
            inventory.setStackInSlot(CORE_SLOT, coreStack.split(1));
            playSound(SoundEvents.END_PORTAL_FRAME_FILL, 1.2f);
            return true;
        }
        playSound(SoundEvents.VILLAGER_NO, 1.0f);
        return false;
    }
    public boolean addCatalyst(ItemStack catalystStack) {
        if (inventory.getStackInSlot(CATALYST_SLOT_1).isEmpty()) {
            inventory.setStackInSlot(CATALYST_SLOT_1, catalystStack.split(1));
            playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f);
            return true;
        }
        if (inventory.getStackInSlot(CATALYST_SLOT_2).isEmpty()) {
            inventory.setStackInSlot(CATALYST_SLOT_2, catalystStack.split(1));
            playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f);
            return true;
        }
        playSound(SoundEvents.VILLAGER_NO, 1.0f);
        return false;
    }

    public void retrieveItem(Player player) {
        for (int i = INVENTORY_SIZE - 1; i >= 0; i--) { // 11부터 0까지
            ItemStack stackInSlot = inventory.getStackInSlot(i);
            if (!stackInSlot.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(player, stackInSlot);
                inventory.setStackInSlot(i, ItemStack.EMPTY);
                playSound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 1.0f);
                return;
            }
        }
    }

    // 사운드 재생 헬퍼 메서드
    private void playSound(net.minecraft.sounds.SoundEvent sound, float pitch) {
        if (level != null) {
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.5f, pitch);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        assert level instanceof ServerLevel; // 서버 전용 로직임을 명시

        // 진행률 동기화를 위해 이전 progress 값을 기억합니다.
        int oldProgress = this.progress;

        // 1. 제작 준비 확인
        ItemStack coreStack = inventory.getStackInSlot(9);
        boolean hasData = false;
        for (int i = 0; i < 9; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                hasData = true;
                break;
            }
        }

        // 코어나 데이터가 없으면 진행도를 점차 감소시킴
        if (coreStack.isEmpty() || !hasData) {
            if (progress > 0) {
                progress = Math.max(0, progress - 1024);
            }
        } else {
            // 2. 코어 등급에 따른 파라미터 설정
            int requiredSpeed = getRequiredSpeed(); // 고글 툴팁용으로 만든 헬퍼 메서드 재활용
            boolean resetOnFail = false;
            int progressDecay = 128;

            if (coreStack.getItem() instanceof StabilizerCoreItem stabilizer) {
                switch (stabilizer.getTier()) {
                    case BRASS -> progressDecay = AdsDrillConfigs.SERVER.brassCoreFailurePenalty.get();
                    case STEEL -> progressDecay = AdsDrillConfigs.SERVER.steelCoreFailurePenalty.get();
                    case NETHERITE -> resetOnFail = true;
                }
            }

            // 3. 진행도 업데이트
            int currentDrillSpeed = getDrillSpeedAbove();

            if (currentDrillSpeed >= requiredSpeed) {
                progress += currentDrillSpeed;

                // 제작 진행 피드백 (파티클 및 사운드)
                if (level.getRandom().nextInt(10) == 0) {
                    ((ServerLevel) level).sendParticles(ParticleTypes.ENCHANT,
                            worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                            2, 0.3, 0.1, 0.3, 0.05);
                }
                // 제작 막바지에 더 화려한 효과 추가
                if (progress > REQUIRED_PROGRESS * 0.9f && level.getRandom().nextInt(5) == 0) {
                    ((ServerLevel) level).sendParticles(ParticleTypes.PORTAL,
                            worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                            1, 0.5, 0.5, 0.5, 0.0);
                    level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.3f, 1.8f);
                }

            } else {
                // 제작 실패 시 진행도 감소
                if (progress > 0) {
                    if (resetOnFail) {
                        progress = 0;
                        // 네더라이트 코어 실패 피드백 (강력함)
                        ((ServerLevel) level).sendParticles(ParticleTypes.LAVA,
                                worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                                10, 0.4, 0.2, 0.4, 0.0);
                        level.playSound(null, worldPosition, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0f, 0.8f);
                    } else {
                        progress = Math.max(0, progress - progressDecay);
                        // 일반 실패 피드백 (약함)
                        if (level.getRandom().nextInt(10) == 0) {
                            ((ServerLevel) level).sendParticles(ParticleTypes.SMOKE,
                                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                                    5, 0.4, 0.2, 0.4, 0.01);
                        }

                    }
                    setChanged();
                    sendData();
                }
            }
        }


        // 4. 제작 완료 확인
        if (progress >= REQUIRED_PROGRESS) {
            completeCrafting();
            return; // 제작 완료 시, 아래의 동기화 로직을 실행할 필요 없음
        }

        // 5. 진행률 변경 시 클라이언트에 동기화
        if (oldProgress != this.progress) {
            // 10틱(0.5초)마다 동기화하여 부하를 줄임
            if (level.getGameTime() % 10 == 0) {
                setChanged();
                sendData();
            }
        }
    }

    private int getDrillSpeedAbove() {
        assert level != null;
        BlockEntity aboveBE = level.getBlockEntity(worldPosition.above());

        if (aboveBE instanceof RotaryDrillHeadBlockEntity headBE) {

            if (headBE.getCore() != null) {
                return (int) Math.abs(headBE.getCore().getFinalSpeed());
            }
        }
        return 0;
    }



    public void completeCrafting() {
        if (level == null || level.isClientSide) return;

        // 1. 데이터 수집 및 계산
        CraftingData craftingData = collectCraftingData();

        // 2. 코어 등급에 따른 최종 속성 계산
        StabilizerCoreItem.Tier tier = getCoreItemTier();
        FinalAttributes attributes = calculateFinalAttributes(craftingData, tier);

        // 3. 특성(Quirk) 생성
        List<Quirk> finalQuirks = generateQuirks(tier);

        // 4. 유체 처리
        FluidData fluidData = processFluids(craftingData.fluidCapacities, attributes.yieldMultiplier);

        // 5. 블록 교체 및 데이터 주입
        replaceWithArtificialNode(attributes, finalQuirks, craftingData, fluidData);

        // 6. 완료 효과
        playCraftingCompleteEffects();
    }

    /**
     * 인벤토리에서 제작 데이터를 수집합니다.
     */
    private CraftingData collectCraftingData() {
        Map<Item, Float> weightedComposition = new HashMap<>();
        Map<Item, Block> finalItemToBlockMap = new HashMap<>();
        Map<Fluid, Integer> fluidCapacities = new HashMap<>();
        float totalInputYield = 0;

        // 데이터 슬롯 처리
        for (int i = DATA_SLOT_START; i < CORE_SLOT; i++) {
            ItemStack dataStack = inventory.getStackInSlot(i);
            if (dataStack.isEmpty()) continue;

            CompoundTag nbt = extractCustomData(dataStack);

            totalInputYield += processDataSlot(nbt, weightedComposition, finalItemToBlockMap);
        }

        // 유체 데이터 처리
        processFluidData(fluidCapacities);

        // 기본값 설정 (빈 경우)
        if (weightedComposition.isEmpty()) {
            setDefaultComposition(weightedComposition, finalItemToBlockMap);
            totalInputYield = 1000;
        }

        return new CraftingData(weightedComposition, finalItemToBlockMap, fluidCapacities, totalInputYield);
    }

    /**
     * 아이템 스택에서 커스텀 데이터를 추출합니다.
     */
    private CompoundTag extractCustomData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        assert customData != null;
        return customData.copyTag();
    }

    /**
     * 개별 데이터 슬롯을 처리합니다.
     */
    private float processDataSlot(CompoundTag nbt, Map<Item, Float> weightedComposition,
                                  Map<Item, Block> itemToBlockMap) {
        float yield = nbt.getFloat("Yield");

        processComposition(nbt, weightedComposition, yield);
        processItemToBlockMapping(nbt, itemToBlockMap);

        return yield;
    }

    /**
     * 조성 데이터를 처리합니다.
     */
    private void processComposition(CompoundTag nbt, Map<Item, Float> weightedComposition, float yield) {
        ListTag compositionList = nbt.getList("Composition", 10);

        for (int i = 0; i < compositionList.size(); i++) {
            CompoundTag entry = compositionList.getCompound(i);
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getString("Item")));
            float ratio = entry.getFloat("Ratio");
            weightedComposition.merge(item, ratio * yield, Float::sum);
        }
    }

    /**
     * 아이템-블록 매핑을 처리합니다.
     */
    private void processItemToBlockMapping(CompoundTag nbt, Map<Item, Block> itemToBlockMap) {
        ListTag itemToBlockList = nbt.getList("ItemToBlockMap", 10);

        for (int i = 0; i < itemToBlockList.size(); i++) {
            CompoundTag entry = itemToBlockList.getCompound(i);
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getString("Item")));
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.getString("Block")));

            if (item != Items.AIR && block != Blocks.AIR) {
                itemToBlockMap.putIfAbsent(item, block);
            }
        }
    }

    /**
     * 유체 데이터를 처리합니다.
     */
    private void processFluidData(Map<Fluid, Integer> fluidCapacities) {
        for (int i = 0; i < DATA_SLOT_COUNT; i++) {
            ItemStack dataStack = inventory.getStackInSlot(i);
            if (dataStack.isEmpty()) continue;

            CompoundTag nbt = extractCustomData(dataStack);
            if (!nbt.contains("FluidContent")) continue;

            assert level != null;
            FluidStack fluid = FluidStack.parse(level.registryAccess(), nbt.getCompound("FluidContent"))
                    .orElse(FluidStack.EMPTY);
            if (!fluid.isEmpty()) {
                int capacity = nbt.getInt("MaxFluidCapacity");
                fluidCapacities.merge(fluid.getFluid(), capacity, Integer::sum);
            }
        }
    }

    /**
     * 기본 조성을 설정합니다.
     */
    private void setDefaultComposition(Map<Item, Float> weightedComposition, Map<Item, Block> itemToBlockMap) {
        weightedComposition.put(Items.RAW_IRON, 1.0f);
        itemToBlockMap.put(Items.RAW_IRON, Blocks.IRON_ORE);
    }

    /**
     * 코어 아이템의 등급을 가져옵니다.
     */
    private StabilizerCoreItem.Tier getCoreItemTier() {
        ItemStack coreStack = inventory.getStackInSlot(CORE_SLOT);
        return (coreStack.getItem() instanceof StabilizerCoreItem sci) ?
                sci.getTier() : StabilizerCoreItem.Tier.BRASS;
    }

    /**
     * 최종 속성을 계산합니다.
     */
    private FinalAttributes calculateFinalAttributes(CraftingData craftingData, StabilizerCoreItem.Tier tier) {
        // 조성 정규화
        Map<Item, Float> finalComposition = normalizeComposition(craftingData.weightedComposition);

        // 수율 계산
        float yieldMultiplier = getYieldMultiplier(tier);
        int finalMaxYield = (int) (craftingData.totalInputYield * yieldMultiplier);

        // 특성 값 계산
        assert level != null;
        RandomSource random = level.getRandom();
        AttributeValues values = generateAttributeValues(tier, random);

        return new FinalAttributes(
                finalComposition, yieldMultiplier, finalMaxYield,
                values.hardness, values.richness, values.regeneration
        );
    }

    /**
     * 조성을 정규화합니다.
     */
    private Map<Item, Float> normalizeComposition(Map<Item, Float> weightedComposition) {
        Map<Item, Float> finalComposition = new HashMap<>();
        float totalWeight = weightedComposition.values().stream().reduce(0f, Float::sum);

        if (totalWeight > 0) {
            for (Map.Entry<Item, Float> entry : weightedComposition.entrySet()) {
                finalComposition.put(entry.getKey(), entry.getValue() / totalWeight);
            }
        } else {
            finalComposition.put(Items.RAW_IRON, 1.0f);
        }

        return finalComposition;
    }

    /**
     * 등급에 따른 수율 배수를 반환합니다.
     */
    private float getYieldMultiplier(StabilizerCoreItem.Tier tier) {
        return switch (tier) {
            case BRASS -> 0.8f;
            case STEEL -> 1.0f;
            case NETHERITE -> 1.25f;
        };
    }

    /**
     * 등급에 따른 속성 값을 생성합니다.
     */
    private AttributeValues generateAttributeValues(StabilizerCoreItem.Tier tier, RandomSource random) {
        return switch (tier) {
            case BRASS -> new AttributeValues(
                    0.6f + (random.nextFloat() * 0.4f),
                    0.9f + (random.nextFloat() * 0.2f),
                    0.0005f + (random.nextFloat() * 0.001f)
            );
            case STEEL -> new AttributeValues(
                    0.9f + (random.nextFloat() * 0.6f),
                    1.05f + (random.nextFloat() * 0.3f),
                    0.0015f + (random.nextFloat() * 0.002f)
            );
            case NETHERITE -> new AttributeValues(
                    1.4f + (random.nextFloat() * 0.8f),
                    1.2f + (random.nextFloat() * 0.4f),
                    0.0025f + (random.nextFloat() * 0.003f)
            );
        };
    }

    /**
     * 등급에 따른 특성(Quirk)을 생성합니다.
     */
    private List<Quirk> generateQuirks(StabilizerCoreItem.Tier tier) {
        List<Quirk> availableQuirks = getAvailableQuirks(tier);
        List<Quirk.Tier> quirkTiersToGenerate = determineQuirkTiers(tier);

        return selectQuirks(availableQuirks, quirkTiersToGenerate, tier);
    }

    /**
     * 사용 가능한 특성 목록을 가져옵니다.
     */
    private List<Quirk> getAvailableQuirks(StabilizerCoreItem.Tier tier) {
        List<Quirk> availableQuirks = new ArrayList<>();

        for (Quirk quirk : Quirk.values()) {
            var config = AdsDrillConfigs.getQuirkConfig(quirk);
            if (config.isEnabled() && !config.blacklistedTiers().contains(tier.name())) {
                availableQuirks.add(quirk);
            }
        }

        return availableQuirks;
    }

    /**
     * 생성할 특성의 등급을 결정합니다.
     */
    private List<Quirk.Tier> determineQuirkTiers(StabilizerCoreItem.Tier tier) {
        List<Quirk.Tier> quirkTiers = new ArrayList<>();
        assert level != null;
        RandomSource random = level.getRandom();

        switch (tier) {
            case BRASS -> quirkTiers.add(random.nextFloat() < 0.1f ? Quirk.Tier.RARE : Quirk.Tier.COMMON);
            case STEEL -> {
                int numQuirks = random.nextFloat() < 0.3f ? 2 : 1;
                for (int i = 0; i < numQuirks; i++) {
                    quirkTiers.add(selectSteelQuirkTier(random));
                }
            }
            case NETHERITE -> {
                int numQuirks = random.nextFloat() < 0.7f ? 3 : 2;
                for (int i = 0; i < numQuirks; i++) {
                    quirkTiers.add(selectNetheriteQuirkTier(random));
                }
            }
        }

        return quirkTiers;
    }

    /**
     * Steel 등급의 특성 티어를 선택합니다.
     */
    private Quirk.Tier selectSteelQuirkTier(RandomSource random) {
        float r = random.nextFloat();
        if (r < 0.05f) return Quirk.Tier.EPIC;
        if (r < 0.60f) return Quirk.Tier.RARE;
        return Quirk.Tier.COMMON;
    }

    /**
     * Netherite 등급의 특성 티어를 선택합니다.
     */
    private Quirk.Tier selectNetheriteQuirkTier(RandomSource random) {
        float r = random.nextFloat();
        if (r < 0.4f) return Quirk.Tier.EPIC;
        if (r < 0.9f) return Quirk.Tier.RARE;
        return Quirk.Tier.COMMON;
    }

    /**
     * 특성을 선택합니다.
     */
    private List<Quirk> selectQuirks(List<Quirk> availableQuirks, List<Quirk.Tier> quirkTiers,
                                     StabilizerCoreItem.Tier coreTier) {
        List<Quirk> finalQuirks = new ArrayList<>();
        assert level != null;
        RandomSource random = level.getRandom();

        for (Quirk.Tier tierToGen : quirkTiers) {
            Map<Quirk, Float> candidates = buildCandidateMap(availableQuirks, tierToGen, coreTier);
            if (candidates.isEmpty()) continue;

            Quirk selectedQuirk = selectWeightedRandom(candidates, random);
            if (selectedQuirk != null) {
                finalQuirks.add(selectedQuirk);
                availableQuirks.remove(selectedQuirk);
            }
        }

        return finalQuirks;
    }

    /**
     * 후보 특성과 가중치 맵을 구성합니다.
     */
    private Map<Quirk, Float> buildCandidateMap(List<Quirk> availableQuirks, Quirk.Tier targetTier,
                                                StabilizerCoreItem.Tier coreTier) {
        Map<Quirk, Float> candidates = availableQuirks.stream()
                .filter(q -> q.getTier() == targetTier)
                .collect(Collectors.toMap(q -> q, q -> 3.0f));

        if (candidates.isEmpty()) return candidates;

        // 촉매 보너스 적용
        applyCatalystBonus(candidates);

        // Steel 등급 특별 제한
        if (coreTier == StabilizerCoreItem.Tier.STEEL) {
            removeSteelRestrictedQuirks(candidates);
        }

        return candidates;
    }

    /**
     * 촉매 보너스를 적용합니다.
     */
    private void applyCatalystBonus(Map<Quirk, Float> candidates) {
        final float CATALYST_BONUS = 10.0f;
        ItemStack catalyst1 = inventory.getStackInSlot(CATALYST_SLOT_1);
        ItemStack catalyst2 = inventory.getStackInSlot(CATALYST_SLOT_2);

        if (!catalyst1.isEmpty()) {
            candidates.replaceAll((quirk, weight) ->
                    quirk.getCatalyst().get() == catalyst1.getItem() ? weight + CATALYST_BONUS : weight);
        }
        if (!catalyst2.isEmpty()) {
            candidates.replaceAll((quirk, weight) ->
                    quirk.getCatalyst().get() == catalyst2.getItem() ? weight + CATALYST_BONUS : weight);
        }
    }

    /**
     * Steel 등급에서 제한된 특성을 제거합니다.
     */
    private void removeSteelRestrictedQuirks(Map<Quirk, Float> candidates) {
        candidates.remove(Quirk.OVERLOAD_DISCHARGE);
        candidates.remove(Quirk.BONE_CHILL);
        candidates.remove(Quirk.WITHERING_ECHO);
        candidates.remove(Quirk.WILD_MAGIC);
    }

    /**
     * 가중치를 기반으로 랜덤 선택을 수행합니다.
     */
    private Quirk selectWeightedRandom(Map<Quirk, Float> candidates, RandomSource random) {
        float totalWeight = candidates.values().stream().reduce(0f, Float::sum);
        if (totalWeight <= 0) return null;

        float randomValue = random.nextFloat() * totalWeight;
        for (Map.Entry<Quirk, Float> entry : candidates.entrySet()) {
            randomValue -= entry.getValue();
            if (randomValue <= 0) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 유체 데이터를 처리합니다.
     */
    private FluidData processFluids(Map<Fluid, Integer> fluidCapacities, float yieldMultiplier) {
        if (fluidCapacities.isEmpty()) {
            return new FluidData(FluidStack.EMPTY, 0);
        }

        Fluid dominantFluid = fluidCapacities.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (dominantFluid == null) {
            return new FluidData(FluidStack.EMPTY, 0);
        }

        int totalCapacity = fluidCapacities.values().stream().mapToInt(Integer::intValue).sum();
        int finalFluidCapacity = (int) (totalCapacity * yieldMultiplier);
        FluidStack finalFluid = new FluidStack(dominantFluid, 1);

        return new FluidData(finalFluid, finalFluidCapacity);
    }

    /**
     * 블록을 인공 노드로 교체하고 데이터를 주입합니다.
     */
    private void replaceWithArtificialNode(FinalAttributes attributes, List<Quirk> finalQuirks,
                                           CraftingData craftingData, FluidData fluidData) {
        this.isCompleting = true;
        BlockPos currentPos = this.worldPosition;

        // 블록을 인공 노드로 교체
        assert level != null;
        level.setBlock(currentPos, AdsDrillBlocks.ARTIFICIAL_NODE.get().defaultBlockState(), 3);
        BlockEntity newBE = level.getBlockEntity(currentPos);

        // 데이터 주입
        if (newBE instanceof ArtificialNodeBlockEntity artificialNode) {
            artificialNode.configureFromCrafting(
                    finalQuirks,
                    attributes.finalComposition,
                    craftingData.finalItemToBlockMap,
                    attributes.finalMaxYield,
                    attributes.hardness,
                    attributes.richness,
                    attributes.regeneration,
                    fluidData.fluidStack,
                    fluidData.capacity
            );
        }
    }

    /**
     * 제작 완료 효과를 재생합니다.
     */
    private void playCraftingCompleteEffects() {
        BlockPos currentPos = this.worldPosition;

        assert level != null;
        level.playSound(null, currentPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0f, 0.8f);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    currentPos.getX() + 0.5, currentPos.getY() + 1.2, currentPos.getZ() + 0.5,
                    50, 0.6, 0.6, 0.6, 0.1);
        }
    }

// --- 데이터 클래스들 ---

    /**
         * 제작 데이터를 담는 클래스
         */
        private record CraftingData(Map<Item, Float> weightedComposition, Map<Item, Block> finalItemToBlockMap,
                                    Map<Fluid, Integer> fluidCapacities, float totalInputYield) {
    }

    /**
         * 최종 속성을 담는 클래스
         */
        private record FinalAttributes(Map<Item, Float> finalComposition, float yieldMultiplier, int finalMaxYield,
                                       float hardness, float richness, float regeneration) {
    }

    /**
         * 속성 값을 담는 클래스
         */
        private record AttributeValues(float hardness, float richness, float regeneration) {
    }

    /**
         * 유체 데이터를 담는 클래스
         */
        private record FluidData(FluidStack fluidStack, int capacity) {
    }
    public void onBroken() {
        if (isCompleting || level == null || level.isClientSide) return;

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack));
            }
        }
    }


    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Progress", progress);
        if (clientPacket) {
            // 클라이언트로 보낼 때만 후보군 정보를 NBT에 씁니다.
            ListTag list = new ListTag();
            if (clientQuirkCandidates != null) {
                for (Map.Entry<Quirk, Float> entry : clientQuirkCandidates.entrySet()) {
                    if (entry.getValue() <= 0) continue;
                    CompoundTag compound = new CompoundTag();
                    compound.putString("id", entry.getKey().name());
                    compound.putFloat("w", entry.getValue()); // w for weight
                    list.add(compound);
                }
            }
            tag.put("QuirkCandidates", list);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
        if (clientPacket) {
            // 클라이언트에서 패킷을 받았을 때만 후보군 정보를 NBT에서 읽습니다.
            clientQuirkCandidates.clear();
            ListTag list = tag.getList("QuirkCandidates", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag compound = list.getCompound(i);
                try {
                    Quirk q = Quirk.valueOf(compound.getString("id"));
                    float w = compound.getFloat("w");
                    clientQuirkCandidates.put(q, w);
                } catch (IllegalArgumentException ignored) {}
            }
        }

    }


    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // 헤더
        tooltip.add(Component.literal("    ").append(Component.translatable(AdsDrillBlocks.NODE_FRAME.get().getDescriptionId()).withStyle(ChatFormatting.GOLD)));
        tooltip.add(Component.literal(""));


        // 내용물
        ItemStack coreStack = inventory.getStackInSlot(9);
        long dataItemCount = 0;
        for (int i = 0; i < 9; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                dataItemCount++;
            }
        }

        if (coreStack.isEmpty() && dataItemCount == 0) {
            tooltip.add(Component.literal(" ").append(Component.translatable("goggle.adsdrill.drill_core.storage.empty").withStyle(ChatFormatting.DARK_GRAY)));
            return true;
        }

        // 진행률
        float progressPercent = (float) progress / REQUIRED_PROGRESS * 100f;
        tooltip.add(Component.literal(" " + String.format("%.2f%% ", progressPercent))
                .append(Component.translatable("goggle.adsdrill.node_frame.progress").withStyle(ChatFormatting.GRAY)));

        // 요구 속도
        int requiredSpeed = getRequiredSpeed();
        int currentSpeed = getDrillSpeedAbove();

        MutableComponent speedReqLine = Component.literal(" ")
                .append(Component.translatable("goggle.adsdrill.node_frame.speed_requirement").withStyle(ChatFormatting.GRAY));

        if (requiredSpeed == Integer.MAX_VALUE) {
            speedReqLine.append(Component.translatable("goggle.adsdrill.node_frame.none").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            ChatFormatting color = currentSpeed >= requiredSpeed ? ChatFormatting.GREEN : ChatFormatting.RED;
            speedReqLine.append(Component.literal(String.format(" %d RPM", requiredSpeed)).withStyle(color));
        }
        tooltip.add(speedReqLine);



        // 상세 내용물 (플레이어가 Sneaking 중일 때만 표시)
        if (isPlayerSneaking) {
            tooltip.add(Component.literal(""));

            coreStack = inventory.getStackInSlot(CORE_SLOT);
            if (!coreStack.isEmpty()) {
                tooltip.add(Component.literal(" ").append(Component.translatable("adsdrill.stabilizer_core.header").withStyle(ChatFormatting.GRAY)));
                tooltip.add(Component.literal("  - ").append(coreStack.getHoverName()));
            }

            // [추가] 촉매 정보 표시
            ItemStack catalyst1 = inventory.getStackInSlot(CATALYST_SLOT_1);
            ItemStack catalyst2 = inventory.getStackInSlot(CATALYST_SLOT_2);
            if (!catalyst1.isEmpty() || !catalyst2.isEmpty()) {
                tooltip.add(Component.literal(" ").append(Component.translatable("adsdrill.catalyst.header").withStyle(ChatFormatting.GRAY)));
                if (!catalyst1.isEmpty()) {
                    tooltip.add(Component.literal("  - ").append(catalyst1.getHoverName()));
                }
                if (!catalyst2.isEmpty()) {
                    tooltip.add(Component.literal("  - ").append(catalyst2.getHoverName()));
                }
            }

            dataItemCount = 0;
            for (int i = DATA_SLOT_START; i < CORE_SLOT; i++) {
                if (!inventory.getStackInSlot(i).isEmpty()) dataItemCount++;
            }
            if (dataItemCount > 0) {
                tooltip.add(Component.literal(" ").append(Component.translatable("adsdrill.node_data.header", dataItemCount).withStyle(ChatFormatting.GRAY)));
            }
            tooltip.add(Component.literal("")); // 구분선
            tooltip.add(Component.translatable("adsdrill.quirk_candidates.header").withStyle(ChatFormatting.GOLD));

            if (clientQuirkCandidates == null || clientQuirkCandidates.isEmpty()) {
                tooltip.add(Component.literal("  - ").append(Component.translatable("goggle.adsdrill.node_frame.none").withStyle(ChatFormatting.DARK_GRAY)));
            } else {
                float totalWeight = clientQuirkCandidates.values().stream().reduce(0f, Float::sum);
                if (totalWeight <= 0) {
                    tooltip.add(Component.literal("  - ").append(Component.translatable("goggle.adsdrill.node_frame.none").withStyle(ChatFormatting.DARK_GRAY)));
                } else {
                    // 확률이 높은 순으로 정렬하여 상위 5개만 표시
                    List<Map.Entry<Quirk, Float>> sortedList = clientQuirkCandidates.entrySet().stream()
                            .filter(e -> e.getValue() > 0)
                            .sorted(Map.Entry.<Quirk, Float>comparingByValue().reversed())
                            .limit(6)
                            .toList();

                    for (Map.Entry<Quirk, Float> entry : sortedList) {
                        float chance = (entry.getValue() / totalWeight) * 100f;
                        ChatFormatting color = chance > 30f ? ChatFormatting.GREEN : (chance > 10f ? ChatFormatting.YELLOW : ChatFormatting.GRAY);

                        MutableComponent line = Component.literal(String.format("  - %s: %.1f%%", entry.getKey().getDisplayName().getString(), chance))
                                .withStyle(color);
                        tooltip.add(line);
                    }
                    if (clientQuirkCandidates.values().stream().filter(v->v>0).count() > 5) {
                        tooltip.add(Component.literal("  ...").withStyle(ChatFormatting.DARK_GRAY));
                    }
                }
            }
        } else {
            tooltip.add(Component.literal("").append(Component.translatable("goggle.adsdrill.sneak_for_details").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        }

        return true;
    }

    // 요구 속도를 가져오는 헬퍼 메서드
    private int getRequiredSpeed() {
        ItemStack coreStack = inventory.getStackInSlot(9);
        if (coreStack.getItem() instanceof StabilizerCoreItem stabilizer) {
            return switch (stabilizer.getTier()) {
                case BRASS -> AdsDrillConfigs.SERVER.brassCoreSpeedRequirement.get();
                case STEEL -> AdsDrillConfigs.SERVER.steelCoreSpeedRequirement.get();
                case NETHERITE -> AdsDrillConfigs.SERVER.netheriteCoreSpeedRequirement.get();
            };
        }
        return Integer.MAX_VALUE; // 코어가 없으면 작동 불가
    }

}