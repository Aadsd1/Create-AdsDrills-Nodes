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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public class NodeFrameBlockEntity extends SmartBlockEntity {

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

        // 1. 제작 준비 확인
        ItemStack coreStack = inventory.getStackInSlot(9);
        boolean hasData = false;
        for (int i = 0; i < 9; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                hasData = true;
                break;
            }
        }

        if (coreStack.isEmpty() || !hasData) {
            if (progress > 0) {
                progress = Math.max(0, progress - 128);
                setChanged();
            }
            return;
        }

        // 2. 코어 등급에 따른 파라미터 설정
        int requiredSpeed = Integer.MAX_VALUE; // 기본적으로 작동 안함
        boolean resetOnFail = false;
        int progressDecay = 128;

        if (coreStack.getItem() instanceof StabilizerCoreItem stabilizer) {
            switch (stabilizer.getTier()) {
                case BRASS -> {
                    requiredSpeed = 512;
                    progressDecay = 64;
                }
                case STEEL -> {
                    requiredSpeed = 1024;
                    progressDecay = 256;
                }
                case NETHERITE -> {
                    requiredSpeed = 2048;
                    resetOnFail = true;
                }
            }
        }

        // 3. 진행도 업데이트
        int currentDrillSpeed = getDrillSpeedAbove();

        if (currentDrillSpeed >= requiredSpeed) {
            progress += currentDrillSpeed;
            // [1단계 추가] 제작 진행 피드백
            if (level.getRandom().nextInt(20) == 0) {
                ((ServerLevel) level).sendParticles(ParticleTypes.ENCHANT,
                        worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                        2, 0.3, 0.1, 0.3, 0.1);
                level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.2f, 1.5f);
            }
        } else {
            // 제작 실패
            if (progress > 0) {
                if (resetOnFail) {
                    progress = 0;
                    // [1단계 추가] 네더라이트 코어 실패 피드백 (강력함)
                    ((ServerLevel) level).sendParticles(ParticleTypes.LAVA,
                            worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                            10, 0.4, 0.2, 0.4, 0.0);
                    level.playSound(null, worldPosition, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0f, 0.8f);
                } else {
                    progress = Math.max(0, progress - progressDecay);
                    // [1단계 추가] 일반 실패 피드백 (약함)
                    if (level.getRandom().nextInt(10) == 0) {
                        ((ServerLevel) level).sendParticles(ParticleTypes.SMOKE,
                                worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                                5, 0.4, 0.2, 0.4, 0.01);
                    }
                }
            }
        }

        // 4. 제작 완료 확인
        if (progress >= REQUIRED_PROGRESS) {
            completeCrafting();
        }

        setChanged();
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

        // --- TODO: 2단계에서 이 부분을 구체적인 로직으로 채웁니다 ---
        Map<Item, Float> finalComposition = new HashMap<>();
        finalComposition.put(Items.RAW_IRON, 1.0f);

        Map<Item, Block> finalItemToBlockMap = new HashMap<>();
        finalItemToBlockMap.put(Items.RAW_IRON, Blocks.IRON_ORE);

        Item representativeItem = Items.RAW_IRON;
        Block representativeBlock = Blocks.IRON_ORE;
        int finalMaxYield = 5000;
        float finalHardness = 1.2f;
        float finalRichness = 1.1f;
        float finalRegeneration = 0.002f;
        FluidStack finalFluid = FluidStack.EMPTY;
        int finalFluidCapacity = 0;

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
                    MyAddonBlocks.ARTIFICIAL_NODE.get(),
                    representativeBlock,
                    finalFluid,
                    finalFluidCapacity
            );
        }

        level.playSound(null, currentPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0f, 0.8f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    currentPos.getX() + 0.5, currentPos.getY() + 1.2, currentPos.getZ() + 0.5,
                    30, 0.5, 0.5, 0.5, 0.1);
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
}