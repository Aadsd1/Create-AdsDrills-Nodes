package com.yourname.mycreateaddon.datagen;

import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MyAddonItemModelProvider extends ItemModelProvider {
    public MyAddonItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MyCreateAddon.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // --- 여기서 직접 아이템 모델을 생성합니다. ---
        simpleBlockItem(MyAddonBlocks.DRILL_CORE);
        simpleBlockItem(MyAddonBlocks.FRAME_MODULE);
    }

    // 블록의 아이템 모델을 생성하는 헬퍼 메서드
    private void simpleBlockItem(DeferredHolder<Block, ? extends Block> block) {
        // 아이템 모델의 부모를 "mycreateaddon:block/블록이름/block" 으로 설정합니다.
        // 이는 MyAddonBlockStateProvider에서 사용하는 모델 경로 규칙과 일치합니다.
        withExistingParent(
                block.getId().getPath(),
                modLoc("block/" + block.getId().getPath() + "/block")
        );
    }
}