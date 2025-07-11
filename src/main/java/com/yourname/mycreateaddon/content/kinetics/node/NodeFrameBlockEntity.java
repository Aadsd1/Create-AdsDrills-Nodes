package com.yourname.mycreateaddon.content.kinetics.node;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.yourname.mycreateaddon.content.item.StabilizerCoreItem;
import com.yourname.mycreateaddon.content.kinetics.drill.head.RotaryDrillHeadBlockEntity;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeFrameBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    protected ItemStackHandler inventory = createInventory();
    private int progress = 0;
    private static final int REQUIRED_PROGRESS = 240000;
    private boolean isCompleting = false;

    public NodeFrameBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    private ItemStackHandler createInventory() {
        return new ItemStackHandler(11) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                sendData();
            }
        };
    }

    public boolean addData(ItemStack dataStack) {
        for (int i = 0; i < 9; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, dataStack.split(1));
                // [1단계 추가] 성공 효과음
                assert level != null;
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
                return true;
            }
        }
        assert level != null;
        level.playSound(null, worldPosition, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.5f, 1.0f);
        return false;
    }

    public boolean addStabilizerCore(ItemStack coreStack) {
        if (inventory.getStackInSlot(9).isEmpty()) {
            inventory.setStackInSlot(9, coreStack.split(1));
            // [1단계 추가] 성공 효과음
            assert level != null;
            level.playSound(null, worldPosition, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0f, 1.2f);
            return true;
        }
        assert level != null;
        level.playSound(null, worldPosition, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.5f, 1.0f);
        return false;
    }

    // [1단계 추가] 아이템 회수 로직
    public void retrieveItem(Player player) {
        // 코어 -> 데이터 순으로 맨 마지막에 넣은 아이템부터 회수
        for (int i = 9; i >= 0; i--) {
            ItemStack stackInSlot = inventory.getStackInSlot(i);
            if (!stackInSlot.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(player, stackInSlot);
                inventory.setStackInSlot(i, ItemStack.EMPTY);
                assert level != null;
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
                return;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        assert level instanceof ServerLevel; // 서버 전용 로직임을 명시

        // 진행률 동기화를 위해 이전 progress 값을 기억합니다.
        int oldProgress = this.progress;

        // 1. 제작 준비 확인
        ItemStack coreStack = inventory.getStackInSlot(9);
        boolean hasData = false;
        for (int i = 0; i < 9; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                hasData = true;
                break;
            }
        }

        // 코어나 데이터가 없으면 진행도를 점차 감소시킴
        if (coreStack.isEmpty() || !hasData) {
            if (progress > 0) {
                progress = Math.max(0, progress - 128);
            }
        } else {
            // 2. 코어 등급에 따른 파라미터 설정
            int requiredSpeed = getRequiredSpeed(); // 고글 툴팁용으로 만든 헬퍼 메서드 재활용
            boolean resetOnFail = false;
            int progressDecay = 128;

            if (coreStack.getItem() instanceof StabilizerCoreItem stabilizer) {
                switch (stabilizer.getTier()) {
                    case BRASS -> progressDecay = 64;
                    case STEEL -> progressDecay = 256;
                    case NETHERITE -> resetOnFail = true;
                }
            }

            // 3. 진행도 업데이트
            int currentDrillSpeed = getDrillSpeedAbove();

            if (currentDrillSpeed >= requiredSpeed) {
                progress += currentDrillSpeed;

                // 제작 진행 피드백 (파티클 및 사운드)
                if (level.getRandom().nextInt(10) == 0) {
                    ((ServerLevel) level).sendParticles(ParticleTypes.ENCHANT,
                            worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                            2, 0.3, 0.1, 0.3, 0.05);
                }
                // 제작 막바지에 더 화려한 효과 추가
                if (progress > REQUIRED_PROGRESS * 0.9f && level.getRandom().nextInt(5) == 0) {
                    ((ServerLevel) level).sendParticles(ParticleTypes.PORTAL,
                            worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                            1, 0.5, 0.5, 0.5, 0.0);
                    level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.3f, 1.8f);
                }

            } else {
                // 제작 실패 시 진행도 감소
                if (progress > 0) {
                    if (resetOnFail) {
                        progress = 0;
                        // 네더라이트 코어 실패 피드백 (강력함)
                        ((ServerLevel) level).sendParticles(ParticleTypes.LAVA,
                                worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                                10, 0.4, 0.2, 0.4, 0.0);
                        level.playSound(null, worldPosition, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0f, 0.8f);
                    } else {
                        progress = Math.max(0, progress - progressDecay);
                        // 일반 실패 피드백 (약함)
                        if (level.getRandom().nextInt(10) == 0) {
                            ((ServerLevel) level).sendParticles(ParticleTypes.SMOKE,
                                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                                    5, 0.4, 0.2, 0.4, 0.01);
                        }
                    }
                }
            }
        }


        // 4. 제작 완료 확인
        if (progress >= REQUIRED_PROGRESS) {
            completeCrafting();
            return; // 제작 완료 시, 아래의 동기화 로직을 실행할 필요 없음
        }

        // 5. 진행률 변경 시 클라이언트에 동기화
        if (oldProgress != this.progress) {
            // 10틱(0.5초)마다 동기화하여 부하를 줄임
            if (level.getGameTime() % 10 == 0) {
                setChanged();
                sendData();
            }
        }
    }

    // [1단계 수정] 메서드 완성
    private int getDrillSpeedAbove() {
        assert level != null;
        BlockEntity aboveBE = level.getBlockEntity(worldPosition.above());
        if (aboveBE instanceof RotaryDrillHeadBlockEntity headBE) {
            // 네더라이트 드릴 헤드만 작동하도록 제한
            if (headBE.getBlockState().getBlock() == MyAddonBlocks.NETHERITE_ROTARY_DRILL_HEAD.get()) {
                if (headBE.getCore() != null) {
                    // 최종 속도의 절댓값을 정수로 반환
                    return (int) Math.abs(headBE.getCore().getFinalSpeed());
                }
            }
        }
        return 0;
    }

    private void completeCrafting() {
        if (level == null || level.isClientSide) return;

        // --- 1. 재료 분석 및 데이터 집계 ---
        Map<Item, Float> weightedComposition = new HashMap<>();
        Map<Item, Block> finalItemToBlockMap = new HashMap<>();
        float totalInputYield = 0;
        int dataItemCount = 0;

        for (int i = 0; i < 9; i++) {
            ItemStack dataStack = inventory.getStackInSlot(i);
            if (dataStack.isEmpty()) continue;

            CustomData customData = dataStack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) continue;
            CompoundTag nbt = customData.copyTag();

            float yield = nbt.getFloat("Yield");
            totalInputYield += yield;
            dataItemCount++;

            // 가중치 기반 성분 계산
            ListTag compositionList = nbt.getList("Composition", 10);
            for (int j = 0; j < compositionList.size(); j++) {
                CompoundTag entry = compositionList.getCompound(j);
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getString("Item")));
                float ratio = entry.getFloat("Ratio");
                weightedComposition.merge(item, ratio * yield, Float::sum);
            }

            // 아이템-블록 맵 병합
            ListTag itemToBlockList = nbt.getList("ItemToBlockMap", 10);
            for (int j = 0; j < itemToBlockList.size(); j++) {
                CompoundTag entry = itemToBlockList.getCompound(j);
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getString("Item")));
                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.getString("Block")));
                if (item != Items.AIR && block != Blocks.AIR) {
                    finalItemToBlockMap.putIfAbsent(item, block);
                }
            }
        }

        if (weightedComposition.isEmpty()) {
            // 데이터가 없으면 기본 철 광맥으로 생성 (안전장치)
            weightedComposition.put(Items.RAW_IRON, 1.0f);
            finalItemToBlockMap.put(Items.RAW_IRON, Blocks.IRON_ORE);
            totalInputYield = 1000;
        }

        // --- 2. 최종 결과물 속성 결정 ---

        // 2a. 최종 성분 정규화
        Map<Item, Float> finalComposition = new HashMap<>();
        float totalWeight = weightedComposition.values().stream().reduce(0f, Float::sum);
        if (totalWeight > 0) {
            for (Map.Entry<Item, Float> entry : weightedComposition.entrySet()) {
                finalComposition.put(entry.getKey(), entry.getValue() / totalWeight);
            }
        } else { // 예외 처리
            finalComposition.put(Items.RAW_IRON, 1.0f);
        }


        ItemStack coreStack = inventory.getStackInSlot(9);
        StabilizerCoreItem.Tier tier = (coreStack.getItem() instanceof StabilizerCoreItem sci) ? sci.getTier() : StabilizerCoreItem.Tier.BRASS;

        // 2b. 최종 매장량 계산 (코어 등급에 따라 효율 보정)
        float yieldMultiplier = switch (tier) {
            case BRASS -> 0.8f;   // 황동: 효율 80%
            case STEEL -> 1.0f;   // 강철: 효율 100%
            case NETHERITE -> 1.25f; // 네더라이트: 효율 125% (보너스)
        };
        int finalMaxYield = (int) (totalInputYield * yieldMultiplier);


        // 2c. 최종 특성 결정 (코어 등급에 따라 범위 내 랜덤)
        RandomSource random = level.getRandom();
        float finalHardness, finalRichness, finalRegeneration;
        switch (tier) {
            case BRASS -> {
                finalHardness = 0.6f + (random.nextFloat() * 0.4f); // 0.6 ~ 1.0
                finalRichness = 0.9f + (random.nextFloat() * 0.2f); // 0.9 ~ 1.1
                finalRegeneration = 0.0005f + (random.nextFloat() * 0.001f); // 초당 0.01~0.03
            }
            case STEEL -> {
                finalHardness = 0.9f + (random.nextFloat() * 0.6f); // 0.9 ~ 1.5
                finalRichness = 1.05f + (random.nextFloat() * 0.3f); // 1.05 ~ 1.35
                finalRegeneration = 0.0015f + (random.nextFloat() * 0.002f); // 초당 0.03~0.07
            }
            case NETHERITE -> {
                finalHardness = 1.4f + (random.nextFloat() * 0.8f); // 1.4 ~ 2.2
                finalRichness = 1.2f + (random.nextFloat() * 0.4f); // 1.2 ~ 1.6
                finalRegeneration = 0.0025f + (random.nextFloat() * 0.003f); // 초당 0.05~0.11
            }
            default -> throw new IllegalStateException("Unexpected value: " + tier);
        }


        // 2d. 대표 블록 찾기
        Block representativeBlock = finalComposition.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> finalItemToBlockMap.getOrDefault(entry.getKey(), Blocks.IRON_ORE))
                .orElse(Blocks.IRON_ORE);

        // 인공 노드는 유체를 가지지 않음
        FluidStack finalFluid = FluidStack.EMPTY;
        int finalFluidCapacity = 0;

        // --- 3. 인공 노드 생성 및 월드에 배치 ---
        this.isCompleting = true;
        BlockPos currentPos = this.worldPosition;
        level.setBlock(currentPos, MyAddonBlocks.ARTIFICIAL_NODE.get().defaultBlockState(), 3);

        BlockEntity newBE = level.getBlockEntity(currentPos);
        if (newBE instanceof ArtificialNodeBlockEntity artificialNode) {
            artificialNode.configure(
                    finalComposition,
                    finalItemToBlockMap,
                    finalMaxYield,
                    finalHardness,
                    finalRichness,
                    finalRegeneration,
                    MyAddonBlocks.ARTIFICIAL_NODE.get(), // 배경 블록은 인공 노드 블록 자신
                    representativeBlock,
                    finalFluid,
                    finalFluidCapacity
            );
        }

        // 제작 완료 효과
        level.playSound(null, currentPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0f, 0.8f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    currentPos.getX() + 0.5, currentPos.getY() + 1.2, currentPos.getZ() + 0.5,
                    50, 0.6, 0.6, 0.6, 0.1);
        }
    }

    public void onBroken() {
        if (isCompleting || level == null || level.isClientSide) return;

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack));
            }
        }
    }


    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
    }


    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // 헤더
        tooltip.add(Component.literal("    ").append(Component.translatable(MyAddonBlocks.NODE_FRAME.get().getDescriptionId()).withStyle(ChatFormatting.GOLD)));
        tooltip.add(Component.literal(""));


        // 내용물
        ItemStack coreStack = inventory.getStackInSlot(9);
        long dataItemCount = 0;
        for (int i = 0; i < 9; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                dataItemCount++;
            }
        }

        if (coreStack.isEmpty() && dataItemCount == 0) {
            tooltip.add(Component.literal(" ").append(Component.translatable("goggle.mycreateaddon.drill_core.storage.empty").withStyle(ChatFormatting.DARK_GRAY)));
            return true;
        }

        // 진행률
        float progressPercent = (float) progress / REQUIRED_PROGRESS * 100f;
        tooltip.add(Component.literal(" " + String.format("%.2f%% ", progressPercent))
                .append(Component.translatable("goggle.mycreateaddon.node_frame.progress").withStyle(ChatFormatting.GRAY)));

        // 요구 속도
        int requiredSpeed = getRequiredSpeed();
        int currentSpeed = getDrillSpeedAbove();

        MutableComponent speedReqLine = Component.literal(" ")
                .append(Component.translatable("goggle.mycreateaddon.node_frame.speed_requirement").withStyle(ChatFormatting.GRAY));

        if (requiredSpeed == Integer.MAX_VALUE) {
            speedReqLine.append(Component.translatable("goggle.mycreateaddon.node_frame.none").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            ChatFormatting color = currentSpeed >= requiredSpeed ? ChatFormatting.GREEN : ChatFormatting.RED;
            speedReqLine.append(Component.literal(String.format(" %d RPM", requiredSpeed)).withStyle(color));
        }
        tooltip.add(speedReqLine);



        // 상세 내용물 (플레이어가 Sneaking 중일 때만 표시)
        if (isPlayerSneaking) {
            tooltip.add(Component.literal(""));
            if (!coreStack.isEmpty()) {
                tooltip.add(Component.literal(" ").append(Component.translatable("mycreateaddon.stabilizer_core.header").withStyle(ChatFormatting.GRAY)));
                tooltip.add(Component.literal("  - ").append(coreStack.getHoverName()));
                        }
            if (dataItemCount > 0) {
                tooltip.add(Component.literal(" ").append(Component.translatable("mycreateaddon.node_data.header", dataItemCount).withStyle(ChatFormatting.GRAY)));
            }
        } else {
            tooltip.add(Component.literal("").append(Component.translatable("goggle.mycreateaddon.sneak_for_details").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        }

        return true;
    }

    // [3] 요구 속도를 가져오는 헬퍼 메서드를 추가합니다.
    private int getRequiredSpeed() {
        ItemStack coreStack = inventory.getStackInSlot(9);
        if (coreStack.getItem() instanceof StabilizerCoreItem stabilizer) {
            return switch (stabilizer.getTier()) {
                case BRASS -> 512;
                case STEEL -> 1024;
                case NETHERITE -> 2048;
            };
        }
        return Integer.MAX_VALUE; // 코어가 없으면 작동 불가
    }

}