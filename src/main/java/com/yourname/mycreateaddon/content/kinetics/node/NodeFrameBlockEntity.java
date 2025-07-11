package com.yourname.mycreateaddon.content.kinetics.node;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.yourname.mycreateaddon.content.item.StabilizerCoreItem;
import com.yourname.mycreateaddon.content.kinetics.drill.head.RotaryDrillHeadBlockEntity;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeFrameBlockEntity extends SmartBlockEntity {

    // 인벤토리: 데이터 9칸, 안정화 코어 1칸, 촉매 1칸 (현재는 미사용)
    protected ItemStackHandler inventory = createInventory();
    private int progress = 0;
    private static final int REQUIRED_PROGRESS = 240000; // 목표 진행도 (밸런스 조절 필요)
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
            }
        };
    }

    // 데이터 아이템 추가 로직 (boolean 반환)
    public boolean addData(ItemStack dataStack) {
        for (int i = 0; i < 9; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, dataStack.split(1));
                assert level != null;
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
                return true; // 성공
            }
        }
        // 모든 데이터 슬롯이 꽉 찼으면 실패
        assert level != null;
        level.playSound(null, worldPosition, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.5f, 1.0f);
        return false;
    }

    // 안정화 코어 추가 로직 (boolean 반환)
    public boolean addStabilizerCore(ItemStack coreStack) {
        if (inventory.getStackInSlot(9).isEmpty()) {
            inventory.setStackInSlot(9, coreStack.split(1));
            assert level != null;
            level.playSound(null, worldPosition, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0f, 1.2f);
            return true; // 성공
        }
        // 코어 슬롯이 이미 차있으면 실패
        assert level != null;
        level.playSound(null, worldPosition, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.5f, 1.0f);
        return false;
    }
    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;

        // 1. 제작 준비가 되었는지 확인 (최소 1개의 데이터와 1개의 코어)
        ItemStack coreStack = inventory.getStackInSlot(9);
        boolean hasData = false;
        for (int i = 0; i < 9; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                hasData = true;
                break;
            }
        }

        if (coreStack.isEmpty() || !hasData) {
            // 재료가 없으면 진행도 서서히 감소
            if (progress > 0) {
                progress = Math.max(0, progress - 128);
                setChanged();
            }
            return;
        }

        Item coreItem = coreStack.getItem();

        // 2. 코어 등급에 따른 요구 속도 및 페널티 설정
        int requiredSpeed = 0;
        boolean resetOnFail = false;
        int progressDecay = 128;

        // [핵심 수정] instanceof로 확인하고, 등급을 가져와서 비교
        if (coreItem instanceof StabilizerCoreItem stabilizer) {
            StabilizerCoreItem.Tier tier = stabilizer.getTier();

            if (tier == StabilizerCoreItem.Tier.BRASS) {
                requiredSpeed = 512;
                progressDecay = 64;
            } else if (tier == StabilizerCoreItem.Tier.STEEL) {
                requiredSpeed = 1024;
                progressDecay = 256;
            } else if (tier == StabilizerCoreItem.Tier.NETHERITE) {
                requiredSpeed = 2048;
                resetOnFail = true;
            }
        }

        // 3. 드릴링 조건 확인 및 진행도 업데이트
        int currentDrillSpeed = getDrillSpeedAbove();

        if (currentDrillSpeed >= requiredSpeed) {
            progress += currentDrillSpeed;
        } else {
            if (resetOnFail && progress > 0) {
                progress = 0;
                // 실패 효과음 및 파티클
                level.playSound(null, worldPosition, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0f, 1.0f);
            } else {
                progress = Math.max(0, progress - progressDecay);
            }
        }

        // 4. 진행도 완료 시 제작
        if (progress >= REQUIRED_PROGRESS) {
            completeCrafting();
        }

        setChanged();
    }

    private int getDrillSpeedAbove() {
        assert level != null;
        BlockEntity aboveBE = level.getBlockEntity(worldPosition.above());
        if (aboveBE instanceof RotaryDrillHeadBlockEntity headBE) {
            // 네더라이트 드릴 헤드만 작동하도록 제한
            if (headBE.getBlockState().getBlock() == MyAddonBlocks.NETHERITE_ROTARY_DRILL_HEAD.get()) {
                if (headBE.getCore() != null) {
                    return (int) Math.abs(headBE.getCore().getFinalSpeed());
                }
            }
        }
        return 0;
    }

    private void completeCrafting() {
        if (level == null || level.isClientSide) return;

        // --- 1. 최종 스탯 계산 (임시 로직) ---
        // TODO: 인벤토리의 데이터들을 기반으로 실제 스탯을 계산하는 로직 구현 필요
        // 지금은 임시로 하드코딩된 값을 사용합니다.
        Map<Item, Float> finalComposition = new HashMap<>();
        finalComposition.put(Items.RAW_IRON, 1.0f); // 임시 구성

        Map<Item, Block> finalItemToBlockMap = new HashMap<>();
        finalItemToBlockMap.put(Items.RAW_IRON, Blocks.IRON_ORE); // 임시 맵

        // 대표 아이템 찾기 (가장 비율이 높은 아이템)
        Item representativeItem = finalComposition.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Items.RAW_IRON);

        // 대표 아이템에 해당하는 블록 찾기
        Block representativeBlock = finalItemToBlockMap.getOrDefault(representativeItem, Blocks.IRON_ORE);
        int finalMaxYield = 5000;
        float finalHardness = 1.2f;
        float finalRichness = 1.1f;
        float finalRegeneration = 0.002f;
        FluidStack finalFluid = FluidStack.EMPTY;
        int finalFluidCapacity = 0;

        this.isCompleting = true;

        // --- 2. 블록 교체 ---
        // 현재 블록 상태와 위치 정보를 저장
        BlockPos currentPos = this.worldPosition;
        level.setBlock(currentPos, MyAddonBlocks.ARTIFICIAL_NODE.get().defaultBlockState(), 3);

        // --- 3. 새로 생성된 BE에 데이터 주입 ---
        BlockEntity newBE = level.getBlockEntity(currentPos);
        if (newBE instanceof ArtificialNodeBlockEntity artificialNode) {
            // OreNodeBlockEntity로부터 상속받은 configure 메서드를 사용하여 데이터 주입
            artificialNode.configure(
                    finalComposition,
                    finalItemToBlockMap,
                    finalMaxYield,
                    finalHardness,
                    finalRichness,
                    finalRegeneration,
                    MyAddonBlocks.ARTIFICIAL_NODE.get(), // 배경 블록은 Artificial BE가 알아서 처리하므로 아무거나 전달
                    representativeBlock, // 임시 대표 블록
                    finalFluid,
                    finalFluidCapacity
            );
        }

        // 성공 효과음 및 파티클
        level.playSound(null, currentPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0f, 0.8f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    currentPos.getX() + 0.5, currentPos.getY() + 1.2, currentPos.getZ() + 0.5,
                    30, 0.5, 0.5, 0.5, 0.1);
        }
    }

    public void onBroken() {
        // 제작 완료 과정이 아니거나, 클라이언트 사이드일 때만 아이템을 드롭
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
}