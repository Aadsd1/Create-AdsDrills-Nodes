package com.yourname.mycreateaddon.content.kinetics.node;


import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// OreNodeBlock을 상속받고 IWrenchable를 구현합니다.
public class ArtificialNodeBlock extends OreNodeBlock implements IWrenchable {

    public ArtificialNodeBlock(Properties properties) {
        super(properties);
    }

    // BE 타입은 ArtificialNodeBlockEntity를 사용하도록 오버라이드합니다.
    @Override
    public BlockEntityType<? extends OreNodeBlockEntity> getBlockEntityType() {
        return MyAddonBlockEntity.ARTIFICIAL_NODE.get();
    }

    // 렌치로 쉬프트+우클릭 시 블록을 아이템화하고 드롭하는 로직
    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 블록을 파괴하고, getDrops 메서드가 NBT 데이터가 포함된 아이템을 드롭하도록 합니다.
        level.destroyBlock(pos, false, context.getPlayer());
        //playBreakSound(level, pos);

        return InteractionResult.SUCCESS;
    }
}