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


public class RotaryDrillHeadBlockEntity extends KineticBlockEntity {

    protected BlockPos cachedCorePos;
    private float visualSpeed = 0f;

    // [추가] 업그레이드 상태 필드
    private int fortuneLevel = 0;
    private boolean hasSilkTouch = false;
    private float clientHeat = 0f;
    public RotaryDrillHeadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // [추가] 업그레이드 상태를 외부에서 읽기 위한 getter
    public int getFortuneLevel() {
        return fortuneLevel;
    }

    public boolean hasSilkTouch() {
        return hasSilkTouch;
    }

    // [추가] 업그레이드를 적용하는 메서드
    public void applyUpgrade(Player player, Item upgradeItem) {
        if (level == null || level.isClientSide) return;

        // 실크터치 업그레이드 시도
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
        // 행운 업그레이드 시도
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

        // 상태 변경 후 동기화
        setChanged();
        sendData();
    }
    // [신규] 코어가 호출하여 heat 값을 업데이트하는 메서드
    public void updateClientHeat(float heat) {
        if (this.clientHeat == heat) return;
        this.clientHeat = heat;
        setChanged();
        sendData(); // 클라이언트로 변경된 heat 값을 동기화합니다.
    }


    // [신규] Visual이 사용할 getter
    public float getClientHeat() {
        return this.clientHeat;
    }

    public void updateVisualSpeed(float speed) {
        if (this.visualSpeed == speed) {
            return;
        }
        this.visualSpeed = speed;
        setChanged();
        sendData();
    }

    public float getVisualSpeed() {
        return visualSpeed;
    }

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

        Direction facing = getBlockState().getValue(RotaryDrillHeadBlock.FACING);
        BlockPos potentialCorePos = worldPosition.relative(facing.getOpposite());

        if (level.getBlockEntity(potentialCorePos) instanceof DrillCoreBlockEntity) {
            setCore(potentialCorePos);
        } else {
            setCore(null);
        }
    }
    @Nullable
    public DrillCoreBlockEntity getCore() {
        if (level != null && cachedCorePos != null) {
            if (level.getBlockEntity(cachedCorePos) instanceof DrillCoreBlockEntity core) {
                return core;
            }
        }
        return null;
    }
    // ==================================================
    //  동력 네트워크 독립성 확보
    // ==================================================

    // [FIXED] 클라이언트에서 속도가 0으로 덮어씌워지는 것을 막기 위해,
    // 코어로부터 전달받은 visualSpeed를 자신의 이론상 속도로 사용합니다.
    @Override public float getTheoreticalSpeed() { return visualSpeed; }

    @Override public float calculateStressApplied() { return 0; }
    @Override public float calculateAddedStressCapacity() { return 0; }
    @Override public void attachKinetics() { /* 이 블록은 자체적으로 네트워크에 참여하지 않습니다. */ }

    // ==================================================
    //  NBT 데이터 처리
    // ==================================================

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (cachedCorePos != null) {
            compound.put("CorePos", NbtUtils.writeBlockPos(cachedCorePos));
        }
        // [추가] 업그레이드 정보 저장 및 동기화
        if (fortuneLevel > 0) {
            compound.putInt("Fortune", fortuneLevel);
        }
        if (hasSilkTouch) {
            compound.putBoolean("SilkTouch", true);
        }
        if (clientPacket) {
            compound.putFloat("VisualSpeed", visualSpeed);
            compound.putFloat("ClientHeat", clientHeat);
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (compound.contains("CorePos")) {
            cachedCorePos = NbtUtils.readBlockPos(compound, "CorePos").orElse(null);
        } else {
            cachedCorePos = null;
        }
        // [추가] 업그레이드 정보 로드
        fortuneLevel = compound.getInt("Fortune");
        hasSilkTouch = compound.getBoolean("SilkTouch");
        if (clientPacket) {
            visualSpeed = compound.getFloat("VisualSpeed");
            clientHeat = compound.getFloat("ClientHeat");
        }
    }
}