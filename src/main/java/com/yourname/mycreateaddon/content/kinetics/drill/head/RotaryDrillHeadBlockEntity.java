package com.yourname.mycreateaddon.content.kinetics.drill.head;

import com.yourname.mycreateaddon.registry.MyAddonItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RotaryDrillHeadBlockEntity extends AbstractDrillHeadBlockEntity {

    private int fortuneLevel = 0;
    private boolean hasSilkTouch = false;
    private float clientHeat = 0f;

    public RotaryDrillHeadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getFortuneLevel() { return fortuneLevel; }
    public boolean hasSilkTouch() { return hasSilkTouch; }
    public float getClientHeat() { return this.clientHeat; }

    public void applyUpgrade(Player player, Item upgradeItem) {
        if (level == null || level.isClientSide) return;

        if (upgradeItem == MyAddonItems.SILKY_JEWEL.get()) {
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
        } else if (upgradeItem == MyAddonItems.ROSE_GOLD.get()) {
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

    public void updateClientHeat(float heat) {
        if (this.clientHeat == heat) return;
        this.clientHeat = heat;
        setChanged();
        sendData();
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (fortuneLevel > 0) {
            compound.putInt("Fortune", fortuneLevel);
        }
        if (hasSilkTouch) {
            compound.putBoolean("SilkTouch", true);
        }
        if (clientPacket) {
            compound.putFloat("ClientHeat", clientHeat);
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        fortuneLevel = compound.getInt("Fortune");
        hasSilkTouch = compound.getBoolean("SilkTouch");
        if (clientPacket) {
            clientHeat = compound.getFloat("ClientHeat");
        }
    }
}