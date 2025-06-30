package com.yourname.mycreateaddon.registry;

import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlock;
import com.yourname.mycreateaddon.content.kinetics.drill.head.RotaryDrillHeadBlock;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleBlock;
import com.yourname.mycreateaddon.content.kinetics.module.ModuleType;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlock;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockModel;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;


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


    public static final BlockEntry<OreNodeBlock> ORE_NODE = REGISTRATE
            .block("ore_node", OreNodeBlock::new)
            // SharedProperties.stone()은 기본 돌 속성을 제공합니다.
            // 여기에 파괴 불가 및 피스톤 저항 속성을 추가합니다.
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



    public static final BlockEntry<RotaryDrillHeadBlock> ROTARY_DRILL_HEAD = REGISTRATE
            .block("rotary_drill_head", RotaryDrillHeadBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion())
            .blockstate(BlockStateGen.directionalBlockProvider(true))
            .onRegister(CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new))
            .item()
            .model((c,p)->
                    p.withExistingParent(c.getId().getPath(),
                            p.modLoc("block/"+c.getId().getPath()+"/block")))
            .build()
            .register();





    public static void register() {}
}