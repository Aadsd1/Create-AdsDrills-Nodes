package com.adsd.adsdrill.content.kinetics.drill.head;

import com.adsd.adsdrill.config.AdsDrillConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

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

        // 설정 파일에서 아이템 ID 리스트와 실크터치 아이템 ID를 가져옵니다.
        List<? extends String> fortuneItemIds = AdsDrillConfigs.SERVER.rotaryDrillFortuneItems.get();
        Item silkTouchItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(AdsDrillConfigs.SERVER.rotaryDrillSilkTouchItem.get()));

        // 1. 실크터치 업그레이드 시도
        if (upgradeItem == silkTouchItem) {
            if (hasSilkTouch) {
                player.displayClientMessage(Component.translatable("adsdrill.upgrade_fail.already_applied"), true);
                return;
            }
            if (fortuneLevel > 0) {
                player.displayClientMessage(Component.translatable("adsdrill.upgrade_fail.conflict"), true);
                return;
            }
            hasSilkTouch = true;
            player.displayClientMessage(Component.translatable("adsdrill.upgrade_success.silktouch"), true);

            if (!player.getAbilities().instabuild) {
                player.getItemInHand(player.getUsedItemHand()).shrink(1);
            }

            setChanged();
            sendData();
            return; // 로직 종료
        }

        // 2. 행운 업그레이드 시도
        // 현재 행운 레벨이 설정된 아이템 리스트의 크기보다 작아야 업그레이드 가능
        if (fortuneLevel < fortuneItemIds.size()) {
            // 현재 레벨에 맞는 업그레이드 아이템을 가져옴 (fortuneLevel이 0이면 리스트의 0번째 아이템)
            Item requiredFortuneItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(fortuneItemIds.get(fortuneLevel)));

            if (upgradeItem == requiredFortuneItem) {
                if (hasSilkTouch) {
                    player.displayClientMessage(Component.translatable("adsdrill.upgrade_fail.conflict"), true);
                    return;
                }
                // 행운 레벨 증가
                fortuneLevel++;
                player.displayClientMessage(Component.translatable("adsdrill.upgrade_success.fortune", fortuneLevel), true);

                if (!player.getAbilities().instabuild) {
                    player.getItemInHand(player.getUsedItemHand()).shrink(1);
                }

                setChanged();
                sendData();
                return; // 로직 종료
            }
        }

        // 3. 최고 레벨 도달 시 (fortuneLevel이 리스트 크기 이상일 때)
        if (fortuneLevel >= fortuneItemIds.size()) {
            // 행운 업그레이드 아이템 중 하나라도 일치하는지 확인하여 메시지 출력
            boolean isAnyFortuneItem = fortuneItemIds.stream()
                    .map(id -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)))
                    .anyMatch(item -> item == upgradeItem);
            if (isAnyFortuneItem) {
                player.displayClientMessage(Component.translatable("adsdrill.upgrade_fail.max_level"), true);
            }
        }
    }

    public void updateClientHeat(float heat) {
        if (this.clientHeat == heat) return;
        this.clientHeat = heat;
        setChanged();
        sendData();
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && level.isClientSide) {
            handleSound();
        }
    }

    private void handleSound() {
        if (getVisualSpeed() == 0) {
            return;
        }

        assert level != null;
        if (level.getGameTime() % 5 != 0) {
            return;
        }

        float speed = Math.abs(getVisualSpeed());
        float volume = Mth.clamp(speed / 256f, 0.01f, 0.25f);
        float pitch = 0.8f + Mth.clamp(speed / 256f, 0.0f, 0.7f);

        level.playLocalSound(worldPosition, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, volume, pitch, false);
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