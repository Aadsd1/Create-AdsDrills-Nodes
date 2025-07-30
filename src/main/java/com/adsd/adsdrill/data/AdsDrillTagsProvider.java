package com.adsd.adsdrill.data;


import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.registry.AdsDrillItems;
import com.adsd.adsdrill.registry.AdsDrillTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class AdsDrillTagsProvider extends ItemTagsProvider {

    public AdsDrillTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTagsProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTagsProvider, AdsDrillAddon.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        this.tag(AdsDrillTags.STEEL_INGOTS)
                .add(AdsDrillItems.STEEL_INGOT.get());
    }
}