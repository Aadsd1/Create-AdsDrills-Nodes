package com.adsd.adsdrill.content.kinetics.node;


import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.adsd.adsdrill.crafting.NodeRecipe;
import com.adsd.adsdrill.crafting.Quirk;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
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
import net.minecraft.world.level.block.entity.BlockEntity;
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

    private boolean polarityBonusActive = false;


    private float effectiveHardness;
    private float effectiveRegeneration;
    private boolean isPolarityBonusActiveForClient = false;

    // --- 채굴 관련 필드 ---
    private float sharedMiningProgress = 0f;
    private BlockPos lastMiningDrillPos = null; // 시각 효과를 위한 마지막 채굴자 위치
    private int ticksSinceLastMine = 0;         // 채굴 중단 감지를 위한 타임아웃 타이머

    // 렌더링을 위한 클라이언트 전용 필드
    private float clientMiningProgress = 0f;

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

    // 렌더러가 사용할 Getter
    public BlockPos getMiningDrillPos() {
        return lastMiningDrillPos;
    }

    // 렌더러가 사용할 Getter
    public float getClientMiningProgress() {
        return clientMiningProgress;
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
        return Math.max(0.1f, this.effectiveHardness);
    }

    /**
     * Quirk 클래스에서 순환 참조 없이 안전하게 경도 값을 참조하기 위해 사용됩니다.
     * @return 원본 경도 값
     */
    public float getBaseHardness() {
        return this.hardness;
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

    public int getMaxYield() {
        return this.maxYield;
    }

    public float getCurrentYield() {
        return this.currentYield;
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



    public List<ItemStack> applyMiningTick(int miningAmount, int fortuneLevel, boolean hasSilkTouch, BlockPos miningDrillPos) {
        if (!(level instanceof ServerLevel serverLevel) || currentYield <= 0) {
            return Collections.emptyList();
        }

        // 채굴 신호가 들어올 때마다 타임아웃과 마지막 채굴자 위치를 갱신합니다.
        this.ticksSinceLastMine = 0;
        this.lastMiningDrillPos = miningDrillPos;

        double effectiveMiningAmount = (miningAmount / getHardness());

        if (this instanceof ArtificialNodeBlockEntity artificialNode) {
            Quirk.QuirkContext context = new Quirk.QuirkContext(serverLevel, worldPosition, this, null);
            for (Quirk quirk : artificialNode.getQuirks()) {
                effectiveMiningAmount = quirk.onCalculateMiningAmount(effectiveMiningAmount, context);
            }
        }

        // 공유 진행도에 누적합니다. (초기화 없음)
        this.sharedMiningProgress += (float) effectiveMiningAmount;

        if (level.getGameTime() % 2 == 0) {
            setChanged();
            sendData();
        }

        int miningResistance = 1000;
        List<ItemStack> allDrops = new ArrayList<>();

        // 진행도가 저항을 초과하는 동안 계속해서 채굴 주기를 실행합니다.
        while (this.sharedMiningProgress >= miningResistance) {
            this.sharedMiningProgress -= miningResistance;

            Direction miningFace = Direction.getNearest(
                    (float)(this.worldPosition.getX() - this.lastMiningDrillPos.getX()),
                    (float)(this.worldPosition.getY() - this.lastMiningDrillPos.getY()),
                    (float)(this.worldPosition.getZ() - this.lastMiningDrillPos.getZ())
            );
            spawnMiningParticles(serverLevel, miningFace);

            if (this.isCracked()) {
                for (NodeRecipe recipe : NodeRecipe.RECIPES) {
                    if (checkRecipeConditions(recipe)) {
                        if (serverLevel.getRandom().nextFloat() < recipe.chance()) {
                            this.currentYield -= recipe.consumptionMultiplier();
                            setChanged();
                            sendData();
                            allDrops.add(recipe.output().copy());
                            if (this.currentYield <= 0) break;
                        }
                    }
                }
            }
            if (this.currentYield <= 0) break;

            List<ItemStack> dropsThisCycle = getNormalDrops(fortuneLevel, hasSilkTouch);
            allDrops.addAll(dropsThisCycle);

            this.currentYield--;
            if (this instanceof ArtificialNodeBlockEntity artificialNode) {
                Quirk.QuirkContext context = new Quirk.QuirkContext(serverLevel, worldPosition, this, null);
                for (Quirk quirk : artificialNode.getQuirks()) {
                    quirk.onYieldConsumed(context);
                }
            }
            if (this.currentYield <= 0) break;
        }

        setChanged();
        sendData();
        return allDrops;
    }

    public List<ItemStack> applySpecificMiningTick(int miningAmount, int fortuneLevel, boolean hasSilkTouch, Item specificItemToMine, BlockPos miningDrillPos) {
        if (!(level instanceof ServerLevel serverLevel) || currentYield <= 0 || !resourceComposition.containsKey(specificItemToMine)) {
            return Collections.emptyList();
        }

        this.ticksSinceLastMine = 0;
        this.lastMiningDrillPos = miningDrillPos;

        float effectiveMiningAmount = miningAmount / getHardness();
        this.sharedMiningProgress += effectiveMiningAmount;

        if (level.getGameTime() % 2 == 0) {
            setChanged();
            sendData();
        }

        int miningResistance = 1000;
        List<ItemStack> allDrops = new ArrayList<>();

        while (this.sharedMiningProgress >= miningResistance) {
            this.sharedMiningProgress -= miningResistance;
            this.currentYield--;

            Direction miningFace = Direction.getNearest(
                    (float)(this.worldPosition.getX() - miningDrillPos.getX()),
                    (float)(this.worldPosition.getY() - miningDrillPos.getY()),
                    (float)(this.worldPosition.getZ() - miningDrillPos.getZ())
            );
            spawnMiningParticles(serverLevel, miningFace);

            if (hasSilkTouch) {
                Block blockToDrop = this.itemToBlockMap.get(specificItemToMine);
                allDrops.add(new ItemStack(Objects.requireNonNullElseGet(blockToDrop, () -> BuiltInRegistries.BLOCK.get(oreBlockId))));
            } else {
                ItemStack finalDrop = new ItemStack(specificItemToMine);
                int dropCount = 1;
                if (fortuneLevel > 0) {
                    RandomSource rand = serverLevel.getRandom();
                    for (int i = 0; i < fortuneLevel; i++) {
                        if (rand.nextInt(fortuneLevel + 2) > 1) dropCount++;
                    }
                }
                if (this.richness > 1.0f && serverLevel.getRandom().nextFloat() < (this.richness - 1.0f)) {
                    finalDrop.grow(finalDrop.getCount());
                }
                finalDrop.setCount(dropCount);
                allDrops.add(finalDrop);
            }

            if (this.currentYield <= 0) break;
        }

        setChanged();
        sendData();
        return allDrops;
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
     * 채굴이 완료된 면에 배경 블록 파티클을 생성하는 메서드입니다.
     * @param level 서버 월드
     * @param face 채굴이 이루어진 면
     */
    private void spawnMiningParticles(ServerLevel level, Direction face) {
        BlockState backgroundState = getBackgroundStateClient(); // 렌더링용으로 만든 메서드 재활용
        BlockParticleOption particleOption = new BlockParticleOption(ParticleTypes.BLOCK, backgroundState);
        RandomSource random = level.getRandom();

        face=face.getOpposite();

        // 파티클을 30개 생성
        for (int i = 0; i < 30; ++i) {
            double px = worldPosition.getX() + random.nextDouble();
            double py = worldPosition.getY() + random.nextDouble();
            double pz = worldPosition.getZ() + random.nextDouble();

            // 면의 위치에 따라 파티클 생성 위치를 고정
            switch (face) {
                case DOWN -> py = worldPosition.getY();
                case UP -> py = worldPosition.getY() + 1.0;
                case NORTH -> pz = worldPosition.getZ();
                case SOUTH -> pz = worldPosition.getZ() + 1.0;
                case WEST -> px = worldPosition.getX();
                case EAST -> px = worldPosition.getX() + 1.0;
            }

            // 면의 바깥쪽으로 약간 튀어나가도록 속도 설정
            double xd = (random.nextDouble() - 0.5) * 0.2 + face.getStepX() * 0.1;
            double yd = (random.nextDouble() - 0.5) * 0.2 + face.getStepY() * 0.1;
            double zd = (random.nextDouble() - 0.5) * 0.2 + face.getStepZ() * 0.1;

            level.sendParticles(particleOption, px, py, pz, 1, xd, yd, zd, 0.1);
        }
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
            serverTick();
        }
    }

    private void serverTick() {
        // 1. 채굴 타임아웃 로직: 0.5초간 채굴 신호가 없으면 채굴 관련 상태를 초기화합니다.
        ticksSinceLastMine++;
        if (ticksSinceLastMine > 10 && (lastMiningDrillPos != null || sharedMiningProgress > 0)) {
            lastMiningDrillPos = null;
            sharedMiningProgress = 0f;
            setChanged();
            sendData();
        }

        // 2. 주기적 특성 효과 발동: 40틱(2초)마다 Polarity, AOV 이펙트 등의 특성을 발동시킵니다.
        if (this instanceof ArtificialNodeBlockEntity artificialNode) {
            if (level != null) {
                if (quirkCheckOffset == -1) {
                    quirkCheckOffset = (int) (level.getGameTime() % 40);
                }
                if ((level.getGameTime() + quirkCheckOffset) % 40 == 0) {
                    applyPeriodicQuirkEffects(artificialNode);
                }
            }
        }

        // 3. 1초(20틱)마다 서버에서 모든 능력치를 재계산하고, 변경 시 클라이언트에 동기화합니다.
        if (level != null && level.getGameTime() % 20 == 0) {
            boolean changed = recalculateEffectiveStats();
            if (changed) {
                setChanged();
                sendData();
            }
        }

        // 4. 매장량 재생 로직: 'effectiveRegeneration' 필드를 사용하여 실제 매장량을 회복시킵니다.
        // 이 값은 1초마다 재계산되므로, 툴팁과 실제 회복량이 항상 일치합니다.
        float finalRegenMultiplier = 0.8f + (this.richness * 0.8f);
        float finalRegenPerTick = (this.effectiveRegeneration * 20) * finalRegenMultiplier / 20f;
        if (this.currentYield < this.maxYield) {
            this.currentYield = Math.min(this.maxYield, this.currentYield + finalRegenPerTick);
        }

        // 5. 유체 재생 및 균열 타이머
        if (this.maxFluidCapacity > 0 && this.currentFluidAmount < this.maxFluidCapacity) {
            float baseFluidRegenPerTick = 5.0f;
            float bonusMultiplier = (this.richness - 0.8f) + (this.regeneration / 0.005f * 0.5f);
            float finalFluidRegenPerTick = baseFluidRegenPerTick * (1.0f + bonusMultiplier);
            this.currentFluidAmount = Math.min(this.maxFluidCapacity, this.currentFluidAmount + finalFluidRegenPerTick);
        }

        if (this.cracked && this.crackTimer > 0) {
            this.crackTimer--;
            if (this.crackTimer % 20 == 0) {
                setChanged();
                sendData();
            }
            if (this.crackTimer == 0) {
                setCracked(false);
            }
        }
    }


    /**
     * 서버 측에서만 호출되어 특성 효과가 적용된 최종 능력치를 계산하고,
     * 값이 변경되었는지 여부를 반환합니다.
     * @return 능력치 값이 변경되었으면 true
     */
    private boolean recalculateEffectiveStats() {
        if (level == null || level.isClientSide()) return false;

        // 원본 값으로 시작
        float currentEffectiveHardness = this.hardness;
        float currentEffectiveRegen = this.regeneration;
        boolean currentPolarityState = this.isPolarityBonusActive();

        if (this instanceof ArtificialNodeBlockEntity artificialNode) {
            Quirk.QuirkContext context = new Quirk.QuirkContext((ServerLevel) level, worldPosition, this, null);
            for (Quirk quirk : artificialNode.getQuirks()) {
                currentEffectiveHardness = quirk.onCalculateHardness(currentEffectiveHardness, context);
                currentEffectiveRegen = quirk.onCalculateRegeneration(currentEffectiveRegen, context);
            }
        }

        boolean changed = false;
        if (Math.abs(this.effectiveHardness - currentEffectiveHardness) > 1e-6) {
            this.effectiveHardness = currentEffectiveHardness;
            changed = true;
        }
        if (Math.abs(this.effectiveRegeneration - currentEffectiveRegen) > 1e-9) {
            this.effectiveRegeneration = currentEffectiveRegen;
            changed = true;
        }
        if (this.isPolarityBonusActiveForClient != currentPolarityState) {
            this.isPolarityBonusActiveForClient = currentPolarityState;
            changed = true;
        }

        return changed;
    }


    /**
     * 주기적으로 발동하는 특성들의 로직을 실행합니다.
     * ArtificialNodeBlockEntity 인스턴스를 직접 받아 처리합니다.
     */
    private void applyPeriodicQuirkEffects(ArtificialNodeBlockEntity artificialNode) {
        if (level == null) return;
        Quirk.QuirkContext context = new Quirk.QuirkContext((ServerLevel) level, worldPosition, this, null);

        // Polarity는 자신의 상태를 바꾸므로 먼저 초기화합니다.
        this.setPolarityBonusActive(false);

        for (Quirk quirk : artificialNode.getQuirks()) {
            quirk.onPeriodicTick(context);
        }
    }

    public boolean isPolarityBonusActive() {
        return polarityBonusActive;
    }

    // --- NBT 및 동기화 ---
    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);

        tag.putFloat("SharedMiningProgress", this.sharedMiningProgress);
        if (lastMiningDrillPos != null) {
            tag.put("LastMiningDrillPos", NbtUtils.writeBlockPos(lastMiningDrillPos));
        }

        tag.putString("BackgroundBlock", backgroundBlockId.toString());
        tag.putString("OreBlock", oreBlockId.toString());
        tag.putBoolean("PolarityBonus", this.polarityBonusActive);
        if (quirkCheckOffset != -1) tag.putInt("QuirkOffset", quirkCheckOffset);
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
        if (clientPacket) {
            tag.putFloat("ClientProgress", this.sharedMiningProgress / 1000.0f);
            tag.putFloat("EffectiveHardness", this.effectiveHardness);
            tag.putFloat("EffectiveRegen", this.effectiveRegeneration);
            tag.putBoolean("PolarityBonusClient", this.isPolarityBonusActiveForClient);
        }
    }


    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        this.sharedMiningProgress = tag.getFloat("SharedMiningProgress");
        this.lastMiningDrillPos = tag.contains("LastMiningDrillPos") ? NbtUtils.readBlockPos(tag, "LastMiningDrillPos").orElse(null) : null;

        this.backgroundBlockId = ResourceLocation.tryParse(tag.getString("BackgroundBlock"));
        if (this.backgroundBlockId == null) this.backgroundBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.STONE);
        this.oreBlockId = ResourceLocation.tryParse(tag.getString("OreBlock"));
        if (this.oreBlockId == null) this.oreBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.IRON_ORE);
        this.polarityBonusActive = tag.getBoolean("PolarityBonus");
        if (tag.contains("QuirkOffset")) this.quirkCheckOffset = tag.getInt("QuirkOffset");
        this.maxYield = tag.getInt("MaxYield");
        this.currentYield = tag.getFloat("CurrentYield");
        this.hardness = tag.getFloat("Hardness");
        this.richness = tag.getFloat("Richness");
        this.regeneration = tag.getFloat("Regeneration");
        cracked = tag.getBoolean("Cracked");
        if (cracked) crackTimer = tag.getInt("CrackTimer");
        this.resourceComposition.clear();
        if (tag.contains("Composition", 9)) {
            ListTag compositionList = tag.getList("Composition", 10);
            for (int i = 0; i < compositionList.size(); i++) {
                CompoundTag entryTag = compositionList.getCompound(i);
                BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(entryTag.getString("Item")))
                        .ifPresent(item -> this.resourceComposition.put(item, entryTag.getFloat("Ratio")));
            }
        }
        this.itemToBlockMap.clear();
        if (tag.contains("ItemToBlockMap", 9)) {
            ListTag itemToBlockList = tag.getList("ItemToBlockMap", 10);
            for (int i = 0; i < itemToBlockList.size(); i++) {
                CompoundTag entryTag = itemToBlockList.getCompound(i);
                Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(entryTag.getString("Item")));
                Optional<Block> blockOpt = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(entryTag.getString("Block")));
                if (itemOpt.isPresent() && blockOpt.isPresent()) this.itemToBlockMap.put(itemOpt.get(), blockOpt.get());
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
            this.clientMiningProgress = tag.getFloat("ClientProgress");
            this.effectiveHardness = tag.getFloat("EffectiveHardness");
            this.effectiveRegeneration = tag.getFloat("EffectiveRegen");
            this.isPolarityBonusActiveForClient = tag.getBoolean("PolarityBonusClient");
            requestModelDataUpdate();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
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


        List<Component> statModifierTooltips = getStatModifiersTooltip();
        if (!statModifierTooltips.isEmpty()) {
            tooltip.addAll(statModifierTooltips);
            tooltip.add(Component.literal("")); // 구분선
        }

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
     * 고글 툴팁에 표시될 능력치 변화 정보를 생성합니다.
     * 이 메서드는 클라이언트에서 호출되며, 서버로부터 동기화된 'effective' 필드 값을 사용하여
     * 어떤 특성이 능력치를 어떻게 변화시켰는지 명확하게 보여줍니다.
     */
    private List<Component> getStatModifiersTooltip() {
        List<Component> modifiers = new ArrayList<>();
        // 이 노드가 특성을 가질 수 있는 ArtificialNodeBlockEntity가 아니면 아무것도 표시하지 않습니다.
        if (!(this instanceof ArtificialNodeBlockEntity artificialNode)) {
            return modifiers;
        }

        // --- 1. 경도(Hardness) 변화 툴팁 ---
        // 원본 경도와 특성 효과가 적용된 최종 경도 사이에 유의미한 차이가 있을 때만 표시합니다.
        if (Math.abs(this.hardness - this.effectiveHardness) > 0.01f) {
            MutableComponent line = Component.literal(" ")
                    // "Hardness: "
                    .append(Component.translatable("goggle.adsdrill.ore_node.hardness").withStyle(ChatFormatting.GRAY))
                    .append(": ")
                    // "1.50 -> 1.20"
                    .append(Component.literal(String.format("%.2f -> %.2f", this.hardness, this.effectiveHardness)).withStyle(ChatFormatting.AQUA))
                    // "(Aquifer)"
                    .append(Component.literal(" (Aquifer)").withStyle(ChatFormatting.DARK_AQUA));
            modifiers.add(line);
        }

        // --- 2. 재생력(Regeneration) 변화 툴팁 ---
        // 원본 재생력과 최종 재생력 사이에 유의미한 차이가 있을 때만 표시합니다.
        if (Math.abs(this.regeneration - this.effectiveRegeneration) > 1e-9) {
            // 풍부함(Richness)까지 고려한 최종 초당 재생량을 계산합니다.
            float finalRegenMultiplier = 0.8f + (this.richness * 0.8f);
            float originalRegenPerSecond = (this.regeneration * 20) * finalRegenMultiplier;
            float effectiveRegenPerSecond = (this.effectiveRegeneration * 20) * finalRegenMultiplier;

            // 메인 라인: "Regeneration: 0.0100 -> 0.0150 Yield/s"
            MutableComponent mainLine = Component.literal(" ")
                    .append(Component.translatable("goggle.adsdrill.ore_node.regeneration").withStyle(ChatFormatting.GRAY))
                    .append(": ")
                    .append(Component.literal(String.format("%.4f -> %.4f Yield/s", originalRegenPerSecond, effectiveRegenPerSecond)).withStyle(ChatFormatting.LIGHT_PURPLE));
            modifiers.add(mainLine);

            // --- 2a. 각 특성의 기여도를 세부적으로 표시 ---
            // Petrified Heart 특성이 있는지 확인하고, 있다면 기여도를 표시합니다.
            if (artificialNode.hasQuirk(Quirk.PETRIFIED_HEART)) {
                float petrifiedHeartBonus = this.regeneration * (this.hardness * 0.25f);
                float bonusPerSecond = (petrifiedHeartBonus * 20) * finalRegenMultiplier;
                if (bonusPerSecond > 0) {
                    modifiers.add(Component.literal("   └ ")
                            .append(Component.literal(String.format("+%.4f from Petrified Heart", bonusPerSecond)).withStyle(ChatFormatting.DARK_PURPLE)));
                }
            }

            // Aura of Vitality 특성이 있는지 확인하고, 있다면 기여도를 표시합니다.
            if (artificialNode.hasQuirk(Quirk.AURA_OF_VITALITY)) {
                // AOV의 순수 보너스량을 계산합니다.
                // 이 로직은 Quirk.java의 onCalculateRegeneration과 동일해야 합니다.
                final int scanRadius = 3;
                List<OreNodeBlockEntity> nearbyNodes = new ArrayList<>();
                if (level != null) { // 클라이언트에서도 월드 접근이 가능합니다.
                    BlockPos.betweenClosedStream(this.worldPosition.offset(-scanRadius, -scanRadius, -scanRadius), this.worldPosition.offset(scanRadius, scanRadius, scanRadius))
                            .filter(pos -> !pos.equals(this.worldPosition))
                            .forEach(pos -> {
                                BlockEntity be = level.getBlockEntity(pos);
                                if (be instanceof OreNodeBlockEntity node) {
                                    nearbyNodes.add(node);
                                }
                            });
                }
                float bonusMultiplier = (float) (nearbyNodes.size() * 0.10);
                long synergyCount = nearbyNodes.stream()
                        .filter(node -> node instanceof ArtificialNodeBlockEntity an && an.hasQuirk(Quirk.AURA_OF_VITALITY))
                        .count();
                bonusMultiplier += (float) (synergyCount * 0.25);

                float aovBonus = this.regeneration * bonusMultiplier;
                float bonusPerSecond = (aovBonus * 20) * finalRegenMultiplier;
                if (bonusPerSecond > 0) {
                    modifiers.add(Component.literal("   └ ")
                            .append(Component.literal(String.format("+%.4f from Aura of Vitality", bonusPerSecond)).withStyle(ChatFormatting.DARK_PURPLE)));
                }
            }
        }

        return modifiers;
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