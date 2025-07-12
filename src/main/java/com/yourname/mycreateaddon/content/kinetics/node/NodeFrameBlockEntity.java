package com.yourname.mycreateaddon.content.kinetics.node;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.item.StabilizerCoreItem;
import com.yourname.mycreateaddon.content.kinetics.drill.head.RotaryDrillHeadBlockEntity;
import com.yourname.mycreateaddon.crafting.Quirk;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import com.yourname.mycreateaddon.registry.MyAddonTags;
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
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
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

public class NodeFrameBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {


    // [1] 인벤토리 크기를 11에서 13으로 늘립니다. (데이터 9 + 코어 1 + 촉매 2)
    protected ItemStackHandler inventory = createInventory();
    private int progress = 0;
    private static final int REQUIRED_PROGRESS = 240000;
    private boolean isCompleting = false;


    // 슬롯 인덱스를 상수로 정의하여 가독성을 높입니다.

    // 인벤토리 슬롯 상수 정의 (이 부분은 이전 답변과 동일)
    private static final int DATA_SLOT_START = 0;
    private static final int DATA_SLOT_COUNT = 9;
    private static final int CORE_SLOT = 9;
    private static final int CATALYST_SLOT_1 = 10;
    private static final int CATALYST_SLOT_2 = 11;
    private static final int INVENTORY_SIZE = DATA_SLOT_COUNT + 1 + 2; // 데이터 + 코어 + 촉매

