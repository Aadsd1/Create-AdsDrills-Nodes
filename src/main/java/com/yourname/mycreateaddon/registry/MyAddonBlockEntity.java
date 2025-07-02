package com.yourname.mycreateaddon.registry;

// --- 필요한 import 문 ---
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreRenderer;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreVisual;
import com.yourname.mycreateaddon.content.kinetics.drill.head.RotaryDrillHeadBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleVisual;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleRenderer;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.drill.head.RotaryDrillHeadRenderer;
import com.yourname.mycreateaddon.content.kinetics.drill.head.RotaryDrillHeadVisual ;

public class MyAddonBlockEntity {

    private static final CreateRegistrate REGISTRATE = MyCreateAddon.registrate();

    public static final BlockEntityEntry<DrillCoreBlockEntity> DRILL_CORE = REGISTRATE
            .blockEntity("drill_core", DrillCoreBlockEntity::new)
            .visual(()->DrillCoreVisual::new)
            .validBlocks(MyAddonBlocks.DRILL_CORE)
            .renderer(()->DrillCoreRenderer::new)
            .register();

    public static final BlockEntityEntry<GenericModuleBlockEntity> GENERIC_MODULE = REGISTRATE
            .blockEntity("generic_module", GenericModuleBlockEntity::new)
            .visual(()->GenericModuleVisual::new)
            .validBlocks(MyAddonBlocks.FRAME_MODULE, MyAddonBlocks.SPEED_MODULE, MyAddonBlocks.ITEM_BUFFER_MODULE, MyAddonBlocks.FLUID_BUFFER_MODULE)
            .renderer(()->GenericModuleRenderer::new)
            .register();

    public static final BlockEntityEntry<OreNodeBlockEntity> ORE_NODE = REGISTRATE
            .blockEntity("ore_node", OreNodeBlockEntity::new)
            .validBlocks(MyAddonBlocks.ORE_NODE)
            .register();

    public static final BlockEntityEntry<RotaryDrillHeadBlockEntity> ROTARY_DRILL_HEAD = REGISTRATE
            .blockEntity("rotary_drill_head", RotaryDrillHeadBlockEntity::new)
            .visual(() -> RotaryDrillHeadVisual::new)
            .validBlocks(MyAddonBlocks.ROTARY_DRILL_HEAD)
            .renderer(() -> RotaryDrillHeadRenderer::new)
            .register();


    public static void register() {}
}