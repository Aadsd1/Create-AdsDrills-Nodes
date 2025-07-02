package com.yourname.mycreateaddon.content.kinetics.drill.core;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.base.IResourceAccessor;
import com.yourname.mycreateaddon.content.kinetics.drill.head.IDrillHead;
import com.yourname.mycreateaddon.content.kinetics.drill.head.RotaryDrillHeadBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleBlock;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.module.ModuleType;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.*;



public class DrillCoreBlockEntity extends KineticBlockEntity implements IResourceAccessor {

    private enum InvalidityReason {
        NONE,
        LOOP_DETECTED,
        MULTIPLE_CORES,
        TOO_MANY_MODULES,
        HEAD_MISSING // 오류는 아니지만, 작동 불가 상태를 나타내기 위함
    }
    // --- [신규] 내부 버퍼 관련 필드 ---
    protected ItemStackHandler internalItemBuffer;
    protected FluidTank internalFluidBuffer;


    // 버퍼의 총 용량 (모듈에 의해 결정됨)
    private int totalItemCapacity = 0;
    private int totalFluidCapacity = 0;
    // --- [추가] 과열 시스템 관련 필드 ---
    private float heat = 0.0f;
    private boolean isOverheated = false; // 강제 냉각 상태 여부
    // [추가] 이전 틱의 과열 상태를 기억하기 위한 필드
    private boolean wasOverheated = false;

    // --- [추가] 과열 관련 상수 ---
    public static final float BOOST_START_THRESHOLD = 40.0f; // 부스트 시작 (40%)
    public static final float OVERLOAD_START_THRESHOLD = 90.0f; // 과부하 시작 (90%)
    public static final float COOLDOWN_RESET_THRESHOLD = 30.0f; // 재작동 가능 (30%)
    public static final float MAX_BOOST_MULTIPLIER = 2.0f; // 최대 부스트 배율 (200%)
    public static final float CORE_BASE_COOLING = 0.1f;   // 코어의 기본 냉각 속도

    // [추가] 고글 정보 업데이트 주기를 위한 필드
    private static final int GOGGLE_UPDATE_DEBOUNCE = 10; // 10틱 (0.5초) 마다 업데이트
    private int tickCounter = 0;

    // [추가] 클라이언트와의 시각적 속도 동기화를 위한 필드
    private float visualSpeed = 0f;


    // --- 구조 관련 필드 ---
    private BlockPos cachedHeadPos = null;
    private Set<BlockPos> structureCache = new HashSet<>();
    private boolean structureValid = false;
    private InvalidityReason invalidityReason = InvalidityReason.NONE;
    private boolean needsStructureCheck = true;
    private static final int MAX_STRUCTURE_RANGE = 16;
    private static final int MAX_MODULES = 16;

    // --- 모듈 효과 집계 필드 ---
    private float totalSpeedBonus = 0f;
    private float totalStressImpact = 0f;

    // [핵심 추가] heat 필드에 대한 public getter
    public float getHeat() {
        return this.heat;
    }

    // [추가] isOverheated 필드에 대한 public getter (향후 렌더러 등에서 사용 가능)
    public boolean isOverheated() {
        return this.isOverheated;
    }
    public DrillCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {

        super(type, pos, state);


        // 초기에는 용량이 0인 버퍼를 생성
        internalItemBuffer = new ItemStackHandler(0);
        internalFluidBuffer = new FluidTank(0);

    }

    public boolean isStructureValid() {
        return this.structureValid;
    }



    @Override
    public IItemHandler getInternalItemBuffer() {
        return this.internalItemBuffer;
    }

    @Override
    public IFluidHandler getInternalFluidBuffer() {
        return this.internalFluidBuffer;
    }

    @Override
    public ItemStack consumeItems(ItemStack stackToConsume, boolean simulate) {
        // 이 로직은 나중에 더 복잡하게 만들 수 있습니다. 지금은 단순 구현.
        // TODO: 여러 슬롯을 순회하며 아이템을 찾는 로직 추가
        return ItemStack.EMPTY;
    }

