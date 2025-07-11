package com.yourname.mycreateaddon.content.kinetics.node;


import com.yourname.mycreateaddon.MyCreateAddon; // 모드 메인 클래스
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// OreNodeBlockEntity를 상속받습니다.
public class ArtificialNodeBlockEntity extends OreNodeBlockEntity {

    // [핵심] 인공 노드의 고유한 배경 블록 ID
    private static final ResourceLocation ARTIFICIAL_CASING_ID =
            ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "block/artificial_node");

    public ArtificialNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // getBackgroundBlockId 메서드를 오버라이드하여 항상 커스텀 케이싱을 반환합니다.
    @Override
    public ResourceLocation getBackgroundBlockId() {
        return ARTIFICIAL_CASING_ID;
    }

    // TODO: 특수 속성(Quirks) 관련 데이터 필드와 로직을 여기에 추가할 수 있습니다.
}