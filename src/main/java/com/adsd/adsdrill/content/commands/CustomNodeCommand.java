package com.adsd.adsdrill.content.commands;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.content.kinetics.node.ArtificialNodeBlockEntity;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlockEntity;
import com.adsd.adsdrill.crafting.Quirk;
import com.adsd.adsdrill.registry.AdsDrillBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.*;

public class CustomNodeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("adsdrill")
                .then(Commands.literal("create_node")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("help")
                                .executes(CustomNodeCommand::sendHelp)
                        )
                        .then(Commands.literal("simple")
                                .then(Commands.argument("composition", StringArgumentType.greedyString())
                                        .executes(context -> createSimpleNode(context, StringArgumentType.getString(context, "composition"), 10000))
                                        .then(Commands.argument("max_yield", IntegerArgumentType.integer(1))
                                                .executes(context -> createSimpleNode(context, StringArgumentType.getString(context, "composition"), IntegerArgumentType.getInteger(context, "max_yield")))
                                        )
                                )
                        )
                        .then(Commands.literal("full")
                                .then(Commands.argument("composition", StringArgumentType.string())
                                        .then(Commands.argument("max_yield", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("hardness", FloatArgumentType.floatArg(0.1f))
                                                        .then(Commands.argument("richness", FloatArgumentType.floatArg(0.1f))
                                                                .then(Commands.argument("regeneration", FloatArgumentType.floatArg(0.0f))
                                                                        .executes(context -> createFullNode(context, null, 0, null))
                                                                        .then(Commands.argument("fluid_id", ResourceLocationArgument.id())
                                                                                .suggests((ctx, builder) -> {
                                                                                    BuiltInRegistries.FLUID.keySet().forEach(rl -> builder.suggest(rl.toString()));
                                                                                    return builder.buildFuture();
                                                                                })
                                                                                .then(Commands.argument("fluid_capacity", IntegerArgumentType.integer(1))
                                                                                        .executes(context -> createFullNode(context, ResourceLocationArgument.getId(context, "fluid_id"), IntegerArgumentType.getInteger(context, "fluid_capacity"), null))
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        // [수정됨] artificial 명령어 구조 변경
                        .then(Commands.literal("artificial")
                                .then(Commands.argument("composition", StringArgumentType.string())
                                        .then(Commands.argument("max_yield", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("hardness", FloatArgumentType.floatArg(0.1f))
                                                        .then(Commands.argument("richness", FloatArgumentType.floatArg(0.1f))
                                                                .then(Commands.argument("regeneration", FloatArgumentType.floatArg(0.0f))
                                                                        // 분기 1: 유체 없이 생성
                                                                        .executes(context -> createFullNode(context, null, 0, "")) // 특성 없음
                                                                        .then(Commands.argument("quirks", StringArgumentType.greedyString())
                                                                                .executes(context -> createFullNode(context, null, 0, StringArgumentType.getString(context, "quirks"))) // 특성 있음
                                                                        )
                                                                        // 분기 2: 유체와 함께 생성
                                                                        .then(Commands.argument("fluid_id", ResourceLocationArgument.id())
                                                                                .suggests((ctx, builder) -> {
                                                                                    BuiltInRegistries.FLUID.keySet().forEach(rl -> builder.suggest(rl.toString()));
                                                                                    return builder.buildFuture();
                                                                                })
                                                                                .then(Commands.argument("fluid_capacity", IntegerArgumentType.integer(1))
                                                                                        .executes(context -> createFullNode(context, ResourceLocationArgument.getId(context, "fluid_id"), IntegerArgumentType.getInteger(context, "fluid_capacity"), "")) // 특성 없음
                                                                                        .then(Commands.argument("quirks", StringArgumentType.greedyString())
                                                                                                .executes(context -> createFullNode(context, ResourceLocationArgument.getId(context, "fluid_id"), IntegerArgumentType.getInteger(context, "fluid_capacity"), StringArgumentType.getString(context, "quirks"))) // 특성 있음
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int sendHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("--- AdsDrill: create_node Help ---").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("Creates a custom Ore Node at the block you are looking at.").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(""), false); // Blank line

        // Simple
        source.sendSuccess(() -> Component.literal("/adsdrill create_node simple <composition> [max_yield]").withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("  Creates a node with random stats.").withStyle(ChatFormatting.DARK_GRAY), false);
        source.sendSuccess(() -> Component.literal("  Example: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("/adsdrill create_node simple \"minecraft:iron_ore:1 minecraft:copper_ore:1\" 20000").withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // Full
        source.sendSuccess(() -> Component.literal("/adsdrill create_node full <comp> <yield> <hard> <rich> <regen> [fluid] [cap]").withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("  Creates a natural node with specific stats.").withStyle(ChatFormatting.DARK_GRAY), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // Artificial
        source.sendSuccess(() -> Component.literal("/adsdrill create_node artificial <comp> <yield> <hard> <rich> <regen> [fluid] [cap] [quirks]").withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("  Creates an artificial node with quirks and optional fluid.").withStyle(ChatFormatting.DARK_GRAY), false);
        source.sendSuccess(() -> Component.literal("  Example (no fluid): ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("/adsdrill create_node artificial \"minecraft:diamond_ore:1\" 50000 2.5 1.8 0.008 STEADY_HANDS").withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("  Example (with fluid): ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("/adsdrill create_node artificial \"minecraft:coal_ore:1\" 10000 1.0 1.0 0.0 minecraft:lava 8000 AQUIFER").withStyle(ChatFormatting.WHITE)), false);

        return 1;
    }

    private static int createSimpleNode(CommandContext<CommandSourceStack> context, String compositionStr, int maxYield) throws CommandSyntaxException {
        RandomSource random = context.getSource().getLevel().getRandom();
        float hardness = 0.5f + random.nextFloat() * 1.5f;
        float richness = 0.8f + random.nextFloat() * 0.7f;
        float regeneration = 0.0005f + random.nextFloat() * 0.0045f;

        return executeCreateNode(context, compositionStr, maxYield, hardness, richness, regeneration, null, 0, null, false);
    }

    private static int createFullNode(CommandContext<CommandSourceStack> context, @Nullable ResourceLocation fluidId, int fluidCapacity, @Nullable String quirksStr) throws CommandSyntaxException {
        String compositionStr = StringArgumentType.getString(context, "composition");
        int maxYield = IntegerArgumentType.getInteger(context, "max_yield");
        float hardness = FloatArgumentType.getFloat(context, "hardness");
        float richness = FloatArgumentType.getFloat(context, "richness");
        float regeneration = FloatArgumentType.getFloat(context, "regeneration");
        boolean isArtificial = quirksStr != null;

        return executeCreateNode(context, compositionStr, maxYield, hardness, richness, regeneration, fluidId, fluidCapacity, quirksStr, isArtificial);
    }


    private static int executeCreateNode(CommandContext<CommandSourceStack> context, String compositionStr, int maxYield, float hardness, float richness, float regeneration, @Nullable ResourceLocation fluidId, int fluidCapacity, @Nullable String quirksStr, boolean isArtificial) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();

        BlockPos targetPos = validateTargetBlock(source, player, level);
        if (targetPos == null) return 0;

        CompositionData compositionData = parseComposition(source, compositionStr, level);
        if (compositionData == null) return 0;

        FluidStack fluidStack = processFluid(fluidId);

        List<Quirk> quirks = parseQuirks(source, quirksStr, isArtificial);
        if (quirks == null) return 0;

        return createNode(source, level, targetPos, compositionData, maxYield, hardness,
                richness, regeneration, fluidStack, fluidCapacity, quirks, isArtificial);
    }

    private static BlockPos validateTargetBlock(CommandSourceStack source, ServerPlayer player, ServerLevel level) {
        HitResult hitResult = player.pick(10, 0, false);

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("You must be looking at a block to create a node."));
            return null;
        }

        BlockPos targetPos = ((BlockHitResult) hitResult).getBlockPos();
        BlockState targetState = level.getBlockState(targetPos);

        if (targetState.isAir() || targetState.getDestroySpeed(level, targetPos) < 0) {
            source.sendFailure(Component.literal("Cannot replace the target block."));
            return null;
        }

        return targetPos;
    }

    private static CompositionData parseComposition(CommandSourceStack source, String compositionStr, ServerLevel level) {
        Map<Block, Float> parsedComposition = new HashMap<>();
        float totalRatio = 0f;

        String[] parts = compositionStr.split(" ");
        for (String part : parts) {
            CompositionEntry entry = parseCompositionPart(source, part);
            if (entry == null) return null;

            parsedComposition.put(entry.block, entry.ratio);
            totalRatio += entry.ratio;
        }

        if (parsedComposition.isEmpty()) {
            source.sendFailure(Component.literal("Composition cannot be empty."));
            return null;
        }

        return convertToNodeData(source, parsedComposition, totalRatio, level);
    }

    private static CompositionEntry parseCompositionPart(CommandSourceStack source, String part) {
        int lastColonIndex = part.lastIndexOf(':');

        if (!isValidCompositionFormat(part, lastColonIndex)) {
            source.sendFailure(Component.literal(
                    "Invalid composition format in '" + part + "'. Expected 'modid:block_id:ratio'."
            ));
            return null;
        }

        String blockIdStr = part.substring(0, lastColonIndex);
        String ratioStr = part.substring(lastColonIndex + 1);

        Optional<Block> blockOpt = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(blockIdStr));
        if (blockOpt.isEmpty()) {
            source.sendFailure(Component.literal("Unknown block ID: " + blockIdStr));
            return null;
        }

        try {
            float ratio = Float.parseFloat(ratioStr);
            return new CompositionEntry(blockOpt.get(), ratio);
        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("Invalid ratio number: " + ratioStr));
            return null;
        }
    }

    private static boolean isValidCompositionFormat(String part, int lastColonIndex) {
        return lastColonIndex != -1 &&
                lastColonIndex != 0 &&
                lastColonIndex != part.length() - 1;
    }

    private static CompositionData convertToNodeData(CommandSourceStack source, Map<Block, Float> parsedComposition,
                                                     float totalRatio, ServerLevel level) {
        Map<Item, Float> finalComposition = new HashMap<>();
        Map<Item, Block> itemToBlockMap = new HashMap<>();
        Block representativeBlock = null;
        float highestRatio = -1f;

        for (Map.Entry<Block, Float> entry : parsedComposition.entrySet()) {
            Block oreBlock = entry.getKey();
            Item oreItem = getOreItem(oreBlock, level);

            if (oreItem == Items.AIR) {
                source.sendFailure(Component.literal(
                        "Could not determine a valid item for block: " + BuiltInRegistries.BLOCK.getKey(oreBlock)
                ));
                return null;
            }

            float normalizedRatio = entry.getValue() / totalRatio;
            finalComposition.put(oreItem, normalizedRatio);
            itemToBlockMap.put(oreItem, oreBlock);

            if (normalizedRatio > highestRatio) {
                highestRatio = normalizedRatio;
                representativeBlock = oreBlock;
            }
        }

        return new CompositionData(finalComposition, itemToBlockMap, representativeBlock);
    }

    private static FluidStack processFluid(@Nullable ResourceLocation fluidId) {
        if (fluidId == null) {
            return FluidStack.EMPTY;
        }

        Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
        return fluid != Fluids.EMPTY ? new FluidStack(fluid, 1) : FluidStack.EMPTY;
    }

    private static List<Quirk> parseQuirks(CommandSourceStack source, @Nullable String quirksStr, boolean isArtificial) {
        List<Quirk> quirks = new ArrayList<>();

        if (!isArtificial || quirksStr == null || quirksStr.isBlank()) {
            return quirks;
        }

        String[] quirkNames = quirksStr.toLowerCase().split(" ");
        for (String quirkName : quirkNames) {
            try {
                quirks.add(Quirk.valueOf(quirkName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.literal("Unknown quirk: " + quirkName));
                return null;
            }
        }

        return quirks;
    }

    private static int createNode(CommandSourceStack source, ServerLevel level, BlockPos targetPos,
                                  CompositionData compositionData, int maxYield, float hardness,
                                  float richness, float regeneration, FluidStack fluidStack,
                                  int fluidCapacity, List<Quirk> quirks, boolean isArtificial) {

        BlockState originalState = level.getBlockState(targetPos);
        BlockState nodeBlockState = getNodeBlockState(isArtificial);
        level.setBlock(targetPos, nodeBlockState, 3);
        BlockEntity blockEntity = level.getBlockEntity(targetPos);

        boolean success = configureNode(blockEntity, compositionData, maxYield, hardness, richness,
                regeneration, fluidStack, fluidCapacity, quirks, isArtificial,
                originalState.getBlock());

        if (success) {
            sendSuccessMessage(source, targetPos, isArtificial);
            return 1;
        } else {
            level.setBlock(targetPos, originalState, 3);
            source.sendFailure(Component.literal("Failed to create node BlockEntity."));
            return 0;
        }
    }

    private static BlockState getNodeBlockState(boolean isArtificial) {
        return (isArtificial ? AdsDrillBlocks.ARTIFICIAL_NODE : AdsDrillBlocks.ORE_NODE)
                .get()
                .defaultBlockState();
    }

    private static boolean configureNode(BlockEntity blockEntity, CompositionData compositionData,
                                         int maxYield, float hardness, float richness, float regeneration,
                                         FluidStack fluidStack, int fluidCapacity, List<Quirk> quirks,
                                         boolean isArtificial, Block originalBlock) {

        if (isArtificial && blockEntity instanceof ArtificialNodeBlockEntity artificialNode) {
            artificialNode.configureFromCrafting(
                    quirks,
                    compositionData.finalComposition,
                    compositionData.itemToBlockMap,
                    maxYield, hardness, richness, regeneration,
                    fluidStack, fluidCapacity
            );
            return true;
        } else if (!isArtificial && blockEntity instanceof OreNodeBlockEntity oreNode) {
            oreNode.configureFromFeature(
                    compositionData.finalComposition,
                    compositionData.itemToBlockMap,
                    maxYield, hardness, richness, regeneration,
                    originalBlock, compositionData.representativeBlock,
                    fluidStack, fluidCapacity
            );
            return true;
        }

        return false;
    }

    private static void sendSuccessMessage(CommandSourceStack source, BlockPos targetPos, boolean isArtificial) {
        String nodeType = isArtificial ? "Artificial Ore Node" : "Ore Node";
        Component message = Component.literal("Successfully created an " + nodeType + " at " + targetPos.toShortString())
                .withStyle(ChatFormatting.GREEN);
        source.sendSuccess(() -> message, true);
    }

    private static Item getOreItem(Block oreBlock, ServerLevel level) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(oreBlock);
        Optional<ResourceLocation> manualItemId = AdsDrillConfigs.SERVER.getManualMappingForItem(blockId);
        if (manualItemId.isPresent()) {
            return BuiltInRegistries.ITEM.getOptional(manualItemId.get()).orElse(Items.AIR);
        }

        Item oreBlockItem = oreBlock.asItem();
        if (oreBlockItem == Items.AIR) {
            return Items.AIR;
        }

        var recipeManager = level.getServer().getRecipeManager();
        for (var recipeHolder : recipeManager.getAllRecipesFor(RecipeType.SMELTING)) {
            SmeltingRecipe recipe = recipeHolder.value();
            if (recipe.getIngredients().getFirst().test(new ItemStack(oreBlockItem))) {
                ItemStack resultStack = recipe.getResultItem(level.registryAccess());
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

    private record CompositionEntry(Block block, float ratio) {
    }

    private record CompositionData(Map<Item, Float> finalComposition, Map<Item, Block> itemToBlockMap,
                                   Block representativeBlock) {
    }
}