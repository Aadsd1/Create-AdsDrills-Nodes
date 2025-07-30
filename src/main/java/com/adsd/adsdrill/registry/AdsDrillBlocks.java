package com.adsd.adsdrill.registry;

import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlock;
import com.adsd.adsdrill.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.adsd.adsdrill.content.kinetics.drill.head.*;
import com.adsd.adsdrill.content.kinetics.module.GenericModuleBlock;
import com.adsd.adsdrill.content.kinetics.module.ModuleType;
import com.adsd.adsdrill.content.kinetics.node.ArtificialNodeBlock;
import com.adsd.adsdrill.content.kinetics.node.NodeFrameBlock;
import com.adsd.adsdrill.content.kinetics.node.OreNodeBlock;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockModel;
import com.adsd.adsdrill.content.item.ArtificialNodeBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import com.adsd.adsdrill.content.item.DrillCoreItem;
import com.adsd.adsdrill.content.item.TooltipBlockItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class AdsDrillBlocks {

    private static final CreateRegistrate REGISTRATE = AdsDrillAddon.registrate();

    private static final int MINING_LEVEL_IRON = 1;
    private static final int MINING_LEVEL_DIAMOND = 2;


    public static final BlockEntry<DrillCoreBlock> DRILL_CORE = REGISTRATE
            .block("drill_core", DrillCoreBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((ctx, prov) -> prov.directionalBlock(ctx.get(), state -> {
                DrillCoreBlockEntity.Tier tier = state.getValue(DrillCoreBlock.TIER);
                return prov.models().getExistingFile(
                        prov.modLoc("block/drill_core/" + tier.name().toLowerCase())
                );
            }))
            .item(DrillCoreItem::new)
            .model((ctx, prov) -> {
                ItemModelBuilder builder = prov.withExistingParent(ctx.getName(), prov.modLoc("block/drill_core/brass"));
                builder.override()
                        .predicate(prov.modLoc("tier"), 1.0f)
                        .model(prov.getExistingFile(prov.modLoc("block/drill_core/steel")));
                builder.override()
                        .predicate(prov.modLoc("tier"), 2.0f)
                        .model(prov.getExistingFile(prov.modLoc("block/drill_core/netherite")));
            })
            .build()
            .register();

    public static final BlockEntry<GenericModuleBlock> FRAME_MODULE = REGISTRATE
            .block("frame_module", p -> new GenericModuleBlock(p, ModuleType.FRAME))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block,props)->new TooltipBlockItem(block,props,"tooltip.adsdrill.frame_module.description"))
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<GenericModuleBlock> SPEED_MODULE = REGISTRATE
            .block("speed_module", p -> new GenericModuleBlock(p, ModuleType.SPEED))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.speed_module.description") {
                @Override
                public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    tooltip.add(Component.literal(""));
                    ModuleType type = ModuleType.SPEED;
                    tooltip.add(Component.translatable("tooltip.adsdrill.speed_module.stats",
                                    String.format("%+.0f", type.getSpeedBonus() * 100),
                                    String.format("%+.0f", type.getStressImpact() * 100),
                                    String.format("%+.0f", type.getHeatModifier() * 100))
                            .withStyle(ChatFormatting.GOLD));
                }
            })
            .model((context, provider) -> provider.withExistingParent(context.getId().getPath(), provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();
    public static final BlockEntry<GenericModuleBlock> REINFORCEMENT_MODULE = REGISTRATE
            .block("reinforcement_module", p -> new GenericModuleBlock(p, ModuleType.REINFORCEMENT))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.reinforcement_module.description") {
                @Override
                public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    tooltip.add(Component.literal(""));
                    tooltip.add(Component.translatable("tooltip.adsdrill.reinforcement_module.stats",
                                    String.format("%+.0f", ModuleType.REINFORCEMENT.getStressImpact() * 100))
                            .withStyle(ChatFormatting.GOLD));
                }
            })
            .model((context, provider) -> provider.withExistingParent(context.getId().getPath(), provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();
    public static final BlockEntry<GenericModuleBlock> EFFICIENCY_MODULE = REGISTRATE
            .block("efficiency_module", p -> new GenericModuleBlock(p, ModuleType.EFFICIENCY))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.efficiency_module.description") {
                @Override
                public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    tooltip.add(Component.literal(""));
                    ModuleType type = ModuleType.EFFICIENCY;
                    tooltip.add(Component.translatable("tooltip.adsdrill.efficiency_module.stats",
                                    String.format("%+.0f", type.getSpeedBonus() * 100),
                                    String.format("%+.0f", type.getStressImpact() * 100),
                                    String.format("%+.0f", type.getHeatModifier() * 100))
                            .withStyle(ChatFormatting.GOLD));
                }
            })
            .model((context, provider) -> provider.withExistingParent(context.getId().getPath(), provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();
    public static final BlockEntry<GenericModuleBlock> ITEM_BUFFER_MODULE = REGISTRATE
            .block("item_buffer_module", p -> new GenericModuleBlock(p, ModuleType.ITEM_BUFFER))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.item_buffer_module.description"))
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<GenericModuleBlock> FLUID_BUFFER_MODULE = REGISTRATE
            .block("fluid_buffer_module", p -> new GenericModuleBlock(p, ModuleType.FLUID_BUFFER))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.fluid_buffer_module.description"))
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<GenericModuleBlock> REDSTONE_BRAKE_MODULE = REGISTRATE
            .block("redstone_brake_module", p -> new GenericModuleBlock(p, ModuleType.REDSTONE_BRAKE))
            .initialProperties(SharedProperties::stone)
            .properties(p->p.noOcclusion().isRedstoneConductor((state,level,pos)->true))
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.redstone_brake_module.description"))
            .model((c, p) -> p.withExistingParent(c.getId().getPath(), p.modLoc("block/" + c.getId().getPath() + "/block"))).build()
            .register();

    public static final BlockEntry<OreNodeBlock> ORE_NODE = REGISTRATE
            .block("ore_node", OreNodeBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.destroyTime(-1.0f).explosionResistance(3600000.0f)) // 파괴 불가
            .blockstate((c, p) ->
                    p.simpleBlock(c.get(),
                            AssetLookup.partialBaseModel(c, p)))
            .register();

    public static final BlockEntry<NodeFrameBlock> NODE_FRAME = REGISTRATE
            .block("node_frame", NodeFrameBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion().strength(10.0f, 1200.0f)) // 매우 단단하게 설정
            .blockstate((c, p) ->
                    p.simpleBlock(c.get(),
                            AssetLookup.partialBaseModel(c, p)))
            .lang("Node Frame")
            .item()
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<ArtificialNodeBlock> ARTIFICIAL_NODE = REGISTRATE
            .block("artificial_node", ArtificialNodeBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.destroyTime(-1.0f).explosionResistance(3600000.0f))
            .lang("Artificial Ore Node")
            .blockstate((c, p)
                    -> p.simpleBlock(c.get(), p.models().getExistingFile(
                    p.modLoc("block/" + c.getId().getPath() + "/block"))))
            .loot(RegistrateBlockLootTables::dropSelf)
            .item(ArtificialNodeBlockItem::new)
            .model((c, p)
                    -> p.withExistingParent(c.getId().getPath(),
                    p.modLoc("block/" + c.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<RotaryDrillHeadBlock> IRON_ROTARY_DRILL_HEAD = REGISTRATE
            .block("iron_rotary_drill_head", p -> new RotaryDrillHeadBlock(p, 0.25f, 0.05f, MINING_LEVEL_IRON, 4.0f))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .blockstate(BlockStateGen.directionalBlockProvider(true))
            .onRegister(CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.rotary_drill_head.description") {
                @Override
                public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    tooltip.add(Component.literal(""));
                    tooltip.add(Component.translatable("tooltip.adsdrill.rotary_drill_head.stats",
                            block.getStressImpact(), block.getHeatGeneration(), block.getCoolingRate()
                    ).withStyle(ChatFormatting.GOLD));
                }
            })
            .model((context, provider) -> provider.withExistingParent(context.getId().getPath(), provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<RotaryDrillHeadBlock> DIAMOND_ROTARY_DRILL_HEAD = REGISTRATE
            .block("diamond_rotary_drill_head", p -> new RotaryDrillHeadBlock(p, 0.15f, 0.12f, MINING_LEVEL_DIAMOND, 8.0f))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .blockstate(BlockStateGen.directionalBlockProvider(true))
            .onRegister(CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.rotary_drill_head.description") {
                @Override
                public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    tooltip.add(Component.literal(""));
                    tooltip.add(Component.translatable("tooltip.adsdrill.rotary_drill_head.stats",
                            block.getStressImpact(), block.getHeatGeneration(), block.getCoolingRate()
                    ).withStyle(ChatFormatting.GOLD));
                }
            })
            .model((context, provider) -> provider.withExistingParent(context.getId().getPath(), provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<RotaryDrillHeadBlock> NETHERITE_ROTARY_DRILL_HEAD = REGISTRATE
            .block("netherite_rotary_drill_head", p -> new RotaryDrillHeadBlock(p, 0.1f, 0.15f, 4, 16.0f))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .blockstate(BlockStateGen.directionalBlockProvider(true))
            .onRegister(CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.rotary_drill_head.description") {
                @Override
                public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    tooltip.add(Component.literal(""));
                    tooltip.add(Component.translatable("tooltip.adsdrill.rotary_drill_head.stats",
                            block.getStressImpact(), block.getHeatGeneration(), block.getCoolingRate()
                    ).withStyle(ChatFormatting.GOLD));
                }
            })
            .model((context, provider) -> provider.withExistingParent(context.getId().getPath(), provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<HydraulicDrillHeadBlock> HYDRAULIC_DRILL_HEAD = REGISTRATE
            .block("hydraulic_drill_head", HydraulicDrillHeadBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .blockstate(BlockStateGen.directionalBlockProvider(true))
            .onRegister(CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.hydraulic_drill_head.description") {
                @Override
                public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    tooltip.add(Component.literal("")); // 구분선
                    tooltip.add(Component.translatable("tooltip.adsdrill.hydraulic_drill_head.stats",
                                    AdsDrillConfigs.SERVER.hydraulicWaterConsumption.get())
                            .withStyle(ChatFormatting.GOLD));
                }
            })
            .model((c, p) -> p.withExistingParent(c.getId().getPath(), p.modLoc("block/" + c.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<PumpHeadBlock> PUMP_HEAD = REGISTRATE
            .block("pump_head", p -> new PumpHeadBlock(p, 4.0f))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .blockstate(BlockStateGen.directionalBlockProvider(true))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.pump_head.description") {
                @Override
                public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    tooltip.add(Component.literal("")); // 구분선
                    tooltip.add(Component.translatable("tooltip.adsdrill.pump_head.stats",
                                    AdsDrillConfigs.SERVER.pumpRatePer64RPM.get())
                            .withStyle(ChatFormatting.GOLD));
                }
            })
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/item")))
            .build()
            .register();

    public static final BlockEntry<ExplosiveDrillHeadBlock> EXPLOSIVE_DRILL_HEAD = REGISTRATE
            .block("explosive_drill_head", ExplosiveDrillHeadBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .blockstate((c, p) -> p.directionalBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.explosive_drill_head.description") {
                @Override
                public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    Item consumable = BuiltInRegistries.ITEM.get(ResourceLocation.parse(AdsDrillConfigs.SERVER.explosiveDrillConsumable.get()));
                    if (consumable == Items.AIR) consumable = Items.TNT;

                    tooltip.add(Component.literal(""));
                    tooltip.add(Component.translatable("tooltip.adsdrill.explosive_drill_head.consumable", consumable.getDescription())
                            .withStyle(ChatFormatting.GOLD));
                }
            })
            .model((context, provider) -> provider.withExistingParent(context.getId().getPath(), provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<GenericModuleBlock> FURNACE_MODULE = REGISTRATE
            .block("furnace_module", p -> new GenericModuleBlock(p, ModuleType.FURNACE))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p)
                    -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.furnace_module.description"))
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<GenericModuleBlock> BLAST_FURNACE_MODULE = REGISTRATE
            .block("blast_furnace_module", p -> new GenericModuleBlock(p, ModuleType.BLAST_FURNACE))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p)
                    -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.blast_furnace_module.description"))
            .model((c, p)
                    -> p.withExistingParent(c.getId().getPath(),
                    p.modLoc("block/" + c.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<GenericModuleBlock> CRUSHER_MODULE = REGISTRATE
            .block("crusher_module", p -> new GenericModuleBlock(p, ModuleType.CRUSHER))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p)
                    -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.crusher_module.description"))
            .model((c, p)
                    -> p.withExistingParent(c.getId().getPath(),
                    p.modLoc("block/" + c.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<GenericModuleBlock> WASHER_MODULE = REGISTRATE
            .block("washer_module", p -> new GenericModuleBlock(p, ModuleType.WASHER))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p)
                    -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.washer_module.description"))
            .model((c, p)
                    -> p.withExistingParent(c.getId().getPath(),
                    p.modLoc("block/" + c.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<GenericModuleBlock> HEATSINK_MODULE = REGISTRATE
            .block("heatsink_module", p -> new GenericModuleBlock(p, ModuleType.HEATSINK))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.heatsink_module.description") {
                @Override
                public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    tooltip.add(Component.literal(""));
                    tooltip.add(Component.translatable("tooltip.adsdrill.heatsink_module.stats",
                                    String.format("%.0f", Math.abs(ModuleType.HEATSINK.getHeatModifier()) * 100))
                            .withStyle(ChatFormatting.GOLD));
                }
            })
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<GenericModuleBlock> COOLANT_MODULE = REGISTRATE
            .block("coolant_module", p -> new GenericModuleBlock(p, ModuleType.COOLANT))
            .initialProperties(SharedProperties::stone)
            .properties(p->p.noOcclusion().isRedstoneConductor((state,level,pos)->true))
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.coolant_module.description") {
                @Override
                public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    tooltip.add(Component.literal(""));
                    tooltip.add(Component.translatable("tooltip.adsdrill.coolant_module.stats",
                                    AdsDrillConfigs.SERVER.coolantHeatReduction.get(),
                                    AdsDrillConfigs.SERVER.coolantWaterConsumption.get())
                            .withStyle(ChatFormatting.GOLD));
                    tooltip.add(Component.translatable("tooltip.adsdrill.coolant_module.after_cooling").withStyle(ChatFormatting.DARK_GREEN));
                }
            })
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<GenericModuleBlock> COMPACTOR_MODULE = REGISTRATE
            .block("compactor_module", p -> new GenericModuleBlock(p, ModuleType.COMPACTOR))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.compactor_module.description"))
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();
    public static final BlockEntry<GenericModuleBlock> FILTER_MODULE = REGISTRATE
            .block("filter_module", p -> new GenericModuleBlock(p, ModuleType.FILTER))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.filter_module.description"))
            .model((context, provider) ->
                    provider.withExistingParent(context.getId().getPath(),
                            provider.modLoc("block/" + context.getId().getPath() + "/block")))
            .build()
            .register();
    public static final BlockEntry<GenericModuleBlock> ENERGY_INPUT_MODULE = REGISTRATE
            .block("energy_input_module", p -> new GenericModuleBlock(p, ModuleType.ENERGY_INPUT))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.energy_input_module.description"))
            .model((c, p) -> p.withExistingParent(c.getId().getPath(), p.modLoc("block/" + c.getId().getPath() + "/block"))).build()
            .register();

    public static final BlockEntry<GenericModuleBlock> ENERGY_BUFFER_MODULE = REGISTRATE
            .block("energy_buffer_module", p -> new GenericModuleBlock(p, ModuleType.ENERGY_BUFFER))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.energy_buffer_module.description"))
            .model((c, p) -> p.withExistingParent(c.getId().getPath(), p.modLoc("block/" + c.getId().getPath() + "/block"))).build()
            .register();

    public static final BlockEntry<GenericModuleBlock> KINETIC_DYNAMO_MODULE = REGISTRATE
            .block("kinetic_dynamo_module", p -> new GenericModuleBlock(p, ModuleType.KINETIC_DYNAMO))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.kinetic_dynamo_module.description") {
                @Override
                public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    tooltip.add(Component.literal(""));
                    tooltip.add(Component.translatable("tooltip.adsdrill.kinetic_dynamo_module.stats",
                                    AdsDrillConfigs.SERVER.kineticDynamoConversionRate.get())
                            .withStyle(ChatFormatting.GOLD));
                }
            })
            .model((c, p) -> p.withExistingParent(c.getId().getPath(), p.modLoc("block/" + c.getId().getPath() + "/block"))).build()
            .register();

    public static final BlockEntry<GenericModuleBlock> RESONATOR_MODULE = REGISTRATE
            .block("resonator_module", p -> new GenericModuleBlock(p, ModuleType.RESONATOR))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .loot(RegistrateBlockLootTables::dropSelf)
            .blockstate((c, p) -> p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.resonator_module.description"))
            .model((c, p) ->
                    p.withExistingParent(c.getId().getPath(),
                            p.modLoc("block/" + c.getId().getPath() + "/block")))
            .build()
            .register();

    public static final BlockEntry<LaserDrillHeadBlock> LASER_DRILL_HEAD = REGISTRATE
            .block("laser_drill_head", LaserDrillHeadBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion().isRedstoneConductor((s,l,pos) -> false))
            .blockstate(BlockStateGen.directionalBlockProvider(true))
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.adsdrill.laser_drill_head.description") {
                @Override
                public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    tooltip.add(Component.literal(""));
                    tooltip.add(Component.translatable("tooltip.adsdrill.laser_drill_head.stats",
                                    AdsDrillConfigs.SERVER.laserEnergyPerMiningTick.get(),
                                    AdsDrillConfigs.SERVER.laserEnergyPerDecompositionTick.get())
                            .withStyle(ChatFormatting.GOLD));
                }
            })
            .model((c, p) -> p.withExistingParent(c.getId().getPath(), p.modLoc("block/" + c.getId().getPath() + "/item")))
            .build()
            .register();


    public static void register() {}
}