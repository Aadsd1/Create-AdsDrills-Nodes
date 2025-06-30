package com.yourname.mycreateaddon.etc;

// import com.simibubi.create.foundation.utility.Lang;
// import net.minecraft.resources.ResourceLocation;
// import com.simibubi.create.AllPartialModels; // 나중에 필요할 수 있음
// import dev.engine_room.flywheel.lib.model.baked.PartialModel; // PartialModel 임포트 경로 확인 필요
// import com.yourname.mycreateaddon.MyCreateAddon;


import com.simibubi.create.AllPartialModels;
import com.simibubi.create.Create;
import com.simibubi.create.CreateClient;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;

public class MyAddonPartialModels {

    // --- ResourceLocation을 먼저 정의합니다 ---
    public static final ResourceLocation
            SHAFT_FOR_DRILL_LOCATION = loc("drill_shaft/block"),
            SHAFT_FOR_MODULE_LOCATION = loc("module_shaft/block")
            , ROTARY_DRILL_HEAD_LOCATION = loc("rotary_drill_head/block");

    // --- PartialModel은 위 ResourceLocation을 사용하여 생성합니다 ---
    public static final PartialModel
            SHAFT_FOR_DRILL = PartialModel.of(SHAFT_FOR_DRILL_LOCATION),
            SHAFT_FOR_MODULE = PartialModel.of(SHAFT_FOR_MODULE_LOCATION)
            , ROTARY_DRILL_HEAD = PartialModel.of(ROTARY_DRILL_HEAD_LOCATION);


    // 헬퍼 메서드 이름을 짧게 변경 (가독성)
    private static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "block/" + path);
    }

    public static void init() {
        AllPartialModels.init();
    }
}