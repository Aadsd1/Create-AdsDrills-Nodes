package com.yourname.mycreateaddon.datagen;

import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class MyAddonBlockStateProvider extends BlockStateProvider {
    public MyAddonBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, MyCreateAddon.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Drill Core
        // Create의 규칙을 따라 "block/drill_core/block.json" 모델을 참조합니다.
        ModelFile drillCoreModel = models().getExistingFile(modLoc("block/drill_core/block"));
        getVariantBuilder(MyAddonBlocks.DRILL_CORE.get())
                .forAllStates(state -> {
                    Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);
                    int x = axis == Direction.Axis.X ? 90 : 0;
                    int y = axis == Direction.Axis.Z ? 90 : (axis == Direction.Axis.X ? 90 : 0);
                    return ConfiguredModel.builder().modelFile(drillCoreModel).rotationX(x).rotationY(y).build();
                });

        // Frame Module
        ModelFile frameModuleModel = models().getExistingFile(modLoc("block/frame_module/block"));
        simpleBlock(MyAddonBlocks.FRAME_MODULE.get(), frameModuleModel);
    }
}
