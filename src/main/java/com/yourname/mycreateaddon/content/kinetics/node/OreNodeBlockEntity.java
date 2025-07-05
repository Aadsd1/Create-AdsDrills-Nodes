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
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// IHaveGoggleInformation 인터페이스를 구현합니다.
public class OreNodeBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    // --- 데이터 필드 ---
    private Map<Item, Float> resourceComposition = new HashMap<>();
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

    public ResourceLocation getRepresentativeOreItemId() {
        return resourceComposition.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> BuiltInRegistries.ITEM.getKey(entry.getKey()))
                .orElse(BuiltInRegistries.ITEM.getKey(Items.RAW_IRON));
    }

    // --- 초기화 메서드 ---
    public void configure(Map<Item, Float> composition, int maxYield, float hardness, float richness, float regeneration, Block backgroundBlock, Block representativeOreBlock) {
        this.resourceComposition = composition;
        this.maxYield = maxYield;
        this.currentYield = maxYield;
        this.hardness = hardness;
        this.richness = richness;
        this.regeneration = regeneration;
        this.backgroundBlockId = BuiltInRegistries.BLOCK.getKey(backgroundBlock);
        this.oreBlockId = BuiltInRegistries.BLOCK.getKey(representativeOreBlock);
        setChanged();
        sendData();
    }

    // --- 핵심 로직 ---



    public ItemStack applyMiningTick(int miningAmount, int fortuneLevel, boolean hasSilkTouch) {
        if (level == null || level.isClientSide() || currentYield <= 0) {
            return ItemStack.EMPTY;
        }

        // ... 채굴 진행도 계산 ...
        float effectiveMiningAmount = miningAmount / getHardness();
        this.miningProgress += (int) effectiveMiningAmount;
        int miningResistance = 1000;

        if (this.miningProgress >= miningResistance) {
            this.miningProgress -= miningResistance;
            this.currentYield--;
            setChanged();
            sendData();
            if (this.cracked) {
                // 균열된 노드에서는 '균열된 철 덩어리'를 드롭
                // 여기서는 예시로 철만 사용했지만, 나중에는 노드의 주 구성 광물에 따라 다른 종류의 '균열된 덩어리'를 주도록 확장할 수 있음
                return new ItemStack(MyAddonItems.CRACKED_IRON_CHUNK.get());
            } else {
                // --- 1. 실크터치 처리 ---
                if (hasSilkTouch) {
                    Block oreBlock = BuiltInRegistries.BLOCK.get(this.oreBlockId);
                    return oreBlock != Blocks.AIR ? new ItemStack(oreBlock) : ItemStack.EMPTY;
                }

                // --- 2. 기본 아이템 결정 ---
                ItemStack baseDrop = ItemStack.EMPTY;
                double random = level.getRandom().nextDouble();
                float cumulative = 0f;
                for (Map.Entry<Item, Float> entry : resourceComposition.entrySet()) {
                    cumulative += entry.getValue();
                    if (random < cumulative) {
                        baseDrop = new ItemStack(entry.getKey());
                        break;
                    }
                }
                if (baseDrop.isEmpty()) return ItemStack.EMPTY;

                // --- 3. 행운 로직 직접 구현 ---
                int dropCount = 1;
                if (fortuneLevel > 0) {
                    RandomSource rand = level.getRandom();
                    // 행운 레벨만큼 추가 기회
                    for (int i = 0; i < fortuneLevel; i++) {
                        // 바닐라의 '균등 분포 추가 드롭' 공식을 단순화
                        // 레벨당 대략 1/(레벨+2) 확률로 1개씩 추가
                        if (rand.nextInt(fortuneLevel + 2) > 1) {
                            dropCount++;
                        }
                    }
                }
                baseDrop.setCount(dropCount);

                // --- 4. 풍부함(Richness) 특성 적용 ---
                if (this.richness > 1.0f && level.getRandom().nextFloat() < (this.richness - 1.0f)) {
                    baseDrop.grow(baseDrop.getCount()); // 2배로 만듦
                }

                return baseDrop;


            }
        }

        return ItemStack.EMPTY;
    }


    // [수정] tick 메서드에 타이머 로직 추가
    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide) {
            // 재생력 처리
            if (this.regeneration > 0 && this.currentYield < this.maxYield) {
                this.currentYield = Math.min(this.maxYield, this.currentYield + this.regeneration);
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