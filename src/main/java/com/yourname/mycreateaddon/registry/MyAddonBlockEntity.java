package com.yourname.mycreateaddon.registry;

// --- 필요한 import 문 ---
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual; // 이 클래스를 사용합니다.
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.DrillCoreBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.drill.DrillCoreRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.world.level.block.state.BlockState;

public class MyAddonBlockEntity {

    private static final CreateRegistrate REGISTRATE = MyCreateAddon.registrate();

    public static final BlockEntityEntry<DrillCoreBlockEntity> DRILL_CORE = REGISTRATE
            .blockEntity("drill_core", DrillCoreBlockEntity::new)
            // --- MECHANICAL_PISTON과 완전히 동일하게 수정합니다. ---
            .visual(() -> SingleAxisRotatingVisual::shaft, false)
            .validBlocks(MyAddonBlocks.DRILL_CORE)
            // Flywheel이 꺼졌을 때를 위한 예비 렌더러를 등록합니다.
            .renderer(() -> DrillCoreRenderer::new)
            .register();

    public static void register() {}
}