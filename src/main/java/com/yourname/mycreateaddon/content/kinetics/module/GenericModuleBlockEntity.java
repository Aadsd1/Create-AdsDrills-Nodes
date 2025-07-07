package com.yourname.mycreateaddon.content.kinetics.module;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.base.IResourceAccessor;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.neoforged.neoforge.fluids.FluidStack; // 추가
import net.minecraft.core.particles.ParticleTypes; // 추가
import net.minecraft.sounds.SoundEvents; // 추가
import net.minecraft.sounds.SoundSource; // 추가
import net.minecraft.world.level.Level; // 추가
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;


public class GenericModuleBlockEntity extends KineticBlockEntity implements IProcessingModule,IActiveSystemModule, IBulkProcessingModule {


    // 렌더링 관련 필드
    private Set<Direction> visualConnections = new HashSet<>();
    private float visualSpeed = 0f;
    // --- [신규] 독립 저장소 필드 ---
    protected @Nullable ItemStackHandler itemHandler;
    protected @Nullable FluidTank fluidHandler;

    // [신규] 냉각 로직 관련 상수
    private static final float COOLANT_ACTIVATION_HEAT = 5.0f; // 5도 이상일 때 작동
    private static final int WATER_CONSUMPTION_PER_TICK = 5; // 틱당 물 5mb 소모 (초당 100mb)
    private static final float HEAT_REDUCTION_PER_TICK = 0.4f; // 틱당 열 0.4 감소
    // [추가] 우선순위 필드. 기본값은 99 (가장 낮음)
    private int processingPriority = 99;
    private static final int MAX_PRIORITY = 10;
    // [추가] 우선순위 값을 얻는 getter
    public int getProcessingPriority() {
        return processingPriority;
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
                serverLevel.playSound(null, modulePos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 1.2F);

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
        // 이 BE가 냉각 모듈일 경우에만 로직 실행
        if (getModuleType() != ModuleType.COOLANT) {
            return;
        }

        // 코어의 열이 활성화 온도 이상일 때만 작동
        if (core.getHeat() > COOLANT_ACTIVATION_HEAT) {
            assert level != null;
            // 1. 소모할 물을 정의하고, 코어의 버퍼에 충분한 양이 있는지 시뮬레이션
            FluidStack waterToConsume = new FluidStack(Fluids.WATER, WATER_CONSUMPTION_PER_TICK);
            if (core.consumeFluid(waterToConsume, true).getAmount() == WATER_CONSUMPTION_PER_TICK) {
                // 2. 충분하다면, 실제로 물을 소모
                core.consumeFluid(waterToConsume, false);

                // 3. 코어의 열을 식힘
                core.addHeat(-HEAT_REDUCTION_PER_TICK);

                // 4. 시각/청각 효과 재생 (서버에서만)
                if (!level.isClientSide && level.getRandom().nextFloat() < 0.2f) {
                    level.playSound(null, getBlockPos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.5F + level.getRandom().nextFloat() * 0.5f);
                    if (level instanceof ServerLevel serverLevel) {
                        double px = getBlockPos().getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8;
                        double py = getBlockPos().getY() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8;
                        double pz = getBlockPos().getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8;
                        serverLevel.sendParticles(ParticleTypes.CLOUD, px, py, pz, 1, 0, 0.1, 0, 0.0);
                    }
                }
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
