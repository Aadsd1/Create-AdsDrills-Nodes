package com.yourname.mycreateaddon;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.yourname.mycreateaddon.config.MyAddonConfigs;
import com.yourname.mycreateaddon.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MyCreateAddon.MOD_ID)
public class MyCreateAddon {

    public static final String MOD_ID = "mycreateaddon";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    public MyCreateAddon(IEventBus modEventBus, ModContainer modContainer) {
        REGISTRATE.registerEventListeners(modEventBus);


        // [수정] 설정 등록 방식 변경
        MyAddonConfigs.register(modContainer);
        MyAddonBlocks.register();
        MyAddonItems.register();
        MyAddonBlockEntity.register();
        MyAddonFeatures.register(); // 추가
        MyAddonBiomeModifiers.register(modEventBus); // [신규] BiomeModifier 등록
    }


    public static CreateRegistrate registrate() {
        return REGISTRATE;
    }
}