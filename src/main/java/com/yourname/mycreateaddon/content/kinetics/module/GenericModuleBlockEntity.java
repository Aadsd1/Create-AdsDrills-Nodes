package com.yourname.mycreateaddon.content.kinetics.module;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;


public class GenericModuleBlockEntity extends KineticBlockEntity implements IProcessingModule {


    // 렌더링 관련 필드
    private Set<Direction> visualConnections = new HashSet<>();
    private float visualSpeed = 0f;
    // --- [신규] 독립 저장소 필드 ---
    protected @Nullable ItemStackHandler itemHandler;
    protected @Nullable FluidTank fluidHandler;


    public GenericModuleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        ModuleType moduleType = getModuleType();
        if (moduleType.getItemCapacity() > 0) {
            // [핵심 추가] ItemStackHandler를 익명 클래스로 생성하여 onContentsChanged를 오버라이드합니다.
            this.itemHandler = new ItemStackHandler(moduleType.getItemCapacity()) {
                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                    sendData(); // 내용물이 바뀌면 클라이언트에 업데이트를 보냅니다.
                }
            };
        }
        if (moduleType.getFluidCapacity() > 0) {
            // [핵심 추가] FluidTank를 익명 클래스로 생성하여 onContentsChanged를 오버라이드합니다.
            this.fluidHandler = new FluidTank(moduleType.getFluidCapacity()) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                    sendData(); // 내용물이 바뀌면 클라이언트에 업데이트를 보냅니다.
                }
            };
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
        // 모듈 타입에 따라 다른 조건을 검사
        return switch (getModuleType()) {
            case FURNACE -> core.getHeat() >= 50f; // 히터는 열 50 이상 필요
            case BLAST_FURNACE -> core.getHeat() >= 90f; // [2단계 추가]
            case WASHER -> !core.getInternalFluidBuffer().drain(100, IFluidHandler.FluidAction.SIMULATE).isEmpty(); // [2단계 추가]
            case CRUSHER -> true; // [2단계 추가] 크러셔는 추가 조건 없음
            default -> false; // 처리 모듈이 아니면 항상 false
        };
    }

    @Override
    public void consumeResources(DrillCoreBlockEntity core) {
        switch (getModuleType()) {
            case FURNACE -> core.addHeat(-10f);
            case BLAST_FURNACE -> core.addHeat(-20f); // [2단계 추가]
            case WASHER -> core.getInternalFluidBuffer().drain(100, IFluidHandler.FluidAction.EXECUTE); // [2단계 추가]
            // 크러셔는 ModuleType에 정의된 높은 스트레스를 가하는 것 외에 추가 자원 소모 없음
            default -> {}
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
