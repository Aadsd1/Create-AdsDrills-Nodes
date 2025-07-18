package com.yourname.mycreateaddon.content.commands;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yourname.mycreateaddon.config.MyAddonConfigs;
import com.yourname.mycreateaddon.content.kinetics.node.ArtificialNodeBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlockEntity;
import com.yourname.mycreateaddon.crafting.Quirk;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
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
        dispatcher.register(Commands.literal("myaddon")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("create_node")
                        .then(Commands.literal("simple")
                                .then(Commands.argument("composition", StringArgumentType.greedyString())
                                        .executes(context -> createSimpleNode(context, StringArgumentType.getString(context, "composition"), 10000)) // 기본 매장량
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
                                                                        // 유체 없이 생성
                                                                        .executes(context -> createFullNode(context, null, 0, null))
                                                                        // 유체와 함께 생성
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
                        .then(Commands.literal("artificial")
                                .then(Commands.argument("composition", StringArgumentType.string())
                                        .then(Commands.argument("max_yield", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("hardness", FloatArgumentType.floatArg(0.1f))
                                                        .then(Commands.argument("richness", FloatArgumentType.floatArg(0.1f))
                                                                .then(Commands.argument("regeneration", FloatArgumentType.floatArg(0.0f))
                                                                        // 특성 없이 생성
                                                                        .executes(context -> createFullNode(context, null, 0, ""))
                                                                        // 특성과 함께 생성
                                                                        .then(Commands.argument("quirks", StringArgumentType.greedyString())
                                                                                .executes(context -> createFullNode(context, null, 0, StringArgumentType.getString(context, "quirks")))
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

    private static int createSimpleNode(CommandContext<CommandSourceStack> context, String compositionStr, int maxYield) throws CommandSyntaxException {
        RandomSource random = context.getSource().getLevel().getRandom();
        // 월드 생성 로직과 유사한 랜덤 값 사용
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

        // 1. 플레이어가 바라보는 블록 확인
        HitResult hitResult = player.pick(10, 0, false);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("You must be looking at a block to create a node."));
            return 0;
        }
        BlockPos targetPos = ((BlockHitResult) hitResult).getBlockPos();
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.isAir() || targetState.getDestroySpeed(level, targetPos) < 0) {
            source.sendFailure(Component.literal("Cannot replace the target block."));
            return 0;
        }

        // 2. Composition 문자열 파싱
        Map<Block, Float> parsedComposition = new HashMap<>();
        float totalRatio = 0f;
        for (String part : compositionStr.split(" ")) {
            int lastColonIndex = part.lastIndexOf(':');
            if (lastColonIndex == -1 || lastColonIndex == 0 || lastColonIndex == part.length() - 1) {
                source.sendFailure(Component.literal("Invalid composition format in '" + part + "'. Expected 'modid:block_id:ratio'."));
                return 0;
            }

            String blockIdStr = part.substring(0, lastColonIndex);
            String ratioStr = part.substring(lastColonIndex + 1);

            Optional<Block> blockOpt = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(blockIdStr));
            if (blockOpt.isEmpty()) {
                source.sendFailure(Component.literal("Unknown block ID: " + blockIdStr));
                return 0;
            }
            try {
                float ratio = Float.parseFloat(ratioStr);
                parsedComposition.put(blockOpt.get(), ratio);
                totalRatio += ratio;
            } catch (NumberFormatException e) {
                source.sendFailure(Component.literal("Invalid ratio number: " + ratioStr));
                return 0;
            }
        }
        if (parsedComposition.isEmpty()) {
            source.sendFailure(Component.literal("Composition cannot be empty."));
            return 0;
        }

        // 3. 파싱된 데이터를 노드 데이터로 변환
        Map<Item, Float> finalComposition = new HashMap<>();
        Map<Item, Block> itemToBlockMap = new HashMap<>();
        Block representativeBlock = null;
        float highestRatio = -1f;

        for (Map.Entry<Block, Float> entry : parsedComposition.entrySet()) {
            Block oreBlock = entry.getKey();
            Item oreItem = getOreItem(oreBlock, level);

            if (oreItem == Items.AIR) {
                source.sendFailure(Component.literal("Could not determine a valid item for block: " + BuiltInRegistries.BLOCK.getKey(oreBlock)));
                return 0;
            }
            float normalizedRatio = entry.getValue() / totalRatio;
            finalComposition.put(oreItem, normalizedRatio);
            itemToBlockMap.put(oreItem, oreBlock);

            if (normalizedRatio > highestRatio) {
                highestRatio = normalizedRatio;
                representativeBlock = oreBlock;
            }
        }

        // 4. 유체 및 특성(Quirk) 처리
        FluidStack fluidStack = FluidStack.EMPTY;
        if (fluidId != null) {
            Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
            if (fluid != Fluids.EMPTY) {
                fluidStack = new FluidStack(fluid, 1);
            }
        }
        List<Quirk> quirks = new ArrayList<>();
        if (isArtificial && quirksStr != null && !quirksStr.isBlank()) {
            for (String quirkName : quirksStr.toLowerCase().split(" ")) {
                try {
                    quirks.add(Quirk.valueOf(quirkName.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    source.sendFailure(Component.literal("Unknown quirk: " + quirkName));
                    return 0;
                }
            }
        }

        // 5. 노드 블록 설치 및 BlockEntity 설정
        BlockState nodeBlockState = (isArtificial ? MyAddonBlocks.ARTIFICIAL_NODE : MyAddonBlocks.ORE_NODE).get().defaultBlockState();
        level.setBlock(targetPos, nodeBlockState, 3);
        BlockEntity be = level.getBlockEntity(targetPos);

        if (isArtificial && be instanceof ArtificialNodeBlockEntity artificialNode) {
            artificialNode.configureFromCrafting(quirks, finalComposition, itemToBlockMap, maxYield, hardness, richness, regeneration, fluidStack, fluidCapacity);
            source.sendSuccess(() -> Component.literal("Successfully created an Artificial Ore Node at " + targetPos.toShortString()).withStyle(ChatFormatting.GREEN), true);
        } else if (!isArtificial && be instanceof OreNodeBlockEntity oreNode) {
            oreNode.configureFromFeature(finalComposition, itemToBlockMap, maxYield, hardness, richness, regeneration, targetState.getBlock(), representativeBlock, fluidStack, fluidCapacity);
            source.sendSuccess(() -> Component.literal("Successfully created an Ore Node at " + targetPos.toShortString()).withStyle(ChatFormatting.GREEN), true);
        } else {
            // 실패 시 블록을 원래대로 되돌림
            level.setBlock(targetPos, targetState, 3);
            source.sendFailure(Component.literal("Failed to create node BlockEntity."));
            return 0;
        }

        return 1;
    }

    /**
     * OreNodeFeature의 로직과 동일한 결과를 보장하는 헬퍼 메서드.
     * 광석 블록으로부터 대표 아이템을 찾습니다.
     */
    private static Item getOreItem(Block oreBlock, ServerLevel level) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(oreBlock);
        Optional<ResourceLocation> manualItemId = MyAddonConfigs.SERVER.getManualMappingForItem(blockId);
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
}