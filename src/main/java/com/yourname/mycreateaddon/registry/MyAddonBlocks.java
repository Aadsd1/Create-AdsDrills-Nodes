package com.yourname.mycreateaddon.registry;

import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.DrillCoreBlock;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleBlock;
import com.yourname.mycreateaddon.content.kinetics.module.ModuleType;


public class MyAddonBlocks {

    private static final CreateRegistrate REGISTRATE = MyCreateAddon.registrate();

    public static final BlockEntry<DrillCoreBlock> DRILL_CORE = REGISTRATE
            .block("drill_core", DrillCoreBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion())
            .loot((tables, block) -> tables.dropSelf(block))
            .blockstate((c, p) -> p.directionalBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item()
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<GenericModuleBlock> FRAME_MODULE = REGISTRATE
            .block("frame_module", p -> new GenericModuleBlock(p, ModuleType.FRAME))
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion())
            .loot((tables, block) -> tables.dropSelf(block))
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item()
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<GenericModuleBlock> SPEED_MODULE = REGISTRATE
            .block("speed_module", p -> new GenericModuleBlock(p, ModuleType.SPEED))
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion())
            .loot((tables, block) -> tables.dropSelf(block))
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item()
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static void register() {}
}