package com.yourname.mycreateaddon.content.kinetics.module;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.base.DrillEnergyStorage;
import com.yourname.mycreateaddon.content.kinetics.base.IResourceAccessor;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.neoforged.neoforge.fluids.FluidStack; // 추가
import net.minecraft.core.particles.ParticleTypes; // 추가
import net.minecraft.sounds.SoundEvents; // 추가
import net.minecraft.sounds.SoundSource; // 추가
import net.minecraft.world.level.Level; // 추가
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;


public class GenericModuleBlockEntity extends KineticBlockEntity implements IProcessingModule,IActiveSystemModule, IBulkProcessingModule, IHaveGoggleInformation {


    // 렌더링 관련 필드
    private Set<Direction> visualConnections = new HashSet<>();
    private float visualSpeed = 0f;
    private Set<Direction> energyConnections = new HashSet<>(); // [신규] 에너지 연결 방향 저장
    // --- [신규] 독립 저장소 필드 ---
    protected @Nullable ItemStackHandler itemHandler;
    protected @Nullable FluidTank fluidHandler;
    protected @Nullable DrillEnergyStorage energyInputBuffer; // [핵심 수정] 타입 변경

    private Item resonatorFilter = null; // [신규] 공명 필터 아이템 저장
    // [신규] 냉각 로직 관련 상수
    private static final float COOLANT_ACTIVATION_HEAT = 5.0f; // 5도 이상일 때 작동
    private static final int WATER_CONSUMPTION_PER_TICK = 5; // 틱당 물 5mb 소모 (초당 100mb)
    private static final float HEAT_REDUCTION_PER_TICK = 0.4f; // 틱당 열 0.4 감소
    // [추가] 우선순위 필드. 기본값은 99 (가장 낮음)
    private int processingPriority = 99;
    private static final int MAX_PRIORITY = 10;

    public int getProcessingPriority() {
        // 필터 모듈은 최우선 순위(0)를 가짐
        return processingPriority;
    }
    // [신규] 필터 슬롯에 쉽게 접근하기 위한 헬퍼 메서드
    public ItemStack getFilter() {
        if (getModuleType() == ModuleType.FILTER && itemHandler != null) {
            return itemHandler.getStackInSlot(0);
        }
        return ItemStack.EMPTY;
    }

    public void setFilter(ItemStack stack) {
        if (getModuleType() == ModuleType.FILTER && itemHandler != null) {
            itemHandler.setStackInSlot(0, stack);
            setChanged(); // 상태 변경 알림
        }
    }

    // [신규] 공명 필터 설정 메서드
    public void setResonatorFilter(ItemStack stack, Player player) {
        if (stack.isEmpty()) {
            this.resonatorFilter = null;
            player.displayClientMessage(Component.translatable("mycreateaddon.resonator.cleared"), true);
        } else {
            this.resonatorFilter = stack.getItem();
            player.displayClientMessage(Component.translatable("mycreateaddon.resonator.set", this.resonatorFilter.getDescription()), true);
        }
        setChanged();
        sendData();
    }

    // [신규] 코어가 사용할 getter
    public Optional<Item> getResonatorFilterItem() {
        return Optional.ofNullable(this.resonatorFilter);
    }

    @Override
    public void initialize() {
        super.initialize();
        // 월드에 처음 로드될 때 주변을 확인
        updateEnergyConnections();
    }

    /**
     * [신규] 주변 블록을 스캔하여 에너지 연결이 가능한 방향을 업데이트합니다.
     */
    public void updateEnergyConnections() {
        if (level == null || level.isClientSide() || getModuleType() != ModuleType.ENERGY_INPUT) {
            return;
        }

        Set<Direction> newConnections = new HashSet<>();
        for (Direction dir : Direction.values()) {
            // 이웃 블록이 해당 방향에서 에너지 Capability를 제공하는지 확인
            IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (energy != null && energy.canReceive()) {
                newConnections.add(dir);
            }
        }

        if (!this.energyConnections.equals(newConnections)) {
            this.energyConnections = newConnections;
            setChanged();
            sendData(); // 클라이언트에 변경사항 전송
        }
    }

