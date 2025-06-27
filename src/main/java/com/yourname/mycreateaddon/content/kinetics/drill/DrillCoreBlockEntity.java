package com.yourname.mycreateaddon.content.kinetics.drill;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.module.Frame.FrameModuleBlock;
import com.yourname.mycreateaddon.content.kinetics.module.Frame.FrameModuleBlockEntity;
import com.yourname.mycreateaddon.registry.MyAddonBlocks; // SpeedModuleBlock을 위해 임포트 (가정)
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;




public class DrillCoreBlockEntity extends KineticBlockEntity {

    // --- 구조 관련 필드 ---
    private Set<BlockPos> structureCache = new HashSet<>(); // List 대신 Set을 사용하여 중복 방지 및 검색 속도 향상
    private boolean structureValid = false;
    private boolean needsStructureCheck = true;
    private static final int MAX_STRUCTURE_RANGE = 16;
    private static final int MAX_MODULES = 128;

    public DrillCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ==================================================
    //  구조 검사 및 관리 로직
    // ==================================================

    public void scheduleStructureCheck() {
        if (level != null && !level.isClientSide) {
            this.needsStructureCheck = true;
        }
    }


    private void scanAndValidateStructure() {
        if (level == null || level.isClientSide()) return;

        Set<BlockPos> newStructure = new HashSet<>();
        Map<BlockPos, Set<Direction>> moduleConnections = new HashMap<>(); // 연결 정보 수집용
        boolean wasValid = this.structureValid;

        Set<BlockPos> visitedInScan = new HashSet<>();
        List<BlockPos> foundFunctionalModules = new ArrayList<>();
        List<BlockPos> foundOtherCores = new ArrayList<>();

        // 재귀 탐색을 통해 구조와 연결 정보를 수집
        StructureCheckResult result = searchStructureRecursive(
                this.worldPosition, null, visitedInScan, foundFunctionalModules, foundOtherCores, moduleConnections, 0);

        // 유효성 검사
        if (!result.loopDetected && !result.multipleCoresDetected && foundFunctionalModules.size() <= MAX_MODULES) {
            this.structureValid = true;
            newStructure.addAll(foundFunctionalModules);

            // --- ▼▼▼ 수정된 부분 시작 ▼▼▼ ---

            // 유효한 구조에 포함된 모든 모듈들에게 렌더링 정보 전파
            float currentSpeed = getSpeed();
            Direction coreFacing = this.getBlockState().getValue(DirectionalKineticBlock.FACING);
            Direction coreBack = coreFacing.getOpposite();

            for (BlockPos modulePos : newStructure) {
                if (level.getBlockEntity(modulePos) instanceof FrameModuleBlockEntity frameBE) {
                    // 이 모듈이 가져야 할 모든 연결 방향을 가져옴
                    Set<Direction> connections = new HashSet<>(moduleConnections.getOrDefault(modulePos, new HashSet<>()));

                    // 만약 이 모듈이 코어의 앞쪽에 붙어있다면, 코어를 향하는 연결(coreBack)을 제거
                    if (modulePos.equals(this.worldPosition.relative(coreFacing))) {
                        connections.remove(coreBack);
                    }
                    // 만약 이 모듈이 코어의 뒤쪽에 붙어있다면, 코어를 향하는 연결(coreFacing)을 제거
                    if (modulePos.equals(this.worldPosition.relative(coreBack))) {
                        connections.remove(coreFacing);
                    }

                    frameBE.updateVisualConnections(connections, currentSpeed);
                }
            }
            // --- ▲▲▲ 수정된 부분 끝 ▲▲▲ ---

        } else {
            this.structureValid = false;
            // (오류 로그 부분은 기존과 동일하게 유지)
            if (result.multipleCoresDetected) {
                MyCreateAddon.LOGGER.warn("Multiple cores detected in structure starting at {}!", worldPosition);
                for (BlockPos corePos : foundOtherCores) {
                    if (level.getBlockEntity(corePos) instanceof DrillCoreBlockEntity otherCore) {
                        otherCore.scheduleStructureCheck();
                    }
                }
            }
        }

        // 구조가 변경되었는지 확인하고 동기화
        if (!this.structureCache.equals(newStructure) || this.structureValid != wasValid) {
            // 비활성화되어야 할 모듈들을 찾아서 신호를 보냄
            Set<BlockPos> detachedModules = new HashSet<>(this.structureCache);
            detachedModules.removeAll(newStructure);
            for (BlockPos detachedPos : detachedModules) {
                if (level.getBlockEntity(detachedPos) instanceof FrameModuleBlockEntity frameBE) {
                    frameBE.updateVisualConnections(new HashSet<>(), 0f); // 비활성화 신호
                }
            }

            this.structureCache = newStructure;
            setChanged();
            sendData();
        }
    }

