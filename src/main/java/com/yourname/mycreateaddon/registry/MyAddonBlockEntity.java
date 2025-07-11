package com.yourname.mycreateaddon.registry;

// --- 필요한 import 문 ---
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreRenderer;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreVisual;
import com.yourname.mycreateaddon.content.kinetics.drill.head.*;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleVisual;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleRenderer;
import com.yourname.mycreateaddon.content.kinetics.node.ArtificialNodeBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.node.NodeFrameBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlockEntity;

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
            .validBlocks(
                    MyAddonBlocks.FRAME_MODULE,
                    MyAddonBlocks.SPEED_MODULE,
                    MyAddonBlocks.EFFICIENCY_MODULE, // [신규]
                    MyAddonBlocks.REINFORCEMENT_MODULE, // [신규]
                    MyAddonBlocks.ITEM_BUFFER_MODULE,
                    MyAddonBlocks.FLUID_BUFFER_MODULE,
                    MyAddonBlocks.HEATSINK_MODULE,
                    MyAddonBlocks.COOLANT_MODULE,
                    MyAddonBlocks.REDSTONE_BRAKE_MODULE,
                    MyAddonBlocks.COMPACTOR_MODULE,
                    MyAddonBlocks.FILTER_MODULE,
                    MyAddonBlocks.FURNACE_MODULE,
                    MyAddonBlocks.BLAST_FURNACE_MODULE,
                    MyAddonBlocks.CRUSHER_MODULE,
                    MyAddonBlocks.WASHER_MODULE,
                    MyAddonBlocks.RESONATOR_MODULE, // [신규]
                    MyAddonBlocks.ENERGY_INPUT_MODULE,
                    MyAddonBlocks.ENERGY_BUFFER_MODULE,
                    MyAddonBlocks.KINETIC_DYNAMO_MODULE
            )
            .renderer(()->GenericModuleRenderer::new)
            .register();
    public static final BlockEntityEntry<LaserDrillHeadBlockEntity> LASER_DRILL_HEAD = REGISTRATE
            .blockEntity("laser_drill_head", LaserDrillHeadBlockEntity::new)
            .visual(() -> LaserDrillHeadVisual::new) // [핵심 수정] Visual 등록
            .validBlocks(MyAddonBlocks.LASER_DRILL_HEAD)
            .register();

    public static final BlockEntityEntry<OreNodeBlockEntity> ORE_NODE = REGISTRATE
            .blockEntity("ore_node", OreNodeBlockEntity::new)
             .validBlocks(MyAddonBlocks.ORE_NODE)
            .register();
    public static final BlockEntityEntry<NodeFrameBlockEntity> NODE_FRAME = REGISTRATE
            .blockEntity("node_frame", NodeFrameBlockEntity::new)
            .validBlocks(MyAddonBlocks.NODE_FRAME)
            .register();
    public static final BlockEntityEntry<ArtificialNodeBlockEntity> ARTIFICIAL_NODE = REGISTRATE
            .blockEntity("artificial_node", ArtificialNodeBlockEntity::new)
            .validBlocks(MyAddonBlocks.ARTIFICIAL_NODE)
            .register();
    // [신규] 펌프 헤드 BE 등록
    public static final BlockEntityEntry<PumpHeadBlockEntity> PUMP_HEAD = REGISTRATE
            .blockEntity("pump_head", PumpHeadBlockEntity::new)
            .visual(()->PumpHeadVisual::new) // 나중에 만들 파일
            .validBlocks(MyAddonBlocks.PUMP_HEAD)
            .renderer(()->PumpHeadRenderer::new) // 나중에 만들 파일
            .register();
    public static final BlockEntityEntry<HydraulicDrillHeadBlockEntity> HYDRAULIC_DRILL_HEAD = REGISTRATE
            .blockEntity("hydraulic_drill_head", HydraulicDrillHeadBlockEntity::new)
            .visual(()->HydraulicDrillHeadVisual::new)
            .validBlocks(MyAddonBlocks.HYDRAULIC_DRILL_HEAD)
            // 렌더러는 비워둬도 Visual이 작동합니다.
            .register();
    public static final BlockEntityEntry<RotaryDrillHeadBlockEntity> ROTARY_DRILL_HEAD = REGISTRATE
            .blockEntity("rotary_drill_head", RotaryDrillHeadBlockEntity::new)
            .visual(()->RotaryDrillHeadVisual::new)
            .validBlocks(MyAddonBlocks.IRON_ROTARY_DRILL_HEAD, MyAddonBlocks.DIAMOND_ROTARY_DRILL_HEAD,
                    MyAddonBlocks.NETHERITE_ROTARY_DRILL_HEAD )
            .renderer(()->RotaryDrillHeadRenderer::new)
            .register();

    public static void register() {}
}