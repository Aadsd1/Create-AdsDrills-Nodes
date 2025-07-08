package com.yourname.mycreateaddon.content.kinetics.drill.head;


import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class HydraulicDrillHeadBlockEntity extends KineticBlockEntity {

    protected BlockPos cachedCorePos;
    private float visualSpeed = 0f;
    private int fortuneLevel = 0;
    private boolean hasSilkTouch = false;

    public HydraulicDrillHeadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // 업그레이드 상태 Getter
    public int getFortuneLevel() { return fortuneLevel; }
    public boolean hasSilkTouch() { return hasSilkTouch; }

    // 업그레이드 적용 로직 (Rotary와 동일)
    public void applyUpgrade(Player player, Item upgradeItem) {
        if (level == null || level.isClientSide) return;

        if (upgradeItem == Items.EMERALD) {
            if (hasSilkTouch) {
                player.displayClientMessage(Component.translatable("mycreateaddon.upgrade_fail.already_applied"), true);
                return;
            }
            if (fortuneLevel > 0) {
                player.displayClientMessage(Component.translatable("mycreateaddon.upgrade_fail.conflict"), true);
                return;
            }
            hasSilkTouch = true;
            player.displayClientMessage(Component.translatable("mycreateaddon.upgrade_success.silktouch"), true);
        }
        else if (upgradeItem == Items.GOLD_INGOT) {
            if (hasSilkTouch) {
                player.displayClientMessage(Component.translatable("mycreateaddon.upgrade_fail.conflict"), true);
                return;
            }
            if (fortuneLevel >= 3) {
                player.displayClientMessage(Component.translatable("mycreateaddon.upgrade_fail.max_level"), true);
                return;
            }
            fortuneLevel++;
            player.displayClientMessage(Component.translatable("mycreateaddon.upgrade_success.fortune", fortuneLevel), true);
        }

        setChanged();
        sendData();
    }

    public void updateVisualSpeed(float speed) {
        if (this.visualSpeed == speed) return;
        this.visualSpeed = speed;
        setChanged();
        sendData();
    }

    public float getVisualSpeed() { return visualSpeed; }

    public void setCore(BlockPos corePos) {
        if (corePos == null || !corePos.equals(cachedCorePos)) {
            cachedCorePos = corePos;
            if (corePos == null) {
                updateVisualSpeed(0);
            }
            setChanged();
            sendData();
        }
    }

    public void updateCoreConnection() {
        if (level == null || level.isClientSide) return;
        Direction facing = getBlockState().getValue(HydraulicDrillHeadBlock.FACING);
        BlockPos potentialCorePos = worldPosition.relative(facing.getOpposite());
        if (level.getBlockEntity(potentialCorePos) instanceof DrillCoreBlockEntity) {
            setCore(potentialCorePos);
        } else {
            setCore(null);
        }
    }

    @Nullable
    public DrillCoreBlockEntity getCore() {
        if (level != null && cachedCorePos != null && level.getBlockEntity(cachedCorePos) instanceof DrillCoreBlockEntity core) {
            return core;
        }
        return null;
    }

    @Override public float getTheoreticalSpeed() { return visualSpeed; }
    @Override public float calculateStressApplied() { return 0; }
    @Override public float calculateAddedStressCapacity() { return 0; }
    @Override public void attachKinetics() {}

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (cachedCorePos != null) compound.put("CorePos", NbtUtils.writeBlockPos(cachedCorePos));
        if (fortuneLevel > 0) compound.putInt("Fortune", fortuneLevel);
        if (hasSilkTouch) compound.putBoolean("SilkTouch", true);
        if (clientPacket) compound.putFloat("VisualSpeed", visualSpeed);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        cachedCorePos = compound.contains("CorePos") ? NbtUtils.readBlockPos(compound, "CorePos").orElse(null) : null;
        fortuneLevel = compound.getInt("Fortune");
        hasSilkTouch = compound.getBoolean("SilkTouch");
        if (clientPacket) visualSpeed = compound.getFloat("VisualSpeed");
    }
}