    // 재귀 탐색 결과 레코드
    private record StructureCheckResult(boolean loopDetected, boolean multipleCoresDetected) {}

    // ★★★ 재귀 탐색 메서드 시그니처 및 로직 수정 ★★★
    private StructureCheckResult searchStructureRecursive(BlockPos currentPos, Direction from, Set<BlockPos> visited, List<BlockPos> functionalModules, List<BlockPos> otherCores, Map<BlockPos, Set<Direction>> moduleConnections, int depth) {
        if (depth > MAX_STRUCTURE_RANGE || !visited.add(currentPos)) {
            return new StructureCheckResult(from != null, false);
        }

        boolean isThisCore = currentPos.equals(this.worldPosition);
        BlockState currentState = level.getBlockState(currentPos);
        Block currentBlock = currentState.getBlock();

        if (currentBlock instanceof DrillCoreBlock && !isThisCore) {
            otherCores.add(currentPos);
            return new StructureCheckResult(false, true);
        }

        if (!isThisCore && !isValidStructureBlock(currentBlock)) {
            return new StructureCheckResult(false, false);
        }

        if (isActualModule(currentBlock)) {
            functionalModules.add(currentPos);
        }

        // 자식 노드 탐색
        for (Direction dir : Direction.values()) {
            if (from != null && dir == from.getOpposite()) {
                continue;
            }

            BlockPos neighborPos = currentPos.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            BlockState neighborState = level.getBlockState(neighborPos);
            if (isValidStructureBlock(neighborState.getBlock())) {
                // ★★★ 연결 정보 기록 ★★★
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
        // --- 이제 DrillCoreBlock도 유효한 구조의 일부로 인식합니다. ---
        return block instanceof DrillCoreBlock || block instanceof FrameModuleBlock || isActualModule(block);
    }

    private boolean isActualModule(Block block) {
        // TODO: 더 많은 모듈 블록 추가
        // 예시: return block instanceof SpeedModuleBlock || block instanceof FortuneModuleBlock;
        return block == MyAddonBlocks.FRAME_MODULE.get(); // 지금은 FRAME_MODULE을 기능성 모듈로 취급하여 테스트
    }

    // ==================================================
    //  Visual과의 연동을 위한 헬퍼 메서드
    // ==================================================

    /**
     * 지정된 방향에 모듈이 연결되어 있는지 확인 (클라이언트에서도 사용 가능)
     * @param absoluteDirection 월드 기준의 절대 방향 (NORTH, SOUTH, EAST, WEST)
     */
    public boolean isModuleConnectedAt(Direction absoluteDirection) {
        if (!absoluteDirection.getAxis().isHorizontal()) return false;

        BlockPos adjacentPos = this.worldPosition.relative(absoluteDirection);
        // 구조 캐시에 해당 위치가 포함되어 있는지 확인
        return this.structureCache.contains(adjacentPos);
    }

    // ==================================================
    //  NBT 데이터 처리
    // ==================================================
    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putBoolean("StructureValid", this.structureValid);

        // --- 클라이언트 패킷일 때만 구조 캐시를 NBT로 변환하여 전송합니다. ---
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
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        this.structureValid = compound.getBoolean("StructureValid");

        // --- 클라이언트 패킷일 때만 NBT를 읽어 구조 캐시를 복원합니다. ---
        if (clientPacket) {
            Set<BlockPos> newCache = new HashSet<>();
            if (compound.contains("StructureCache", 9)) { // 9 = ListTag 타입
                ListTag cacheList = compound.getList("StructureCache", 10); // 10 = CompoundTag 타입
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
            // 서버에서 월드를 로드할 때는 구조를 다시 확인해야 합니다.
            needsStructureCheck = true;
        }
    }

    // ==================================================
    //  틱 업데이트 및 효과
    // ==================================================

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide) {
            if (needsStructureCheck) {
                scanAndValidateStructure();
                needsStructureCheck = false;
            }
        }
    }

    // ==================================================
    //  Goggle 정보 표시
    // ==================================================

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        Component structureStatus = structureValid
                ? Component.translatable("tooltip.mycreateaddon.structure.valid", structureCache.size()).withStyle(ChatFormatting.GREEN)
                : Component.translatable("tooltip.mycreateaddon.structure.invalid").withStyle(ChatFormatting.RED);
        tooltip.add(Component.literal(" ").append(structureStatus));
        return true;
    }
}