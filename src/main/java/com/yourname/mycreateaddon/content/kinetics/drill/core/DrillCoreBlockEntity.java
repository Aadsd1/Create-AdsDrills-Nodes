package com.yourname.mycreateaddon.content.kinetics.drill.core;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.base.IResourceAccessor;
import com.yourname.mycreateaddon.content.kinetics.drill.head.IDrillHead;
import com.yourname.mycreateaddon.content.kinetics.drill.head.PumpHeadBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.drill.head.RotaryDrillHeadBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.module.*;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlockEntity;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.*;



public class DrillCoreBlockEntity extends KineticBlockEntity implements IResourceAccessor {

    private enum InvalidityReason {
        NONE,
        LOOP_DETECTED,
        MULTIPLE_CORES,
        TOO_MANY_MODULES,
        HEAD_MISSING, // 오류는 아니지만, 작동 불가 상태를 나타내기 위함
        DUPLICATE_PROCESSING_MODULE
    }
    // --- [신규] 연결된 버퍼 모듈의 핸들러를 저장할 리스트 ---
    private final List<IItemHandler> itemBufferHandlers = new ArrayList<>();
    private final List<IFluidHandler> fluidBufferHandlers = new ArrayList<>();
    private final List<BlockPos> activeSystemModules = new ArrayList<>();
    private final List<BlockPos> bulkProcessingModules = new ArrayList<>();
    // --- [추가] 과열 시스템 관련 필드 ---
    private float heat = 0.0f;
    private boolean isOverheated = false; // 강제 냉각 상태 여부

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

    private final List<BlockPos> processingModuleChain = new ArrayList<>();

    // --- 구조 관련 필드 ---
    private BlockPos cachedHeadPos = null;
    private Set<BlockPos> structureCache = new HashSet<>();
    private boolean structureValid = false;
    private InvalidityReason invalidityReason = InvalidityReason.NONE;
    private boolean needsStructureCheck = true;
    private static final int MAX_STRUCTURE_RANGE = 16;
    private static final int MAX_MODULES = 16;

    // [신규] 드릴 코어 자체의 기본 스트레스 부하를 정의합니다.
    public static final float BASE_STRESS_IMPACT = 4.0f;
    // --- 모듈 효과 집계 필드 ---
    private float totalSpeedBonus = 0f;
    private float totalStressImpact = 0f;
    private float totalHeatModifier = 0f; // [신규]

