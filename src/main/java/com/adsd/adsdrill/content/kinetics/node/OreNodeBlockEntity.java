package com.adsd.adsdrill.content.kinetics.node;


import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.adsd.adsdrill.crafting.NodeRecipe;
import com.adsd.adsdrill.crafting.Quirk;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.List;


public class OreNodeBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    public static final ModelProperty<BlockState> BACKGROUND_STATE = new ModelProperty<>();


    // --- 데이터 필드 ---
    private Map<Item, Float> resourceComposition = new HashMap<>();
    private Map<Item, Block> itemToBlockMap = new HashMap<>();

    private FluidStack fluidContent = FluidStack.EMPTY;
    private int maxFluidCapacity;
    private float currentFluidAmount;

    private float temporaryRegenBoost = 0;
    private boolean polarityBonusActive = false;
    private int auraCheckCooldown = 0;

    private int miningProgress;
    private ResourceLocation backgroundBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.STONE);
    private ResourceLocation oreBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.IRON_ORE);

    // --- 노드 특성 필드 ---
    private int maxYield;
    private float currentYield;
    private float hardness;
    private float richness;
    private float regeneration;

    private int quirkCheckOffset = -1;
    private boolean cracked = false;
    private int crackTimer = 0;
    private static final int CRACK_DURATION_TICKS = 100;


    public OreNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Map<Item, Float> getResourceComposition() {
        return this.resourceComposition;
    }

    public void setPolarityBonusActive(boolean active) {
        this.polarityBonusActive = active;
    }

    public List<ItemStack> applySpecificMiningTick(int miningAmount, int fortuneLevel, boolean hasSilkTouch, Item specificItemToMine) {
        if (!(level instanceof ServerLevel serverLevel) || currentYield <= 0 || !resourceComposition.containsKey(specificItemToMine)) {
            return Collections.emptyList();
        }

        float effectiveMiningAmount = miningAmount / getHardness();
        this.miningProgress += (int) effectiveMiningAmount;
        int miningResistance = 1000;

        if (this.miningProgress >= miningResistance) {
            this.miningProgress -= miningResistance;
            this.currentYield--;
            setChanged();
            sendData();

            // 실크터치는 여기서도 작동
            if (hasSilkTouch) {
                Block blockToDrop = this.itemToBlockMap.get(specificItemToMine);
                return Collections.singletonList(new ItemStack(Objects.requireNonNullElseGet(blockToDrop, () -> BuiltInRegistries.BLOCK.get(oreBlockId))));
            }

            // 행운/풍부함 로직 적용
            ItemStack finalDrop = new ItemStack(specificItemToMine);
            int dropCount = 1;
            if (fortuneLevel > 0) {
                RandomSource rand = serverLevel.getRandom();
                for (int i = 0; i < fortuneLevel; i++) {
                    if (rand.nextInt(fortuneLevel + 2) > 1) dropCount++;
                }
            }
            finalDrop.setCount(dropCount);
            if (this.richness > 1.0f && serverLevel.getRandom().nextFloat() < (this.richness - 1.0f)) {
                finalDrop.grow(finalDrop.getCount());
            }

            return Collections.singletonList(finalDrop);
        }

        return Collections.emptyList();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    public int getMaxFluidCapacity() {
        return maxFluidCapacity;
    }

    public boolean isCracked() {
        return cracked;
    }


    public void setCracked(boolean cracked) {
        if (this.cracked == cracked) return;
        this.cracked = cracked;

        if (cracked) {
            // 균열 상태가 되면 타이머를 설정
            this.crackTimer = CRACK_DURATION_TICKS;
        } else {
            // 균열 상태가 풀리면 타이머를 리셋
            this.crackTimer = 0;
        }

        // sendData()와 함께, 블록 업데이트를 요청하여
        // 클라이언트 렌더링 및 상태를 확실하게 갱신합니다.
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    public float getHardness() {
        float finalHardness = this.hardness;
        if (this instanceof ArtificialNodeBlockEntity artificialNode) {
            Quirk.QuirkContext context = new Quirk.QuirkContext((ServerLevel) level, worldPosition, this, null);
            for (Quirk quirk : artificialNode.getQuirks()) {
                finalHardness = quirk.onCalculateHardness(finalHardness, context);
            }
        }
        return Math.max(0.1f, finalHardness);
    }

    public ResourceLocation getBackgroundBlockId() {
        return backgroundBlockId;
    }

    // --- 유체 관련 Getter/Setter ---
    public FluidStack getFluid() {
        if (fluidContent.isEmpty() || currentFluidAmount <= 0) {
            return FluidStack.EMPTY;
        }
        // 실제 유체 양을 반영하여 반환
        return new FluidStack(fluidContent.getFluid(), (int) currentFluidAmount);
    }

    /**
     * 외부(펌프 헤드)에서 유체를 추출하는 메서드
     * @param amountToDrain 추출할 양
     * @return 실제로 추출된 유체 스택
     */
    public FluidStack drainFluid(int amountToDrain) {
        if (fluidContent.isEmpty() || currentFluidAmount <= 0) {
            return FluidStack.EMPTY;
        }

        int actualDrainAmount = Math.min(amountToDrain, (int) currentFluidAmount);
        this.currentFluidAmount -= actualDrainAmount;

        setChanged();
        sendData();

        return new FluidStack(fluidContent.getFluid(), actualDrainAmount);
    }


    public ResourceLocation getRepresentativeOreItemId() {
        return resourceComposition.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> BuiltInRegistries.ITEM.getKey(entry.getKey()))
                .orElse(BuiltInRegistries.ITEM.getKey(Items.RAW_IRON));
    }
    // --- 초기화 메서드 ---
    public void configureFromFeature(Map<Item, Float> composition, Map<Item, Block> itemToBlockMap, int maxYield, float hardness, float richness, float regeneration, Block backgroundBlock, Block representativeOreBlock, FluidStack fluid, int fluidCapacity) {
        this.backgroundBlockId = BuiltInRegistries.BLOCK.getKey(backgroundBlock);
        this.oreBlockId = BuiltInRegistries.BLOCK.getKey(representativeOreBlock);
        // 새로운 범용 설정 메서드를 호출합니다.
        configureNode(composition, itemToBlockMap, maxYield, hardness, richness, regeneration, fluid, fluidCapacity);
    }

    /**
     * 모든 노드 타입(자연산, 인공)의 기본 속성을 설정하는 범용 메서드.
     */
    public void configureNode(Map<Item, Float> composition, Map<Item, Block> itemToBlockMap, int maxYield, float hardness, float richness, float regeneration, FluidStack fluid, int fluidCapacity) {
        this.resourceComposition = composition;
        this.itemToBlockMap = itemToBlockMap;
        this.maxYield = maxYield;
        this.currentYield = maxYield;
        this.hardness = hardness;
        this.richness = richness;
        this.regeneration = regeneration;

        if (fluid != null && !fluid.isEmpty() && fluidCapacity > 0) {
            this.fluidContent = fluid.copy();
            this.maxFluidCapacity = fluidCapacity;
            this.currentFluidAmount = fluidCapacity;
        } else {
            this.fluidContent = FluidStack.EMPTY;
            this.maxFluidCapacity = 0;
            this.currentFluidAmount = 0;
        }

        setChanged();
        sendData();
    }



    public List<ItemStack> applyMiningTick(int miningAmount, int fortuneLevel, boolean hasSilkTouch) {
        if (!(level instanceof ServerLevel serverLevel) || currentYield <= 0) {
            return Collections.emptyList();
        }

        // [1. 특성 적용] 행운 레벨을 특성에 따라 먼저 계산합니다.
        int effectiveFortune = fortuneLevel;
        if (this instanceof ArtificialNodeBlockEntity artificialNode) {
            for (Quirk quirk : artificialNode.getQuirks()) {
                effectiveFortune = quirk.onCalculateFortune(effectiveFortune);
            }
        }

        // [2. 채굴량 계산] 경도와 양극성 보너스를 적용합니다.
        double effectiveMiningAmount = (miningAmount / getHardness());

        if (this instanceof ArtificialNodeBlockEntity artificialNode) {
            Quirk.QuirkContext context = new Quirk.QuirkContext(serverLevel, worldPosition, this, null);
            for (Quirk quirk : artificialNode.getQuirks()) {
                effectiveMiningAmount = quirk.onCalculateMiningAmount(effectiveMiningAmount, context);
            }
        }

        this.miningProgress += (int) effectiveMiningAmount;

        int miningResistance = 1000; // 채굴 저항값 (설정으로 뺄 수도 있음)

        if (this.miningProgress >= miningResistance) {
            this.miningProgress -= miningResistance;

            // [3. 조합 레시피 확인] 균열 상태일 때만 조합을 시도합니다.
            if (this.isCracked()) {
                for (NodeRecipe recipe : NodeRecipe.RECIPES) {
                    if (checkRecipeConditions(recipe)) {
                        if (serverLevel.getRandom().nextFloat() < recipe.chance()) {
                            this.currentYield -= recipe.consumptionMultiplier();
                            setChanged();
                            sendData();
                            // 조합 성공 시, 조합 결과물만 반환하고 즉시 종료
                            return Collections.singletonList(recipe.output().copy());
                        }
                    }
                }
            }

            // [4. 일반 채굴 진행]
            this.currentYield--;

            if (this instanceof ArtificialNodeBlockEntity artificialNode) {
                Quirk.QuirkContext context = new Quirk.QuirkContext(serverLevel, worldPosition, this, null);
                for (Quirk quirk : artificialNode.getQuirks()) {
                    quirk.onYieldConsumed(context);
                }
            }

            setChanged();
            sendData();

            // [5. 일반 드롭 아이템 생성]
            List<ItemStack> drops = getNormalDrops(effectiveFortune, hasSilkTouch);

            // [6. 드롭 후 발동하는 특성 적용]
            if (this instanceof ArtificialNodeBlockEntity artificialNode) {
                Quirk.QuirkContext context = new Quirk.QuirkContext(serverLevel, worldPosition, this, null);
                for (Quirk quirk : artificialNode.getQuirks()) {
                    quirk.onAfterDropsCalculated(drops, context);
                }
            }

            return drops;
        }

        return Collections.emptyList();
    }


    private boolean checkRecipeConditions(NodeRecipe recipe) {
        // 1. 유체 조건 확인
        if (recipe.requiredFluid() != null &&
                (this.fluidContent.isEmpty() || this.fluidContent.getFluid() != recipe.requiredFluid())) {
            return false;
        }

        // 2. 광물 조건 확인 (수정된 부분)
        for (Item requiredItem : recipe.requiredItems()) { // Supplier.get() 호출 제거

            if (!this.resourceComposition.containsKey(requiredItem)) {
                return false;
            }

            float currentRatio = this.resourceComposition.get(requiredItem);
            Float minimumRatio = recipe.minimumRatios().get(requiredItem); // 직접 Item으로 조회

            if (minimumRatio == null || currentRatio < minimumRatio) {
                return false;
            }
        }

        return true;
    }

    /**
     * 일반적인 채굴 규칙에 따라 드롭될 아이템 리스트를 반환합니다.
     * 이 메서드는 조합 레시피가 발동하지 않았을 때 호출됩니다.
     *
     * @param fortuneLevel 적용할 행운 인챈트 레벨
     * @param hasSilkTouch 실크터치 적용 여부
     * @return 드롭될 아이템 스택의 리스트
     */
    private List<ItemStack> getNormalDrops(int fortuneLevel, boolean hasSilkTouch) {
        if (level == null || resourceComposition.isEmpty()) {
            return Collections.emptyList();
        }

        // [1. 드롭할 아이템 종류 결정]
        Item itemToDrop = null;

        if (this instanceof ArtificialNodeBlockEntity artificialNode) {
            Quirk.QuirkContext context = new Quirk.QuirkContext((ServerLevel) level, worldPosition, this, null);
            for (Quirk quirk : artificialNode.getQuirks()) {
                Optional<Item> override = quirk.onSelectItemToDrop(context);
                if (override.isPresent()) {
                    itemToDrop = override.get();
                    break; // 첫 번째로 아이템을 지정한 특성의 결정을 따름
                }
            }
        }

        // 위에서 특성에 의해 결정되지 않았다면, 기존의 가중치 랜덤 로직 실행
        if (itemToDrop == null) {
            List<Map.Entry<Item, Float>> sortedComposition = new ArrayList<>(resourceComposition.entrySet());
            sortedComposition.sort(Comparator.comparing(entry -> BuiltInRegistries.ITEM.getKey(entry.getKey()).toString()));
            double randomValue = level.getRandom().nextDouble();
            float cumulativeProbability = 0f;
            for (Map.Entry<Item, Float> entry : sortedComposition) {
                cumulativeProbability += entry.getValue();
                if (randomValue < cumulativeProbability) {
                    itemToDrop = entry.getKey();
                    break;
                }
            }
            if (itemToDrop == null && !sortedComposition.isEmpty()) {
                itemToDrop = sortedComposition.getFirst().getKey();
            }
        }

        if (itemToDrop == null) {
            return new ArrayList<>();
        }

        // [2. 실크 터치 처리]
        if (hasSilkTouch) {
            Block blockToDrop = this.itemToBlockMap.get(itemToDrop);
            ItemStack silkStack = new ItemStack(Objects.requireNonNullElseGet(blockToDrop, () -> BuiltInRegistries.BLOCK.get(oreBlockId)));
            return new ArrayList<>(Collections.singletonList(silkStack));
        }

        // [3. 드롭량 계산]
        ItemStack finalDrop = new ItemStack(itemToDrop);
        int dropCount = 1;

        // 행운(Fortune) 효과 적용
        if (fortuneLevel > 0) {
            RandomSource random = level.getRandom();
            for (int i = 0; i < fortuneLevel; i++) {
                if (random.nextInt(100) < 50) {
                    dropCount++;
                }
            }
            dropCount += random.nextInt((int) (fortuneLevel * 0.5f) + 1); // 약간의 변동성 추가
        }

        // 풍부함(Richness) 효과 적용
        if (this.richness > 1.0f && level.getRandom().nextFloat() < (this.richness - 1.0f)) {
            dropCount++;
        }

        // [4. 드롭량 관련 특성 적용]
        if (this instanceof ArtificialNodeBlockEntity artificialNode) {
            Quirk.QuirkContext context = new Quirk.QuirkContext((ServerLevel) level, worldPosition, this, null);
            for (Quirk quirk : artificialNode.getQuirks()) {
                dropCount = quirk.onCalculateDrops(dropCount, context);
            }
        }

        finalDrop.setCount(dropCount);

        // 최종적으로 생성된 아이템 리스트를 반환
        return new ArrayList<>(Collections.singletonList(finalDrop));
    }


    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide) {

            if (quirkCheckOffset == -1) {
                quirkCheckOffset = (int) (level.getGameTime() % 40);
            }
            if ((level.getGameTime() + quirkCheckOffset) % 40 == 0) {
                applyPeriodicQuirkEffects();
            }

            float regenerationRate = this.regeneration;
            if (this instanceof ArtificialNodeBlockEntity artificialNode) {
                Quirk.QuirkContext context = new Quirk.QuirkContext((ServerLevel) level, worldPosition, this, null);
                for (Quirk quirk : artificialNode.getQuirks()) {
                    regenerationRate = quirk.onCalculateRegeneration(regenerationRate, context);
                }
            }
            if (regenerationRate > 0 || this.temporaryRegenBoost > 0) {
                float baseRegen = regenerationRate + this.temporaryRegenBoost; // 부스트 적용
                float baseRegenPerSecond = baseRegen * 20;
                float finalRegenMultiplier = 0.8f + (this.richness * 0.8f);
                float finalRegenPerTick = (baseRegenPerSecond * finalRegenMultiplier) / 20f;

                if (this.currentYield < this.maxYield) {
                    this.currentYield = Math.min(this.maxYield, this.currentYield + finalRegenPerTick);
                }
            }

            // 부스트는 매 틱 감소하여 점차 사라짐
            if (this.temporaryRegenBoost > 0) {
                this.temporaryRegenBoost = Math.max(0, this.temporaryRegenBoost - 0.0001f);
            }


            if (this.maxFluidCapacity > 0 && this.currentFluidAmount < this.maxFluidCapacity) {
                // 1. 기본 초당 재생량 (5mb/tick = 100mb/sec)
                float baseFluidRegenPerTick = 5.0f;

                // 2. 풍부함(richness)과 재생력(regeneration)에 따른 보너스 계산
                // richness는 최대 +50% 보너스, regeneration은 최대 +50% 보너스를 줌
                float bonusMultiplier = (this.richness - 0.8f) + (this.regeneration / 0.005f * 0.5f);

                // 3. 최종 재생량 계산 (기본값에 보너스를 더함)
                float finalFluidRegenPerTick = baseFluidRegenPerTick * (1.0f + bonusMultiplier);

                this.currentFluidAmount = Math.min(this.maxFluidCapacity, this.currentFluidAmount + finalFluidRegenPerTick);
            }

            // 1초에 한 번만 동기화
            if(level.getGameTime() % 20 == 0) {
                setChanged();
            }


            // 균열 타이머 처리
            if (this.cracked && this.crackTimer > 0) {
                this.crackTimer--;

                // 타이머가 1초(20틱) 지날 때마다 클라이언트에 동기화 신호를 보냅니다.
                // 매 틱마다 보내면 부하가 크므로, 1초에 한 번만 갱신해도 충분합니다.
                if (this.crackTimer % 20 == 0) {
                    setChanged();
                    sendData(); // sendData()는 setChanged()와 함께 쓰여 동기화를 확실하게 합니다.
                }

                if (this.crackTimer == 0) {
                    // 타이머가 다 되면 균열 상태를 해제 (이때 setCracked 내부에서 sendBlockUpdated가 호출됨)
                    setCracked(false);
                }
            }
        }//] 주기적 특수 효과 처리
        if (auraCheckCooldown > 0) {
            auraCheckCooldown--;
        } else {
            auraCheckCooldown = 40;
            applyPeriodicQuirkEffects();
        }

        assert level != null;
        if(level.getGameTime() % 20 == 0) {
            setChanged();
        }
    }
    // 주기적 효과를 처리하는 새 메서드
    private void applyPeriodicQuirkEffects() {
        if (!(this instanceof ArtificialNodeBlockEntity artificialNode)) return;

        // 주기적 효과가 있는 특성들을 위해 컨텍스트 생성
        Quirk.QuirkContext context = new Quirk.QuirkContext((ServerLevel) level, worldPosition, this, null);

        // 양극성 보너스는 매번 체크하기 전에 초기화
        this.setPolarityBonusActive(false);

        // 이 노드가 가진 모든 특성에 대해 onPeriodicTick 이벤트 호출
        for (Quirk quirk : artificialNode.getQuirks()) {
            quirk.onPeriodicTick(context);
        }
    }

    public boolean isPolarityBonusActive() {
        return polarityBonusActive;
    }
    // 활력 오라 효과를 받기 위한 public 메서드
    public void receiveRegenBoost() {
        // 부스트 값을 최대치로 설정 (0.001은 초당 0.02개 추가 재생에 해당)
        this.temporaryRegenBoost = Math.max(this.temporaryRegenBoost, 0.01f);
    }

    // --- NBT 및 동기화 ---
    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("MiningProgress", this.miningProgress);
        tag.putString("BackgroundBlock", backgroundBlockId.toString());
        tag.putString("OreBlock", oreBlockId.toString());
        tag.putBoolean("PolarityBonus", this.polarityBonusActive);
        if (quirkCheckOffset != -1) {
            tag.putInt("QuirkOffset", quirkCheckOffset);
        }

        tag.putInt("MaxYield", this.maxYield);
        tag.putFloat("CurrentYield", this.currentYield);
        tag.putFloat("Hardness", this.hardness);
        tag.putFloat("Richness", this.richness);
        tag.putFloat("Regeneration", this.regeneration);


        if (cracked) {
            tag.putBoolean("Cracked", true);
            tag.putInt("CrackTimer", crackTimer);
        }
        ListTag compositionList = new ListTag();
        for (Map.Entry<Item, Float> entry : resourceComposition.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Item", BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
            entryTag.putFloat("Ratio", entry.getValue());
            compositionList.add(entryTag);
        }
        tag.put("Composition", compositionList);

        ListTag itemToBlockList = new ListTag();
        for (Map.Entry<Item, Block> entry : itemToBlockMap.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Item", BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
            entryTag.putString("Block", BuiltInRegistries.BLOCK.getKey(entry.getValue()).toString());
            itemToBlockList.add(entryTag);
        }
        tag.put("ItemToBlockMap", itemToBlockList);


        if (!fluidContent.isEmpty()) {
            tag.put("FluidContent", fluidContent.save(registries));
            tag.putInt("MaxFluidCapacity", maxFluidCapacity);
            tag.putFloat("CurrentFluidAmount", currentFluidAmount);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.miningProgress = tag.getInt("MiningProgress");
        this.backgroundBlockId = ResourceLocation.tryParse(tag.getString("BackgroundBlock"));
        if (this.backgroundBlockId == null) this.backgroundBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.STONE);
        this.oreBlockId = ResourceLocation.tryParse(tag.getString("OreBlock"));
        if (this.oreBlockId == null) this.oreBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.IRON_ORE);

        this.polarityBonusActive = tag.getBoolean("PolarityBonus");
        if (tag.contains("QuirkOffset")) {
            this.quirkCheckOffset = tag.getInt("QuirkOffset");
        }
        this.maxYield = tag.getInt("MaxYield");
        this.currentYield = tag.getFloat("CurrentYield");
        this.hardness = tag.getFloat("Hardness");
        this.richness = tag.getFloat("Richness");
        this.regeneration = tag.getFloat("Regeneration");

        cracked = tag.getBoolean("Cracked");
        if (cracked) {
            crackTimer = tag.getInt("CrackTimer");
        }

        this.resourceComposition.clear();
        if (tag.contains("Composition", 9)) {
            ListTag compositionList = tag.getList("Composition", 10);
            for (int i = 0; i < compositionList.size(); i++) {
                CompoundTag entryTag = compositionList.getCompound(i);
                BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(entryTag.getString("Item")))
                        .ifPresent(item -> {
                            float ratio = entryTag.getFloat("Ratio");
                            this.resourceComposition.put(item, ratio);
                        });
            }
        }
        this.itemToBlockMap.clear();
        if (tag.contains("ItemToBlockMap", 9)) {
            ListTag itemToBlockList = tag.getList("ItemToBlockMap", 10);
            for (int i = 0; i < itemToBlockList.size(); i++) {
                CompoundTag entryTag = itemToBlockList.getCompound(i);
                Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(entryTag.getString("Item")));
                Optional<Block> blockOpt = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(entryTag.getString("Block")));
                if (itemOpt.isPresent() && blockOpt.isPresent()) {
                    this.itemToBlockMap.put(itemOpt.get(), blockOpt.get());
                }
            }
        }

        if (tag.contains("FluidContent")) {
            this.fluidContent = FluidStack.parse(registries, tag.getCompound("FluidContent")).orElse(FluidStack.EMPTY);
            this.maxFluidCapacity = tag.getInt("MaxFluidCapacity");
            this.currentFluidAmount = tag.getFloat("CurrentFluidAmount");
        } else {
            this.fluidContent = FluidStack.EMPTY;
        }

        if (clientPacket) {
            requestModelDataUpdate();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }
    }

    // --- Goggle 툴팁 ---
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Component header = Component.translatable("goggle.adsdrill.ore_node.header").withStyle(ChatFormatting.GOLD);
        tooltip.add(Component.literal("    ").append(header));


        if (!resourceComposition.isEmpty()) {
            Component compositionHeader = Component.translatable("goggle.adsdrill.ore_node.composition").withStyle(ChatFormatting.GRAY);
            tooltip.add(Component.literal(" ").append(compositionHeader));

            List<Map.Entry<Item, Float>> sortedComposition = new ArrayList<>(resourceComposition.entrySet());
            sortedComposition.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            for (Map.Entry<Item, Float> entry : sortedComposition) {
                Component compositionLine = Component.literal("  - ")
                        .append(entry.getKey().getDescription().copy().withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(String.format(": %.1f%%", entry.getValue() * 100)).withStyle(ChatFormatting.DARK_AQUA));
                tooltip.add(compositionLine);
            }
        }

        tooltip.add(Component.literal("")); // 구분선

        // 매장량
        tooltip.add(Component.literal(" ")
                .append(Component.translatable("goggle.adsdrill.ore_node.yield", String.format("%d / %d", (int)this.currentYield, this.maxYield))
                        .withStyle(ChatFormatting.GRAY)));

        // 특성
        tooltip.add(Component.literal("")); // 구분선

        //  단단함(Hardness) 표시
        MutableComponent hardnessLine = Component.literal(" ")
                .append(Component.translatable("goggle.adsdrill.ore_node.hardness").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": "));

        if (this.hardness < 0.75f) {
            hardnessLine.append(Component.translatable("goggle.adsdrill.ore_node.hardness.brittle").withStyle(ChatFormatting.GREEN));
        } else if (this.hardness < 1.25f) {
            hardnessLine.append(Component.translatable("goggle.adsdrill.ore_node.hardness.normal").withStyle(ChatFormatting.GRAY));
        } else if (this.hardness < 1.75f) {
            hardnessLine.append(Component.translatable("goggle.adsdrill.ore_node.hardness.tough").withStyle(ChatFormatting.GOLD));
        } else {
            hardnessLine.append(Component.translatable("goggle.adsdrill.ore_node.hardness.resilient").withStyle(ChatFormatting.RED));
        }
        tooltip.add(hardnessLine);

        // 풍부함(Richness) 표시
        MutableComponent richnessLine = Component.literal(" ")
                .append(Component.translatable("goggle.adsdrill.ore_node.richness").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": "));

        if (this.richness < 1.0f) {
            richnessLine.append(Component.translatable("goggle.adsdrill.ore_node.richness.sparse").withStyle(ChatFormatting.DARK_GRAY));
        } else if (this.richness < 1.2f) {
            richnessLine.append(Component.translatable("goggle.adsdrill.ore_node.richness.normal").withStyle(ChatFormatting.GRAY));
        } else if (this.richness < 1.4f) {
            richnessLine.append(Component.translatable("goggle.adsdrill.ore_node.richness.rich").withStyle(ChatFormatting.AQUA));
        } else {
            richnessLine.append(Component.translatable("goggle.adsdrill.ore_node.richness.bountiful").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        }
        tooltip.add(richnessLine);

        // 재생력
        if (this.regeneration > 0) {
            MutableComponent regenLine = Component.literal(" ")
                    .append(Component.translatable("goggle.adsdrill.ore_node.regeneration").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": "));

            if (this.regeneration * 20 > 0.015f) {
                regenLine.append(Component.translatable("goggle.adsdrill.ore_node.regeneration.strong").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            } else {
                regenLine.append(Component.translatable("goggle.adsdrill.ore_node.regeneration.weak").withStyle(ChatFormatting.DARK_PURPLE));
            }
            tooltip.add(regenLine);
        }

        // 플레이어가 Sneaking(웅크리기) 상태일 때만 상세 수치를 보여줍니다.
        if (isPlayerSneaking) {
            tooltip.add(Component.literal("  ")
                    .append(Component.literal(String.format("(H:%.2f, R:%.2f, G:%.5f/s)", this.hardness, this.richness, this.regeneration * 20))
                            .withStyle(ChatFormatting.DARK_GRAY)));
        }
        //유체 정보 툴팁 추가
        tooltip.add(Component.literal("")); // 구분선
        MutableComponent fluidHeader = Component.translatable("goggle.adsdrill.drill_core.storage.fluid").withStyle(ChatFormatting.AQUA);
        tooltip.add(Component.literal(" ").append(fluidHeader));

        if (fluidContent.isEmpty()) {
            tooltip.add(Component.literal("  - ").append(Component.translatable("goggle.adsdrill.ore_node.fluid.empty").withStyle(ChatFormatting.DARK_GRAY)));
        } else {
            MutableComponent fluidLine = Component.literal("  - ")
                    .append(fluidContent.getHoverName().copy().withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(String.format(": %,d / %,d mb", (int)currentFluidAmount, maxFluidCapacity)).withStyle(ChatFormatting.GRAY));
            tooltip.add(fluidLine);
        }

        if (cracked) {
            // 남은 시간을 초 단위로 표시
            int secondsLeft = crackTimer / 20;
            tooltip.add(Component.literal(" ").append(Component.literal("[Cracked] (" + secondsLeft + "s left)").withStyle(ChatFormatting.YELLOW)));
        }
        return true;
    }
    /**
     * 클라이언트 측에서 배경 블록의 BlockState를 안전하게 가져옵니다.
     * 렌더링 및 색상 처리에 사용됩니다.
     * @return 배경 블록의 BlockState, 실패 시 돌(Stone)의 기본 상태
     */
    public BlockState getBackgroundStateClient() {
        Block backgroundBlock = BuiltInRegistries.BLOCK.get(getBackgroundBlockId());
        if (backgroundBlock != Blocks.AIR) {
            return backgroundBlock.defaultBlockState();
        }
        return Blocks.STONE.defaultBlockState();
    }

    @Nonnull
    @Override
    public ModelData getModelData() {
        // 배경 블록의 '상태'를 ModelData에 직접 넣어줍니다.
        return ModelData.builder()
                .with(BACKGROUND_STATE, getBackgroundStateClient())
                .build();
    }
}