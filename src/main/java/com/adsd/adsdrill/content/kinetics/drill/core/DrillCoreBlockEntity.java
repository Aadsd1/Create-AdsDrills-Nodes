package com.adsd.adsdrill.content.kinetics.drill.core;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.content.kinetics.base.IResourceAccessor;
import com.adsd.adsdrill.content.kinetics.drill.head.*;
import com.adsd.adsdrill.content.kinetics.module.*;
import com.adsd.adsdrill.content.kinetics.node.ArtificialNodeBlockEntity;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlockEntity;
import com.adsd.adsdrill.crafting.Quirk;
import com.adsd.adsdrill.registry.AdsDrillItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.adsd.adsdrill.content.kinetics.base.DrillEnergyStorage;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;



public class DrillCoreBlockEntity extends KineticBlockEntity implements IResourceAccessor {

    private enum InvalidityReason {
        NONE,
        LOOP_DETECTED,
        MULTIPLE_CORES,
        TOO_MANY_MODULES,
        HEAD_MISSING,
        DUPLICATE_PROCESSING_MODULE,
        NO_ENERGY_SOURCE,
        INSUFFICIENT_ENERGY
    }
    public enum Tier implements StringRepresentable {
        BRASS,
        STEEL,
        NETHERITE;

        Tier() {}


        public int getMaxModules() {
            return switch (this) {
                case BRASS -> AdsDrillConfigs.SERVER.brassDrillMaxModules.get();
                case STEEL -> AdsDrillConfigs.SERVER.steelDrillMaxModules.get();
                case NETHERITE -> AdsDrillConfigs.SERVER.netheriteDrillMaxModules.get();
            };
        }

        public float getSpeedBonus() {

            return switch (this) {
                case BRASS -> AdsDrillConfigs.SERVER.brassDrillSpeedBonus.get().floatValue();
                case STEEL -> AdsDrillConfigs.SERVER.steelDrillSpeedBonus.get().floatValue();
                case NETHERITE -> AdsDrillConfigs.SERVER.netheriteDrillSpeedBonus.get().floatValue();
            };
        }

        public float getBaseCooling() {

            return switch (this) {
                case BRASS -> 0.1f;
                case STEEL -> 0.15f;
                case NETHERITE -> 0.2f;
            };
        }

        public float getBaseStress() {
            return switch (this) {
                case BRASS -> AdsDrillConfigs.SERVER.brassDrillBaseStress.get().floatValue();
                case STEEL -> AdsDrillConfigs.SERVER.steelDrillBaseStress.get().floatValue();
                case NETHERITE -> AdsDrillConfigs.SERVER.netheriteDrillBaseStress.get().floatValue();
            };
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.name().toLowerCase();
        }
    }

    private Tier coreTier = Tier.BRASS;

    public static final ModelProperty<DrillCoreBlockEntity> CORE_ENTITY_PROPERTY = new ModelProperty<>();

    private final List<IItemHandler> itemBufferHandlers = new ArrayList<>();
    private final List<IFluidHandler> fluidBufferHandlers = new ArrayList<>();
    private final List<BlockPos> activeSystemModules = new ArrayList<>();
    private final List<BlockPos> bulkProcessingModules = new ArrayList<>();
    private final List<BlockPos> resonatorModules = new ArrayList<>();
    private final List<BlockPos> redstoneBrakeModules = new ArrayList<>();

    private final DrillEnergyStorage energyBuffer = new DrillEnergyStorage(0, Integer.MAX_VALUE, Integer.MAX_VALUE, this::setChanged);

    private float heat = 0.0f;
    private boolean isOverheated = false;

    public static final float BOOST_START_THRESHOLD = AdsDrillConfigs.SERVER.heatBoostStartThreshold.get().floatValue();
    public static final float OVERLOAD_START_THRESHOLD = AdsDrillConfigs.SERVER.heatOverloadStartThreshold.get().floatValue();
    public static final float COOLDOWN_RESET_THRESHOLD = AdsDrillConfigs.SERVER.heatCooldownResetThreshold.get().floatValue();

    private static final int GOGGLE_UPDATE_DEBOUNCE = 10; // 10틱 (0.5초) 마다 업데이트
    private int tickCounter = 0;
    private static final int STRUCTURE_CHECK_COOLDOWN_TICKS = 2;

    private float visualSpeed = 0f;

    private final List<BlockPos> processingModuleChain = new ArrayList<>();

    private BlockPos cachedHeadPos = null;
    private Set<BlockPos> structureCache = new HashSet<>();
    private boolean structureValid = false;
    private InvalidityReason invalidityReason = InvalidityReason.NONE;

    private int structureCheckCooldown;
    private static final int MAX_STRUCTURE_RANGE = AdsDrillConfigs.SERVER.maxDrillStructureSize.get();

    private double speedMultiplier = 1.0;
    private double stressMultiplier = 1.0;
    private double heatMultiplier = 1.0;
    private float totalStressImpact = 0f;

    private boolean polarityBonusActive = false;



    public DrillCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {

        super(type, pos, state);

    }


    /**
     * 드릴 코어가 현재 과열로 인해 작동 중지 상태인지 여부를 반환합니다.
     * @return 과열 상태이면 true
     */
    public boolean isOverheated() {
        return this.isOverheated;
    }
    public float getHeat() {
        return this.heat;
    }