    @Override
    public FluidStack consumeFluid(FluidStack fluidToConsume, boolean simulate) {
        return this.internalFluidBuffer.drain(fluidToConsume, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
    }

  // --- [신규] 구조 검사 시 버퍼 용량을 집계하는 로직 ---
    private void aggregateResourceCapacity() {
        int newItemCapacity = 0;
        int newFluidCapacity = 0;

        for (BlockPos modulePos : structureCache) {
            if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                ModuleType type = moduleBE.getModuleType();
                // 각 모듈 타입이 제공하는 용량을 더합니다 (ModuleType enum 수정 필요).
                newItemCapacity += type.getItemCapacity();
                newFluidCapacity += type.getFluidCapacity();
            }
        }

        // 용량이 변경되었는지 확인
        if (totalItemCapacity != newItemCapacity || totalFluidCapacity != newFluidCapacity) {
            totalItemCapacity = newItemCapacity;
            totalFluidCapacity = newFluidCapacity;

            // 기존 데이터를 보존하면서 새로운 용량의 버퍼로 교체
            ItemStackHandler newItemBuffer = new ItemStackHandler(totalItemCapacity);
            FluidTank newFluidBuffer = new FluidTank(totalFluidCapacity);

            // 데이터 복사
            for (int i = 0; i < Math.min(internalItemBuffer.getSlots(), newItemBuffer.getSlots()); i++) {
                newItemBuffer.setStackInSlot(i, internalItemBuffer.getStackInSlot(i));
            }
            newFluidBuffer.setFluid(internalFluidBuffer.getFluid());

            internalItemBuffer = newItemBuffer;
            internalFluidBuffer = newFluidBuffer;

            setChanged();
        }
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

    // [추가] 열 효율까지 모두 계산된 최종 속도를 반환하는 getter
    public float getFinalSpeed() {
        if (!this.structureValid || isOverStressed()) {
            return 0;
        }
        return getSpeed() * getHeatEfficiency();
    }

    // [수정] 새로운 효율 계산 메서드
    public float getHeatEfficiency() {
        if (isOverheated || heat >= 100.0f) {
            return 0f;
        }

        if (heat > OVERLOAD_START_THRESHOLD) {
            // 90% 열(효율 200%)에서 100% 열(효율 0%)까지 급격히 감소
            // f(x) = -0.2x + 20
            return Math.max(0, -20.0f * (this.heat / 100.0f) + 20.0f);
        } else if (heat > BOOST_START_THRESHOLD) {
            // 40% 열(효율 100%)에서 90% 열(효율 200%)까지 선형적으로 증가
            // f(x) = 0.02x + 0.2
            return 2.0f * (this.heat / 100.0f) + 0.2f;
        }

        // 0% ~ 40% 구간은 기본 효율
        return 1.0f;
    }

    public float getVisualSpeed() {
        return this.visualSpeed;
    }

    public void scheduleStructureCheck() {
        if (level != null && !level.isClientSide) {
            this.needsStructureCheck = true;
        }
    }

    private void scanAndValidateStructure() {
        if (level == null || level.isClientSide()) return;

        Set<BlockPos> oldStructureCache = new HashSet<>(this.structureCache);

        // --- 1. 상태 초기화 ---
        this.structureValid = false;
        this.invalidityReason = InvalidityReason.NONE;
        this.totalSpeedBonus = 0f;
        this.totalStressImpact = 0f;
        this.cachedHeadPos = null; // 매번 초기화

        Set<BlockPos> newStructureCache = new HashSet<>();
        Map<BlockPos, Set<Direction>> moduleConnections = new HashMap<>();
        Direction searchDir = getBlockState().getValue(DirectionalKineticBlock.FACING).getOpposite();

        // --- 2. 모듈 구조 탐색 및 검증 ---
        Set<BlockPos> visitedInScan = new HashSet<>();
        List<BlockPos> foundFunctionalModules = new ArrayList<>();
        List<BlockPos> foundOtherCores = new ArrayList<>();

        StructureCheckResult result = searchStructureRecursive(this.worldPosition, searchDir, visitedInScan, foundFunctionalModules, foundOtherCores, moduleConnections, 0);

        if (result.loopDetected()) {
            this.invalidityReason = InvalidityReason.LOOP_DETECTED;
        } else if (result.multipleCoresDetected()) {
            this.invalidityReason = InvalidityReason.MULTIPLE_CORES;
        } else if (foundFunctionalModules.size() - 1 > MAX_MODULES) {
            this.invalidityReason = InvalidityReason.TOO_MANY_MODULES;
        } else {
            // 모듈 구조에 문제가 없으면, 일단 '구조는 유효'하다고 판단합니다.
            this.structureValid = true;

            newStructureCache.addAll(foundFunctionalModules);
            newStructureCache.remove(this.worldPosition);

            for (BlockPos modulePos : newStructureCache) {
                if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                    ModuleType type = moduleBE.getModuleType();
                    this.totalSpeedBonus += type.getSpeedBonus();
                    this.totalStressImpact += type.getStressImpact();
                }
            }
        }

        // --- 3. 헤드 존재 여부 확인 ---
        // [핵심 수정] 이 로직을 structureValid와 독립적으로 실행하여 cachedHeadPos를 확실히 설정합니다.
        BlockPos potentialHeadPos = this.worldPosition.relative(searchDir);
        BlockState headState = level.getBlockState(potentialHeadPos);

        if (headState.getBlock() instanceof IDrillHead && headState.getValue(DirectionalKineticBlock.FACING) == searchDir) {
            this.cachedHeadPos = potentialHeadPos;
            BlockPos cachedNodePos = potentialHeadPos.relative(searchDir);
        } else if (this.structureValid) {
            // 모듈 구조는 괜찮은데 헤드가 없는 경우에만 경고를 설정합니다.
            this.invalidityReason = InvalidityReason.HEAD_MISSING;
        }

        // --- 4. 캐시 및 시각 효과 업데이트 ---
        this.structureCache = newStructureCache;
        Set<BlockPos> detachedModules = new HashSet<>(oldStructureCache);
        detachedModules.removeAll(newStructureCache);
        for (BlockPos detachedPos : detachedModules) {
            if (level.getBlockEntity(detachedPos) instanceof GenericModuleBlockEntity moduleBE) {
                moduleBE.updateVisualConnections(new HashSet<>());
                moduleBE.updateVisualSpeed(0f);
            }
        }
        for (BlockPos modulePos : newStructureCache) {
            if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                Set<Direction> connections = moduleConnections.getOrDefault(modulePos, new HashSet<>());
                moduleBE.updateVisualConnections(connections);
            }
        }

