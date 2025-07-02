package com.yourname.mycreateaddon.content.kinetics.node;


import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
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

    public static final ModelProperty<OreNodeBlockEntity> MODEL_DATA_PROPERTY = new ModelProperty<>();

    public OreNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
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

    // [핵심 수정] 메서드의 반환 타입을 void에서 ItemStack으로 변경합니다.
    public ItemStack applyMiningTick(int miningAmount) {
        if (level == null || level.isClientSide || currentYield <= 0 || resourceComposition.isEmpty()) {
            // [수정] 아이템 스택이 비어있음을 의미하는 ItemStack.EMPTY를 반환합니다.
            return ItemStack.EMPTY;
        }

        float effectiveMiningAmount = miningAmount / getHardness();
        this.miningProgress += effectiveMiningAmount;

        int miningResistance = 1000;

        if (this.miningProgress >= miningResistance) {
            this.miningProgress -= miningResistance;
            this.currentYield--;

            double random = level.getRandom().nextDouble();
            float cumulative = 0f;
            for (Map.Entry<Item, Float> entry : resourceComposition.entrySet()) {
                cumulative += entry.getValue();
                if (random < cumulative) {
                    ItemStack yieldedStack = new ItemStack(entry.getKey());

                    if (this.richness > 1.0f && level.getRandom().nextFloat() < (this.richness - 1.0f)) {
                        yieldedStack.setCount(2);
                    }

                    setChanged();
                    sendData();

                    // [추가] 생성된 아이템 스택을 반환합니다.
                    return yieldedStack;
                }
            }
        }

        // 채굴에 실패했거나, 아직 진행도가 부족하면 빈 스택을 반환합니다.
        return ItemStack.EMPTY;
    }
    @Override
    public void tick() {
        super.tick();
        // 재생력 특성 처리 (서버에서만)
        if (level != null && !level.isClientSide && this.regeneration > 0) {
            if (this.currentYield < this.maxYield) {
                this.currentYield = Math.min(this.maxYield, this.currentYield + this.regeneration);
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

        return true;
    }

    @Nonnull
    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(MODEL_DATA_PROPERTY, this).build();
    }
}