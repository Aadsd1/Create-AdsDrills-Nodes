package com.yourname.mycreateaddon.registry;

import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.DrillCoreBlock;
import com.yourname.mycreateaddon.content.kinetics.module.Frame.FrameModuleBlock;

public class MyAddonBlocks {

    private static final CreateRegistrate REGISTRATE = MyCreateAddon.registrate();

    public static final BlockEntry<DrillCoreBlock> DRILL_CORE = REGISTRATE
            .block("drill_core", DrillCoreBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion())
            .loot((tables, block) -> tables.dropSelf(block))
            // --- blockstate 빌더에 람다를 직접 전달하여 경로를 명시합니다. ---
            .blockstate((context, provider) -> {
                // Create의 directionalBlock 헬퍼를 사용하되, 모델 파일을 AssetLookup으로 찾도록 합니다.
                provider.directionalBlock(context.get(), AssetLookup.partialBaseModel(context, provider));
            })
            .item()
            .model((context, provider) -> {
                // 아이템 모델은 블록 모델을 그대로 상속합니다.
                provider.withExistingParent(context.getId().getPath(), provider.modLoc("block/" + context.getId().getPath() + "/block"));
            })
            .build()
            .register();

    public static final BlockEntry<FrameModuleBlock> FRAME_MODULE = REGISTRATE
            .block("frame_module", FrameModuleBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion())
            .loot((tables, block) -> tables.dropSelf(block))
            .blockstate((c, p) ->
                    p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c,p)))
            .item()
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static void register() {}
}