    private transient Map<Quirk, Float> clientQuirkCandidates = new HashMap<>();
    public static final TagKey<Item> CATALYST_TAG = ItemTags.create(ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "catalysts"));


    public NodeFrameBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    private ItemStackHandler createInventory() {
        return new ItemStackHandler(INVENTORY_SIZE) { // 올바른 인벤토리 크기 사용
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
    // [3. 추가] 특수 효과 후보군과 가중치를 계산하는 메서드
    private void updateQuirkCandidates() {
        if (level == null) return; // 월드가 없으면 계산 불가

        // 이 메서드는 서버/클라이언트 양쪽에서 호출될 수 있으나,
        // 실제 동기화는 서버 -> 클라이언트로만 이루어집니다.

        // --- `completeCrafting`에 있던 로직을 그대로 가져옵니다 ---
        Map<Quirk, Float> candidates = new HashMap<>();
        for (Quirk quirk : Quirk.values()) {
            candidates.put(quirk, 3.0f);
        }

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

        ItemStack coreStack = inventory.getStackInSlot(CORE_SLOT);
        StabilizerCoreItem.Tier tier = (coreStack.getItem() instanceof StabilizerCoreItem sci) ? sci.getTier() : StabilizerCoreItem.Tier.BRASS;

        switch (tier) {
            case BRASS:
                candidates.merge(Quirk.OVERLOAD_DISCHARGE, 2.0f, Float::sum);
                candidates.merge(Quirk.BONE_CHILL, 1.0f, Float::sum);
                candidates.merge(Quirk.WILD_MAGIC,1.0f,Float::sum);
                break;
            case STEEL:
                candidates.put(Quirk.OVERLOAD_DISCHARGE, 0.0f);
                candidates.put(Quirk.BONE_CHILL, 0.0f);
                candidates.put(Quirk.WITHERING_ECHO, 0.0f);
                candidates.put(Quirk.WILD_MAGIC,0.0f);
                break;
            case NETHERITE:
                candidates.merge(Quirk.GEMSTONE_FACETS, 5.0f, Float::sum);
                candidates.merge(Quirk.AURA_OF_VITALITY, 3.0f, Float::sum);
                break;
        }

        // 계산된 결과를 클라이언트 동기화용 필드에 저장
        this.clientQuirkCandidates = candidates;
    }
    public boolean isCatalystItem(ItemStack stack) {
        return stack.is(MyAddonTags.CATALYSTS);
    }

    public boolean addData(ItemStack dataStack) {
        // [수정] DATA_SLOT_START와 DATA_SLOT_COUNT 상수를 사용하여 루프를 구성합니다.
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
                progress = Math.max(0, progress - 128);
            }
        } else {
            // 2. 코어 등급에 따른 파라미터 설정
            int requiredSpeed = getRequiredSpeed(); // 고글 툴팁용으로 만든 헬퍼 메서드 재활용
            boolean resetOnFail = false;
            int progressDecay = 128;

            if (coreStack.getItem() instanceof StabilizerCoreItem stabilizer) {
                switch (stabilizer.getTier()) {
                    case BRASS -> progressDecay = 64;
                    case STEEL -> progressDecay = 256;
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


    private void completeCrafting() {
        if (level == null || level.isClientSide) return;

        // --- 1. 재료 분석 및 데이터 집계 ---
        Map<Item, Float> weightedComposition = new HashMap<>();
        Map<Item, Block> finalItemToBlockMap = new HashMap<>();
        float totalInputYield = 0;
        int dataItemCount = 0;

        for (int i = DATA_SLOT_START; i < CORE_SLOT; i++) {
            ItemStack dataStack = inventory.getStackInSlot(i);
            if (dataStack.isEmpty()) continue;

            CustomData customData = dataStack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) continue;
            CompoundTag nbt = customData.copyTag();

            float yield = nbt.getFloat("Yield");
            totalInputYield += yield;
            dataItemCount++;

            ListTag compositionList = nbt.getList("Composition", 10);
            for (int j = 0; j < compositionList.size(); j++) {
                CompoundTag entry = compositionList.getCompound(j);
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getString("Item")));
                float ratio = entry.getFloat("Ratio");
                weightedComposition.merge(item, ratio * yield, Float::sum);
            }

            ListTag itemToBlockList = nbt.getList("ItemToBlockMap", 10);
            for (int j = 0; j < itemToBlockList.size(); j++) {
                CompoundTag entry = itemToBlockList.getCompound(j);
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getString("Item")));
                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.getString("Block")));
                if (item != Items.AIR && block != Blocks.AIR) {
                    finalItemToBlockMap.putIfAbsent(item, block);
                }
            }
        }

        if (weightedComposition.isEmpty()) {
            weightedComposition.put(Items.RAW_IRON, 1.0f);
            finalItemToBlockMap.put(Items.RAW_IRON, Blocks.IRON_ORE);
            totalInputYield = 1000;
        }
        Map<Fluid, Integer> fluidCapacities = new HashMap<>();

        for (int i = 0; i < DATA_SLOT_COUNT; i++) {
            ItemStack dataStack = inventory.getStackInSlot(i);
            if (dataStack.isEmpty()) continue;
            CustomData customData = dataStack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) continue;
            CompoundTag nbt = customData.copyTag();

            if (nbt.contains("FluidContent")) {
                FluidStack fluid = FluidStack.parse(level.registryAccess(), nbt.getCompound("FluidContent")).orElse(FluidStack.EMPTY);
                if (!fluid.isEmpty()) {
                    int capacity = nbt.getInt("MaxFluidCapacity");
                    fluidCapacities.merge(fluid.getFluid(), capacity, Integer::sum);
                }
            }
        }

        // --- 2. 최종 결과물 속성 결정 ---

        // 2a. 최종 성분 정규화
        Map<Item, Float> finalComposition = new HashMap<>();
        float totalWeight = weightedComposition.values().stream().reduce(0f, Float::sum);
        if (totalWeight > 0) {
            for (Map.Entry<Item, Float> entry : weightedComposition.entrySet()) {
                finalComposition.put(entry.getKey(), entry.getValue() / totalWeight);
            }
        } else {
            finalComposition.put(Items.RAW_IRON, 1.0f);
        }

        ItemStack coreStack = inventory.getStackInSlot(CORE_SLOT);
        StabilizerCoreItem.Tier tier = (coreStack.getItem() instanceof StabilizerCoreItem sci) ? sci.getTier() : StabilizerCoreItem.Tier.BRASS;

        // 2b. 최종 매장량 계산
        float yieldMultiplier = switch (tier) {
            case BRASS -> 0.8f;
            case STEEL -> 1.0f;
            case NETHERITE -> 1.25f;
        };
        int finalMaxYield = (int) (totalInputYield * yieldMultiplier);

        // 2c. 최종 특성 결정
        RandomSource random = level.getRandom();
        float finalHardness, finalRichness, finalRegeneration;
        switch (tier) {
            case BRASS -> {
                finalHardness = 0.6f + (random.nextFloat() * 0.4f);
                finalRichness = 0.9f + (random.nextFloat() * 0.2f);
                finalRegeneration = 0.0005f + (random.nextFloat() * 0.001f);
            }
            case STEEL -> {
                finalHardness = 0.9f + (random.nextFloat() * 0.6f);
                finalRichness = 1.05f + (random.nextFloat() * 0.3f);
                finalRegeneration = 0.0015f + (random.nextFloat() * 0.002f);
            }
            case NETHERITE -> {
                finalHardness = 1.4f + (random.nextFloat() * 0.8f);
                finalRichness = 1.2f + (random.nextFloat() * 0.4f);
                finalRegeneration = 0.0025f + (random.nextFloat() * 0.003f);
            }
            default -> throw new IllegalStateException("Unexpected value: " + tier);
        }

        // 2d. 대표 블록 찾기
        Block representativeBlock = finalComposition.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> finalItemToBlockMap.getOrDefault(entry.getKey(), Blocks.IRON_ORE))
                .orElse(Blocks.IRON_ORE);

        // 2e. 특수 효과(Quirk) 결정 (랜덤성 강화 버전)
        Map<Quirk, Float> weightedQuirks = new HashMap<>();
        for (Quirk quirk : Quirk.values()) {
            weightedQuirks.put(quirk, 3.0f);
        }

        ItemStack catalyst1 = inventory.getStackInSlot(CATALYST_SLOT_1);
        ItemStack catalyst2 = inventory.getStackInSlot(CATALYST_SLOT_2);
        final float CATALYST_BONUS = 10.0f;

        if (!catalyst1.isEmpty()) {
            weightedQuirks.replaceAll((quirk, weight) ->
                    quirk.getCatalyst().get() == catalyst1.getItem() ? weight + CATALYST_BONUS : weight
            );
        }
        if (!catalyst2.isEmpty()) {
            weightedQuirks.replaceAll((quirk, weight) ->
                    quirk.getCatalyst().get() == catalyst2.getItem() ? weight + CATALYST_BONUS : weight
            );
        }

        int quirkCount = 0;
        switch (tier) {
            case BRASS:
                quirkCount = 1;
                weightedQuirks.merge(Quirk.OVERLOAD_DISCHARGE, 2.0f, Float::sum);
                weightedQuirks.merge(Quirk.BONE_CHILL, 1.0f, Float::sum);
                break;
            case STEEL:
                quirkCount = (random.nextFloat() < 0.7f) ? 2 : 1;
                weightedQuirks.put(Quirk.OVERLOAD_DISCHARGE, 0.0f);
                weightedQuirks.put(Quirk.BONE_CHILL, 0.0f);
                weightedQuirks.put(Quirk.WITHERING_ECHO, 0.0f);
                break;
            case NETHERITE:
                quirkCount = (random.nextFloat() < 0.8f) ? 3 : 2;
                weightedQuirks.merge(Quirk.GEMSTONE_FACETS, 5.0f, Float::sum);
                weightedQuirks.merge(Quirk.AURA_OF_VITALITY, 3.0f, Float::sum);
                break;
        }

        List<Quirk> finalQuirks = new ArrayList<>();
        List<Map.Entry<Quirk, Float>> candidates = new ArrayList<>(weightedQuirks.entrySet());

        for (int i = 0; i < quirkCount && !candidates.isEmpty(); i++) {
            float totalQuirkWeight = 0;
            for (Map.Entry<Quirk, Float> entry : candidates) {
                totalQuirkWeight += entry.getValue();
            }

            if (totalQuirkWeight <= 0) break;

            float randomValue = random.nextFloat() * totalQuirkWeight;

            Map.Entry<Quirk, Float> selectedEntry = null;
            for (Map.Entry<Quirk, Float> entry : candidates) {
                randomValue -= entry.getValue();
                if (randomValue <= 0) {
                    selectedEntry = entry;
                    break;
                }
            }

            if (selectedEntry != null) {
                finalQuirks.add(selectedEntry.getKey());
                candidates.remove(selectedEntry);
            }
        }

        FluidStack finalFluid = FluidStack.EMPTY;
        int finalFluidCapacity = 0;

        if (!fluidCapacities.isEmpty()) {
            // 가장 용량이 많은 액체를 최종 액체로 선택
            Fluid dominantFluid = fluidCapacities.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (dominantFluid != null) {
                // 모든 용량을 합산
                int totalCapacity = fluidCapacities.values().stream().mapToInt(Integer::intValue).sum();
                // 코어 효율 보정 적용
                finalFluidCapacity = (int) (totalCapacity * yieldMultiplier); // 광물 매장량과 동일한 보정 적용
                finalFluid = new FluidStack(dominantFluid, 1); // 양은 중요하지 않으므로 1로 설정
            }
        }


        // --- [!!! 핵심 수정: 인공 노드 생성 및 데이터 주입 방식 변경 !!!] ---
        this.isCompleting = true;
        BlockPos currentPos = this.worldPosition;

        // 1. 생성될 ArtificialNodeBlockEntity에 주입할 모든 데이터를 담을 NBT 태그를 준비합니다.
        CompoundTag finalNbtForNewNode = new CompoundTag();

        // 2. OreNodeBlockEntity의 write 메서드와 동일한 형식으로 모든 데이터를 NBT에 저장합니다.
        // 2a. 특성(Quirks) 저장
        if (!finalQuirks.isEmpty()) {
            ListTag quirkListTag = new ListTag();
            for (Quirk quirk : finalQuirks) {
                CompoundTag quirkTag = new CompoundTag();
                quirkTag.putString("id", quirk.name());
                quirkListTag.add(quirkTag);
            }
            finalNbtForNewNode.put("Quirks", quirkListTag);
        }

        // 2b. 광물 구성(Composition) 저장
        ListTag compositionList = new ListTag();
        for (Map.Entry<Item, Float> entry : finalComposition.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Item", BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
            entryTag.putFloat("Ratio", entry.getValue());
            compositionList.add(entryTag);
        }
        finalNbtForNewNode.put("Composition", compositionList);

        // 2c. 아이템-블록 맵(ItemToBlockMap) 저장
        ListTag itemToBlockList = new ListTag();
        for (Map.Entry<Item, Block> entry : finalItemToBlockMap.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Item", BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
            entryTag.putString("Block", BuiltInRegistries.BLOCK.getKey(entry.getValue()).toString());
            itemToBlockList.add(entryTag);
        }
        finalNbtForNewNode.put("ItemToBlockMap", itemToBlockList);

        // 2d. 기본 속성들 저장
        finalNbtForNewNode.putInt("MaxYield", finalMaxYield);
        finalNbtForNewNode.putFloat("CurrentYield", finalMaxYield); // 처음엔 가득 찬 상태로
        finalNbtForNewNode.putFloat("Hardness", finalHardness);
        finalNbtForNewNode.putFloat("Richness", finalRichness);
        finalNbtForNewNode.putFloat("Regeneration", finalRegeneration);

        if (!finalFluid.isEmpty() && finalFluidCapacity > 0) {
            finalNbtForNewNode.put("FluidContent", finalFluid.save(level.registryAccess()));
            finalNbtForNewNode.putInt("MaxFluidCapacity", finalFluidCapacity);
            finalNbtForNewNode.putFloat("CurrentFluidAmount", finalFluidCapacity); // 처음엔 가득 차도록
        }

        // 3. 블록을 인공 노드로 교체
        level.setBlock(currentPos, MyAddonBlocks.ARTIFICIAL_NODE.get().defaultBlockState(), 3);
        BlockEntity newBE = level.getBlockEntity(currentPos);

        if (newBE instanceof ArtificialNodeBlockEntity artificialNode) {
            // 4. 모든 정보가 담긴 NBT를 단 한 번만 로드합니다.
            // 이 한 줄이 configure()와 이전 load()를 모두 대체합니다.
            artificialNode.loadWithComponents(finalNbtForNewNode, level.registryAccess());

            // 데이터가 모두 주입되었음을 확실히 하기 위해 변경사항을 알립니다.
            artificialNode.setChanged();
        }

        // 5. 제작 완료 효과
        level.playSound(null, currentPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0f, 0.8f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    currentPos.getX() + 0.5, currentPos.getY() + 1.2, currentPos.getZ() + 0.5,
                    50, 0.6, 0.6, 0.6, 0.1);
        }
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
        tooltip.add(Component.literal("    ").append(Component.translatable(MyAddonBlocks.NODE_FRAME.get().getDescriptionId()).withStyle(ChatFormatting.GOLD)));
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
            tooltip.add(Component.literal(" ").append(Component.translatable("goggle.mycreateaddon.drill_core.storage.empty").withStyle(ChatFormatting.DARK_GRAY)));
            return true;
        }

        // 진행률
        float progressPercent = (float) progress / REQUIRED_PROGRESS * 100f;
        tooltip.add(Component.literal(" " + String.format("%.2f%% ", progressPercent))
                .append(Component.translatable("goggle.mycreateaddon.node_frame.progress").withStyle(ChatFormatting.GRAY)));

        // 요구 속도
        int requiredSpeed = getRequiredSpeed();
        int currentSpeed = getDrillSpeedAbove();

        MutableComponent speedReqLine = Component.literal(" ")
                .append(Component.translatable("goggle.mycreateaddon.node_frame.speed_requirement").withStyle(ChatFormatting.GRAY));

        if (requiredSpeed == Integer.MAX_VALUE) {
            speedReqLine.append(Component.translatable("goggle.mycreateaddon.node_frame.none").withStyle(ChatFormatting.DARK_GRAY));
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
                tooltip.add(Component.literal(" ").append(Component.translatable("mycreateaddon.stabilizer_core.header").withStyle(ChatFormatting.GRAY)));
                tooltip.add(Component.literal("  - ").append(coreStack.getHoverName()));
            }

            // [추가] 촉매 정보 표시
            ItemStack catalyst1 = inventory.getStackInSlot(CATALYST_SLOT_1);
            ItemStack catalyst2 = inventory.getStackInSlot(CATALYST_SLOT_2);
            if (!catalyst1.isEmpty() || !catalyst2.isEmpty()) {
                tooltip.add(Component.literal(" ").append(Component.translatable("mycreateaddon.catalyst.header").withStyle(ChatFormatting.GRAY)));
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
                tooltip.add(Component.literal(" ").append(Component.translatable("mycreateaddon.node_data.header", dataItemCount).withStyle(ChatFormatting.GRAY)));
            }
            tooltip.add(Component.literal("")); // 구분선
            tooltip.add(Component.translatable("mycreateaddon.quirk_candidates.header").withStyle(ChatFormatting.GOLD));

            if (clientQuirkCandidates == null || clientQuirkCandidates.isEmpty()) {
                tooltip.add(Component.literal("  - ").append(Component.translatable("goggle.mycreateaddon.node_frame.none").withStyle(ChatFormatting.DARK_GRAY)));
            } else {
                float totalWeight = clientQuirkCandidates.values().stream().reduce(0f, Float::sum);
                if (totalWeight <= 0) {
                    tooltip.add(Component.literal("  - ").append(Component.translatable("goggle.mycreateaddon.node_frame.none").withStyle(ChatFormatting.DARK_GRAY)));
                } else {
                    // 확률이 높은 순으로 정렬하여 상위 5개만 표시
                    List<Map.Entry<Quirk, Float>> sortedList = clientQuirkCandidates.entrySet().stream()
                            .filter(e -> e.getValue() > 0)
                            .sorted(Map.Entry.<Quirk, Float>comparingByValue().reversed())
                            .limit(5)
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
            tooltip.add(Component.literal("").append(Component.translatable("goggle.mycreateaddon.sneak_for_details").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        }

        return true;
    }

    // [3] 요구 속도를 가져오는 헬퍼 메서드를 추가합니다.
    private int getRequiredSpeed() {
        ItemStack coreStack = inventory.getStackInSlot(9);
        if (coreStack.getItem() instanceof StabilizerCoreItem stabilizer) {
            return switch (stabilizer.getTier()) {
                case BRASS -> 512;
                case STEEL -> 1024;
                case NETHERITE -> 2048;
            };
        }
        return Integer.MAX_VALUE; // 코어가 없으면 작동 불가
    }

}