    public boolean tryUpgrade(Item upgradeMaterial) {
        if (level == null || level.isClientSide) return false;

        Tier nextTier = null;
        if (upgradeMaterial == AdsDrillItems.DRILL_CORE_STEEL_UPGRADE.get() && this.coreTier == Tier.BRASS) {
            nextTier = Tier.STEEL;
        } else if (upgradeMaterial == AdsDrillItems.DRILL_CORE_NETHERITE_UPGRADE.get() && this.coreTier == Tier.STEEL) {
            nextTier = Tier.NETHERITE;
        }

        if (nextTier != null) {
            this.coreTier = nextTier;

            // 블록의 상태(BlockState)를 새로운 티어로 업데이트
            BlockState oldState = getBlockState();
            BlockState newState = oldState.setValue(DrillCoreBlock.TIER, nextTier);
            level.setBlock(getBlockPos(), newState, Block.UPDATE_ALL);

            setChanged();
            sendData();
            scheduleStructureCheck();
            return true;
        }
        return false;
    }
    public IEnergyStorage getInternalEnergyBuffer() {
        return this.energyBuffer;
    }
    @Nonnull
    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(CORE_ENTITY_PROPERTY, this).build();
    }

    public Tier getCoreTier() {
        return this.coreTier;
    }
    @Override
    public IItemHandler getInternalItemBuffer() {
        return new DrillStructureItemHandler(this.itemBufferHandlers);
    }
    @Override
    public IFluidHandler getInternalFluidBuffer() {
        return new DrillStructureFluidHandler(this.fluidBufferHandlers);
    }


    @Override
    public ItemStack consumeItems(ItemStack stackToConsume, boolean simulate) {
        if (this.itemBufferHandlers.isEmpty() || stackToConsume.isEmpty()) {
            return stackToConsume; // 버퍼가 없거나 요청이 없으면 소모 불가능
        }

        int amountToConsume = stackToConsume.getCount();

        // 연결된 모든 버퍼를 순회
        for (IItemHandler handler : this.itemBufferHandlers) {
            // 각 버퍼의 모든 슬롯을 순회
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stackInSlot = handler.getStackInSlot(slot);

                // 슬롯에 있는 아이템이 우리가 찾는 아이템과 같은 종류인지 확인
                if (ItemStack.isSameItemSameComponents(stackInSlot, stackToConsume)) {
                    // 이 슬롯에서 필요한 만큼 아이템을 빼냄
                    ItemStack extracted = handler.extractItem(slot, amountToConsume, simulate);

                    // 실제로 빼낸 양만큼 남은 양을 줄임
                    amountToConsume -= extracted.getCount();

                    // 필요한 양을 모두 채웠다면, 더 이상 찾을 필요가 없으므로 루프 종료
                    if (amountToConsume <= 0) {
                        break;
                    }
                }
            }
            if (amountToConsume <= 0) {
                break;
            }
        }

        // 최종적으로 소모하고 '남은' 아이템의 개수를 반환
        // 만약 1개를 요청해서 1개를 다 소모했다면, amountToConsume은 0이 됨
        if (amountToConsume <= 0) {
            return ItemStack.EMPTY; // 모두 소모 성공
        } else {
            // 일부만 소모했거나 전혀 소모하지 못했다면, 소모하지 못한 양을 반환
            ItemStack remainder = stackToConsume.copy();
            remainder.setCount(amountToConsume);
            return remainder;
        }
    }




    /** heat 값을 안전하게 변경합니다. */
    public void addHeat(float amount) {
        this.heat = Mth.clamp(this.heat + amount, 0, 100);
    }
    @Override
    public FluidStack consumeFluid(FluidStack fluidToConsume, boolean simulate) {
        // 가상 핸들러의 drain 메서드를 직접 호출하여 유체를 소모
        IFluidHandler virtualHandler = getInternalFluidBuffer();
        return virtualHandler.drain(fluidToConsume, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
    }
    /**
     * IResourceAccessor의 consumeEnergy 메서드 구현
     */
    @Override
    public int consumeEnergy(int amount, boolean simulate) {
        return this.energyBuffer.extractEnergy(amount, simulate);
    }

    public void onBroken() {
        if (level != null && !level.isClientSide()) {
            Set<BlockPos> allToClear = new HashSet<>(structureCache);
            if (cachedHeadPos != null) allToClear.add(cachedHeadPos);

            for (BlockPos modulePos : allToClear) {
                if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                    moduleBE.updateVisualConnections(new HashSet<>());
                }
            }
        }
    }

    // 열 효율까지 모두 계산된 최종 속도를 반환하는 getter
    public float getFinalSpeed() {
        //  다른 모든 검사보다 먼저 레드스톤 제동을 확인
        if (isHaltedByRedstone()) {
            return 0;
        }

        if (!this.structureValid || isOverStressed()) {
            return 0;
        }
        float finalSpeed=getSpeed()*getHeatEfficiency();


        if (this.polarityBonusActive) {
            finalSpeed *= (float) AdsDrillConfigs.getQuirkConfig(Quirk.POLARITY_POSITIVE).valueMultiplier();
        }

        return finalSpeed;
    }
    // 효율 계산 메서드
    public float getHeatEfficiency() {
        if (isOverheated || heat >= 100.0f) {
            return 0f;
        }

        // 설정값 가져오기
        float bonus = AdsDrillConfigs.SERVER.heatEfficiencyBonus.get().floatValue();
        float penalty = AdsDrillConfigs.SERVER.heatOverloadPenalty.get().floatValue();

        if (heat > OVERLOAD_START_THRESHOLD) {
            // 90% 열(보너스 효율)에서 100% 열(효율 0%)까지 급격히 감소
            float fraction = (heat - OVERLOAD_START_THRESHOLD) / (100f - OVERLOAD_START_THRESHOLD);
            return Math.max(0, bonus - (fraction * penalty));
        } else if (heat > BOOST_START_THRESHOLD) {
            // 40% 열(효율 100%)에서 90% 열(보너스 효율)까지 선형적으로 증가
            float fraction = (heat - BOOST_START_THRESHOLD) / (OVERLOAD_START_THRESHOLD - BOOST_START_THRESHOLD);
            return 1.0f + (fraction * (bonus - 1.0f));
        }

        // 0% ~ 40% 구간은 기본 효율
        return 1.0f;
    }

    public float getVisualSpeed() {
        return this.visualSpeed;
    }

    public Optional<Item> getResonatorFilter() {
        if (level == null || resonatorModules.isEmpty()) {
            return Optional.empty();
        }
        BlockPos resonatorPos = resonatorModules.getFirst();
        if (level.getBlockEntity(resonatorPos) instanceof GenericModuleBlockEntity moduleBE) {
            return moduleBE.getResonatorFilterItem();
        }
        return Optional.empty();
    }

    /**
     * 지정된 노드에 대해 채굴 로직을 적용하고 결과물을 반환합니다.
     * 모든 드릴 헤드는 이 메서드를 통해 채굴을 수행합니다.
     *
     * @param nodeBE       채굴할 OreNodeBlockEntity
     * @param miningAmount 채굴량
     * @param fortune      적용할 행운 레벨
     * @param silkTouch    실크터치 적용 여부
     * @param drillHeadPos 채굴을 수행하는 드릴 헤드의 위치
     * @return 채굴된 아이템 목록
     */
    public List<ItemStack> mineNode(OreNodeBlockEntity nodeBE, int miningAmount, int fortune, boolean silkTouch, BlockPos drillHeadPos) {
        if (level == null || level.isClientSide() || drillHeadPos == null) {
            return Collections.emptyList();
        }
        return nodeBE.applyMiningTick(miningAmount, fortune, silkTouch, drillHeadPos);
    }
    public List<ItemStack> mineSpecificNode(OreNodeBlockEntity nodeBE, int miningAmount, int fortune, boolean silkTouch, Item specificItem, BlockPos drillHeadPos) {
        if (level == null || level.isClientSide()) return List.of();
        return nodeBE.applySpecificMiningTick(miningAmount, fortune, silkTouch, specificItem, drillHeadPos);
    }

    public void scheduleStructureCheck() {
        if (level != null && !level.isClientSide) {
            // 즉시 검사 플래그 대신, 2틱의 쿨다운을 설정합니다.
            // 이미 쿨다운이 진행 중이라면, 다시 초기화합니다.
            this.structureCheckCooldown = 2;
        }
    }

    private void scanAndValidateStructure() {
        if (level == null || level.isClientSide()) return;

        Set<BlockPos> oldStructure = new HashSet<>(this.structureCache);
        if (this.cachedHeadPos != null) {
            oldStructure.add(this.cachedHeadPos);
        }
        // --- 1. 상태 초기화 ---
        this.structureCache.clear();
        this.processingModuleChain.clear();
        this.itemBufferHandlers.clear();
        this.fluidBufferHandlers.clear();
        this.activeSystemModules.clear();
        this.bulkProcessingModules.clear();
        this.resonatorModules.clear();
        this.redstoneBrakeModules.clear();
        this.speedMultiplier = 1.0;
        this.stressMultiplier = 1.0;
        this.heatMultiplier = 1.0;
        this.totalStressImpact = coreTier.getBaseStress();
        int totalEnergyCapacity = 0;
        this.cachedHeadPos = null;
        this.invalidityReason = InvalidityReason.NONE;
        this.structureValid = false;
        Map<BlockPos, Set<Direction>> moduleConnections = new HashMap<>();

        Direction inputFacing = getBlockState().getValue(DirectionalKineticBlock.FACING);
        Direction headFacing = inputFacing.getOpposite();

        // --- 2. 헤드 확인 ---

        BlockPos potentialHeadPos = worldPosition.relative(headFacing);
        BlockState headState = level.getBlockState(potentialHeadPos);
        boolean isLaserHeadAttached = false;

        if (headState.getBlock() instanceof IDrillHead head &&
                headState.hasProperty(DirectionalKineticBlock.FACING) &&
                headState.getValue(DirectionalKineticBlock.FACING) == headFacing) {

            this.cachedHeadPos = potentialHeadPos;
            this.totalStressImpact += head.getStressImpact();

            if (head instanceof LaserDrillHeadBlock) {
                isLaserHeadAttached = true;
            }

        } else {
            this.invalidityReason = InvalidityReason.HEAD_MISSING;
        }

        // 조건 1: 해당 위치의 블록이 IDrillHead인가?
        // 조건 2: 그 헤드 블록의 FACING 방향이 코어가 바라보는 방향(headFacing)과 일치하는가?
        if (headState.getBlock() instanceof IDrillHead head &&
                headState.hasProperty(DirectionalKineticBlock.FACING) &&
                headState.getValue(DirectionalKineticBlock.FACING) == headFacing) {

            this.cachedHeadPos = potentialHeadPos;
            this.totalStressImpact += head.getStressImpact();
        } else {
            this.invalidityReason = InvalidityReason.HEAD_MISSING;
        }
        // --- 3. 재귀적 모듈 구조 탐색 ---
        Set<BlockPos> allFoundModules = new HashSet<>();
        List<BlockPos> allOtherCores = new ArrayList<>();
        Set<BlockPos> allVisited = new HashSet<>();
        allVisited.add(worldPosition);

        for (Direction startDir : Direction.values()) {
            if (startDir.getAxis() == inputFacing.getAxis()) continue;

            BlockPos startPos = worldPosition.relative(startDir);
            if (allVisited.contains(startPos)) continue;

            BlockState startState = level.getBlockState(startPos);
            if (startState.getBlock() instanceof GenericModuleBlock) {
                // 코어와 첫 모듈의 연결 정보를 기록
                moduleConnections.computeIfAbsent(worldPosition, k -> new HashSet<>()).add(startDir);
                moduleConnections.computeIfAbsent(startPos, k -> new HashSet<>()).add(startDir.getOpposite());

                StructureCheckResult result = searchStructureRecursive(startPos, startDir.getOpposite(), allVisited, allFoundModules, allOtherCores, moduleConnections, 0);
                if (result.loopDetected()) { this.invalidityReason = InvalidityReason.LOOP_DETECTED; break; }
                if (result.multipleCoresDetected()) { this.invalidityReason = InvalidityReason.MULTIPLE_CORES; break; }


            }
        }

        // --- 4. 유효성 검사 및 데이터 집계 ---

        if (this.invalidityReason == InvalidityReason.NONE || this.invalidityReason == InvalidityReason.HEAD_MISSING) {
            if (allFoundModules.size() >coreTier.getMaxModules()) {
                this.invalidityReason = InvalidityReason.TOO_MANY_MODULES;
            } else {
                this.structureValid = true;
                this.structureCache = allFoundModules;

                Set<Object> foundProcessingKeys = new HashSet<>();
                for (BlockPos modulePos : this.structureCache) {
                    if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                        ModuleType type = moduleBE.getModuleType();

                        speedMultiplier *= (1.0 + type.getSpeedBonus());
                        stressMultiplier *= (1.0 + type.getStressImpact()); // 스트레스도 곱셈으로 변경
                        heatMultiplier *= (1.0 + type.getHeatModifier());

                        totalEnergyCapacity+=type.getEnergyCapacity();
                        if (type.getItemCapacity() > 0 && moduleBE.getItemHandler() != null) itemBufferHandlers.add(moduleBE.getItemHandler());
                        if (type.getFluidCapacity() > 0 && moduleBE.getFluidHandler() != null) fluidBufferHandlers.add(moduleBE.getFluidHandler());

                        boolean isDuplicate = false;

                        RecipeType<?> recipeType = type.getBehavior().getRecipeType();
                        // 1. 이 모듈이 "우선순위를 가진" 모듈인지 확인 (일반 처리 모듈 또는 필터 모듈)
                        if (recipeType != null || type == ModuleType.FILTER) {
                            // 중복 검사용 키를 결정
                            Object key = (type == ModuleType.FILTER) ? ModuleType.FILTER : recipeType;

                            if (!foundProcessingKeys.add(key)) {
                                isDuplicate = true;
                            } else {
                                // 중복이 아니면, 처리 체인에 추가
                                processingModuleChain.add(modulePos);
                            }
                        }
                        // 2. 이 모듈이 "압축 모듈"인가?
                        else if (type == ModuleType.COMPACTOR) {
                            if (!foundProcessingKeys.add(ModuleType.COMPACTOR)) {
                                isDuplicate = true;
                            } else {
                                bulkProcessingModules.add(modulePos);
                            }

                        } else if (type == ModuleType.RESONATOR) {
                            if (!foundProcessingKeys.add(ModuleType.RESONATOR)) {
                                isDuplicate = true;
                            } else {
                                resonatorModules.add(modulePos);
                            }
                        }


                        else if (type == ModuleType.COOLANT || type == ModuleType.KINETIC_DYNAMO || type == ModuleType.ENERGY_INPUT) {
                            activeSystemModules.add(modulePos);

                        } else if (type == ModuleType.REDSTONE_BRAKE) {
                            redstoneBrakeModules.add(modulePos);
                        }

                        if (isDuplicate) {
                            this.invalidityReason = InvalidityReason.DUPLICATE_PROCESSING_MODULE;
                            this.structureValid = false;
                            break;
                        }
                    }
                }

                if (this.structureValid) {
                    // 이제 processingModuleChain에는 일반 처리 모듈과 필터 모듈이 모두 들어있음
                    // 우선순위에 따라 정렬하면, 플레이어가 설정한 순서대로 실행됨
                    processingModuleChain.sort(Comparator.comparingInt(pos -> {
                        if (level.getBlockEntity(pos) instanceof GenericModuleBlockEntity gme) return gme.getProcessingPriority();
                        return 99;
                    }));
                }
            }
        }



        this.energyBuffer.setCapacity(totalEnergyCapacity);
        // 구조가 유효하고 레이저 헤드가 부착된 경우에만 에너지 경고를 확인합니다.
        if (this.structureValid && isLaserHeadAttached) {
            if (this.energyBuffer.getMaxEnergyStored() == 0) {
                // 에너지 버퍼나 발전기가 없어 최대 용량이 0이면
                this.invalidityReason = InvalidityReason.NO_ENERGY_SOURCE;
            }
        }

        if (!this.structureValid) {
            this.structureCache.clear();
            this.processingModuleChain.clear();
            this.activeSystemModules.clear();
            this.bulkProcessingModules.clear();
        }

        Set<BlockPos> currentStructure = new HashSet<>(this.structureCache);
        if (this.cachedHeadPos != null) {
            currentStructure.add(this.cachedHeadPos);
        }

        // 이전 구조에는 있었지만 현재 구조에는 없는 부품(연결 해제된 부품)을 찾습니다.
        Set<BlockPos> detachedParts = new HashSet<>(oldStructure);
        detachedParts.removeAll(currentStructure);

        // 연결 해제된 부품들의 상태를 강제로 초기화합니다.
        for (BlockPos detachedPos : detachedParts) {
            BlockEntity be = level.getBlockEntity(detachedPos);
            if (be instanceof AbstractDrillHeadBlockEntity headBE) {
                // 코어 연결을 끊고, 속도를 0으로 만듭니다.
                headBE.setCore(null);
            } else if (be instanceof GenericModuleBlockEntity moduleBE) {
                // 모듈의 시각적 연결과 속도를 모두 초기화합니다.
                moduleBE.updateVisualConnections(new HashSet<>());
                moduleBE.updateVisualSpeed(0);
            }
        }

        // 현재 구조에 포함된 모듈들의 연결 상태를 업데이트합니다.
        for (BlockPos modulePos : this.structureCache) {
            if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                moduleBE.updateVisualConnections(moduleConnections.getOrDefault(modulePos, new HashSet<>()));
            }
        }

        setChanged();
        sendData();
    }


    // 레드스톤 신호로 정지되었는지 확인하는 헬퍼 메서드
    private boolean isHaltedByRedstone() {
        if (level == null || redstoneBrakeModules.isEmpty()) {
            return false;
        }
        // 연결된 브레이크 모듈 중 하나라도 레드스톤 신호를 받으면 true 반환
        for (BlockPos modulePos : redstoneBrakeModules) {

            if (level.hasNeighborSignal(modulePos)) {
                return true;
            }
        }
        return false;
    }


    private StructureCheckResult searchStructureRecursive(BlockPos currentPos, Direction cameFrom, Set<BlockPos> visited, Set<BlockPos> functionalModules, List<BlockPos> otherCores, Map<BlockPos, Set<Direction>> moduleConnections, int depth) {
        if (depth > MAX_STRUCTURE_RANGE) return new StructureCheckResult(false, false);
        if (!visited.add(currentPos)) return new StructureCheckResult(true, false);

        assert level != null;
        BlockState currentState = level.getBlockState(currentPos);
        Block currentBlock = currentState.getBlock();

        if (currentBlock instanceof DrillCoreBlock) {
            otherCores.add(currentPos);
            return new StructureCheckResult(false, true);
        }

        if (!(currentBlock instanceof GenericModuleBlock)) {
            return new StructureCheckResult(false, false);
        }

        functionalModules.add(currentPos);

        for (Direction searchDir : Direction.values()) {
            if (searchDir == cameFrom) continue;

            BlockPos neighborPos = currentPos.relative(searchDir);
            if (!level.isLoaded(neighborPos)) continue;

            BlockState neighborState = level.getBlockState(neighborPos);
            Block neighborBlock = neighborState.getBlock();

            if (neighborBlock instanceof GenericModuleBlock || neighborBlock instanceof DrillCoreBlock) {
                if(neighborBlock instanceof DrillCoreBlock) {
                    Direction coreFacing = neighborState.getValue(DirectionalKineticBlock.FACING);
                    if(searchDir.getAxis() == coreFacing.getAxis()) continue;
                }

                // 연결 정보 기록
                moduleConnections.computeIfAbsent(currentPos, k -> new HashSet<>()).add(searchDir);
                moduleConnections.computeIfAbsent(neighborPos, k -> new HashSet<>()).add(searchDir.getOpposite());

                StructureCheckResult result = searchStructureRecursive(neighborPos, searchDir.getOpposite(), visited, functionalModules, otherCores, moduleConnections, depth + 1);
                if (result.loopDetected() || result.multipleCoresDetected()) {
                    return result;
                }
            }
        }
        return new StructureCheckResult(false, false);
    }

    private record StructureCheckResult(boolean loopDetected, boolean multipleCoresDetected) {}




    public boolean isModuleConnectedAt(Direction absoluteDirection) {
        // 코어와 '직접' 연결된 모듈이 있는지 확인하는 로직
        BlockPos adjacentPos = this.worldPosition.relative(absoluteDirection);
        return this.structureCache.contains(adjacentPos);
    }

    /**
     * Visual이 출력축 렌더링 여부를 결정하는 데 사용합니다.
     *
     * @return 헤드가 존재하고 캐시되었으면 true
     */
    public boolean hasHead() {
        return this.cachedHeadPos != null;
    }

    @Override
    public float getSpeed() {
        if (isOverStressed()) return 0;

        float baseSpeed = super.getTheoreticalSpeed();
        return (float) (baseSpeed * this.speedMultiplier * coreTier.getSpeedBonus());
    }

    public float getInputSpeed() {
        return super.getTheoreticalSpeed();
    }



    @Override
    public float calculateStressApplied() {
        if (!structureValid || invalidityReason == InvalidityReason.HEAD_MISSING) return 0;
        float finalStress = (float) (this.totalStressImpact * Math.max(0, this.stressMultiplier));
        return Math.max(0, finalStress);
    }

    @Override
    public float calculateAddedStressCapacity() {
        // 드릴 코어가 스트레스 용량을 제공하는 것으로 취급되지 않도록 명시적으로 0을 반환합니다.
        return 0;
    }


    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putBoolean("StructureValid", this.structureValid);

        if (this.speedMultiplier != 1.0)
            compound.putDouble("SpeedMultiplier", this.speedMultiplier);
        if (this.stressMultiplier != 1.0)
            compound.putDouble("StressMultiplier", this.stressMultiplier);
        if (this.heatMultiplier != 1.0)
            compound.putDouble("HeatMultiplier", this.heatMultiplier);

        compound.putString("CoreTier", coreTier.name());
        compound.putFloat("BaseStressImpact", this.totalStressImpact);
        compound.putInt("InvalidityReason", this.invalidityReason.ordinal());

        CompoundTag energyTag = new CompoundTag();
        energyTag.putInt("Amount", this.energyBuffer.getEnergyStored());
        energyTag.putInt("Capacity", this.energyBuffer.getMaxEnergyStored());
        compound.put("EnergyData", energyTag);
        ListTag chainTag = new ListTag();
        for (BlockPos pos : processingModuleChain) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("X", pos.getX());
            posTag.putInt("Y", pos.getY());
            posTag.putInt("Z", pos.getZ());
            chainTag.add(posTag);
        }
        compound.put("ProcessingChain", chainTag);
        compound.putFloat("Heat", this.heat);
        compound.putBoolean("Overheated", this.isOverheated);
        if (clientPacket) {
            compound.putFloat("VisualSpeed", this.visualSpeed);
            ListTag cacheList = new ListTag();
            for (BlockPos pos : structureCache) {
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("X", pos.getX());
                posTag.putInt("Y", pos.getY());
                posTag.putInt("Z", pos.getZ());
                cacheList.add(posTag);
            }
            compound.put("StructureCache", cacheList);

            if (!resonatorModules.isEmpty()) {
                ListTag resonatorList = new ListTag();
                for (BlockPos pos : resonatorModules) {
                    CompoundTag posTag = new CompoundTag();
                    posTag.putInt("X", pos.getX());
                    posTag.putInt("Y", pos.getY());
                    posTag.putInt("Z", pos.getZ());
                    resonatorList.add(posTag);
                }
                compound.put("ResonatorModules", resonatorList);
            }
        }

    }



    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        this.structureValid = compound.getBoolean("StructureValid");

        this.speedMultiplier = compound.contains("SpeedMultiplier") ? compound.getDouble("SpeedMultiplier") : 1.0;
        this.stressMultiplier = compound.contains("StressMultiplier") ? compound.getDouble("StressMultiplier") : 1.0;
        this.heatMultiplier = compound.contains("HeatMultiplier") ? compound.getDouble("HeatMultiplier") : 1.0;

        if (compound.contains("CoreTier")) {
            try {
                this.coreTier = Tier.valueOf(compound.getString("CoreTier"));
            } catch (IllegalArgumentException e) {
                this.coreTier = Tier.BRASS;
            }
        } else {
            this.coreTier = Tier.BRASS;
        }
        // BaseStressImpact를 읽어와 totalStressImpact에 할당합니다.
        this.totalStressImpact = compound.contains("BaseStressImpact") ? compound.getFloat("BaseStressImpact") : coreTier.getBaseStress();

        this.heat = compound.getFloat("Heat");
        this.isOverheated = compound.getBoolean("Overheated");

        int reasonOrdinal = compound.getInt("InvalidityReason");
        if (reasonOrdinal >= 0 && reasonOrdinal < InvalidityReason.values().length) {
            this.invalidityReason = InvalidityReason.values()[reasonOrdinal];
        } else {
            this.invalidityReason = InvalidityReason.NONE;
        }

        if (compound.contains("EnergyData", 10)) {
            CompoundTag energyTag = compound.getCompound("EnergyData");
            this.energyBuffer.setCapacity(energyTag.getInt("Capacity"));
            this.energyBuffer.setEnergy(energyTag.getInt("Amount"));
        }

        processingModuleChain.clear();
        if (compound.contains("ProcessingChain", 9)) {
            ListTag chainTag = compound.getList("ProcessingChain", 10);
            for (int i = 0; i < chainTag.size(); i++) {
                CompoundTag posTag = chainTag.getCompound(i);
                this.processingModuleChain.add(new BlockPos(
                        posTag.getInt("X"),
                        posTag.getInt("Y"),
                        posTag.getInt("Z")
                ));
            }
        }

        if (clientPacket) {
            // 클라이언트 전용 데이터 로드
            this.visualSpeed = compound.getFloat("VisualSpeed");

            Set<BlockPos> newCache = new HashSet<>();
            if (compound.contains("StructureCache", 9)) {
                ListTag cacheList = compound.getList("StructureCache", 10);
                for (int i = 0; i < cacheList.size(); i++) {
                    CompoundTag posTag = cacheList.getCompound(i);
                    newCache.add(new BlockPos(
                            posTag.getInt("X"),
                            posTag.getInt("Y"),
                            posTag.getInt("Z")
                    ));
                }
            }
            this.structureCache = newCache;


            // 동기화된 공명기 모듈 위치 리스트를 읽어옵니다.
            this.resonatorModules.clear();
            if (compound.contains("ResonatorModules", 9)) { // 9 = ListTag
                ListTag resonatorList = compound.getList("ResonatorModules", 10); // 10 = CompoundTag
                for (int i = 0; i < resonatorList.size(); i++) {
                    CompoundTag posTag = resonatorList.getCompound(i);
                    this.resonatorModules.add(new BlockPos(
                            posTag.getInt("X"),
                            posTag.getInt("Y"),
                            posTag.getInt("Z")
                    ));
                }
            }
        } else {
            this.structureCheckCooldown = STRUCTURE_CHECK_COOLDOWN_TICKS;
        }
    }

    @Override
    public void tick() {
        super.tick();

        assert level != null;
        if (level.isClientSide()) {
            // --- 클라이언트 전용 시각/청각 효과 ---
            clientTick();
        } else {
            // --- 서버 전용 계산 및 동기화 로직 ---
            serverTick();
        }
    }
    // 클라이언트 로직을 위한 메서드
    public void clientTick() {
        if (level == null) return;

        // isOverheated 상태에서는 효과 없음
        if (isOverheated) return;

        float heatPercent = this.heat / 100f;
        if (heatPercent <= 0.4f) return;

        RandomSource random = level.random;

        Vec3 center = Vec3.atCenterOf(worldPosition);

        // 과부하 구간 (90% ~ 100%)
        if (heatPercent > 0.9f) {
            if (random.nextFloat() < heatPercent * 0.5f) {
                level.addParticle(ParticleTypes.SMOKE, center.x, center.y, center.z,
                        (random.nextFloat() - 0.5f) * 0.2f,
                        (random.nextFloat() - 0.5f) * 0.2f,
                        (random.nextFloat() - 0.5f) * 0.2f);
            }
            if (random.nextInt(20) == 0) {
                level.playLocalSound(worldPosition, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.5f, 1.0f, false);
            }
        }
        // 최적 부스트 구간 (40% ~ 90%)
        else {
            if (random.nextFloat() < heatPercent * 0.2f) {
                level.addParticle(ParticleTypes.WHITE_SMOKE, center.x, center.y, center.z,
                        (random.nextFloat() - 0.5f) * 0.1f,
                        (random.nextFloat() - 0.5f) * 0.1f,
                        (random.nextFloat() - 0.5f) * 0.1f);
            }
            if (random.nextInt(40) == 0) {
                level.playLocalSound(worldPosition, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, 0.3f, 1.5f + heatPercent, false);
            }
        }
    }

    public void serverTick() {
        tickCounter++;
        assert level != null;
        //  쿨다운 기반의 구조 검사 실행
        if (structureCheckCooldown > 0) {
            structureCheckCooldown--;
            if (structureCheckCooldown == 0) {
                scanAndValidateStructure();
            }
        }
        this.polarityBonusActive = false; // 매 틱 초기화
        if (hasHead()) {
            Direction headFacing = getBlockState().getValue(DirectionalKineticBlock.FACING).getOpposite();
            BlockPos nodePos = cachedHeadPos.relative(headFacing);
            if (level != null && level.getBlockEntity(nodePos) instanceof OreNodeBlockEntity nodeBE) {
                if (nodeBE.isPolarityBonusActive()) {
                    this.polarityBonusActive = true;
                }
            }
        }
        assert level != null;

        for (BlockPos modulePos : activeSystemModules) {
            if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity module) {
                module.onCoreTick(this);
            }
        }
        float finalSpeed = getFinalSpeed();

        IDrillHead headBlock = null;
        if (hasHead() && level.getBlockState(cachedHeadPos).getBlock() instanceof IDrillHead h) {
            headBlock = h;
        }
        // 레이저 헤드 작동 시 에너지 부족 경고 처리
        if (hasHead() && level.getBlockState(cachedHeadPos).getBlock() instanceof LaserDrillHeadBlock) {
            // 레이저 헤드가 있고, 에너지 공급원이 있는 상태에서(NO_ENERGY_SOURCE가 아님)
            // 현재 에너지가 부족한 경우 경고 상태를 설정합니다.

            if (this.invalidityReason != InvalidityReason.NO_ENERGY_SOURCE && this.energyBuffer.getEnergyStored() < AdsDrillConfigs.SERVER.laserEnergyPerMiningTick.get()) {
                if (getFinalSpeed() != 0) { // 드릴이 작동 중일 때만 에너지 부족 경고 표시
                    this.invalidityReason = InvalidityReason.INSUFFICIENT_ENERGY;
                }
            } else if (this.invalidityReason == InvalidityReason.INSUFFICIENT_ENERGY) {
                // 에너지가 충분해지거나 드릴이 멈추면 경고를 해제합니다.
                this.invalidityReason = InvalidityReason.NONE;
            }
        } else if (this.invalidityReason == InvalidityReason.INSUFFICIENT_ENERGY || this.invalidityReason == InvalidityReason.NO_ENERGY_SOURCE) {
            // 레이저 헤드가 제거되면 에너지 관련 경고도 함께 해제합니다.
            this.invalidityReason = InvalidityReason.NONE;
        }
        // --- 과열 로직 ---
        // finalSpeed의 절댓값을 사용하거나, 0이 아닌지 확인합니다.
        if (finalSpeed != 0 && headBlock != null) {
            float baseHeatGen = headBlock.getHeatGeneration();
            float speedFactor = Math.max(1, Math.abs(finalSpeed) / 64f);

            // 곱셈으로 누적된 열 생성 배율을 적용합니다.
            // heatMultiplier는 음수 보너스가 적용되어 1.0보다 작은 값이 됩니다. (예: 0.8)
            float finalHeatMultiplier = (float) Math.max(0, this.heatMultiplier);
            float heatThisTick = baseHeatGen * speedFactor * finalHeatMultiplier;

            addHeat(heatThisTick);

        } else {
            // 냉각: 드릴이 멈춰있을 때 열이 식는 로직
            float coolingRate = coreTier.getBaseCooling();
            if (headBlock != null) {
                coolingRate += headBlock.getCoolingRate();
            }
            this.heat -= coolingRate;
        }

        // (열 수치 제한 및 과열 상태 갱신 로직은 이전과 동일)
        this.heat = Mth.clamp(this.heat, 0, 100);
        // 이전 틱의 과열 상태를 기억하기 위한 필드
        // --- isOverheated 상태 업데이트 ---
        boolean wasOverheated = isOverheated;
        if (isOverheated && this.heat <= COOLDOWN_RESET_THRESHOLD) {
            isOverheated = false;
        } else if (!isOverheated && this.heat >= 100.0f) {
            isOverheated = true;
        }

        // --- 과열 이벤트 처리 ---
        if (!wasOverheated && isOverheated) {
            boolean eventHandledByHead = false;
            if (headBlock != null) {
                eventHandledByHead = headBlock.onOverheat(level, cachedHeadPos, this);
            }

            Direction headFacing = getBlockState().getValue(DirectionalKineticBlock.FACING).getOpposite();
            BlockPos nodePos = cachedHeadPos.relative(headFacing);
            if (level.getBlockEntity(nodePos) instanceof ArtificialNodeBlockEntity artificialNode) {
                Quirk.QuirkContext context = new Quirk.QuirkContext((ServerLevel) level, nodePos, artificialNode, this);
                for (Quirk quirk : artificialNode.getQuirks()) {
                    quirk.onDrillCoreOverheat(context);
                }
            }

            if (!eventHandledByHead) {
                level.playSound(null, worldPosition, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0f, 0.8f);
            }
        }
        // --- 동기화 로직 ---
        boolean needsSync = false;
        if (this.visualSpeed != finalSpeed) {
            this.visualSpeed = finalSpeed;
            needsSync = true;
        }
        if (tickCounter % GOGGLE_UPDATE_DEBOUNCE == 0) {
            needsSync = true;
        }
        if (needsSync) {
            setChanged();
            sendData();
        }

        // --- 모듈 및 헤드 업데이트 ---
        for (BlockPos modulePos : this.structureCache) {
            if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                moduleBE.updateVisualSpeed(finalSpeed);
            }
        }


        // --- 채굴 로직 호출 ---
        if (hasHead() && headBlock != null) {
            // 헤드 BE의 종류에 따라 올바른 처리를 하도록 변경
            if (level.getBlockEntity(cachedHeadPos) instanceof RotaryDrillHeadBlockEntity headBE) {
                headBE.updateVisualSpeed(-finalSpeed); // Rotary는 반대로 회전
                if (needsSync) {
                    headBE.updateClientHeat(this.heat);
                }
            }
            // 펌프 헤드에 대한 속도 전달 로직
            else if (level.getBlockEntity(cachedHeadPos) instanceof PumpHeadBlockEntity headBE) {
                headBE.updateVisualSpeed(finalSpeed); // 펌프는 정방향 회전
                // 펌프는 열 시각화가 없으므로 updateClientHeat는 호출하지 않음

            } else if (level.getBlockEntity(cachedHeadPos) instanceof LaserDrillHeadBlockEntity headBE) {
                headBE.updateVisualSpeed(finalSpeed); // 레이저는 정방향 회전
            }
            else if (level.getBlockEntity(cachedHeadPos) instanceof HydraulicDrillHeadBlockEntity headBE) {
                headBE.updateVisualSpeed(-finalSpeed);
            }
            if (finalSpeed != 0) {
                headBlock.onDrillTick(level, cachedHeadPos, level.getBlockState(cachedHeadPos), this);
            }
        }
    }

    /**
     * 채굴된 아이템을 받아 처리 체인을 시작하는 메서드.
     * 이 메서드는 IDrillHead 구현체 내부에서 호출됩니다.
     */

    public void processMinedItem(ItemStack minedItem) {
        if (level == null || level.isClientSide() || minedItem.isEmpty()) return;

        // 처리될 아이템들을 담을 리스트. 처음엔 채굴된 아이템 하나만 담겨 있음.
        List<ItemStack> stacksToProcess = new ArrayList<>();
        stacksToProcess.add(minedItem);

        // 처리 체인을 순회하며 각 모듈의 역할을 수행
        for (BlockPos modulePos : processingModuleChain) {
            if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {

                // 현재 처리할 아이템 묶음(stacksToProcess)을 다음 단계로 넘겨줄 아이템 묶음(nextStacks)으로 변환
                List<ItemStack> nextStacks = new ArrayList<>();

                // 현재 단계에서 처리할 모든 아이템 스택에 대해 반복
                for (ItemStack currentStack : stacksToProcess) {
                    if (currentStack.isEmpty()) continue;

                    // 모듈의 타입에 따라 다른 처리를 수행
                    ModuleType type = moduleBE.getModuleType();

                    // 1. 이 모듈이 "필터 모듈"이라면, 필터링을 수행
                    if (type == ModuleType.FILTER) {
                        // processItem은 필터를 통과한 아이템만 리스트로 반환 (통과 못하면 빈 리스트)
                        nextStacks.addAll(moduleBE.processItem(currentStack, this));
                    }
                    // 2. 이 모듈이 "일반 처리 모듈"이라면, 아이템 가공을 수행
                    else if (type.getBehavior().getRecipeType() != null) {
                        // processItem은 가공된 결과물(들)을 리스트로 반환
                        nextStacks.addAll(moduleBE.processItem(currentStack, this));
                    }
                    // 3. 그 외의 모듈(우선순위가 없는 모듈)은 아무것도 하지 않고 아이템을 그대로 통과시킴
                    else {
                        nextStacks.add(currentStack);
                    }
                }

                // 현재 단계의 처리가 끝나면, 결과물(nextStacks)이 다음 단계의 입력(stacksToProcess)이 됨
                stacksToProcess = nextStacks;
            }
        }

        // --- 모든 처리 체인이 끝난 후 ---

        // 최종적으로 살아남은 아이템들을 내부 버퍼에 삽입
        for (ItemStack finalStack : stacksToProcess) {
            ItemStack remainder = ItemHandlerHelper.insertItem(getInternalItemBuffer(), finalStack, false);
            if (!remainder.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(level, getBlockPos().getX() + 0.5, getBlockPos().getY() + 1.5, getBlockPos().getZ() + 0.5, remainder);
                level.addFreshEntity(itemEntity);
            }
        }

        // 벌크 처리(압축 등)는 모든 아이템이 버퍼에 들어간 후에 실행 (기존과 동일)
        boolean successfulBulkProcess;
        do {
            successfulBulkProcess = false;
            for (BlockPos modulePos : bulkProcessingModules) {
                if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity bulkProcessor) {
                    if (bulkProcessor.processBulk(this)) {
                        successfulBulkProcess = true;
                    }
                }
            }
        } while (successfulBulkProcess);
    }
    public int getTickCounter() {
        return this.tickCounter;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // Create의 기본 운동 정보(Stress 등)를 먼저 표시
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        // 1. 코어 티어 및 구조 상태 정보 추가
        addTierAndStatusTooltips(tooltip);

        // 2. 과열 및 레드스톤 정지 경고 추가
        addWarningTooltips(tooltip);
        //2.5. 채굴력 정보 툴팁
        addMiningPowerTooltip(tooltip);

        // 3. 플레이어의 입력(isPlayerSneaking)에 따라 요약 또는 상세 정보 추가
        if (isPlayerSneaking) {
            addDetailedInfoTooltips(tooltip);
        } else {
            addSummaryInfoTooltips(tooltip);
        }

        // 4. 레이저 헤드 전용 정보 추가
        addLaserHeadTooltips(tooltip);

        // 5. 저장 공간 정보 추가 (항상 표시)
        addStorageTooltips(tooltip, isPlayerSneaking);

        return true;
    }

    private void addTierAndStatusTooltips(List<Component> tooltip) {
        // --- 코어 티어 ---
        tooltip.add(Component.literal(""));
        MutableComponent tierComponent = Component.translatable("goggle.adsdrill.drill_core.tier." + coreTier.name().toLowerCase())
                .withStyle(coreTier == Tier.NETHERITE ? ChatFormatting.DARK_PURPLE : (coreTier == Tier.STEEL ? ChatFormatting.AQUA : ChatFormatting.GOLD));
        tooltip.add(Component.literal("").append(tierComponent));

        // --- 드릴 조립 헤더 ---
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("goggle.adsdrill.drill_core.header").withStyle(ChatFormatting.GRAY));

        // --- 구조 유효성 상태 ---
        MutableComponent status;
        if (structureValid && invalidityReason != InvalidityReason.HEAD_MISSING && invalidityReason != InvalidityReason.NO_ENERGY_SOURCE && invalidityReason != InvalidityReason.INSUFFICIENT_ENERGY) {
            status = Component.translatable("goggle.adsdrill.drill_core.valid", structureCache.size(), coreTier.getMaxModules())
                    .withStyle(ChatFormatting.GREEN);
        } else {
            status = Component.translatable("goggle.adsdrill.drill_core.invalid")
                    .withStyle(ChatFormatting.RED);
        }
        tooltip.add(Component.literal(" ").append(status));

        // --- 오류 원인 ---
        if (!structureValid || invalidityReason != InvalidityReason.NONE) {
            String reasonKey = switch (invalidityReason) {
                case LOOP_DETECTED -> "goggle.adsdrill.drill_core.reason.loop_detected";
                case MULTIPLE_CORES -> "goggle.adsdrill.drill_core.reason.multiple_cores";
                case TOO_MANY_MODULES -> "goggle.adsdrill.drill_core.reason.too_many_modules";
                case HEAD_MISSING -> "goggle.adsdrill.drill_core.reason.head_missing";
                case DUPLICATE_PROCESSING_MODULE -> "goggle.adsdrill.drill_core.reason.duplicate_processing_module";
                case NO_ENERGY_SOURCE -> "goggle.adsdrill.drill_core.reason.no_energy_source";
                case INSUFFICIENT_ENERGY -> "goggle.adsdrill.drill_core.reason.insufficient_energy";
                default -> null;
            };
            if (reasonKey != null) {
                MutableComponent reason = (invalidityReason == InvalidityReason.TOO_MANY_MODULES)
                        ? Component.translatable(reasonKey, coreTier.getMaxModules())
                        : Component.translatable(reasonKey);

                ChatFormatting reasonColor = switch (invalidityReason) {
                    case HEAD_MISSING, NO_ENERGY_SOURCE, INSUFFICIENT_ENERGY -> ChatFormatting.YELLOW;
                    default -> ChatFormatting.DARK_RED;
                };
                tooltip.add(Component.literal(" ").append(reason.withStyle(reasonColor)));
            }
        }
    }


    private void addWarningTooltips(List<Component> tooltip) {
        // --- 레드스톤 정지 경고 ---
        if (isHaltedByRedstone()) {
            tooltip.add(Component.literal(" ")
                    .append(Component.translatable("goggle.adsdrill.drill_core.halted_by_redstone")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
        }
        // --- 과열 경고 ---
        if (isOverheated) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal(" ")
                    .append(Component.translatable("goggle.adsdrill.drill_core.overheated")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withBold(true).withUnderlined(true))));
            tooltip.add(Component.literal(" ")
                    .append(Component.translatable("goggle.adsdrill.drill_core.cooling_down", String.format("%.0f%%", COOLDOWN_RESET_THRESHOLD))
                            .withStyle(ChatFormatting.DARK_GRAY)));
        }
    }

    // 채굴력 정보를 표시하는 헬퍼 메서드
    private void addMiningPowerTooltip(List<Component> tooltip) {
        if (!structureValid || !hasHead()) return;

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Mining Power:").withStyle(ChatFormatting.GRAY));

        Direction headFacing = getBlockState().getValue(DirectionalKineticBlock.FACING).getOpposite();
        BlockPos nodePos = cachedHeadPos.relative(headFacing);

        if (level != null && level.getBlockEntity(nodePos) instanceof OreNodeBlockEntity nodeBE) {
            float finalSpeed = getFinalSpeed(); // 양극성 보너스가 적용된 최종 속도
            double basePower = Math.abs(finalSpeed) / AdsDrillConfigs.SERVER.rotarySpeedDivisor.get();
            double effectivePower = basePower / nodeBE.getHardness();

            // 툴팁 라인 생성
            MutableComponent line = Component.literal(" ")
                    .append(Component.literal(String.format("%.2f", effectivePower)).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" / 1000 per Cycle").withStyle(ChatFormatting.DARK_GRAY));

            // 양극성 보너스가 활성화되었을 때만 추가 텍스트 표시
            if (this.polarityBonusActive) {
                line.append(Component.literal(" (Polarity Active!)").withStyle(ChatFormatting.GOLD));
            }

            tooltip.add(line);
        } else {
            tooltip.add(Component.literal(" ").append(Component.translatable("goggle.adsdrill.drill_core.reason.head_missing").withStyle(ChatFormatting.YELLOW)));
        }
    }

    private void addSummaryInfoTooltips(List<Component> tooltip) {
        if (!structureValid) return;

        // --- 과열 상태가 아닐 때의 온도 요약 ---
        if (!isOverheated) {
            tooltip.add(Component.literal(""));
            MutableComponent heatSummary = Component.literal(" ")
                    .append(Component.translatable("goggle.adsdrill.drill_core.heat_label").withStyle(ChatFormatting.GRAY));

            float currentHeat = getHeat();
            MutableComponent heatValue = Component.literal(String.format("%.1f%%", currentHeat));

            if (currentHeat > OVERLOAD_START_THRESHOLD) {
                heatValue.withStyle(ChatFormatting.RED);
                heatSummary.append(heatValue).append(Component.literal(" (Overloading)").withStyle(ChatFormatting.RED));
            } else if (currentHeat > BOOST_START_THRESHOLD) {
                heatValue.withStyle(ChatFormatting.GOLD);
                heatSummary.append(heatValue).append(Component.literal(" (Optimal Boost)").withStyle(ChatFormatting.GOLD));
            } else {
                heatValue.withStyle(ChatFormatting.DARK_GRAY);
                heatSummary.append(heatValue);
            }
            tooltip.add(heatSummary);
        }

        // --- 처리 모듈 개수 요약 ---
        if (!processingModuleChain.isEmpty()) {
            tooltip.add(Component.literal(" ")
                    .append(Component.literal(String.valueOf(processingModuleChain.size())).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" Processing Modules active").withStyle(ChatFormatting.DARK_GRAY))
            );
        }

        // --- 상세 정보 안내 ---
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal(" ").append(Component.translatable("goggle.adsdrill.sneak_for_details").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
    }

    private void addDetailedInfoTooltips(List<Component> tooltip) {
        // --- 성능 보너스 상세 ---
        addPerformanceBonusesTooltip(tooltip);

        // --- 과열 시스템 상세 ---
        addHeatAndEfficiencyTooltip(tooltip);

        // --- 처리 순서 ---
        addProcessingOrderTooltip(tooltip);
    }

    private void addPerformanceBonusesTooltip(List<Component> tooltip) {
        if (!structureValid || (speedMultiplier == 1.0 && stressMultiplier == 1.0 && heatMultiplier == 1.0)) {
            return;
        }

        tooltip.add(Component.literal(""));
        if (speedMultiplier != 1.0) {
            String sign = speedMultiplier > 1.0 ? "+" : "";
            ChatFormatting color = speedMultiplier > 1.0 ? ChatFormatting.AQUA : ChatFormatting.RED;
            tooltip.add(Component.literal(" ")
                    .append(Component.translatable("goggle.adsdrill.drill_core.speed_bonus").withStyle(ChatFormatting.GRAY))
                    .append(": ")
                    .append(Component.literal(String.format("%s%.1f%%", sign, (speedMultiplier - 1.0) * 100))
                            .withStyle(style -> style.withColor(color).withBold(true))));
        }
        if (stressMultiplier != 1.0) {
            String sign = stressMultiplier > 1.0 ? "+" : "";
            ChatFormatting color = stressMultiplier > 1.0 ? ChatFormatting.GOLD : ChatFormatting.GREEN;
            tooltip.add(Component.literal(" ")
                    .append(Component.translatable("goggle.adsdrill.drill_core.stress_impact").withStyle(ChatFormatting.GRAY))
                    .append(": ")
                    .append(Component.literal(String.format("%s%.1f%%", sign, (stressMultiplier - 1.0) * 100))
                            .withStyle(style -> style.withColor(color).withBold(true))));
        }
        if (heatMultiplier != 1.0) {
            double heatReduction = (1.0 - heatMultiplier) * 100;
            tooltip.add(Component.literal(" ")
                    .append(Component.translatable("goggle.adsdrill.drill_core.heat_reduction").withStyle(ChatFormatting.GRAY))
                    .append(": ")
                    .append(Component.literal(String.format("%.1f%%", heatReduction))
                            .withStyle(style -> style.withColor(ChatFormatting.BLUE).withBold(true))));
        }
    }

    private void addHeatAndEfficiencyTooltip(List<Component> tooltip) {
        tooltip.add(Component.literal(""));

        float currentHeat = this.heat;
        float currentEfficiency = getHeatEfficiency();
        float finalSpeed = getFinalSpeed();

        // 1. 온도 표시 라인
        MutableComponent heatLine = Component.literal(" ")
                .append(Component.translatable("goggle.adsdrill.drill_core.heat_label"));
        MutableComponent heatValue = Component.literal(String.format("%.1f%%", currentHeat));
        if (currentHeat > OVERLOAD_START_THRESHOLD) heatValue.withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        else if (currentHeat > BOOST_START_THRESHOLD) heatValue.withStyle(ChatFormatting.GOLD);
        else heatValue.withStyle(ChatFormatting.GRAY);
        tooltip.add(heatLine.append(heatValue));

        // 2. 효율 표시 라인
        MutableComponent efficiencyLine = Component.literal(" ")
                .append(Component.translatable("goggle.adsdrill.drill_core.efficiency_label"));
        MutableComponent efficiencyValue = Component.literal(String.format("%.0f%%", currentEfficiency * 100));
        if (currentEfficiency > 1.0f) {
            efficiencyValue.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            efficiencyLine.append(efficiencyValue).append(Component.literal(" (Optimal Boost)").withStyle(ChatFormatting.DARK_AQUA));
        } else if (currentHeat > OVERLOAD_START_THRESHOLD) {
            efficiencyValue.withStyle(ChatFormatting.DARK_RED);
            efficiencyLine.append(efficiencyValue).append(Component.literal(" (Overloading)").withStyle(ChatFormatting.RED));
        } else {
            efficiencyValue.withStyle(ChatFormatting.GRAY);
            efficiencyLine.append(efficiencyValue);
        }
        tooltip.add(efficiencyLine);

        // 3. 유효 속도 표시 라인
        MutableComponent speedLine = Component.literal(" ")
                .append(Component.translatable("goggle.adsdrill.drill_core.effective_speed_label"));
        MutableComponent speedValue = Component.literal(String.format("%.1f RPM", Math.abs(finalSpeed)));
        if (currentEfficiency > 1.0f) speedValue.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
        else if (currentHeat > OVERLOAD_START_THRESHOLD) speedValue.withStyle(ChatFormatting.DARK_RED);
        else speedValue.withStyle(ChatFormatting.GRAY);
        tooltip.add(speedLine.append(speedValue));
    }

    private void addProcessingOrderTooltip(List<Component> tooltip) {
        if (processingModuleChain.isEmpty()) return;

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Processing Order:").withStyle(ChatFormatting.GRAY));

        for (BlockPos modulePos : processingModuleChain) {
            if (level != null && level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity gme) {
                Block moduleBlock = gme.getBlockState().getBlock();
                Component moduleName = moduleBlock.getName().withStyle(ChatFormatting.AQUA);
                int priority = gme.getProcessingPriority();

                MutableComponent line = Component.literal(" > " + priority + ". ")
                        .withStyle(ChatFormatting.DARK_GRAY)
                        .append(moduleName);
                tooltip.add(line);
            }
        }
    }

    private void addLaserHeadTooltips(List<Component> tooltip) {
        if (!hasHead() || level == null || !(level.getBlockEntity(cachedHeadPos) instanceof LaserDrillHeadBlockEntity laserBE)) {
            return;
        }

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal(" ")
                .append(laserBE.getMode().getDisplayName().copy().withStyle(ChatFormatting.RED)));

        int energyCost;
        if (laserBE.getMode() == LaserDrillHeadBlockEntity.OperatingMode.DECOMPOSITION) {
            energyCost = 500;
        } else {
            energyCost = 100 * laserBE.activeTargets.size();
        }
        tooltip.add(Component.literal(" ")
                .append(Component.translatable("goggle.adsdrill.drill_core.energy_cost", energyCost)
                        .withStyle(ChatFormatting.GRAY)));
    }

    private void addStorageTooltips(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean hasItemBuffers = false;
        boolean hasFluidBuffers = false;
        boolean hasEnergyBuffers = energyBuffer.getMaxEnergyStored() > 0;
        int totalItemSlots = 0;
        int nonEmptyItemSlots = 0;
        int totalItems = 0;
        int totalFluidCapacity = 0;
        int totalFluidAmount = 0;
        Map<Fluid, Integer> fluidComposition = new HashMap<>();

        if (level != null) {
            for (BlockPos modulePos : this.structureCache) {
                if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                    ModuleType type = moduleBE.getModuleType();

                    if (type == ModuleType.ITEM_BUFFER && moduleBE.getItemHandler() != null) {
                        hasItemBuffers = true;
                        IItemHandler handler = moduleBE.getItemHandler();
                        totalItemSlots += handler.getSlots();
                        for (int i = 0; i < handler.getSlots(); i++) {
                            ItemStack stackInSlot = handler.getStackInSlot(i);
                            if (!stackInSlot.isEmpty()) {
                                nonEmptyItemSlots++;
                                totalItems += stackInSlot.getCount();
                            }
                        }
                    }

                    if (type == ModuleType.FLUID_BUFFER && moduleBE.getFluidHandler() != null) {
                        hasFluidBuffers = true;
                        IFluidHandler handler = moduleBE.getFluidHandler();
                        for (int i = 0; i < handler.getTanks(); i++) {
                            totalFluidCapacity += handler.getTankCapacity(i);
                            FluidStack fluidInTank = handler.getFluidInTank(i);
                            if (!fluidInTank.isEmpty()) {
                                totalFluidAmount += fluidInTank.getAmount();
                                fluidComposition.merge(fluidInTank.getFluid(), fluidInTank.getAmount(), Integer::sum);
                            }
                        }
                    }
                }
            }
        }

        if (!hasItemBuffers && !hasFluidBuffers && !hasEnergyBuffers) return;

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal(" ").append(Component.translatable("goggle.adsdrill.drill_core.storage_header").withStyle(ChatFormatting.GRAY)));

        if (hasItemBuffers) {
            MutableComponent itemLine = Component.literal(" ")
                    .append(Component.translatable("goggle.adsdrill.drill_core.storage.items").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.format(": %,d / %,d", totalItems, totalItemSlots * 64)));
            if (isPlayerSneaking && nonEmptyItemSlots > 0) {
                itemLine.append(Component.literal(String.format(" (%d Slots)", nonEmptyItemSlots)).withStyle(ChatFormatting.DARK_GRAY));
            }
            tooltip.add(itemLine);
        }

        if (hasFluidBuffers) {
            if (isPlayerSneaking) {
                MutableComponent fluidHeader = Component.literal(" ")
                        .append(Component.translatable("goggle.adsdrill.drill_core.storage.fluid").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.format(": %,d / %,d mb", totalFluidAmount, totalFluidCapacity)));
                tooltip.add(fluidHeader);

                if (fluidComposition.isEmpty()) {
                    tooltip.add(Component.literal("   ").append(Component.translatable("goggle.adsdrill.drill_core.storage.empty").withStyle(ChatFormatting.DARK_GRAY)));
                } else {
                    fluidComposition.entrySet().stream()
                            .sorted(Map.Entry.<Fluid, Integer>comparingByValue().reversed())
                            .forEach(entry -> {
                                MutableComponent fluidLine = Component.literal("  - ")
                                        .append(Component.literal(String.format("%,d mb ", entry.getValue())).withStyle(ChatFormatting.GOLD))
                                        .append(entry.getKey().getFluidType().getDescription().copy().withStyle(ChatFormatting.GRAY));
                                tooltip.add(fluidLine);
                            });
                }
            } else {
                MutableComponent fluidLine = Component.literal(" ")
                        .append(Component.translatable("goggle.adsdrill.drill_core.storage.fluid").withStyle(ChatFormatting.GRAY))
                        .append(": ");

                if (totalFluidAmount == 0) {
                    fluidLine.append(Component.translatable("goggle.adsdrill.drill_core.storage.empty").withStyle(ChatFormatting.DARK_GRAY));
                } else {
                    fluidLine.append(Component.literal(String.format("%,d / %,d mb", totalFluidAmount, totalFluidCapacity))
                            .withStyle(ChatFormatting.GOLD));
                    if (fluidComposition.size() > 1) {
                        fluidLine.append(Component.literal(String.format(" (%d Types)", fluidComposition.size())).withStyle(ChatFormatting.DARK_GRAY));
                    }
                }
                tooltip.add(fluidLine);
            }
        }

        if (hasEnergyBuffers) {
            MutableComponent energyLine = Component.literal(" ")
                    .append(Component.translatable("goggle.adsdrill.drill_core.storage.energy").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.format(": %,d / %,d FE", energyBuffer.getEnergyStored(), energyBuffer.getMaxEnergyStored())));
            tooltip.add(energyLine);
        }
    }
}
