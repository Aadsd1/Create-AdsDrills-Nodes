package com.yourname.mycreateaddon.content.kinetics.node;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class OreNodeBlockEntity extends SmartBlockEntity {

    private int miningProgress;
    private int maxProgress = 200; // 채굴 완료에 필요한 총 틱. (임시 값)
    private ItemStack resourceToYield = new ItemStack(Items.RAW_IRON); // 채굴 시 나올 자원. (임시 값)

    public OreNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // 이 BE는 특별한 행동(Behaviour)이 필요 없습니다.
    }

    /**
     * 드릴 헤드가 호출할 핵심 채굴 로직입니다.
     * @param amount 진행시킬 틱의 양
     * @return 채굴이 완료되어 나온 아이템. 완료되지 않았으면 ItemStack.EMPTY.
     */
    public ItemStack applyMiningTick(int amount) {
        if (level == null || level.isClientSide) {
            return ItemStack.EMPTY;
        }

        this.miningProgress += amount;
        setChanged(); // 데이터가 변경되었음을 알림

        if (this.miningProgress >= this.maxProgress) {
            this.miningProgress = 0; // 진행도 초기화
            ItemStack yielded = this.resourceToYield.copy();

            // TODO: 나중에는 이 아이템을 드릴 코어의 내부 버퍼로 직접 보내야 합니다.
            // 지금은 테스트를 위해 월드에 드롭합니다.
            ItemEntity itemEntity = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, yielded);
            level.addFreshEntity(itemEntity);

            sendData(); // 클라이언트에 변경 사항(진행도 초기화 등) 전송
            return yielded;
        }

        return ItemStack.EMPTY;
    }

    // NBT 데이터 저장
    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("MiningProgress", this.miningProgress);
        tag.putInt("MaxProgress", this.maxProgress);
        tag.put("ResourceToYield", this.resourceToYield.save(registries));
    }

    // NBT 데이터 로드
    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.miningProgress = tag.getInt("MiningProgress");
        if (tag.contains("MaxProgress")) {
            this.maxProgress = tag.getInt("MaxProgress");
        }
        if (tag.contains("ResourceToYield")) {
            this.resourceToYield = ItemStack.parse(registries, tag.getCompound("ResourceToYield")).orElse(new ItemStack(Items.RAW_IRON));
        }
    }
}