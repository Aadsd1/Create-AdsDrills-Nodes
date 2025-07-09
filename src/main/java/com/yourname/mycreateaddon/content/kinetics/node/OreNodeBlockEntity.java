package com.yourname.mycreateaddon.content.kinetics.node;


import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.yourname.mycreateaddon.registry.MyAddonItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.*;

// IHaveGoggleInformation 인터페이스를 구현합니다.
public class OreNodeBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    // --- 데이터 필드 ---
    // --- 데이터 필드 ---
    // [핵심 수정] 저장 타입을 Map<Item, Float>에서 Map<Block, Float>으로 변경
    // [유지] 아이템 기반의 성분 맵
    private Map<Item, Float> resourceComposition = new HashMap<>();
    // [추가] 아이템-블록 매핑 맵
    private Map<Item, Block> itemToBlockMap = new HashMap<>();

    private FluidStack fluidContent = FluidStack.EMPTY;
    private int maxFluidCapacity;
    private float currentFluidAmount; // 정밀한 양 조절을 위해 float 유지


    private int miningProgress;
    private ResourceLocation backgroundBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.STONE);
    private ResourceLocation oreBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.IRON_ORE);

    // --- 노드 특성 필드 ---
    private int maxYield;
    private float currentYield; // float으로 변경하여 재생력의 미세한 증가를 반영
    private float hardness;
    private float richness;
    private float regeneration;

    // [추가] 균열 상태 필드
    private boolean cracked = false;
    // [추가] 균열 상태가 지속될 시간을 틱 단위로 저장할 타이머
    private int crackTimer = 0;
    private static final int CRACK_DURATION_TICKS = 100; // 30초 (20틱 * 30초)

    public static final ModelProperty<OreNodeBlockEntity> MODEL_DATA_PROPERTY = new ModelProperty<>();

    public OreNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // [신규] 수압 헤드 등이 노드의 전체 구성을 알 수 있도록 public getter를 추가합니다.
    public Map<Item, Float> getResourceComposition() {
        return this.resourceComposition;
    }
    // [신규] 특정 아이템만 채굴하는 메서드
    public List<ItemStack> applySpecificMiningTick(int miningAmount, int fortuneLevel, boolean hasSilkTouch, Item specificItemToMine) {
        if (!(level instanceof ServerLevel serverLevel) || currentYield <= 0 || !resourceComposition.containsKey(specificItemToMine)) {
            return Collections.emptyList();
        }

        float effectiveMiningAmount = miningAmount / getHardness();
        this.miningProgress += (int) effectiveMiningAmount;
        int miningResistance = 1000;

        if (this.miningProgress >= miningResistance) {
            this.miningProgress -= miningResistance;
            this.currentYield--;
            setChanged();
            sendData();

            // 실크터치는 여기서도 작동
            if (hasSilkTouch) {
                Block blockToDrop = this.itemToBlockMap.get(specificItemToMine);
                return Collections.singletonList(new ItemStack(Objects.requireNonNullElseGet(blockToDrop, () -> BuiltInRegistries.BLOCK.get(oreBlockId))));
            }

            // 행운/풍부함 로직 적용
            ItemStack finalDrop = new ItemStack(specificItemToMine);
            int dropCount = 1;
            if (fortuneLevel > 0) {
                RandomSource rand = serverLevel.getRandom();
                for (int i = 0; i < fortuneLevel; i++) {
                    if (rand.nextInt(fortuneLevel + 2) > 1) dropCount++;
                }
            }
            finalDrop.setCount(dropCount);
            if (this.richness > 1.0f && serverLevel.getRandom().nextFloat() < (this.richness - 1.0f)) {
                finalDrop.grow(finalDrop.getCount());
            }

            return Collections.singletonList(finalDrop);
        }

        return Collections.emptyList();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }
    // [추가] 균열 상태 getter
    public boolean isCracked() {
        return cracked;
    }


    public void setCracked(boolean cracked) {
        if (this.cracked == cracked) return;
        this.cracked = cracked;

        if (cracked) {
            // 균열 상태가 되면 타이머를 설정
            this.crackTimer = CRACK_DURATION_TICKS;
        } else {
            // 균열 상태가 풀리면 타이머를 리셋
            this.crackTimer = 0;
        }

        // [핵심 수정] sendData()와 함께, 블록 업데이트를 요청하여
        // 클라이언트 렌더링 및 상태를 확실하게 갱신합니다.
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    // --- 특성 Getter ---
    public float getHardness() {
        return Math.max(0.1f, this.hardness); // 0으로 나누는 것을 방지
    }

    public ResourceLocation getBackgroundBlockId() {
        return backgroundBlockId;
    }

    public ResourceLocation getOreBlockId() {
        return oreBlockId;
    }

    // --- [신규] 유체 관련 Getter/Setter ---
    public FluidStack getFluid() {
        if (fluidContent.isEmpty() || currentFluidAmount <= 0) {
            return FluidStack.EMPTY;
        }
        // 실제 유체 양을 반영하여 반환
        return new FluidStack(fluidContent.getFluid(), (int) currentFluidAmount);
    }

    public int getMaxFluidCapacity() {
        return maxFluidCapacity;
    }
    /**
     * 외부(펌프 헤드)에서 유체를 추출하는 메서드
     * @param amountToDrain 추출할 양
     * @return 실제로 추출된 유체 스택
     */
    public FluidStack drainFluid(int amountToDrain) {
        if (fluidContent.isEmpty() || currentFluidAmount <= 0) {
            return FluidStack.EMPTY;
        }

        int actualDrainAmount = Math.min(amountToDrain, (int) currentFluidAmount);
        this.currentFluidAmount -= actualDrainAmount;

        setChanged();
        sendData();

        return new FluidStack(fluidContent.getFluid(), actualDrainAmount);
    }


    public ResourceLocation getRepresentativeOreItemId() {
        return resourceComposition.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> BuiltInRegistries.ITEM.getKey(entry.getKey()))
                .orElse(BuiltInRegistries.ITEM.getKey(Items.RAW_IRON));
    }
    // --- 초기화 메서드 ---
    // [수정] 파라미터 타입을 Map<Block, Float>으로 변경

    public void configure(Map<Item, Float> composition, Map<Item, Block> itemToBlockMap, int maxYield, float hardness, float richness, float regeneration, Block backgroundBlock, Block representativeOreBlock, FluidStack fluid, int fluidCapacity) {        this.resourceComposition = composition;
        this.itemToBlockMap = itemToBlockMap; // 새 맵 저장
        this.maxYield = maxYield;
        this.currentYield = maxYield;
        this.hardness = hardness;
        this.richness = richness;
        this.regeneration = regeneration;
        this.backgroundBlockId = BuiltInRegistries.BLOCK.getKey(backgroundBlock);
        this.oreBlockId = BuiltInRegistries.BLOCK.getKey(representativeOreBlock);
        this.fluidContent = fluid.copy();
        this.maxFluidCapacity = fluidCapacity;
        this.currentFluidAmount = fluidCapacity; // 처음엔 가득 차 있도록 설정
        setChanged();
        sendData();
    }
    // --- 핵심 로직 ---



    // [수정] 실크터치 로직을 새 맵을 사용하도록 변경
    public List<ItemStack> applyMiningTick(int miningAmount, int fortuneLevel, boolean hasSilkTouch) {
        if (!(level instanceof ServerLevel serverLevel) || currentYield <= 0) {
            return Collections.emptyList();
        }

        float effectiveMiningAmount = miningAmount / getHardness();
        this.miningProgress += (int) effectiveMiningAmount;
        int miningResistance = 1000;

        if (this.miningProgress >= miningResistance) {
            this.miningProgress -= miningResistance;
            this.currentYield--;
            setChanged();
            sendData();

            if (this.cracked) {
                return Collections.singletonList(new ItemStack(MyAddonItems.CRACKED_IRON_CHUNK.get()));
            }

            // --- [핵심 수정] 확률 계산 로직 ---

            // 1. 맵의 엔트리를 리스트로 변환
            List<Map.Entry<Item, Float>> sortedComposition = new ArrayList<>(resourceComposition.entrySet());

            // 2. 아이템 ID를 기준으로 리스트를 정렬하여 항상 동일한 순서를 보장
            sortedComposition.sort(Comparator.comparing(entry -> BuiltInRegistries.ITEM.getKey(entry.getKey())));

            Item itemToDrop = null;
            double random = serverLevel.getRandom().nextDouble();
            float cumulative = 0f;

            // 3. 정렬된 리스트를 순회하며 확률 계산
            for (Map.Entry<Item, Float> entry : sortedComposition) {
                cumulative += entry.getValue();
                if (random < cumulative) {
                    itemToDrop = entry.getKey();
                    break;
                }
            }

            if (itemToDrop == null) {
                // 만약의 경우(부동소수점 오류 등)를 대비해, 리스트의 첫 번째 아이템이라도 선택
                if (!sortedComposition.isEmpty()) {
                    itemToDrop = sortedComposition.getFirst().getKey();
                } else {
                    return Collections.emptyList(); // 노드 구성이 비어있으면 아무것도 드롭하지 않음
                }
            }

            // --- 1. 실크터치 처리 ---
            if (hasSilkTouch) {
                // 선택된 아이템에 해당하는 블록을 맵에서 찾아 드롭
                Block blockToDrop = this.itemToBlockMap.get(itemToDrop);
                return Collections.singletonList(new ItemStack(Objects.requireNonNullElseGet(blockToDrop, () -> BuiltInRegistries.BLOCK.get(oreBlockId))));
                // 만약 맵에 없다면 (오류 상황), 대표 블록이라도 드롭
            }

            // --- 2. 일반 채굴 (아이템 직접 드롭) ---
            ItemStack finalDrop = new ItemStack(itemToDrop);

            // 행운 로직
            int dropCount = 1;
            if (fortuneLevel > 0) {
                RandomSource rand = serverLevel.getRandom();
                for (int i = 0; i < fortuneLevel; i++) {
                    if (rand.nextInt(fortuneLevel + 2) > 1) {
                        dropCount++;
                    }
                }
            }
            finalDrop.setCount(dropCount);

            // 풍부함 로직
            if (this.richness > 1.0f && serverLevel.getRandom().nextFloat() < (this.richness - 1.0f)) {
                finalDrop.grow(finalDrop.getCount());
            }

            return Collections.singletonList(finalDrop);
        }

        return Collections.emptyList();
    }


    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide) {
            // [핵심 수정] 재생력 로직 개선

            if (this.regeneration > 0) {
                float baseRegenPerSecond = this.regeneration * 20;
                float finalRegenMultiplier = 0.8f + (this.richness * 0.8f);
                float finalRegenPerTick = (baseRegenPerSecond * finalRegenMultiplier) / 20f;

                if (this.currentYield < this.maxYield) {
                    this.currentYield = Math.min(this.maxYield, this.currentYield + finalRegenPerTick);
                }
            }

            // --- [핵심 수정] 유체 재생 로직 분리 ---
            if (this.maxFluidCapacity > 0 && this.currentFluidAmount < this.maxFluidCapacity) {
                // 1. 기본 초당 재생량 (5mb/tick = 100mb/sec)
                float baseFluidRegenPerTick = 5.0f;

                // 2. 풍부함(richness)과 재생력(regeneration)에 따른 보너스 계산
                // richness는 최대 +50% 보너스, regeneration은 최대 +50% 보너스를 줌
                float bonusMultiplier = (this.richness - 0.8f) + (this.regeneration / 0.005f * 0.5f);

                // 3. 최종 재생량 계산 (기본값에 보너스를 더함)
                float finalFluidRegenPerTick = baseFluidRegenPerTick * (1.0f + bonusMultiplier);

                this.currentFluidAmount = Math.min(this.maxFluidCapacity, this.currentFluidAmount + finalFluidRegenPerTick);
            }

            // 1초에 한 번만 동기화
            if(level.getGameTime() % 20 == 0) {
                setChanged();
            }


            // 균열 타이머 처리
            if (this.cracked && this.crackTimer > 0) {
                this.crackTimer--;

                // [핵심 수정] 타이머가 1초(20틱) 지날 때마다 클라이언트에 동기화 신호를 보냅니다.
                // 매 틱마다 보내면 부하가 크므로, 1초에 한 번만 갱신해도 충분합니다.
                if (this.crackTimer % 20 == 0) {
                    setChanged();
                    sendData(); // sendData()는 setChanged()와 함께 쓰여 동기화를 확실하게 합니다.
                }

                if (this.crackTimer == 0) {
                    // 타이머가 다 되면 균열 상태를 해제 (이때 setCracked 내부에서 sendBlockUpdated가 호출됨)
                    setCracked(false);
                }
            }
        }
    }


    // --- NBT 및 동기화 ---
    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("MiningProgress", this.miningProgress);
        tag.putString("BackgroundBlock", backgroundBlockId.toString());
        tag.putString("OreBlock", oreBlockId.toString());

        tag.putInt("MaxYield", this.maxYield);
        tag.putFloat("CurrentYield", this.currentYield);
        tag.putFloat("Hardness", this.hardness);
        tag.putFloat("Richness", this.richness);
        tag.putFloat("Regeneration", this.regeneration);


        if (cracked) {
            tag.putBoolean("Cracked", true);
            // [추가] 남은 타이머 시간도 저장
            tag.putInt("CrackTimer", crackTimer);
        }
        ListTag compositionList = new ListTag();
        for (Map.Entry<Item, Float> entry : resourceComposition.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Item", BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
            entryTag.putFloat("Ratio", entry.getValue());
            compositionList.add(entryTag);
        }
        tag.put("Composition", compositionList);

        // 새 맵 저장
        ListTag itemToBlockList = new ListTag();
        for (Map.Entry<Item, Block> entry : itemToBlockMap.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Item", BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
            entryTag.putString("Block", BuiltInRegistries.BLOCK.getKey(entry.getValue()).toString());
            itemToBlockList.add(entryTag);
        }
        tag.put("ItemToBlockMap", itemToBlockList);


        if (!fluidContent.isEmpty()) {
            tag.put("FluidContent", fluidContent.save(registries));
            tag.putInt("MaxFluidCapacity", maxFluidCapacity);
            tag.putFloat("CurrentFluidAmount", currentFluidAmount);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.miningProgress = tag.getInt("MiningProgress");
        this.backgroundBlockId = ResourceLocation.tryParse(tag.getString("BackgroundBlock"));
        if (this.backgroundBlockId == null) this.backgroundBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.STONE);
        this.oreBlockId = ResourceLocation.tryParse(tag.getString("OreBlock"));
        if (this.oreBlockId == null) this.oreBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.IRON_ORE);

        this.maxYield = tag.getInt("MaxYield");
        this.currentYield = tag.getFloat("CurrentYield");
        this.hardness = tag.getFloat("Hardness");
        this.richness = tag.getFloat("Richness");
        this.regeneration = tag.getFloat("Regeneration");

        cracked = tag.getBoolean("Cracked");
        if (cracked) {
            // [추가] 타이머 시간 로드
            crackTimer = tag.getInt("CrackTimer");
        }

        this.resourceComposition.clear();
        if (tag.contains("Composition", 9)) {
            ListTag compositionList = tag.getList("Composition", 10);
            for (int i = 0; i < compositionList.size(); i++) {
                CompoundTag entryTag = compositionList.getCompound(i);
                BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(entryTag.getString("Item")))
                        .ifPresent(item -> {
                            float ratio = entryTag.getFloat("Ratio");
                            this.resourceComposition.put(item, ratio);
                        });
            }
        }
        // 새 맵 로드
        this.itemToBlockMap.clear();
        if (tag.contains("ItemToBlockMap", 9)) {
            ListTag itemToBlockList = tag.getList("ItemToBlockMap", 10);
            for (int i = 0; i < itemToBlockList.size(); i++) {
                CompoundTag entryTag = itemToBlockList.getCompound(i);
                Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(entryTag.getString("Item")));
                Optional<Block> blockOpt = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(entryTag.getString("Block")));
                if (itemOpt.isPresent() && blockOpt.isPresent()) {
                    this.itemToBlockMap.put(itemOpt.get(), blockOpt.get());
                }
            }
        }

        if (tag.contains("FluidContent")) {
            this.fluidContent = FluidStack.parse(registries, tag.getCompound("FluidContent")).orElse(FluidStack.EMPTY);
            this.maxFluidCapacity = tag.getInt("MaxFluidCapacity");
            this.currentFluidAmount = tag.getFloat("CurrentFluidAmount");
        } else {
            this.fluidContent = FluidStack.EMPTY;
        }

        if (clientPacket) {
            requestModelDataUpdate();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }
    }

    // --- Goggle 툴팁 ---
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Component header = Component.translatable("goggle.mycreateaddon.ore_node.header").withStyle(ChatFormatting.GOLD);
        tooltip.add(Component.literal("    ").append(header));


        if (!resourceComposition.isEmpty()) {
            Component compositionHeader = Component.translatable("goggle.mycreateaddon.ore_node.composition").withStyle(ChatFormatting.GRAY);
            tooltip.add(Component.literal(" ").append(compositionHeader));

            List<Map.Entry<Item, Float>> sortedComposition = new ArrayList<>(resourceComposition.entrySet());
            sortedComposition.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            for (Map.Entry<Item, Float> entry : sortedComposition) {
                Component compositionLine = Component.literal("  - ")
                        .append(entry.getKey().getDescription().copy().withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(String.format(": %.1f%%", entry.getValue() * 100)).withStyle(ChatFormatting.DARK_AQUA));
                tooltip.add(compositionLine);
            }
        }

        tooltip.add(Component.literal("")); // 구분선

        // 매장량
        tooltip.add(Component.literal(" ")
                .append(Component.translatable("goggle.mycreateaddon.ore_node.yield", String.format("%d / %d", (int)this.currentYield, this.maxYield))
                        .withStyle(ChatFormatting.GRAY)));

        // 특성
        tooltip.add(Component.literal("")); // 구분선

        // --- [핵심 수정] 단단함(Hardness) 표시 ---
        MutableComponent hardnessLine = Component.literal(" ")
                .append(Component.translatable("goggle.mycreateaddon.ore_node.hardness").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": "));

        if (this.hardness < 0.75f) {
            hardnessLine.append(Component.translatable("goggle.mycreateaddon.ore_node.hardness.brittle").withStyle(ChatFormatting.GREEN));
        } else if (this.hardness < 1.25f) {
            hardnessLine.append(Component.translatable("goggle.mycreateaddon.ore_node.hardness.normal").withStyle(ChatFormatting.GRAY));
        } else if (this.hardness < 1.75f) {
            hardnessLine.append(Component.translatable("goggle.mycreateaddon.ore_node.hardness.tough").withStyle(ChatFormatting.GOLD));
        } else {
            hardnessLine.append(Component.translatable("goggle.mycreateaddon.ore_node.hardness.resilient").withStyle(ChatFormatting.RED));
        }
        tooltip.add(hardnessLine);

        // --- [핵심 수정] 풍부함(Richness) 표시 ---
        MutableComponent richnessLine = Component.literal(" ")
                .append(Component.translatable("goggle.mycreateaddon.ore_node.richness").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": "));

        if (this.richness < 1.0f) {
            richnessLine.append(Component.translatable("goggle.mycreateaddon.ore_node.richness.sparse").withStyle(ChatFormatting.DARK_GRAY));
        } else if (this.richness < 1.2f) {
            richnessLine.append(Component.translatable("goggle.mycreateaddon.ore_node.richness.normal").withStyle(ChatFormatting.GRAY));
        } else if (this.richness < 1.4f) {
            richnessLine.append(Component.translatable("goggle.mycreateaddon.ore_node.richness.rich").withStyle(ChatFormatting.AQUA));
        } else {
            richnessLine.append(Component.translatable("goggle.mycreateaddon.ore_node.richness.bountiful").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        }
        tooltip.add(richnessLine);

        // 재생력 (기존과 동일, 또는 단어로 변경 가능)
        if (this.regeneration > 0) {
            MutableComponent regenLine = Component.literal(" ")
                    .append(Component.translatable("goggle.mycreateaddon.ore_node.regeneration").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": "));

            if (this.regeneration * 20 > 0.015f) {
                regenLine.append(Component.translatable("goggle.mycreateaddon.ore_node.regeneration.strong").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            } else {
                regenLine.append(Component.translatable("goggle.mycreateaddon.ore_node.regeneration.weak").withStyle(ChatFormatting.DARK_PURPLE));
            }
            tooltip.add(regenLine);
        }

        // 플레이어가 Sneaking(웅크리기) 상태일 때만 상세 수치를 보여줍니다.
        if (isPlayerSneaking) {
            tooltip.add(Component.literal("  ")
                    .append(Component.literal(String.format("(H:%.2f, R:%.2f, G:%.5f/s)", this.hardness, this.richness, this.regeneration * 20))
                            .withStyle(ChatFormatting.DARK_GRAY)));
        }
        // [신규] 유체 정보 툴팁 추가
        tooltip.add(Component.literal("")); // 구분선
        MutableComponent fluidHeader = Component.translatable("goggle.mycreateaddon.drill_core.storage.fluid").withStyle(ChatFormatting.AQUA);
        tooltip.add(Component.literal(" ").append(fluidHeader));

        if (fluidContent.isEmpty()) {
            tooltip.add(Component.literal("  - ").append(Component.translatable("goggle.mycreateaddon.ore_node.fluid.empty").withStyle(ChatFormatting.DARK_GRAY)));
        } else {
            MutableComponent fluidLine = Component.literal("  - ")
                    .append(fluidContent.getHoverName().copy().withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(String.format(": %,d / %,d mb", (int)currentFluidAmount, maxFluidCapacity)).withStyle(ChatFormatting.GRAY));
            tooltip.add(fluidLine);
        }

        if (cracked) {
            // [수정] 남은 시간을 초 단위로 표시
            int secondsLeft = crackTimer / 20;
            tooltip.add(Component.literal(" ").append(Component.literal("[Cracked] (" + secondsLeft + "s left)").withStyle(ChatFormatting.YELLOW)));
        }
        return true;
    }

    @Nonnull
    @Override
    public ModelData getModelData() {
        // 이 BlockEntity는 자신의 정보만 제공합니다.
        return ModelData.builder().with(MODEL_DATA_PROPERTY, this).build();
    }
}