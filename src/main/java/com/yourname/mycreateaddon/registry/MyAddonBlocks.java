package com.yourname.mycreateaddon.registry;

import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlock;
import com.yourname.mycreateaddon.content.kinetics.drill.head.RotaryDrillHeadBlock;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleBlock;
import com.yourname.mycreateaddon.content.kinetics.module.ModuleType;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlock;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockModel;
import net.minecraft.world.level.block.state.BlockBehaviour;



public class MyAddonBlocks {

    private static final CreateRegistrate REGISTRATE = MyCreateAddon.registrate();

    public static final BlockEntry<DrillCoreBlock> DRILL_CORE = REGISTRATE
            .block("drill_core", DrillCoreBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
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
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
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
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item()
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<GenericModuleBlock> ITEM_BUFFER_MODULE = REGISTRATE
            .block("item_buffer_module", p -> new GenericModuleBlock(p, ModuleType.ITEM_BUFFER))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item()
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<GenericModuleBlock> FLUID_BUFFER_MODULE = REGISTRATE
            .block("fluid_buffer_module", p -> new GenericModuleBlock(p, ModuleType.FLUID_BUFFER))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item()
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();


    public static final BlockEntry<OreNodeBlock> ORE_NODE = REGISTRATE
            .block("ore_node", OreNodeBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.destroyTime(-1.0f).explosionResistance(3600000.0f)) // 파괴 불가
            .blockstate((c, p) ->
                    p.simpleBlock(c.get(),
                            AssetLookup.partialBaseModel(c, p)))
            .item()
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();


    public static final BlockEntry<RotaryDrillHeadBlock> IRON_ROTARY_DRILL_HEAD = REGISTRATE
            .block("iron_rotary_drill_head", p -> new RotaryDrillHeadBlock(p, 0.25f, 0.05f))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .blockstate(BlockStateGen.directionalBlockProvider(true))
            .onRegister(CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new))
            .item()
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<RotaryDrillHeadBlock> DIAMOND_ROTARY_DRILL_HEAD = REGISTRATE
            .block("diamond_rotary_drill_head", p -> new RotaryDrillHeadBlock(p, 0.15f, 0.12f))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .blockstate(BlockStateGen.directionalBlockProvider(true))
            .onRegister(CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new))
            .item()
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();


    public static final BlockEntry<GenericModuleBlock> FURNACE_MODULE = REGISTRATE
            .block("furnace_module", p -> new GenericModuleBlock(p, ModuleType.FURNACE))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p)
                    -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item()
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();


    // [2단계 추가] 신규 모듈 블록 등록
    public static final BlockEntry<GenericModuleBlock> BLAST_FURNACE_MODULE = REGISTRATE
            .block("blast_furnace_module", p -> new GenericModuleBlock(p, ModuleType.BLAST_FURNACE))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p)
                    -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item()
            .model((c, p)
                    -> p.withExistingParent(c.getId().getPath(),
                    p.modLoc("block/" + c.getId().getPath() + "/block")))
            .build()
            .register();


    public static final BlockEntry<GenericModuleBlock> CRUSHER_MODULE = REGISTRATE
            .block("crusher_module", p -> new GenericModuleBlock(p, ModuleType.CRUSHER))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p)
                    -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item().model((c, p)
                    -> p.withExistingParent(c.getId().getPath(),
                    p.modLoc("block/" + c.getId().getPath() + "/block")))
            .build()
            .register();


    public static final BlockEntry<GenericModuleBlock> WASHER_MODULE = REGISTRATE
            .block("washer_module", p -> new GenericModuleBlock(p, ModuleType.WASHER))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p)
                    -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item().model((c, p)
                    -> p.withExistingParent(c.getId().getPath(),
                    p.modLoc("block/" + c.getId().getPath() + "/block")))
            .build()
            .register();

    public static void register() {}
}