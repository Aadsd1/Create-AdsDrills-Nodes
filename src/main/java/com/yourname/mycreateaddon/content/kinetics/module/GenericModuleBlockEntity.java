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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.*;



public class GenericModuleBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {

    // 렌더링 관련 필드
    private Set<Direction> visualConnections = new HashSet<>();
    private float visualSpeed = 0f;
    private Set<Direction> energyConnections = new HashSet<>(); // [신규] 에너지 연결 방향 저장
    // --- [신규] 독립 저장소 필드 ---
    protected @Nullable ItemStackHandler itemHandler;
    protected @Nullable FluidTank fluidHandler;
    protected @Nullable DrillEnergyStorage energyInputBuffer; // [핵심 수정] 타입 변경

    private Item resonatorFilter = null; // [신규] 공명 필터 아이템 저장
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
        boolean hasPriority = (getModuleType().getBehavior().getRecipeType() != null || type == ModuleType.FILTER);

        if (hasPriority) {
            tooltip.add(Component.literal("")); // 줄바꿈
            tooltip.add(Component.translatable("goggle.mycreateaddon.module.processing_priority").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(" " + this.processingPriority).withStyle(ChatFormatting.AQUA));
            return true;
        }
        return false;
    }

    // [추가] 우선순위를 순환시키는 메서드
    public void cyclePriority(Player player) {
        processingPriority++;
        if (processingPriority > MAX_PRIORITY) {
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

    public ModuleType getModuleType() {
        if (getBlockState().getBlock() instanceof GenericModuleBlock gmb) {
            return gmb.getModuleType();
        }
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

    public List<ItemStack> processItem(ItemStack stack, DrillCoreBlockEntity core) {
        return getModuleType().getBehavior().processItem(this, stack, core);
    }

    public boolean processBulk(IResourceAccessor coreResources) {
        return getModuleType().getBehavior().processBulk(this, coreResources);
    }

    public void onCoreTick(DrillCoreBlockEntity core) {
        getModuleType().getBehavior().onCoreTick(this, core);
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

    /**
     * 모듈이 다른 블록으로 교체되기 전에 내부의 모든 아이템을 월드에 드롭합니다.
     */
    public void dropContents() {
        if (level == null || level.isClientSide()) {
            return;
        }

        // 1. 아이템 핸들러(인벤토리)의 내용물을 드롭합니다.
        // 필터 모듈의 필터 아이템도 이 로직으로 함께 드롭됩니다.
        if (itemHandler != null) {
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                ItemStack stackInSlot = itemHandler.getStackInSlot(i);
                if (!stackInSlot.isEmpty()) {
                    level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stackInSlot));
                }
            }
        }

        // 2. 공명기 모듈의 필터 아이템을 드롭합니다.
        // 이 필터는 별도의 필드에 저장되므로 따로 처리해야 합니다.
        if (resonatorFilter != null) {
            level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, new ItemStack(resonatorFilter)));
        }

        // 참고: 유체는 변환 시 소멸되는 것으로 처리합니다.
        // 유체를 아이템화하는 것은 훨씬 복잡한 로직이 필요합니다.
    }

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
