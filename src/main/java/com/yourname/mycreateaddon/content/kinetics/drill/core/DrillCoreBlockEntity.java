package com.yourname.mycreateaddon.content.kinetics.drill.core;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.drill.head.IDrillHead;
import com.yourname.mycreateaddon.content.kinetics.drill.head.RotaryDrillHeadBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleBlock;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.module.ModuleType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;



public class DrillCoreBlockEntity extends KineticBlockEntity {

    private enum InvalidityReason {
        NONE,
        LOOP_DETECTED,
        MULTIPLE_CORES,
        TOO_MANY_MODULES,
        HEAD_MISSING // 오류는 아니지만, 작동 불가 상태를 나타내기 위함
    }

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

    public DrillCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isStructureValid() {
        return this.structureValid;
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

        assert level != null;
        if(level.isClientSide()){
            return;
        }

        // --- 이하 서버 전용 로직 ---
        tickCounter++;

        if (needsStructureCheck) {
            scanAndValidateStructure();
            needsStructureCheck = false;
        }

        // [핵심 수정] 최종 속도를 tick 로직의 가장 위에서 먼저 계산합니다.
        // 이 값은 동력, 열, 스트레스 상태를 모두 반영하는 '최종 결과'입니다.
        float finalSpeed = getFinalSpeed();

        IDrillHead headBlock = null;
        if (hasHead() && level.getBlockState(cachedHeadPos).getBlock() instanceof IDrillHead h) {
            headBlock = h;
        }

        // --- 과열 로직 수정 ---
        // 이제 finalSpeed가 0보다 클 때만 가열하고, 그렇지 않으면 무조건 냉각합니다.
        if (finalSpeed > 0 && headBlock != null) {
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
        if (isOverheated && this.heat <= COOLDOWN_RESET_THRESHOLD) {
            isOverheated = false;
        } else if (!isOverheated && this.heat >= 100.0f) {
            isOverheated = true;
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
            }

            // 채굴은 finalSpeed가 0보다 클 때만 이루어집니다.
            // (onDrillTick 내부에서도 확인하지만, 여기서 한 번 더 확인하는 것이 더 명확합니다.)
            if (finalSpeed > 0) {
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

        return true;

    }
}