    // [신규] Visual이 사용할 getter
    public Set<Direction> getEnergyConnections() {
        return energyConnections;
    }
    // [신규] addToGoggleTooltip 메서드 추가
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // 이 BE의 블록이 GenericModuleBlock인지 확인
        if (!(getBlockState().getBlock() instanceof GenericModuleBlock block)) {
            return false;
        }

        ModuleType type = block.getModuleType();
        boolean hasPriority = (type.getRecipeTypeSupplier() != null || type == ModuleType.FILTER);

        // 우선순위 기능이 있는 모듈일 경우에만 툴팁을 추가
        if (hasPriority) {
            // Create의 기본 운동 정보(Stress 등)를 표시하지 않으려면 super 호출을 제거하거나 주석 처리할 수 있습니다.
            // 여기서는 유지하여 운동 정보도 함께 보이도록 합니다.
            // super.addToGoggleTooltip(tooltip, isPlayerSneaking); // KineticBlockEntity에는 이 메서드가 없으므로 호출 불가

            // "Processing Priority" 헤더 추가
            tooltip.add(Component.literal("")); // 줄바꿈
            tooltip.add(Component.translatable("goggle.mycreateaddon.module.processing_priority").withStyle(ChatFormatting.GRAY));

            // 현재 설정된 우선순위 값을 표시
            tooltip.add(Component.literal(" " + this.processingPriority).withStyle(ChatFormatting.AQUA));

            return true; // 툴팁이 추가되었음을 알림
        }

