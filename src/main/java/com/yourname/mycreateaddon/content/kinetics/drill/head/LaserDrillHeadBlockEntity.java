package com.yourname.mycreateaddon.content.kinetics.drill.head;



import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.yourname.mycreateaddon.client.LaserBeamRenderer;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlock;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlockEntity;
import com.yourname.mycreateaddon.registry.MyAddonItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class LaserDrillHeadBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {

    public enum OperatingMode {
        WIDE_BEAM,
        RESONANCE,
        DECOMPOSITION;

        public OperatingMode getNext() {
            return values()[(this.ordinal() + 1) % values().length];
        }

        public Component getDisplayName() {
            return Component.translatable("mycreateaddon.laser_head.mode." + this.name().toLowerCase());
        }
    }
    @Override
    public void initialize() {
        super.initialize();
        if (level != null && level.isClientSide) {
            LaserBeamRenderer.addLaser(this);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (level != null && level.isClientSide) {
            LaserBeamRenderer.removeLaser(getBlockPos());
        }
    }
    private float visualSpeed = 0f; // [신규] 시각적 회전 속도
    private OperatingMode currentMode = OperatingMode.WIDE_BEAM;
    private int decompositionProgress = 0;
    private static final int DECOMPOSITION_TIME_TICKS = 200; // 10초
    private static final int ENERGY_PER_DECOMPOSITION_TICK = 500; // 분해 모드 에너지 소모량
    private static final int ENERGY_PER_MINING_TICK = 100; // 채굴 모드 에너지 소모량

    // 렌더링을 위한 타겟 위치 리스트 (클라이언트로 동기화 필요)
    public List<BlockPos> activeTargets = new ArrayList<>();
    private final List<BlockPos> designatedTargets = new ArrayList<>(); // [신규] 플레이어가 지정한 타겟


    public LaserDrillHeadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // [핵심 수정] 모드 변경 시 타겟 초기화 및 즉시 갱신
    public void cycleMode() {
        this.currentMode = this.currentMode.getNext();
        this.designatedTargets.clear(); // 모드 변경 시 지정 타겟 초기화
        this.decompositionProgress = 0;
        updateActiveTargets(); // 즉시 타겟 재탐색
        setChanged();
        sendData();
    }

    // [신규] 조준기로 타겟을 추가/제거하는 메서드
    public void toggleTarget(BlockPos targetPos, Player player) {
        if (designatedTargets.contains(targetPos)) {
            designatedTargets.remove(targetPos);
            player.displayClientMessage(Component.translatable("mycreateaddon.node_designator.target_removed", targetPos.toShortString()).withStyle(ChatFormatting.RED), true);
        } else {
            if (currentMode == OperatingMode.WIDE_BEAM && designatedTargets.size() >= 4) {
                player.displayClientMessage(Component.translatable("mycreateaddon.node_designator.target_limit").withStyle(ChatFormatting.YELLOW), true);
                return;
            }
            if (currentMode != OperatingMode.WIDE_BEAM && !designatedTargets.isEmpty()) {
                // 단일 타겟 모드에서는 기존 타겟을 교체
                designatedTargets.clear();
            }
            designatedTargets.add(targetPos);
            player.displayClientMessage(Component.translatable("mycreateaddon.node_designator.target_set", targetPos.toShortString()).withStyle(ChatFormatting.GREEN), true);
        }
        updateActiveTargets(); // 즉시 타겟 재탐색
        setChanged();
        sendData();
    }
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // Create의 기본 운동 정보(Stress 등)를 표시하지 않으려면 이 라인을 제거
        // super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        tooltip.add(Component.literal("")); // 구분선
        tooltip.add(Component.translatable("goggle.mycreateaddon.laser_head.header").withStyle(ChatFormatting.GOLD));

        // 현재 모드 표시
        tooltip.add(Component.literal(" ")
                .append(getMode().getDisplayName().copy().withStyle(ChatFormatting.RED)));

        // 공명 모드일 때, 공명 대상 표시
        if (getMode() == OperatingMode.RESONANCE) {
            if (getCore() != null) {
                Optional<Item> filter = getCore().getResonatorFilter();
                Component targetName = filter.map(Item::getDescription)
                        .orElse(Component.translatable("goggle.mycreateaddon.laser_head.no_resonance_target").withStyle(ChatFormatting.DARK_GRAY));

                tooltip.add(Component.literal("  - ")
                        .append(Component.translatable("goggle.mycreateaddon.laser_head.resonance_target").withStyle(ChatFormatting.GRAY))
                        .append(targetName));
            }
        }

        // 지정된 타겟 좌표 표시
        if (!designatedTargets.isEmpty()) {
            tooltip.add(Component.literal(" ")
                    .append(Component.translatable("goggle.mycreateaddon.laser_head.designated_targets").withStyle(ChatFormatting.AQUA)));
            for (int i = 0; i < Math.min(designatedTargets.size(), 4) ; i++) {
                BlockPos pos = designatedTargets.get(i);
                tooltip.add(Component.literal("   > " + pos.toShortString()).withStyle(ChatFormatting.GRAY));
            }
            if (designatedTargets.size() > 4) {
                tooltip.add(Component.literal("   ...").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Component.literal(" ")
                    .append(Component.translatable("goggle.mycreateaddon.laser_head.no_targets").withStyle(ChatFormatting.DARK_GRAY)));
        }

        return true; // 툴팁을 추가했음을 알림
    }

    // [헬퍼] 코어에 접근하는 메서드
    public DrillCoreBlockEntity getCore() {
        if (level == null) return null;
        if (getBlockState().getBlock() instanceof LaserDrillHeadBlock) {
            Direction facing = getBlockState().getValue(LaserDrillHeadBlock.FACING);
            BlockPos corePos = getBlockPos().relative(facing.getOpposite());
            if (level.getBlockEntity(corePos) instanceof DrillCoreBlockEntity core) {
                return core;
            }
        }
        return null;
    }

    // [핵심 수정] 실제 빔이 나갈 타겟(activeTargets)을 결정하는 중앙 로직
    public void updateActiveTargets() {
        if (level == null) return;

        List<BlockPos> newActiveTargets = new ArrayList<>();
        if (!designatedTargets.isEmpty()) {
            // 지정된 타겟이 있으면 그것을 사용
            newActiveTargets.addAll(designatedTargets);
        } else {
            // 지정 타겟이 없고, 광역 채굴 모드일 때만 자동 탐색
            if (currentMode == OperatingMode.WIDE_BEAM) {
                newActiveTargets.addAll(findClosestTargets(4));
            }
        }

        if (!this.activeTargets.equals(newActiveTargets)) {
            this.activeTargets = newActiveTargets;
            setChanged();
            sendData(); // 클라이언트에 변경사항 전송
        }
    }


    // [신규] 코어가 호출하여 속도를 업데이트하는 메서드
    public void updateVisualSpeed(float speed) {
        if (this.visualSpeed == speed) return;
        this.visualSpeed = speed;
        setChanged();
        sendData();
    }

    // [신규] Visual이 사용할 getter
    public float getVisualSpeed() {
        return visualSpeed;
    }
    public OperatingMode getMode() {
        return this.currentMode;
    }

    public void onDrillTick(DrillCoreBlockEntity core) {
        if (level == null || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // 매 틱 타겟을 다시 확인하여 유효하지 않은 타겟 제거 (예: 노드가 중화제로 파괴된 경우)
        designatedTargets.removeIf(pos -> !(level.getBlockState(pos).getBlock() instanceof OreNodeBlock));
        updateActiveTargets();

        if (activeTargets.isEmpty()) {
            return; // 쏠 타겟이 없으면 아무것도 안 함
        }

        switch (currentMode) {
            case WIDE_BEAM -> handleWideBeamMode(core, serverLevel);
            case RESONANCE -> handleResonanceMode(core, activeTargets.getFirst());
            case DECOMPOSITION -> handleDecompositionMode(core, activeTargets.getFirst());
        }
    }

    private void handleWideBeamMode(DrillCoreBlockEntity core, ServerLevel serverLevel) {
        for (BlockPos targetPos : activeTargets) {
            if (core.consumeEnergy(ENERGY_PER_MINING_TICK, false) == ENERGY_PER_MINING_TICK) {
                if (serverLevel.getBlockEntity(targetPos) instanceof OreNodeBlockEntity nodeBE) {
                    List<ItemStack> drops = core.mineNode(nodeBE, 20);
                    drops.forEach(core::processMinedItem);
                }
            }
        }
    }
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            LaserBeamRenderer.addLaser(this);
        }
    }

    // [유지] 청크 언로드 시에도 렌더러에서 제거
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && level.isClientSide) {
            LaserBeamRenderer.removeLaser(getBlockPos());
        }
    }
    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && level.isClientSide) {
            LaserBeamRenderer.removeLaser(getBlockPos());
        }
    }

    private void handleResonanceMode(DrillCoreBlockEntity core, BlockPos targetPos) {
        this.activeTargets = List.of(targetPos); // 렌더링용 타겟 업데이트

        Optional<Item> filter = core.getResonatorFilter();
        if (filter.isEmpty()) {
            return; // 필터가 없으면 작동 안 함
        }

        if (core.consumeEnergy(ENERGY_PER_MINING_TICK, false) == ENERGY_PER_MINING_TICK) {
            assert level != null;
            if (level.getBlockEntity(targetPos) instanceof OreNodeBlockEntity nodeBE) {
                // 특정 자원만 채굴하도록 코어에 요청
                List<ItemStack> drops = core.mineSpecificNode(nodeBE, 20, 0, false, filter.get());
                drops.forEach(core::processMinedItem);
            }
        }
    }

    private void handleDecompositionMode(DrillCoreBlockEntity core, BlockPos targetPos) {
        this.activeTargets = List.of(targetPos); // 렌더링용 타겟 업데이트

        if (core.consumeEnergy(ENERGY_PER_DECOMPOSITION_TICK, false) == ENERGY_PER_DECOMPOSITION_TICK) {
            decompositionProgress++;
            setChanged();

            if (decompositionProgress >= DECOMPOSITION_TIME_TICKS) {
                assert level != null;
                if (level.getBlockEntity(targetPos) instanceof OreNodeBlockEntity nodeBE) {
                    // 1. 정보 추출
                    CompoundTag nodeData = new CompoundTag();
                    nodeBE.saveAdditional(nodeData, level.registryAccess());

                    // 2. 아이템 생성 및 NBT 저장
                    ItemStack dataItem = new ItemStack(MyAddonItems.UNFINISHED_NODE_DATA.get());
                    CompoundTag itemNbt = new CompoundTag();
                    if (nodeData.contains("Composition")) itemNbt.put("Composition", Objects.requireNonNull(nodeData.get("Composition")));
                    if (nodeData.contains("CurrentYield")) itemNbt.putFloat("Yield", nodeData.getFloat("CurrentYield"));
                    dataItem.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(itemNbt));

                    // 3. 노드 파괴 및 아이템 드롭
                    level.destroyBlock(targetPos, false);
                    level.addFreshEntity(new ItemEntity(level, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, dataItem));
                }
                decompositionProgress = 0;
                setChanged();
            }
        } else {
            // 에너지가 부족하면 진행도 초기화
            if (decompositionProgress > 0) {
                decompositionProgress = 0;
                setChanged();
            }
        }
    }

    private List<BlockPos> findClosestTargets(int limit) {
        if (level == null) return List.of();
        BlockPos headPos = getBlockPos();
        int range = 16;

        return BlockPos.betweenClosedStream(headPos.offset(-range, -range, -range), headPos.offset(range, range, range))
                .filter(pos -> level.getBlockEntity(pos) instanceof OreNodeBlockEntity)
                .map(BlockPos::immutable)
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(headPos)))
                .limit(limit)
                .toList();
    }



    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean forClient) {
        super.write(compound, registries, forClient);
        compound.putString("Mode", currentMode.name());
        compound.putInt("DecompositionProgress", decompositionProgress);

        // [핵심 수정] BlockPos 리스트를 IntArrayTag 리스트로 저장합니다.
        ListTag designatedList = new ListTag();
        for (BlockPos pos : designatedTargets) {
            designatedList.add(new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        }
        compound.put("DesignatedTargets", designatedList);

        if (forClient) {
            compound.putFloat("VisualSpeed", visualSpeed);
            ListTag activeList = new ListTag();
            for (BlockPos pos : activeTargets) {
                activeList.add(new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
            }
            compound.put("ActiveTargets", activeList);
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean forClient) {
        super.read(compound, registries, forClient);
        try {
            currentMode = OperatingMode.valueOf(compound.getString("Mode"));
        } catch (IllegalArgumentException e) {
            currentMode = OperatingMode.WIDE_BEAM;
        }
        decompositionProgress = compound.getInt("DecompositionProgress");

        // [핵심 수정] IntArrayTag 리스트에서 BlockPos를 읽어옵니다.
        designatedTargets.clear();
        if (compound.contains("DesignatedTargets", 9)) { // 11 = IntArrayTag
            ListTag designatedList = compound.getList("DesignatedTargets", 11);
            for (int i = 0; i < designatedList.size(); i++) {
                int[] posArray = designatedList.getIntArray(i);
                if (posArray.length == 3) {
                    designatedTargets.add(new BlockPos(posArray[0], posArray[1], posArray[2]));
                }
            }
        }

        if (forClient) {
            visualSpeed = compound.getFloat("VisualSpeed");
            activeTargets.clear();
            if (compound.contains("ActiveTargets", 9)) {
                ListTag activeList = compound.getList("ActiveTargets", 11);
                for (int i = 0; i < activeList.size(); i++) {
                    int[] posArray = activeList.getIntArray(i);
                    if (posArray.length == 3) {
                        activeTargets.add(new BlockPos(posArray[0], posArray[1], posArray[2]));
                    }
                }
            }
        }
    }
}