package com.yourname.mycreateaddon.config;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;


public class MyAddonConfigs {

    public static final MyAddonServerConfig SERVER;
    private static final ModConfigSpec SERVER_SPEC;

    static {
        // 서버 설정 빌더와 스펙 생성
        final ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();
        SERVER = new MyAddonServerConfig(serverBuilder);
        SERVER_SPEC = serverBuilder.build();
    }

    // 서버 설정 값들을 정의하는 내부 클래스
    @SuppressWarnings("deprecation")
    public static class MyAddonServerConfig {
        public final ModConfigSpec.ConfigValue<List<? extends String>> allowedDimensions;

        public MyAddonServerConfig(ModConfigSpec.Builder builder) {
            builder.push("worldgen");

            allowedDimensions = builder
                    .comment(
                            "A list of dimension IDs where Ore Nodes are allowed to generate.",
                            "Examples: [\"minecraft:overworld\", \"minecraft:the_nether\"]"
                    )
                    .defineList("allowedDimensions",
                            List.of("minecraft:overworld"),
                            (obj) -> obj instanceof String);

            builder.pop();
        }
    }

    // 설정 등록을 위한 중앙 메서드
    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "mycreateaddon-server.toml");
    }
}