        return false; // 툴팁을 추가하지 않았음을 알림
    }

    // [추가] 우선순위를 순환시키는 메서드
    public void cyclePriority(Player player) {
        processingPriority++;
        if (processingPriority > MAX_PRIORITY) {
            processingPriority = 1;
        }

        // 기본값(99)에서 처음 변경될 때도 1로 설정
        if (processingPriority == 100) {
            processingPriority = 1;
        }

        // 플레이어에게 변경된 우선순위를 알림
        player.displayClientMessage(Component.translatable("mycreateaddon.priority_changed", processingPriority), true); // true: 액션바에 표시

        setChanged();
        sendData();

        // 코어에 재검사를 요청하여 정렬 순서를 즉시 업데이트
        GenericModuleBlock.findAndNotifyCore(getLevel(), getBlockPos());
    }
    public GenericModuleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);


        ModuleType moduleType = getModuleType();

        // [핵심 수정] 공명기 모듈도 필터처럼 1칸짜리 인벤토리를 갖도록 함
        if (moduleType == ModuleType.FILTER || moduleType == ModuleType.RESONATOR) {
            this.itemHandler = new FilterModuleItemStackHandler(1, this);
        }
        else if (moduleType.getItemCapacity() > 0) {
            // 다른 아이템 버퍼 모듈일 경우, 기존의 일반 핸들러를 사용
            this.itemHandler = new ItemStackHandler(moduleType.getItemCapacity()) {
                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                    sendData();
                }
            };
        }

        if (moduleType.getFluidCapacity() > 0) {
            this.fluidHandler = new FluidTank(moduleType.getFluidCapacity()) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                    sendData();
                }
            };
        }
        // [신규] 에너지 입력 모듈일 경우, 자체 버퍼 생성

        // [핵심 수정] 새로운 DrillEnergyStorage 사용
        if (moduleType == ModuleType.ENERGY_INPUT) {
            this.energyInputBuffer = new DrillEnergyStorage(10000, 1000, 0, this::setChanged);
        }
    }
    @Override
    public List<ItemStack> processItem(ItemStack stack, DrillCoreBlockEntity core) {

        // 1. 이 모듈이 "필터 모듈"일 경우의 로직
        if (getModuleType() == ModuleType.FILTER) {
            ItemStack filterStack = getFilter();
            if (filterStack.isEmpty()) {
                return Collections.singletonList(stack);
            }

            // 1. FilterItemStack.of()를 사용하여 필터 아이템의 모든 NBT 데이터를 포함한 객체를 생성합니다.
            //    이 메서드는 아이템 타입에 따라 ListFilterItemStack 등을 알아서 반환해 줍니다.
            FilterItemStack filter = FilterItemStack.of(filterStack);

            // 2. 생성된 객체의 test 메서드를 호출합니다.
            //    이 메서드는 허용/거부 모드를 포함한 모든 필터링 로직을 내부적으로 수행합니다.
            //    test()가 true를 반환하면 "이 아이템은 통과해야 한다"는 최종 판정입니다.
            if (filter.test(level, stack)) {
                return Collections.singletonList(stack); // 통과
            } else {
                return Collections.emptyList(); // 거부 (파괴)
            }
        }
        // 2. 이 모듈이 "필터가 아닌 다른 모든 처리 모듈"일 경우의 로직
        else {
            return IProcessingModule.super.processItem(stack, core);
        }
    }
    /**
     * [신규] IBulkProcessingModule 인터페이스의 구현 메서드
     */
    @Override
    public boolean processBulk(IResourceAccessor coreResources) {
        if (getModuleType() != ModuleType.COMPACTOR) {
            return false;
        }

        assert level != null;
        RecipeManager recipeManager = level.getRecipeManager();

        for (CraftingRecipe recipe : recipeManager.getAllRecipesFor(RecipeType.CRAFTING).stream().map(RecipeHolder::value).toList()) {
            if (recipe.getIngredients().size() == 9 && !recipe.getResultItem(level.registryAccess()).isEmpty()) {

                Ingredient firstIngredient = recipe.getIngredients().getFirst();
                // [수정] 첫 번째 재료의 아이템 목록이 비어있으면 이 레시피는 건너뜁니다.
                if (firstIngredient.isEmpty() || firstIngredient.getItems().length == 0) continue;

                boolean allSame = true;
                for (int i = 1; i < 9; i++) {
                    ItemStack[] items = recipe.getIngredients().get(i).getItems();
                    // [수정] 다른 재료들의 아이템 목록도 비어있지 않은지 확인합니다.
                    if (items.length == 0 || !firstIngredient.test(items[0])) {
                        allSame = false;
                        break;
                    }
                }

                if (allSame) {
                    ItemStack ingredientStack = firstIngredient.getItems()[0].copy();
                    ingredientStack.setCount(9);

                    if (coreResources.consumeItems(ingredientStack, true).isEmpty()) {
                        coreResources.consumeItems(ingredientStack, false);
                        ItemStack resultStack = recipe.getResultItem(level.registryAccess()).copy();
                        ItemHandlerHelper.insertItem(coreResources.getInternalItemBuffer(), resultStack, false);
                        playCompactingEffects();
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void playCompactingEffects() {
        if (level != null && !level.isClientSide) {
            level.playSound(null, getBlockPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.2f);
            if (level instanceof ServerLevel serverLevel) {
                double px = getBlockPos().getX() + 0.5;
                double py = getBlockPos().getY() + 0.5;
                double pz = getBlockPos().getZ() + 0.5;
                serverLevel.sendParticles(ParticleTypes.CRIT, px, py, pz, 15, 0.4, 0.4, 0.4, 0.1);
            }
        }
    }
    @Override
    public void playEffects(Level level, BlockPos modulePos) {
        // 이 메서드는 서버에서만 호출되므로, ServerLevel로 캐스팅해도 안전합니다.
        if (!(level instanceof ServerLevel serverLevel)) return;

        switch (getModuleType()) {
            case FURNACE,BLAST_FURNACE -> {
                serverLevel.playSound(null, modulePos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.7F, 1.5F);

                double px = modulePos.getX() + 0.5;
                double py = modulePos.getY() + 1.0; // 블록 상단 표면 바로 위
                double pz = modulePos.getZ() + 0.5;
                serverLevel.sendParticles(ParticleTypes.LAVA, px, py, pz, 2, 0.1, 0.1, 0.1, 0.0); // 용암 파티클 5개
            }
            case CRUSHER -> {
                serverLevel.playSound(null, modulePos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.7F, 1.5F);

                // [위치 조정] 파티클이 블록 밖으로 잘 보이도록 생성 위치 조정
                double px = modulePos.getX() + 0.5;
                double py = modulePos.getY() + 0.5;
                double pz = modulePos.getZ() + 0.5;
                serverLevel.sendParticles(ParticleTypes.CRIT, px, py, pz, 7, 0.3, 0.3, 0.3, 0.1);
            }
            case WASHER -> {
                serverLevel.playSound(null, modulePos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.2F);

                // [위치 조정] 파티클이 블록 위에서 튀도록 조정
                double px = modulePos.getX() + 0.5;
                double py = modulePos.getY() + 1.1;
                double pz = modulePos.getZ() + 0.5;
                serverLevel.sendParticles(ParticleTypes.SPLASH, px, py, pz, 12, 0.3, 0.1, 0.3, 0.0);
            }
            default -> {}
        }
    }
    @Override
    public void onCoreTick(DrillCoreBlockEntity core) {
        ModuleType type = getModuleType();

        // 냉각 모듈 로직
        if (type == ModuleType.COOLANT) {
            if (core.getHeat() > COOLANT_ACTIVATION_HEAT) {
                assert level != null;
                FluidStack waterToConsume = new FluidStack(Fluids.WATER, WATER_CONSUMPTION_PER_TICK);
                if (core.consumeFluid(waterToConsume, true).getAmount() == WATER_CONSUMPTION_PER_TICK) {
                    core.consumeFluid(waterToConsume, false);
                    core.addHeat(-HEAT_REDUCTION_PER_TICK);
                    if (!level.isClientSide && level.getRandom().nextFloat() < 0.2f) {
                        level.playSound(null, getBlockPos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.5F + level.getRandom().nextFloat() * 0.5f);
                        if (level instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.CLOUD, getBlockPos().getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8, getBlockPos().getY() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8, getBlockPos().getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8, 1, 0, 0.1, 0, 0.0);
                        }
                    }
                }
            }
        }

        // [신규] 에너지 입력 모듈: 자신의 버퍼 에너지를 코어로 옮김
        if (type == ModuleType.ENERGY_INPUT && energyInputBuffer != null) {
            int energyToTransfer = energyInputBuffer.extractEnergy(1000, true);
            if (energyToTransfer > 0) {
                int accepted = core.getInternalEnergyBuffer().receiveEnergy(energyToTransfer, false);
                energyInputBuffer.extractEnergy(accepted, false);
            }
        }

        // [신규] 회전력 발전 모듈: 회전 속도에 비례하여 FE 생성
        if (type == ModuleType.KINETIC_DYNAMO) {
            float currentSpeed = Math.abs(getVisualSpeed()); // 코어에서 동기화된 속도 사용
            if (currentSpeed > 0) {
                int energyGenerated = (int) (currentSpeed / 4f); // 밸런스 조절 필요
                core.getInternalEnergyBuffer().receiveEnergy(energyGenerated, false);
            }
        }
    }
    // --- [1단계 추가] IProcessingModule 구현 ---

    @Override
    public RecipeType<?> getRecipeType() {
        // [수정] Supplier에서 실제 RecipeType 값을 가져옵니다.
        ModuleType type = getModuleType();
        if (type != null) {
            Supplier<RecipeType<?>> supplier = type.getRecipeTypeSupplier();
            if (supplier != null) {
                return supplier.get(); // 실제 값이 필요한 이 시점에 .get() 호출!
            }
        }
        return null;
    }

    @Override
    public boolean checkProcessingPreconditions(DrillCoreBlockEntity core) {

        // 필터 모듈은 항상 작동 준비 완료 상태
        if (getModuleType() == ModuleType.FILTER) {
            return true;
        }

        // [핵심 수정] 모듈 타입에 따라 작동 조건을 실시간으로 확인
        return switch (getModuleType()) {
            case WASHER -> {
                // 와셔 모듈은 작동 시점에 물 100mb를 소모할 수 있는지 직접 확인
                FluidStack waterToSimulate = new FluidStack(Fluids.WATER, 100);
                yield core.getInternalFluidBuffer().drain(waterToSimulate, IFluidHandler.FluidAction.SIMULATE).getAmount() >= 100;
            }
            case FURNACE ->
                // 용광로 모듈은 코어의 열이 50 이상인지 직접 확인
                    core.getHeat() >= 50f;
            case BLAST_FURNACE ->
                // 제련 용광로 모듈은 코어의 열이 90 이상인지 직접 확인
                    core.getHeat() >= 90f;
            case CRUSHER ->
                // 분쇄기는 별도의 조건이 없음
                    true;
            default -> false;
        };
    }

    @Override
    public void consumeResources(DrillCoreBlockEntity core) {

        // 필터 모듈은 별도의 자원을 소모하지 않음
        if (getModuleType() == ModuleType.FILTER) {
            return;
        }
        float efficiency = core.getHeatEfficiency();

        switch (getModuleType()) {
            case FURNACE: {
                float heatToConsume = 10.1f / (efficiency + 0.1f);
                core.addHeat(-heatToConsume);
                break;
            }
            case BLAST_FURNACE: {
                float heatToConsume = 20.1f / (efficiency + 0.1f);
                core.addHeat(-heatToConsume);
                break;
            }
            case WASHER: {
                // [수정] 여기서도 고정된 값으로 물을 소모
                int waterToConsume = 100;
                FluidStack waterRequest = new FluidStack(Fluids.WATER, waterToConsume);
                core.getInternalFluidBuffer().drain(waterRequest, IFluidHandler.FluidAction.EXECUTE);
                break; // case 문에 break 추가
            }
            default: {}
        }
    }
    // --- [신규] Capability 등록 이벤트에서 사용할 Getter ---
    @Nullable
    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Nullable
    public FluidTank getFluidHandler() {
        return fluidHandler;
    }
    // [신규] 외부에서 에너지 Capability를 요청할 때, 에너지 입력 버퍼를 반환
    @Nullable
    public IEnergyStorage getEnergyHandler() {
        if (getModuleType() == ModuleType.ENERGY_INPUT) {
            return energyInputBuffer;
        }
        return null;
    }

    // 자신의 모듈 타입을 반환하는 중요한 메서드
    public ModuleType getModuleType() {
        if (getBlockState().getBlock() instanceof GenericModuleBlock gmb) {
            return gmb.getModuleType();
        }
        // 이 코드가 실행되면 뭔가 잘못된 것이므로, 기본값 FRAME을 반환하고 경고를 남깁니다.
        MyCreateAddon.LOGGER.warn("GenericModuleBlockEntity at {} is not of type GenericModuleBlock! Returning FRAME type as default.", getBlockPos());
        return ModuleType.FRAME;
    }

    public void updateVisualSpeed(float speed) {
        if (this.visualSpeed == speed) {
            return;
        }
        this.visualSpeed = speed;
        setChanged();
        sendData();
    }

    // 코어가 이 메서드를 호출하여 렌더링 상태를 업데이트합니다.

    public void updateVisualConnections(Set<Direction> connections) {
        if (this.visualConnections.equals(connections)) {
            return;
        }
        this.visualConnections = connections;
        setChanged();
        sendData();
    }

    public void updateVisualState(Set<Direction> connections, float speed) {
        if (this.visualConnections.equals(connections) && this.visualSpeed == speed) {
            return; // 변경 사항이 없으면 아무것도 하지 않음
        }
        this.visualConnections = connections;
        this.visualSpeed = speed;
        setChanged();
        sendData();
    }
    // Visual이 사용할 getter
    public Set<Direction> getVisualConnections() {
        return visualConnections;
    }

    public float getVisualSpeed() {
        return visualSpeed;
    }

    // 동력 네트워크 격리를 위한 오버라이드
    @Override public float getGeneratedSpeed() { return 0; }
    @Override public float calculateStressApplied() { return 0; }
    @Override public float calculateAddedStressCapacity() { return 0; }
    @Override public void attachKinetics() {}



    // NBT 처리
    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        // [핵심 수정] if (clientPacket) 조건을 인벤토리/탱크 저장 로직 밖으로 옮겨서,
        // 클라이언트 패킷에도 내용물이 포함되도록 합니다.
        if (itemHandler != null) {
            compound.put("Inventory", itemHandler.serializeNBT(registries));
        }
        if (fluidHandler != null && !fluidHandler.getFluid().isEmpty()) {
            compound.put("Tank", fluidHandler.writeToNBT(registries, new CompoundTag()));
        }

        if (energyInputBuffer != null && energyInputBuffer.getEnergyStored() > 0)
            compound.put("EnergyInputBuffer", energyInputBuffer.serializeNBT(registries)); // [신규]

        if (resonatorFilter != null) { // [신규]
            compound.putString("ResonatorFilter", BuiltInRegistries.ITEM.getKey(resonatorFilter).toString());
        }
        if (!energyConnections.isEmpty()) {
            compound.put("EnergyConnections", new IntArrayTag(energyConnections.stream().mapToInt(Direction::ordinal).toArray()));
        }
        compound.putInt("ProcessingPriority", processingPriority);
        if (clientPacket) {
            if (!visualConnections.isEmpty()) {
                int[] dirs = visualConnections.stream().mapToInt(Direction::ordinal).toArray();
                compound.put("VisualConnections", new IntArrayTag(dirs));
            }
            if (visualSpeed != 0) {
                compound.putFloat("VisualSpeed", visualSpeed);
            }
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        // [핵심 수정] write와 마찬가지로, 클라이언트 패킷에서도 내용물을 읽도록 합니다.
        if (itemHandler != null && compound.contains("Inventory")) {
            itemHandler.deserializeNBT(registries, compound.getCompound("Inventory"));
        }
        if (fluidHandler != null && compound.contains("Tank")) {
            fluidHandler.readFromNBT(registries, compound.getCompound("Tank"));
        }
        if (energyInputBuffer != null && compound.contains("EnergyInputBuffer")) {
            // [핵심 수정] deserializeNBT 대신, 직접 값을 읽어와 setEnergy를 호출
            if (compound.get("EnergyInputBuffer") instanceof net.minecraft.nbt.IntTag intTag) {
                energyInputBuffer.setEnergy(intTag.getAsInt());
            }
        }
        if (compound.contains("ResonatorFilter")) { // [신규]
            this.resonatorFilter = BuiltInRegistries.ITEM.get(ResourceLocation.parse(compound.getString("ResonatorFilter")));
        } else {
            this.resonatorFilter = null;
        }
        energyConnections.clear();
        if (compound.contains("EnergyConnections")) {
            for (int dirOrdinal : compound.getIntArray("EnergyConnections")) {
                if (dirOrdinal >= 0 && dirOrdinal < Direction.values().length) {
                    energyConnections.add(Direction.values()[dirOrdinal]);
                }
            }
        }
        // [추가] 우선순위 값 로드
        if (compound.contains("ProcessingPriority")) {
            processingPriority = compound.getInt("ProcessingPriority");
        } else {
            processingPriority = 99; // 이전 버전 호환
        }
        if (clientPacket) {
            visualConnections.clear();
            if (compound.contains("VisualConnections")) {
                int[] dirs = compound.getIntArray("VisualConnections");
                for (int dirOrdinal : dirs) {
                    if (dirOrdinal >= 0 && dirOrdinal < Direction.values().length) {
                        visualConnections.add(Direction.values()[dirOrdinal]);
                    }
                }
            }
            visualSpeed = compound.getFloat("VisualSpeed");
        }
    }
}
