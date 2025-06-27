package com.yourname.mycreateaddon.registry;

// --- 필요한 import 문 ---
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.DrillCoreBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.drill.DrillCoreRenderer;
import com.yourname.mycreateaddon.content.kinetics.drill.DrillCoreVisual;
import com.yourname.mycreateaddon.content.kinetics.module.Frame.FrameModuleBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.module.Frame.FrameModuleRenderer;
import com.yourname.mycreateaddon.content.kinetics.module.Frame.FrameModuleVisual;


public class MyAddonBlockEntity {

    private static final CreateRegistrate REGISTRATE = MyCreateAddon.registrate();

    public static final BlockEntityEntry<DrillCoreBlockEntity> DRILL_CORE = REGISTRATE
           .blockEntity("drill_core", DrillCoreBlockEntity::new)
            .visual(()-> DrillCoreVisual::new)
            .validBlocks(MyAddonBlocks.DRILL_CORE)
            .renderer(() -> DrillCoreRenderer::new)
            .register();

    // --- FRAME_MODULE 블록 엔티티를 새로 등록합니다. ---
    public static final BlockEntityEntry<FrameModuleBlockEntity> FRAME_MODULE = REGISTRATE
            .blockEntity("frame_module", FrameModuleBlockEntity::new)
            .visual(() -> FrameModuleVisual::new) // Visualizer 연결
            // validBlocks는 MyAddonBlocks에서 처리하므로 여기서는 필요 없음
            .validBlocks(MyAddonBlocks.FRAME_MODULE)
            .renderer(()-> FrameModuleRenderer::new)
            .register();

    public static void register() {}
}