package com.adsd.adsdrill.registry;

// --- 필요한 import 문 ---
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreRenderer;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreVisual;
import com.adsd.adsdrill.content.kinetics.drill.head.*;
import com.adsd.adsdrill.content.kinetics.module.GenericModuleBlockEntity;
import com.adsd.adsdrill.content.kinetics.module.GenericModuleVisual;
import com.adsd.adsdrill.content.kinetics.module.GenericModuleRenderer;
import com.adsd.adsdrill.content.kinetics.node.ArtificialNodeBlockEntity;
import com.adsd.adsdrill.content.kinetics.node.NodeFrameBlockEntity;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlockEntity;
import com.adsd.adsdrill.content.kinetics.node.NodeFrameBlockEntityRenderer;


public class AdsDrillBlockEntity {

    private static final CreateRegistrate REGISTRATE = AdsDrillAddon.registrate();

    public static final BlockEntityEntry<DrillCoreBlockEntity> DRILL_CORE = REGISTRATE
            .blockEntity("drill_core", DrillCoreBlockEntity::new)
            .visual(()->DrillCoreVisual::new)
            .validBlocks(AdsDrillBlocks.DRILL_CORE)
            .renderer(()->DrillCoreRenderer::new)
            .register();

    public static final BlockEntityEntry<GenericModuleBlockEntity> GENERIC_MODULE = REGISTRATE
            .blockEntity("generic_module", GenericModuleBlockEntity::new)
            .visual(()->GenericModuleVisual::new)
            .validBlocks(
                    AdsDrillBlocks.FRAME_MODULE,
                    AdsDrillBlocks.SPEED_MODULE,
                    AdsDrillBlocks.EFFICIENCY_MODULE, // [신규]
                    AdsDrillBlocks.REINFORCEMENT_MODULE, // [신규]
                    AdsDrillBlocks.ITEM_BUFFER_MODULE,
                    AdsDrillBlocks.FLUID_BUFFER_MODULE,
                    AdsDrillBlocks.HEATSINK_MODULE,
                    AdsDrillBlocks.COOLANT_MODULE,
                    AdsDrillBlocks.REDSTONE_BRAKE_MODULE,
                    AdsDrillBlocks.COMPACTOR_MODULE,
                    AdsDrillBlocks.FILTER_MODULE,
                    AdsDrillBlocks.FURNACE_MODULE,
                    AdsDrillBlocks.BLAST_FURNACE_MODULE,
                    AdsDrillBlocks.CRUSHER_MODULE,
                    AdsDrillBlocks.WASHER_MODULE,
                    AdsDrillBlocks.RESONATOR_MODULE, // [신규]
                    AdsDrillBlocks.ENERGY_INPUT_MODULE,
                    AdsDrillBlocks.ENERGY_BUFFER_MODULE,
                    AdsDrillBlocks.KINETIC_DYNAMO_MODULE
            )
            .renderer(()->GenericModuleRenderer::new)
            .register();
    public static final BlockEntityEntry<LaserDrillHeadBlockEntity> LASER_DRILL_HEAD = REGISTRATE
            .blockEntity("laser_drill_head", LaserDrillHeadBlockEntity::new)
            .visual(() -> LaserDrillHeadVisual::new) // [핵심 수정] Visual 등록
            .validBlocks(AdsDrillBlocks.LASER_DRILL_HEAD)
            .register();

    public static final BlockEntityEntry<OreNodeBlockEntity> ORE_NODE = REGISTRATE
            .blockEntity("ore_node", OreNodeBlockEntity::new)
             .validBlocks(AdsDrillBlocks.ORE_NODE)
            .register();
    public static final BlockEntityEntry<NodeFrameBlockEntity> NODE_FRAME = REGISTRATE
            .blockEntity("node_frame", NodeFrameBlockEntity::new)
            .validBlocks(AdsDrillBlocks.NODE_FRAME)
            .renderer(() -> NodeFrameBlockEntityRenderer::new)
            .register();
    public static final BlockEntityEntry<ArtificialNodeBlockEntity> ARTIFICIAL_NODE = REGISTRATE
            .blockEntity("artificial_node", ArtificialNodeBlockEntity::new)
            .validBlocks(AdsDrillBlocks.ARTIFICIAL_NODE)
            .register();
    // [신규] 펌프 헤드 BE 등록
    public static final BlockEntityEntry<PumpHeadBlockEntity> PUMP_HEAD = REGISTRATE
            .blockEntity("pump_head", PumpHeadBlockEntity::new)
            .visual(()->PumpHeadVisual::new) // 나중에 만들 파일
            .validBlocks(AdsDrillBlocks.PUMP_HEAD)
            .renderer(()->PumpHeadRenderer::new) // 나중에 만들 파일
            .register();
    public static final BlockEntityEntry<HydraulicDrillHeadBlockEntity> HYDRAULIC_DRILL_HEAD = REGISTRATE
            .blockEntity("hydraulic_drill_head", HydraulicDrillHeadBlockEntity::new)
            .visual(()->HydraulicDrillHeadVisual::new)
            .validBlocks(AdsDrillBlocks.HYDRAULIC_DRILL_HEAD)
            // 렌더러는 비워둬도 Visual이 작동합니다.
            .register();
    public static final BlockEntityEntry<RotaryDrillHeadBlockEntity> ROTARY_DRILL_HEAD = REGISTRATE
            .blockEntity("rotary_drill_head", RotaryDrillHeadBlockEntity::new)
            .visual(()->RotaryDrillHeadVisual::new)
            .validBlocks(AdsDrillBlocks.IRON_ROTARY_DRILL_HEAD, AdsDrillBlocks.DIAMOND_ROTARY_DRILL_HEAD,
                    AdsDrillBlocks.NETHERITE_ROTARY_DRILL_HEAD )
            .renderer(()->RotaryDrillHeadRenderer::new)
            .register();

    public static void register() {}
}