    private boolean canWasherWork = false;
    private boolean canHeaterWork = false;
    private boolean canBlastHeaterWork = false;


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

    }

    public boolean isStructureValid() {
        return this.structureValid;
    }



    @Override
    public IItemHandler getInternalItemBuffer() {
        // [수정] null 대신, 핸들러 리스트를 사용하는 가상 핸들러를 새로 생성하여 반환
        return new DrillStructureItemHandler(this.itemBufferHandlers);
    }
    @Override
    public IFluidHandler getInternalFluidBuffer() {
        // [수정] null 대신, 유체 핸들러 리스트를 사용하는 가상 핸들러를 새로 생성하여 반환
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
    /** 크러셔 모듈이 추가 스트레스를 가할 때 호출됩니다. (2단계 구현) */
    public void applyCrusherStress() {
        // TODO: 추가 스트레스 적용 로직 구현
    }
    @Override
    public FluidStack consumeFluid(FluidStack fluidToConsume, boolean simulate) {
        // [수정] 가상 핸들러의 drain 메서드를 직접 호출하여 유체를 소모
        IFluidHandler virtualHandler = getInternalFluidBuffer();
        return virtualHandler.drain(fluidToConsume, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
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
        this.structureCache.clear();
        this.processingModuleChain.clear();
        this.itemBufferHandlers.clear();
        this.fluidBufferHandlers.clear();
        this.activeSystemModules.clear(); // [신규] 리스트 초기화
        this.bulkProcessingModules.clear(); // [신규] 리스트 초기화
        this.totalSpeedBonus = 0f;
        this.totalStressImpact = BASE_STRESS_IMPACT;
        this.totalHeatModifier = 0f; // [신규] 초기화
        this.cachedHeadPos = null;
        this.invalidityReason = InvalidityReason.NONE;
        this.structureValid = false;
        Map<BlockPos, Set<Direction>> moduleConnections = new HashMap<>();

        Direction inputFacing = getBlockState().getValue(DirectionalKineticBlock.FACING);
        Direction headFacing = inputFacing.getOpposite();

        // --- 2. 헤드 확인 ---
        BlockState headState = level.getBlockState(worldPosition.relative(headFacing));
        if (headState.getBlock() instanceof IDrillHead head) { // [수정] 인터페이스 인스턴스를 바로 가져옴
            this.cachedHeadPos = worldPosition.relative(headFacing);
            // [신규] 헤드가 가진 스트레스 부하를 totalStressImpact에 더합니다.
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
                // [복원] 코어와 첫 모듈의 연결 정보를 기록
                moduleConnections.computeIfAbsent(worldPosition, k -> new HashSet<>()).add(startDir);
                moduleConnections.computeIfAbsent(startPos, k -> new HashSet<>()).add(startDir.getOpposite());

                StructureCheckResult result = searchStructureRecursive(startPos, startDir.getOpposite(), allVisited, allFoundModules, allOtherCores, moduleConnections, 0);
                if (result.loopDetected()) { this.invalidityReason = InvalidityReason.LOOP_DETECTED; break; }
                if (result.multipleCoresDetected()) { this.invalidityReason = InvalidityReason.MULTIPLE_CORES; break; }


            }
        }

        // --- 4. 유효성 검사 및 데이터 집계 ---
        Set<ModuleType> foundProcessingTypes = new HashSet<>();

        if (invalidityReason == InvalidityReason.NONE) {
            if (allFoundModules.size() > MAX_MODULES) {
                this.invalidityReason = InvalidityReason.TOO_MANY_MODULES;
            } else {
                // 구조는 일단 유효하다고 가정
                this.structureValid = true;
                this.structureCache = allFoundModules;

                for (BlockPos modulePos : this.structureCache) {
                    if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                        ModuleType type = moduleBE.getModuleType();
                        totalSpeedBonus += type.getSpeedBonus();
                        totalStressImpact += type.getStressImpact();
                        totalHeatModifier += type.getHeatModifier(); // [신규] 열 효율 집계
                        if (type.getItemCapacity() > 0 && moduleBE.getItemHandler() != null) itemBufferHandlers.add(moduleBE.getItemHandler());
                        if (type.getFluidCapacity() > 0 && moduleBE.getFluidHandler() != null) fluidBufferHandlers.add(moduleBE.getFluidHandler());


                        // 1. 일반 처리 모듈인 경우
                        if (moduleBE instanceof IProcessingModule && type.getRecipeTypeSupplier() != null) {
                            // 기존과 동일하게 레시피 타입을 Set에 추가
                            if (!foundProcessingTypes.add(type)) {
                                this.invalidityReason = InvalidityReason.DUPLICATE_PROCESSING_MODULE;
                                this.structureCache.clear();
                                this.structureValid = false;
                                break;
                            }
                            processingModuleChain.add(modulePos);
                        }

                        // 2. 벌크 처리 모듈(압축기)인 경우
                        if (moduleBE instanceof IBulkProcessingModule) {
                            // 압축기 모듈을 나타내는 고유한 객체(여기서는 ModuleType.COMPACTOR)를 Set에 추가
                            if (!foundProcessingTypes.add(type)) {
                                // 만약 Set에 이미 COMPACTOR 타입이 있다면, 중복으로 간주
                                this.invalidityReason = InvalidityReason.DUPLICATE_PROCESSING_MODULE;
                                this.structureCache.clear();
                                this.structureValid = false;
                                break;
                            }
                            bulkProcessingModules.add(modulePos);
                        }

                        // 3. 액티브 시스템 모듈 처리 (중복 검사 없음)
                        if (moduleBE instanceof IActiveSystemModule) {
                            activeSystemModules.add(modulePos);
                        }
                    }
                }

                // 중복이 발견되지 않았다면, 체인을 정상적으로 정렬
                if (this.structureValid) {
                    // [핵심 수정] 정렬 기준 변경
                    processingModuleChain.sort(Comparator.comparingInt(pos -> {
                        if (level.getBlockEntity(pos) instanceof GenericModuleBlockEntity gme) {
                            return gme.getProcessingPriority();
                        }
                        return 99; // BE가 없는 예외적인 경우, 가장 낮은 우선순위 부여
                    }));
                }
            }
        }
        // --- 5. [복원] 시각적 연결 정보 동기화 ---
        // 이전 구조에 있었지만 새 구조에는 없는 모듈들의 연결을 초기화
        Set<BlockPos> detachedModules = new HashSet<>(oldStructureCache);
        detachedModules.removeAll(this.structureCache);
        for (BlockPos detachedPos : detachedModules) {
            if (level.getBlockEntity(detachedPos) instanceof GenericModuleBlockEntity moduleBE) {
                moduleBE.updateVisualConnections(new HashSet<>());
            }
        }
        // 새 구조에 포함된 모든 모듈들의 연결을 업데이트
        for (BlockPos modulePos : this.structureCache) {
            if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity moduleBE) {
                moduleBE.updateVisualConnections(moduleConnections.getOrDefault(modulePos, new HashSet<>()));
            }
        }

        setChanged();
        sendData();
    }


    public List<ItemStack> mineNode(OreNodeBlockEntity nodeBE, int miningAmount) {
        if (level == null || level.isClientSide() || cachedHeadPos == null) {
            return Collections.emptyList();
        }

        int fortune = 0;
        boolean silkTouch = false;

        if (level.getBlockEntity(cachedHeadPos) instanceof RotaryDrillHeadBlockEntity headBE) {
            fortune = headBE.getFortuneLevel();
            silkTouch = headBE.hasSilkTouch();
        }

        return nodeBE.applyMiningTick(miningAmount, fortune, silkTouch);
    }

    // [복원] searchStructureRecursive가 moduleConnections 맵을 다시 사용하도록 수정
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

                // [복원] 연결 정보 기록
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

    private boolean isValidStructureBlock(Block block) {
        return block instanceof DrillCoreBlock || block instanceof GenericModuleBlock;
    }


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
        return super.getTheoreticalSpeed() * (1.0f + this.totalSpeedBonus);
    }

    public float getInputSpeed() {
        return super.getTheoreticalSpeed();
    }


    // [유지] 이 메서드들은 스트레스 관련 문제를 최종적으로 해결하기 위해 여전히 필요합니다.
    @Override
    public float calculateStressApplied() {
        if (!structureValid || invalidityReason == InvalidityReason.HEAD_MISSING) return 0;

        // totalStressImpact가 음수일 경우 0을 반환하여 음수 스트레스 전달을 방지합니다.
        return Math.max(0, this.totalStressImpact);
    }

    @Override
    public float calculateAddedStressCapacity() {
        // 드릴 코어가 스트레스 용량을 제공하는 것으로 취급되지 않도록 명시적으로 0을 반환합니다.
        return 0;
    }

    // ... (write, read, addToGoggleTooltip은 이전과 동일) ...
    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putBoolean("StructureValid", this.structureValid);
        compound.putFloat("SpeedBonus", this.totalSpeedBonus);
        compound.putFloat("StressImpact", this.totalStressImpact);
        compound.putInt("InvalidityReason", this.invalidityReason.ordinal());

        // [핵심 수정] NbtUtils 대신 수동으로 X, Y, Z 저장
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
        }

    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        this.structureValid = compound.getBoolean("StructureValid");
        this.totalSpeedBonus = compound.getFloat("SpeedBonus");
        this.totalStressImpact = compound.getFloat("StressImpact");

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

            this.processingModuleChain.clear();
            if (compound.contains("ProcessingChain", 9)) {
                ListTag chainTag = compound.getList("ProcessingChain", 10);
                for (int i = 0; i < chainTag.size(); i++) {
                    // [핵심 수정] NbtUtils 대신 수동으로 X, Y, Z를 읽어 BlockPos 생성
                    CompoundTag posTag = chainTag.getCompound(i);
                    this.processingModuleChain.add(new BlockPos(
                            posTag.getInt("X"),
                            posTag.getInt("Y"),
                            posTag.getInt("Z")
                    ));
                }
            }
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
            // 서버 측에서 월드를 로드할 때
            this.processingModuleChain.clear();
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
            needsStructureCheck = true;
        }
    }

    @Override
    public void tick() {
        super.tick();

        // [수정] 클라이언트와 서버 로직을 분리합니다.
        assert level != null;
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
    private void updateProcessingConditions() {
        // 와셔 모듈 조건
        FluidStack waterToSimulate = new FluidStack(Fluids.WATER, 100);
        this.canWasherWork = getInternalFluidBuffer().drain(waterToSimulate, IFluidHandler.FluidAction.SIMULATE).getAmount() >= 100;
        // 히터 모듈 조건
        this.canHeaterWork = getHeat() >= 50f;
        // 블래스트 히터 모듈 조건
        this.canBlastHeaterWork = getHeat() >= 90f;
    }

    // 모듈이 사용할 getter들
    public boolean canWasherWork() { return this.canWasherWork; }
    public boolean canHeaterWork() { return this.canHeaterWork; }
    public boolean canBlastHeaterWork() { return this.canBlastHeaterWork; }

    public void serverTick() {
        // 기존 tick() 메서드에 있던 모든 서버 로직을 여기로 옮깁니다.
        tickCounter++;
        assert level != null;
        if (needsStructureCheck) {
            scanAndValidateStructure();
            needsStructureCheck = false;
        }

        for (BlockPos modulePos : activeSystemModules) {
            if (level.getBlockEntity(modulePos) instanceof IActiveSystemModule module) {
                module.onCoreTick(this);
            }
        }
        // [핵심 추가] 주기적인 상태 업데이트
        if (tickCounter % 20 == 0) {
            updateProcessingConditions();
        }
        float finalSpeed = getFinalSpeed();

        IDrillHead headBlock = null;
        if (hasHead() && level.getBlockState(cachedHeadPos).getBlock() instanceof IDrillHead h) {
            headBlock = h;
        }

        // --- 과열 로직 수정 ---
        // [핵심 수정] finalSpeed의 절댓값을 사용하거나, 0이 아닌지 확인합니다.
        if (finalSpeed != 0 && headBlock != null) {
            float baseHeatGen = headBlock.getHeatGeneration();
            float speedFactor = Math.max(1, Math.abs(finalSpeed) / 64f);

            // [핵심 수정] 집계된 열 효율 보너스를 적용합니다.
            // totalHeatModifier는 음수 값이므로, 1.0f에 더해 최종 배율을 계산합니다.
            float finalHeatMultiplier = Math.max(0, 1.0f + totalHeatModifier);
            float heatThisTick = baseHeatGen * speedFactor * finalHeatMultiplier;

            addHeat(heatThisTick);
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
        // [추가] 이전 틱의 과열 상태를 기억하기 위한 필드
        // --- isOverheated 상태 업데이트 ---
        boolean wasOverheated = isOverheated;
        if (isOverheated && this.heat <= COOLDOWN_RESET_THRESHOLD) {
            isOverheated = false;
        } else if (!isOverheated && this.heat >= 100.0f) {
            isOverheated = true;
        }

        // --- [핵심 리팩토링] 과열 이벤트 처리 ---
        if (!wasOverheated && isOverheated) {
            boolean eventHandledByHead = false;
            if (headBlock != null) {
                // 코어는 헤드의 종류를 묻지 않고, 그저 '과열 이벤트'가 발생했음을 알립니다.
                eventHandledByHead = headBlock.onOverheat(level, cachedHeadPos, this);
            }

            // 헤드가 이벤트를 처리하지 않았을 경우에만 (false를 반환했을 경우)
            // 코어가 기본 과열 효과(사운드)를 재생합니다.
            if (!eventHandledByHead) {
                level.playSound(null, worldPosition, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0f, 0.8f);
            }
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
            // [핵심 수정] 헤드 BE의 종류에 따라 올바른 처리를 하도록 변경
            if (level.getBlockEntity(cachedHeadPos) instanceof RotaryDrillHeadBlockEntity headBE) {
                headBE.updateVisualSpeed(-finalSpeed); // Rotary는 반대로 회전
                if (needsSync) {
                    headBE.updateClientHeat(this.heat);
                }
            }
            // [신규] 펌프 헤드에 대한 속도 전달 로직 추가
            else if (level.getBlockEntity(cachedHeadPos) instanceof PumpHeadBlockEntity headBE) {
                headBE.updateVisualSpeed(finalSpeed); // 펌프는 정방향 회전
                // 펌프는 열 시각화가 없으므로 updateClientHeat는 호출하지 않음
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

        List<ItemStack> stacksToProcess = new ArrayList<>();
        stacksToProcess.add(minedItem);

        // 1. 일반 처리 체인을 따라 아이템을 전달
        for (BlockPos modulePos : processingModuleChain) {
            if (level.getBlockEntity(modulePos) instanceof IProcessingModule processor) {
                List<ItemStack> nextStacks = new ArrayList<>();
                for (ItemStack currentStack : stacksToProcess) {
                    nextStacks.addAll(processor.processItem(currentStack, this));
                }
                stacksToProcess = nextStacks;
            }
        }

        // 2. 모든 일반 처리가 끝난 결과물을 내부 버퍼에 삽입
        for (ItemStack finalStack : stacksToProcess) {
            // ItemHandlerHelper.insertItem은 남은 아이템을 반환
            ItemStack remainder = ItemHandlerHelper.insertItem(getInternalItemBuffer(), finalStack, false);
            if (!remainder.isEmpty()) {
                // 버퍼가 꽉 찼다면 남은 아이템을 바닥에 드롭
                ItemEntity itemEntity = new ItemEntity(level, getBlockPos().getX() + 0.5, getBlockPos().getY() + 1.5, getBlockPos().getZ() + 0.5, remainder);
                level.addFreshEntity(itemEntity);
            }
        }

        // 3. [신규] 모든 아이템이 버퍼에 들어간 후, 벌크 처리 모듈 작동
        boolean successfulBulkProcess;
        do {
            successfulBulkProcess = false;
            for (BlockPos modulePos : bulkProcessingModules) {
                if (level.getBlockEntity(modulePos) instanceof IBulkProcessingModule bulkProcessor) {
                    // processBulk가 true를 반환하면 내용물에 변화가 있었다는 의미이므로,
                    // 다른 압축 모듈이 이어서 작업할 수 있도록 루프를 다시 시작
                    if (bulkProcessor.processBulk(this)) {
                        successfulBulkProcess = true;
                    }
                }
            }
        } while (successfulBulkProcess); // 내용물 변화가 없을 때까지 반복
    }


    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // Create의 기본 운동 정보(Stress 등)를 표시하기 위해 super 호출을 유지합니다.
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        // "Drill Assembly" 헤더
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("goggle.mycreateaddon.drill_core.header").withStyle(ChatFormatting.GRAY));

        // 구조 유효성 상태
        MutableComponent status;
        if (structureValid && invalidityReason != InvalidityReason.HEAD_MISSING) {
            status = Component.translatable("goggle.mycreateaddon.drill_core.valid", structureCache.size())
                    .withStyle(ChatFormatting.GREEN);
        } else {
            status = Component.translatable("goggle.mycreateaddon.drill_core.invalid")
                    .withStyle(ChatFormatting.RED);
        }
        tooltip.add(Component.literal(" ").append(status));

        // 오류 원인 (오류가 있을 때만 표시)
        if (!structureValid || invalidityReason != InvalidityReason.NONE) {
            String reasonKey = switch (invalidityReason) {
                case LOOP_DETECTED -> "goggle.mycreateaddon.drill_core.reason.loop_detected";
                case MULTIPLE_CORES -> "goggle.mycreateaddon.drill_core.reason.multiple_cores";
                case TOO_MANY_MODULES -> "goggle.mycreateaddon.drill_core.reason.too_many_modules";
                case HEAD_MISSING -> "goggle.mycreateaddon.drill_core.reason.head_missing";
                case DUPLICATE_PROCESSING_MODULE -> "goggle.mycreateaddon.drill_core.reason.duplicate_processing_module";
                default -> null;
            };
            if (reasonKey != null) {
                MutableComponent reason = (invalidityReason == InvalidityReason.TOO_MANY_MODULES)
                        ? Component.translatable(reasonKey, MAX_MODULES)
                        : Component.translatable(reasonKey);
                tooltip.add(Component.literal(" ").append(reason.withStyle(ChatFormatting.DARK_RED)));
            }
        }
        // 과열 상태 특별 표시는 항상 보이도록 함
        if (isOverheated) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal(" ")
                    .append(Component.translatable("goggle.mycreateaddon.drill_core.overheated")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withBold(true).withUnderlined(true))));
            tooltip.add(Component.literal(" ")
                    .append(Component.translatable("goggle.mycreateaddon.drill_core.cooling_down", String.format("%.0f%%", COOLDOWN_RESET_THRESHOLD))
                            .withStyle(ChatFormatting.DARK_GRAY)));
        }

        // --- 쉬프트 상태에 따라 정보 집합을 완전히 전환 ---
        if (isPlayerSneaking) {
            // --- 상세 정보 뷰 (SHIFT) ---

            // [성능 보너스 상세]
            if (structureValid && (totalSpeedBonus > 0 || totalStressImpact != 0 || totalHeatModifier < 0)) { // [수정] 조건 추가
                tooltip.add(Component.literal(""));
                if (totalSpeedBonus > 0) {
                    tooltip.add(Component.literal(" ")
                            .append(Component.translatable("goggle.mycreateaddon.drill_core.speed_bonus").withStyle(ChatFormatting.GRAY))
                            .append(": ")
                            .append(Component.literal("+" + (int) (totalSpeedBonus * 100) + "%")
                                    .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true))));
                }
                if (totalStressImpact != 0) {
                    String sign = totalStressImpact > 0 ? "+" : "";
                    ChatFormatting color = totalStressImpact > 0 ? ChatFormatting.GOLD : ChatFormatting.GREEN;
                    tooltip.add(Component.literal(" ")
                            .append(Component.translatable("goggle.mycreateaddon.drill_core.stress_impact").withStyle(ChatFormatting.GRAY))
                            .append(": ")
                            .append(Component.literal(sign + String.format("%.1f", totalStressImpact) + " SU")
                                    .withStyle(style -> style.withColor(color).withBold(true))));
                }
                // [신규] 열 효율 툴팁
                if (totalHeatModifier < 0) {
                    tooltip.add(Component.literal(" ")
                            .append(Component.translatable("goggle.mycreateaddon.drill_core.heat_reduction").withStyle(ChatFormatting.GRAY))
                            .append(": ")
                            .append(Component.literal(String.format("%.0f%%", -totalHeatModifier * 100))
                                    .withStyle(style -> style.withColor(ChatFormatting.BLUE).withBold(true))));
                }
            }

            // [과열 시스템 상세]
            tooltip.add(Component.literal("")); // 구분선

            float currentHeat = this.heat;
            float currentEfficiency = getHeatEfficiency();

            // 1. 온도 표시 라인
            MutableComponent heatLine = Component.literal(" ")
                    .append(Component.translatable("goggle.mycreateaddon.drill_core.heat_label")); // "Heat: "
            MutableComponent heatValue = Component.literal(String.format("%.1f%%", currentHeat));
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

            // 3. 유효 속도 표시 라인 (상세 뷰에도 포함)
            float finalSpeed = getFinalSpeed();
            MutableComponent speedLine = Component.literal(" ")
                    .append(Component.translatable("goggle.mycreateaddon.drill_core.effective_speed_label")); // "Effective Speed: "
            MutableComponent speedValue = Component.literal(String.format("%.1f RPM", Math.abs(finalSpeed)));
            if (currentEfficiency > 1.0f) {
                speedValue.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            } else if (currentHeat > OVERLOAD_START_THRESHOLD) {
                speedValue.withStyle(ChatFormatting.DARK_RED);
            } else {
                speedValue.withStyle(ChatFormatting.GRAY);
            }
            tooltip.add(speedLine.append(speedValue));

            // [처리 순서]
            if (!processingModuleChain.isEmpty()) {
                tooltip.add(Component.literal(""));
                tooltip.add(Component.literal("Processing Order:").withStyle(ChatFormatting.GRAY));

                // 우선순위대로 정렬된 체인을 순회
                for (BlockPos modulePos : processingModuleChain) { // for-each 루프로 변경하여 혼동 방지
                    assert level != null;
                    if (level.getBlockEntity(modulePos) instanceof GenericModuleBlockEntity gme) {
                        Block moduleBlock = gme.getBlockState().getBlock();
                        Component moduleName = moduleBlock.getName().withStyle(ChatFormatting.AQUA);

                        // [핵심 수정] 루프의 순번(i) 대신, 모듈 BE에 저장된 실제 우선순위 값을 가져옵니다.
                        int priority = gme.getProcessingPriority();

                        MutableComponent line = Component.literal(" > " + priority + ". ")
                                .withStyle(ChatFormatting.DARK_GRAY)
                                .append(moduleName);
                        tooltip.add(line);
                    }
                }
            }

        } else {
            // --- 요약 정보 뷰 (NO SHIFT) ---
            if (structureValid) {
                tooltip.add(Component.literal(""));

                // [핵심 수정] isOverheated에 대한 직접적인 처리를 제거하고,
                // 과열 상태가 아닐 때의 온도 정보만 표시합니다.
                if (!isOverheated) {
                    MutableComponent heatSummary = Component.literal(" ")
                            .append(Component.translatable("goggle.mycreateaddon.drill_core.heat_label").withStyle(ChatFormatting.GRAY));

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
            }

            // [처리 모듈 개수 요약]
            if (!processingModuleChain.isEmpty()) {
                tooltip.add(Component.literal(" ")
                        .append(Component.literal(String.valueOf(processingModuleChain.size())).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" Processing Modules active").withStyle(ChatFormatting.DARK_GRAY))
                );
            }

            // 쉬프트 안내 문구
            tooltip.add(Component.literal(" ").append(Component.translatable("goggle.mycreateaddon.sneak_for_details").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        }


        // --- 저장 공간 정보 (항상 표시) ---
        boolean hasItemBuffers = false;
        boolean hasFluidBuffers = false;
        int totalItemSlots = 0;
        int nonEmptyItemSlots = 0;
        int totalItems = 0;
        FluidStack containedFluid = FluidStack.EMPTY;

        // [핵심 수정] 유체 정보를 담을 새로운 변수들
        int totalFluidCapacity = 0;
        int totalFluidAmount = 0;
        Map<Fluid, Integer> fluidComposition = new HashMap<>(); // 각 유체의 종류와 양을 저장할 맵

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
                                // 맵에 기존 유체가 있으면 양을 더하고, 없으면 새로 추가
                                fluidComposition.merge(fluidInTank.getFluid(), fluidInTank.getAmount(), Integer::sum);
                            }
                        }
                    }
                }
            }
        }


        if (hasItemBuffers || hasFluidBuffers) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal(" ").append(Component.translatable("goggle.mycreateaddon.drill_core.storage_header").withStyle(ChatFormatting.GRAY)));

            if (hasItemBuffers) {
                MutableComponent itemLine = Component.literal(" ")
                        .append(Component.translatable("goggle.mycreateaddon.drill_core.storage.items").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.format(": %,d / %,d", totalItems, totalItemSlots * 64)));
                if (isPlayerSneaking && nonEmptyItemSlots > 0) {
                    itemLine.append(Component.literal(String.format(" (%d Slots)", nonEmptyItemSlots)).withStyle(ChatFormatting.DARK_GRAY));
                }
                tooltip.add(itemLine);
            }

            if (hasFluidBuffers) {
                // [핵심 수정] 쉬프트 상태에 따라 유체 툴팁을 다르게 표시
                if (isPlayerSneaking) {
                    // --- 상세 뷰 (SHIFT): 모든 유체 목록 표시 ---
                    MutableComponent fluidHeader = Component.literal(" ")
                            .append(Component.translatable("goggle.mycreateaddon.drill_core.storage.fluid").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(String.format(": %,d / %,d mb", totalFluidAmount, totalFluidCapacity)));
                    tooltip.add(fluidHeader);

                    if (fluidComposition.isEmpty()) {
                        tooltip.add(Component.literal("   ").append(Component.translatable("goggle.mycreateaddon.drill_core.storage.empty").withStyle(ChatFormatting.DARK_GRAY)));
                    } else {
                        // 유체를 양 순서대로 정렬하여 표시
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
                    // --- 요약 뷰 (NO SHIFT): 전체 용량과 종류 개수만 표시 ---
                    MutableComponent fluidLine = Component.literal(" ")
                            .append(Component.translatable("goggle.mycreateaddon.drill_core.storage.fluid").withStyle(ChatFormatting.GRAY))
                            .append(": ");

                    if (totalFluidAmount == 0) {
                        fluidLine.append(Component.translatable("goggle.mycreateaddon.drill_core.storage.empty").withStyle(ChatFormatting.DARK_GRAY));
                    } else {
                        fluidLine.append(Component.literal(String.format("%,d / %,d mb", totalFluidAmount, totalFluidCapacity))
                                .withStyle(ChatFormatting.GOLD));
                        // 유체 종류가 2개 이상일 때만 종류 개수 표시
                        if (fluidComposition.size() > 1) {
                            fluidLine.append(Component.literal(String.format(" (%d Types)", fluidComposition.size())).withStyle(ChatFormatting.DARK_GRAY));
                        }
                    }
                    tooltip.add(fluidLine);
                }
            }
        }

        return true;
    }
}
