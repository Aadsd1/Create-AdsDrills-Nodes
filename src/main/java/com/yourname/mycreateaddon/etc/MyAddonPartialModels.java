package com.yourname.mycreateaddon.etc;

// import com.simibubi.create.foundation.utility.Lang;
// import net.minecraft.resources.ResourceLocation;
// import com.simibubi.create.AllPartialModels; // 나중에 필요할 수 있음
// import dev.engine_room.flywheel.lib.model.baked.PartialModel; // PartialModel 임포트 경로 확인 필요
// import com.yourname.mycreateaddon.MyCreateAddon;


import com.simibubi.create.AllPartialModels;
import com.yourname.mycreateaddon.MyCreateAddon;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public class MyAddonPartialModels {

    // --- ResourceLocation을 먼저 정의합니다 ---
    public static final ResourceLocation
            SHAFT_FOR_DRILL_LOCATION = loc("drill_shaft/block"),
            SHAFT_FOR_MODULE_LOCATION = loc("module_shaft/block"), IRON_ROTARY_DRILL_HEAD_LOCATION = loc("iron_rotary_drill_head/block"),
            DIAMOND_ROTARY_DRILL_HEAD_LOCATION = loc("diamond_rotary_drill_head/block"),
            IRON_DRILL_BODY_LOCATION = loc("rotary_drill_head/body/iron"),
            DIAMOND_DRILL_BODY_LOCATION = loc("rotary_drill_head/body/diamond"),
            DRILL_TIP_DIAMOND_LOCATION = loc("rotary_drill_head/tip/diamond"),
            DRILL_TIP_IRON_LOCATION = loc("rotary_drill_head/tip/iron"),
            DRILL_TIP_GOLD_LOCATION = loc("rotary_drill_head/tip/gold"),
            DRILL_TIP_EMERALD_LOCATION = loc("rotary_drill_head/tip/emerald");

    // --- PartialModel은 위 ResourceLocation을 사용하여 생성합니다 ---
    public static final PartialModel
            SHAFT_FOR_DRILL = PartialModel.of(SHAFT_FOR_DRILL_LOCATION),
            SHAFT_FOR_MODULE = PartialModel.of(SHAFT_FOR_MODULE_LOCATION),
            IRON_ROTARY_DRILL_HEAD = PartialModel.of(IRON_ROTARY_DRILL_HEAD_LOCATION),
            DIAMOND_ROTARY_DRILL_HEAD = PartialModel.of(DIAMOND_ROTARY_DRILL_HEAD_LOCATION),
            IRON_DRILL_BODY=PartialModel.of(IRON_DRILL_BODY_LOCATION),
            DIAMOND_DRILL_BODY=PartialModel.of(DIAMOND_DRILL_BODY_LOCATION),
            IRON_DRILL_TIP=PartialModel.of(DRILL_TIP_IRON_LOCATION),
            DIAMOND_DRILL_TIP=PartialModel.of(DRILL_TIP_DIAMOND_LOCATION),
            EMERALD_DRILL_TIP=PartialModel.of(DRILL_TIP_EMERALD_LOCATION),
            GOLD_DRILL_TIP=PartialModel.of(DRILL_TIP_GOLD_LOCATION);



    // 헬퍼 메서드 이름을 짧게 변경 (가독성)
    private static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "block/" + path);
    }

    public static void init() {
        AllPartialModels.init();
    }
}