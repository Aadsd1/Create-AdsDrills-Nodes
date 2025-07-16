package com.yourname.mycreateaddon.content.commands;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FindOreModsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("myaddon")
                        .then(Commands.literal("findOreMods")
                                .requires(source -> source.hasPermission(2)) // OP 권한 레벨 2 이상 필요
                                .executes(context -> run(context.getSource()))
                        )
        );
    }

    private static int run(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Registry<PlacedFeature> placedFeatureRegistry = source.getServer().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);

        // 모드 ID를 중복 없이 저장하기 위한 Set
        Set<String> modIdsWithOres = new HashSet<>();

        // 등록된 모든 PlacedFeature를 순회
        for (PlacedFeature placedFeature : placedFeatureRegistry) {
            // PlacedFeature가 가리키는 ConfiguredFeature를 가져옴
            ConfiguredFeature<?, ?> configuredFeature = placedFeature.feature().value();

            // ConfiguredFeature의 설정이 OreConfiguration인지 확인
            if (configuredFeature.config() instanceof OreConfiguration) {
                // 해당 PlacedFeature의 ResourceLocation에서 모드 ID(namespace)를 추출
                placedFeatureRegistry.getResourceKey(placedFeature).ifPresent(key -> {
                    modIdsWithOres.add(key.location().getNamespace());
                });
            }
        }

        if (modIdsWithOres.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No mods found with standard ore features.").withStyle(ChatFormatting.YELLOW), true);
            return 0;
        }

        // 감지된 모드 ID 목록을 알파벳순으로 정렬
        String modList = modIdsWithOres.stream()
                .sorted()
                .collect(Collectors.joining(", "));

        source.sendSuccess(() -> Component.literal("Detected mods with Ore Features:").withStyle(ChatFormatting.GOLD), true);
        player.sendSystemMessage(Component.literal(modList).withStyle(ChatFormatting.AQUA));
        source.sendSuccess(() -> Component.literal("These mods' ores are likely compatible with Ore Node generation.").withStyle(ChatFormatting.GRAY), true);

        return modIdsWithOres.size();
    }
}