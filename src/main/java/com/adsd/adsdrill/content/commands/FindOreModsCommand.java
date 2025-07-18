package com.adsd.adsdrill.content.commands;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.adsd.adsdrill.config.AdsDrillConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.*;
import java.util.stream.Collectors;

public class FindOreModsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("myaddon")
                .then(Commands.literal("findOreMods")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> listMods(context.getSource())) // 서브명령어 없이 실행 시 모드 목록 표시
                        .then(Commands.literal("details")
                                .then(Commands.argument("modid", StringArgumentType.string())
                                        .executes(context -> showModDetails(context.getSource(), StringArgumentType.getString(context, "modid")))
                                )
                        )
                );
        dispatcher.register(command);
    }

    private static int listMods(CommandSourceStack source) {
        Registry<PlacedFeature> placedFeatureRegistry = source.getServer().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        Set<String> modIdsWithOres = new HashSet<>();

        for (PlacedFeature placedFeature : placedFeatureRegistry) {
            ConfiguredFeature<?, ?> configuredFeature = placedFeature.feature().value();
            if (configuredFeature.config() instanceof OreConfiguration) {
                placedFeatureRegistry.getResourceKey(placedFeature).ifPresent(key -> modIdsWithOres.add(key.location().getNamespace()));
            }
        }

        if (modIdsWithOres.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No mods found with standard ore features.").withStyle(ChatFormatting.YELLOW), true);
            return 0;
        }

        String modList = modIdsWithOres.stream().sorted().collect(Collectors.joining(", "));

        source.sendSuccess(() -> Component.literal("Detected mods with Ore Features:").withStyle(ChatFormatting.GOLD), true);
        source.sendSuccess(() -> Component.literal(modList).withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("Use '/myaddon findOreMods details <modid>' for more info.").withStyle(ChatFormatting.GRAY), true);

        return modIdsWithOres.size();
    }

    private static int showModDetails(CommandSourceStack source, String modid) {
        Registry<PlacedFeature> placedFeatureRegistry = source.getServer().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        Set<String> foundOres = new TreeSet<>(); // 자동 정렬을 위해 TreeSet 사용

        for (PlacedFeature placedFeature : placedFeatureRegistry) {
            Optional<ResourceLocation> keyOpt = placedFeatureRegistry.getResourceKey(placedFeature).map(ResourceKey::location);
            if (keyOpt.isEmpty() || !keyOpt.get().getNamespace().equals(modid)) {
                continue;
            }

            ConfiguredFeature<?, ?> configuredFeature = placedFeature.feature().value();
            if (configuredFeature.config() instanceof OreConfiguration oreConfig) {
                for (OreConfiguration.TargetBlockState target : oreConfig.targetStates) {
                    Block oreBlock = target.state.getBlock();
                    Item resultItem = getOreItem(oreBlock, source);

                    if (resultItem != Items.AIR) {
                        String mapping = String.format("%s -> %s",
                                BuiltInRegistries.BLOCK.getKey(oreBlock),
                                BuiltInRegistries.ITEM.getKey(resultItem)
                        );
                        foundOres.add(mapping);
                    }
                }
            }
        }

        if (foundOres.isEmpty()) {
            source.sendFailure(Component.literal("No compatible ore features found for mod: " + modid).withStyle(ChatFormatting.RED));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Ore to Item mappings for '").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(modid).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("':").withStyle(ChatFormatting.GOLD)), true);

        foundOres.forEach(ore -> source.sendSuccess(() -> Component.literal("- " + ore).withStyle(ChatFormatting.GRAY), false));

        return foundOres.size();
    }

    // OreNodeFeature의 로직을 가져와 명령어에서 사용
    private static Item getOreItem(Block oreBlock, CommandSourceStack source) {
        // 1. 수동 매핑 확인
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(oreBlock);
        Optional<ResourceLocation> manualItemId = AdsDrillConfigs.SERVER.getManualMappingForItem(blockId);
        if (manualItemId.isPresent()) {
            return BuiltInRegistries.ITEM.getOptional(manualItemId.get()).orElse(Items.AIR);
        }

        // 2. 제련 레시피 확인
        Item oreBlockItem = oreBlock.asItem();
        if (oreBlockItem == Items.AIR) return Items.AIR;

        var recipeManager = source.getServer().getRecipeManager();
        for (var recipeHolder : recipeManager.getAllRecipesFor(RecipeType.SMELTING)) {
            SmeltingRecipe recipe = recipeHolder.value();
            if (recipe.getIngredients().getFirst().test(new ItemStack(oreBlockItem))) {
                ItemStack resultStack = recipe.getResultItem(source.registryAccess());
                Item resultItem = resultStack.getItem();
                ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(resultItem);

                if (resultId.getPath().endsWith("_ingot")) {
                    String rawMaterialName = resultId.getPath().replace("_ingot", "");
                    ResourceLocation rawId = ResourceLocation.fromNamespaceAndPath(resultId.getNamespace(), "raw_" + rawMaterialName);
                    Item rawItem = BuiltInRegistries.ITEM.get(rawId);
                    if (rawItem != Items.AIR) {
                        return rawItem;
                    }
                }
                return resultItem;
            }
        }
        return oreBlockItem;
    }
}