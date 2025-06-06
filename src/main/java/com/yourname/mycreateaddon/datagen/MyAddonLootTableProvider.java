package com.yourname.mycreateaddon.datagen;

import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Collections;

public class MyAddonLootTableProvider extends BlockLootSubProvider {

    protected MyAddonLootTableProvider(HolderLookup.Provider provider) {
        super(Collections.emptySet(), FeatureFlags.VANILLA_SET, provider);
    }

    @Override
    protected void generate() {
        this.dropSelf(MyAddonBlocks.DRILL_CORE.get());
        this.dropSelf(MyAddonBlocks.FRAME_MODULE.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return MyAddonBlocks.getAllBlocks();
    }
}