        setChanged();
        sendData();

        if (this.structureValid) {
            aggregateResourceCapacity();
        }
    }

    private record StructureCheckResult(boolean loopDetected, boolean multipleCoresDetected) {
    }

    private StructureCheckResult searchStructureRecursive(BlockPos currentPos, Direction from, Set<BlockPos> visited, List<BlockPos> functionalModules, List<BlockPos> otherCores, Map<BlockPos, Set<Direction>> moduleConnections, int depth) {
        if (depth > MAX_STRUCTURE_RANGE || !visited.add(currentPos)) {
            return new StructureCheckResult(from != null, false);
        }

        boolean isThisCore = currentPos.equals(this.worldPosition);
        assert level != null;
        BlockState currentState = level.getBlockState(currentPos);
        Block currentBlock = currentState.getBlock();

        if (isThisCore && from != null && from == currentState.getValue(DirectionalKineticBlock.FACING)) {
            return new StructureCheckResult(false, false);
        }

        if (currentBlock instanceof DrillCoreBlock && !isThisCore) {
            otherCores.add(currentPos);
            return new StructureCheckResult(false, true);
        }

        if (!isValidStructureBlock(currentBlock)) {
            return new StructureCheckResult(false, false);
        }

        functionalModules.add(currentPos);

        for (Direction dir : Direction.values()) {
            if (from != null && dir == from.getOpposite()) {
                continue;
            }
            if (isThisCore && dir == currentState.getValue(DirectionalKineticBlock.FACING)) {
                continue;
            }

            BlockPos neighborPos = currentPos.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            BlockState neighborState = level.getBlockState(neighborPos);
            if (isValidStructureBlock(neighborState.getBlock())) {
                moduleConnections.computeIfAbsent(currentPos, k -> new HashSet<>()).add(dir);
                moduleConnections.computeIfAbsent(neighborPos, k -> new HashSet<>()).add(dir.getOpposite());

                StructureCheckResult result = searchStructureRecursive(neighborPos, dir, visited, functionalModules, otherCores, moduleConnections, depth + 1);
                if (result.loopDetected || result.multipleCoresDetected) {
                    return result;
                }
            }
        }
        return new StructureCheckResult(false, false);
    }

    private boolean isValidStructureBlock(Block block) {
        return block instanceof DrillCoreBlock || block instanceof GenericModuleBlock;
    }

    public boolean isModuleConnectedAt(Direction absoluteDirection) {
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
        return super.getTheoreticalSpeed() * (1.0f + this.totalSpeedBonus);
    }

    public float getInputSpeed() {
        return super.getTheoreticalSpeed();
    }

    @Override
    public float calculateStressApplied() {
        if (!structureValid || invalidityReason == InvalidityReason.HEAD_MISSING) return 0;
        return this.totalStressImpact;
    }

    // ... (write, read, addToGoggleTooltip은 이전과 동일) ...
    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putBoolean("StructureValid", this.structureValid);
        compound.putFloat("SpeedBonus", this.totalSpeedBonus);
        compound.putFloat("StressImpact", this.totalStressImpact);
        compound.putInt("InvalidityReason", this.invalidityReason.ordinal());

        compound.put("InternalItems", internalItemBuffer.serializeNBT(registries));
        compound.put("InternalFluid", internalFluidBuffer.writeToNBT(registries, new CompoundTag()));
        compound.putFloat("Heat", this.heat);
        compound.putBoolean("Overheated", this.isOverheated);
        if (clientPacket) {
            ListTag cacheList = new ListTag();
            for (BlockPos pos : structureCache) {
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("X", pos.getX());
                posTag.putInt("Y", pos.getY());
                posTag.putInt("Z", pos.getZ());
                cacheList.add(posTag);
            }
            compound.put("StructureCache", cacheList);
        }
        if (clientPacket) {
            compound.putFloat("VisualSpeed", this.visualSpeed);
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        this.structureValid = compound.getBoolean("StructureValid");
        this.totalSpeedBonus = compound.getFloat("SpeedBonus");
        this.totalStressImpact = compound.getFloat("StressImpact");

        internalItemBuffer.deserializeNBT(registries, compound.getCompound("InternalItems"));
        internalFluidBuffer.readFromNBT(registries, compound.getCompound("InternalFluid"));
        this.heat = compound.getFloat("Heat");
        this.isOverheated = compound.getBoolean("Overheated");

        int reasonOrdinal = compound.getInt("InvalidityReason");
        if (reasonOrdinal >= 0 && reasonOrdinal < InvalidityReason.values().length) {
            this.invalidityReason = InvalidityReason.values()[reasonOrdinal];
        } else {
            this.invalidityReason = InvalidityReason.NONE;
        }

        if (clientPacket) {
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
        } else {
            needsStructureCheck = true;
        }
    }

    @Override
    public void tick() {
        super.tick();

        // [수정] 클라이언트와 서버 로직을 분리합니다.
        if (level.isClientSide()) {
            // --- 클라이언트 전용 시각/청각 효과 ---
            clientTick();
        } else {
            // --- 서버 전용 계산 및 동기화 로직 ---
            serverTick();
        }
    }
    // [수정] 클라이언트 로직을 위한 메서드
    public void clientTick() {
        // [추가] level이 null일 경우를 대비한 안전장치
        if (level == null) return;

        // isOverheated 상태에서는 효과 없음
        if (isOverheated) return;

        float heatPercent = this.heat / 100f;
        if (heatPercent <= 0.4f) return;

        // [핵심 수정] level에서 RandomSource 인스턴스를 가져옵니다.
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
                // [핵심 수정] .get()을 제거하고 사운드 이벤트 필드를 직접 사용합니다.
                level.playLocalSound(worldPosition, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, 0.3f, 1.5f + heatPercent, false);
            }
        }
    }

    public void serverTick() {
        // 기존 tick() 메서드에 있던 모든 서버 로직을 여기로 옮깁니다.
        tickCounter++;
        assert level != null;
        if (needsStructureCheck) {
            scanAndValidateStructure();
            needsStructureCheck = false;
        }

        float finalSpeed = getFinalSpeed();

        IDrillHead headBlock = null;
        if (hasHead() && level.getBlockState(cachedHeadPos).getBlock() instanceof IDrillHead h) {
            headBlock = h;
        }

        // --- 과열 로직 수정 ---
        // [핵심 수정] finalSpeed의 절댓값을 사용하거나, 0이 아닌지 확인합니다.
        if (finalSpeed != 0 && headBlock != null) {
            // 가열: 드릴이 실제로 회전하고 있을 때만 열이 오릅니다.
            this.heat += headBlock.getHeatGeneration();
        } else {
            // 냉각: 드릴이 어떤 이유로든(동력 없음, 과열 등) 멈춰있으면 열이 식습니다.
            float coolingRate = CORE_BASE_COOLING;
            if (headBlock != null) {
                coolingRate += headBlock.getCoolingRate();
            }
            this.heat -= coolingRate;
        }

        // (열 수치 제한 및 과열 상태 갱신 로직은 이전과 동일)
        this.heat = Mth.clamp(this.heat, 0, 100);
        wasOverheated = isOverheated; // 현재 상태를 이전 상태로 기록

        if (isOverheated && this.heat <= COOLDOWN_RESET_THRESHOLD) {
            isOverheated = false;
        } else if (!isOverheated && this.heat >= 100.0f) {
            isOverheated = true;
        }

        // 상태가 '정상 -> 과열'로 방금 바뀌었다면, 효과를 발동시킵니다.
        if (!wasOverheated && isOverheated) {
            // 모든 플레이어에게 들리도록 서버에서 사운드 재생
            level.playSound(null, worldPosition, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0f, 0.8f);
            // 추가로, 클라이언트 측에 큰 연기 효과를 요청할 수도 있습니다 (지금은 사운드만).
        }

        // --- 동기화 로직 ---
        // [핵심 수정] visualSpeed를 업데이트할 때, 로컬 변수 finalSpeed를 사용합니다.
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
            if(level.getBlockEntity(cachedHeadPos) instanceof RotaryDrillHeadBlockEntity headBE) {
                headBE.updateVisualSpeed(-finalSpeed);
                if (needsSync) {
                    headBE.updateClientHeat(this.heat);
                }
            }

            // [핵심 수정] 여기도 0이 아닌지 확인합니다.
            if (finalSpeed != 0) {
                headBlock.onDrillTick(level, cachedHeadPos, level.getBlockState(cachedHeadPos), this);
            }
        }
    }



    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("goggle.mycreateaddon.drill_core.header").withStyle(ChatFormatting.GRAY));
        MutableComponent status;
        if (structureValid && invalidityReason != InvalidityReason.HEAD_MISSING) {
            status = Component.translatable("goggle.mycreateaddon.drill_core.valid", structureCache.size())
                    .withStyle(ChatFormatting.GREEN);
        } else {
            status = Component.translatable("goggle.mycreateaddon.drill_core.invalid")
                    .withStyle(ChatFormatting.RED);
        }
        tooltip.add(Component.literal(" ").append(status));
        if (!structureValid || invalidityReason != InvalidityReason.NONE) {
            String reasonKey = switch (invalidityReason) {
                case LOOP_DETECTED -> "goggle.mycreateaddon.drill_core.reason.loop_detected";
                case MULTIPLE_CORES -> "goggle.mycreateaddon.drill_core.reason.multiple_cores";
                case TOO_MANY_MODULES -> "goggle.mycreateaddon.drill_core.reason.too_many_modules";
                case HEAD_MISSING -> "goggle.mycreateaddon.drill_core.reason.head_missing";
                default -> null;
            };

            if (reasonKey != null) {
                MutableComponent reason = (invalidityReason == InvalidityReason.TOO_MANY_MODULES)
                        ? Component.translatable(reasonKey, MAX_MODULES)
                        : Component.translatable(reasonKey);
                tooltip.add(Component.literal(" ").append(reason.withStyle(ChatFormatting.DARK_RED)));
            }
        }
        if (structureValid && (totalSpeedBonus > 0 || totalStressImpact > 0)) {
            tooltip.add(Component.literal(""));
            if (totalSpeedBonus > 0) {
                tooltip.add(Component.literal(" ")
                        .append(Component.translatable("goggle.mycreateaddon.drill_core.speed_bonus").withStyle(ChatFormatting.GRAY))
                        .append(": ")
                        .append(Component.literal("+" + (int) (totalSpeedBonus * 100) + "%")
                                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true))));
            }
            if (totalStressImpact > 0) {
                tooltip.add(Component.literal(" ")
                        .append(Component.translatable("goggle.mycreateaddon.drill_core.stress_impact").withStyle(ChatFormatting.GRAY))
                        .append(": ")
                        .append(Component.literal("+" + totalStressImpact + " SU")
                                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true))));
            }
        }

        tooltip.add(Component.literal("")); // 구분선

        float currentHeat = this.heat;
        float currentEfficiency = getHeatEfficiency();

        // 1. 온도 표시 라인
        MutableComponent heatLine = Component.literal(" ")
                .append(Component.translatable("goggle.mycreateaddon.drill_core.heat_label")); // "Heat: "

        MutableComponent heatValue = Component.literal(String.format("%.1f%%", currentHeat));

        // 온도에 따라 색상 결정
        if (currentHeat > OVERLOAD_START_THRESHOLD) {
            heatValue.withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        } else if (currentHeat > BOOST_START_THRESHOLD) {
            heatValue.withStyle(ChatFormatting.GOLD);
        } else {
            heatValue.withStyle(ChatFormatting.GRAY);
        }
        tooltip.add(heatLine.append(heatValue));


        // 2. 효율 표시 라인
        MutableComponent efficiencyLine = Component.literal(" ")
                .append(Component.translatable("goggle.mycreateaddon.drill_core.efficiency_label")); // "Efficiency: "

        MutableComponent efficiencyValue = Component.literal(String.format("%.0f%%", currentEfficiency * 100));

        // 효율에 따라 색상 및 설명 결정
        if (currentEfficiency > 1.0f) {
            efficiencyValue.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            efficiencyLine.append(efficiencyValue)
                    .append(Component.literal(" (Optimal Boost)").withStyle(ChatFormatting.DARK_AQUA));
        } else if (currentHeat > OVERLOAD_START_THRESHOLD) {
            efficiencyValue.withStyle(ChatFormatting.DARK_RED);
            efficiencyLine.append(efficiencyValue)
                    .append(Component.literal(" (Overloading)").withStyle(ChatFormatting.RED));
        } else {
            efficiencyValue.withStyle(ChatFormatting.GRAY);
            efficiencyLine.append(efficiencyValue);
        }
        tooltip.add(efficiencyLine);

        // --- [핵심 추가] 유효 속도(Effective Speed) 표시 라인 ---
        float finalSpeed = getFinalSpeed(); // 열 효율이 모두 반영된 최종 속도를 가져옵니다.

        MutableComponent speedLine = Component.literal(" ")
                .append(Component.translatable("goggle.mycreateaddon.drill_core.effective_speed_label")); // "Effective Speed: "

        MutableComponent speedValue = Component.literal(String.format("%.1f RPM", Math.abs(finalSpeed)));

        // 효율(상태)에 따라 속도 값의 색상을 결정합니다.
        if (currentEfficiency > 1.0f) {
            speedValue.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
        } else if (currentHeat > OVERLOAD_START_THRESHOLD) {
            speedValue.withStyle(ChatFormatting.DARK_RED);
        } else {
            speedValue.withStyle(ChatFormatting.GRAY);
        }
        tooltip.add(speedLine.append(speedValue));

        // 3. 임계 과열 상태 특별 표시
        if (isOverheated) {
            tooltip.add(Component.literal(" ")
                    .append(Component.translatable("goggle.mycreateaddon.drill_core.overheated")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withBold(true).withUnderlined(true))));
            tooltip.add(Component.literal(" ")
                    .append(Component.translatable("goggle.mycreateaddon.drill_core.cooling_down", String.format("%.0f%%", COOLDOWN_RESET_THRESHOLD))
                            .withStyle(ChatFormatting.DARK_GRAY)));
        }

        // --- [핵심 추가] 내부 저장소 정보 ---
        IItemHandler itemHandler = getInternalItemBuffer();
        IFluidHandler fluidHandler = getInternalFluidBuffer();

        // 아이템 또는 유체 버퍼 모듈이 하나라도 연결되어 있을 때만 "Internal Storage" 섹션을 표시합니다.
        if (itemHandler.getSlots() > 0 || fluidHandler.getTanks() > 0) {
            tooltip.add(Component.literal("")); // 구분선
            tooltip.add(Component.literal(" ").append(Component.translatable("goggle.mycreateaddon.drill_core.storage_header").withStyle(ChatFormatting.GRAY)));

            // 아이템 버퍼 정보 표시
            if (itemHandler.getSlots() > 0) {
                int nonEmptySlots = 0;
                int totalItems = 0;
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    ItemStack stackInSlot = itemHandler.getStackInSlot(i);
                    if (!stackInSlot.isEmpty()) {
                        nonEmptySlots++;
                        totalItems += stackInSlot.getCount();
                    }
                }

                MutableComponent itemLine = Component.literal(" ")
                        .append(Component.translatable("goggle.mycreateaddon.drill_core.storage.items").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.format(": %,d / %,d", totalItems, itemHandler.getSlots() * 64)));

                // 상세 정보 (웅크릴 때 표시)
                if (isPlayerSneaking && nonEmptySlots > 0) {
                    itemLine.append(Component.literal(String.format(" (%d Slots)", nonEmptySlots)).withStyle(ChatFormatting.DARK_GRAY));
                }
                tooltip.add(itemLine);
            }

            // 유체 버퍼 정보 표시
            if (fluidHandler.getTanks() > 0) {
                FluidStack fluidStack = fluidHandler.getFluidInTank(0);

                MutableComponent fluidLine = Component.literal(" ")
                        .append(Component.translatable("goggle.mycreateaddon.drill_core.storage.fluid").withStyle(ChatFormatting.GRAY))
                        .append(": ");

                if (fluidStack.isEmpty()) {
                    fluidLine.append(Component.translatable("goggle.mycreateaddon.drill_core.storage.empty").withStyle(ChatFormatting.DARK_GRAY));
                } else {
                    fluidLine.append(Component.literal(String.format("%,d / %,d mb ", fluidStack.getAmount(), fluidHandler.getTankCapacity(0)))
                            .withStyle(ChatFormatting.GOLD)
                            .append(fluidStack.getHoverName().copy().withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true))));
                }
                tooltip.add(fluidLine);
            }
        }

        return true;
    }
}
