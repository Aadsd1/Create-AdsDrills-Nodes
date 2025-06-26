package com.yourname.mycreateaddon.client;

import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.etc.MyAddonPartialModels;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

//@Mod(value = MyCreateAddon.MOD_ID, dist = Dist.CLIENT)
public class MyAddonClient {

    public MyAddonClient(IEventBus modEventBus) {
        // PartialModel 클래스를 강제로 로드하여 static 필드들이 제때 초기화되도록 보장합니다.
        MyAddonPartialModels.init();

        // ClientSetup의 이벤트 리스너들을 등록합니다.
        modEventBus.register(ClientSetup.class);
    }
}