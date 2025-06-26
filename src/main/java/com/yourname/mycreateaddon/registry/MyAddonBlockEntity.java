package com.yourname.mycreateaddon.registry;

// --- 필요한 import 문 ---
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.DrillCoreBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.drill.DrillCoreRenderer;
import com.yourname.mycreateaddon.content.kinetics.drill.DrillCoreVisual;


public class MyAddonBlockEntity {

    private static final CreateRegistrate REGISTRATE = MyCreateAddon.registrate();

    public static final BlockEntityEntry<DrillCoreBlockEntity> DRILL_CORE = REGISTRATE
           .blockEntity("drill_core", DrillCoreBlockEntity::new)
            .visual(()-> DrillCoreVisual::new)
            .validBlocks(MyAddonBlocks.DRILL_CORE)
            .renderer(() -> DrillCoreRenderer::new)
            .register();

    public static void register() {}
}