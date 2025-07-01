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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    private Map<Item, Float> resourceComposition = new HashMap<>();
    private int totalYield;
    private int miningProgress;
    private int miningResistance = 200;

    // --- [추가] 렌더링 정보 필드 ---
    private ResourceLocation backgroundBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.STONE);
    private ResourceLocation oreBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.IRON_ORE); // 대표 광물 블록

    public static final ModelProperty<OreNodeBlockEntity> MODEL_DATA_PROPERTY = new ModelProperty<>();

    public OreNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // Goggle을 위한 Behaviour 추가는 이제 필요 없습니다.
    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // 다른 Behaviour가 있다면 여기에 추가합니다.
    }

    // --- [추가] IModelData를 제공하는 메서드 오버라이드 ---
    @Nonnull
    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(MODEL_DATA_PROPERTY, this).build();
    }


    public ResourceLocation getBackgroundBlockId() {
        return backgroundBlockId;
    }


    public ResourceLocation getOreBlockId() {
        return oreBlockId;
    }


    // [수정] configure 메서드에 렌더링 정보 추가
    public void configure(Map<Item, Float> composition, int yield, int resistance, Block backgroundBlock, Block oreBlock) {
        this.resourceComposition = composition;
        this.totalYield = yield;
        this.miningResistance = resistance;
        this.backgroundBlockId = BuiltInRegistries.BLOCK.getKey(backgroundBlock);
        this.oreBlockId = BuiltInRegistries.BLOCK.getKey(oreBlock);
        setChanged();
        sendData(); // 클라이언트에 즉시 동기화
    }

    public ItemStack applyMiningTick(int amount) {
        if (level == null || level.isClientSide || totalYield <= 0 || resourceComposition.isEmpty()) {
            return ItemStack.EMPTY;
        }

        this.miningProgress += amount;
        if (this.miningProgress >= this.miningResistance) {
            this.miningProgress = 0;
            this.totalYield--;

            double random = level.getRandom().nextDouble();
            float cumulative = 0f;
            for (Map.Entry<Item, Float> entry : resourceComposition.entrySet()) {
                cumulative += entry.getValue();
                if (random < cumulative) {
                    ItemStack yieldedStack = new ItemStack(entry.getKey());
                    ItemEntity itemEntity = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, yieldedStack.copy());
                    level.addFreshEntity(itemEntity);

                    setChanged();
                    sendData();
                    return yieldedStack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("TotalYield", this.totalYield);
        tag.putInt("MiningProgress", this.miningProgress);
        tag.putInt("MiningResistance", this.miningResistance);

        tag.putString("BackgroundBlock", backgroundBlockId.toString());
        tag.putString("OreBlock", oreBlockId.toString());

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
        this.totalYield = tag.getInt("TotalYield");
        this.miningProgress = tag.getInt("MiningProgress");
        this.miningResistance = tag.getInt("MiningResistance");


        this.backgroundBlockId = ResourceLocation.tryParse(tag.getString("BackgroundBlock"));
        if (this.backgroundBlockId == null) this.backgroundBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.STONE);

        this.oreBlockId = ResourceLocation.tryParse(tag.getString("OreBlock"));
        if (this.oreBlockId == null) this.oreBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.IRON_ORE);

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

    // IHaveGoggleInformation 인터페이스의 메서드를 오버라이드합니다.
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Component header = Component.translatable("goggle.mycreateaddon.ore_node.header")
                .withStyle(ChatFormatting.GOLD);
        tooltip.add(Component.literal(" ").append(header));

        Component yieldComponent = Component.translatable("goggle.mycreateaddon.ore_node.yield", this.totalYield)
                .withStyle(ChatFormatting.GRAY);
        tooltip.add(Component.literal(" ").append(yieldComponent));

        if (!resourceComposition.isEmpty()) {
            Component compositionHeader = Component.translatable("goggle.mycreateaddon.ore_node.composition")
                    .withStyle(ChatFormatting.GRAY);
            tooltip.add(Component.literal(" ").append(compositionHeader));

            List<Map.Entry<Item, Float>> sortedComposition = new ArrayList<>(resourceComposition.entrySet());
            sortedComposition.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            for (Map.Entry<Item, Float> entry : sortedComposition) {
                Component compositionLine = Component.literal("  - ")
                        .append(entry.getKey().getDescription().copy().withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(String.format(": %.1f%%", entry.getValue() * 100))
                                .withStyle(ChatFormatting.DARK_AQUA));
                tooltip.add(compositionLine);
            }
        }

        return true;
    }
}