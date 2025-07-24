package com.adsd.adsdrill.compat.jei;


import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.compat.jei.category.*;
import com.adsd.adsdrill.config.AdsDrillConfigs;
import com.adsd.adsdrill.crafting.Quirk;
import com.adsd.adsdrill.registry.AdsDrillBlocks;
import com.adsd.adsdrill.registry.AdsDrillItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.jetbrains.annotations.NotNull;

import mezz.jei.api.constants.VanillaTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


@JeiPlugin
public class AdsDrillJEIPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(AdsDrillAddon.MOD_ID, "jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new NodeFrameCategory(helper),
                new ModuleUpgradeCategory(helper),
                new DrillHeadUpgradeCategory(helper),
                new DrillCoreUpgradeCategory(helper),
                new NodeCombinationCategory(helper),
                new LaserDecompositionCategory(helper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(NodeFrameCategory.TYPE, NodeFrameCategory.getRecipes());
        registration.addRecipes(ModuleUpgradeCategory.TYPE, ModuleUpgradeCategory.getRecipes());
        registration.addRecipes(DrillHeadUpgradeCategory.TYPE, DrillHeadUpgradeCategory.getRecipes());
        registration.addRecipes(DrillCoreUpgradeCategory.TYPE, DrillCoreUpgradeCategory.getRecipes());
        registration.addRecipes(NodeCombinationCategory.TYPE, NodeCombinationCategory.getRecipes());
        registration.addRecipes(LaserDecompositionCategory.TYPE, LaserDecompositionCategory.getRecipes());

        registration.addIngredientInfo(
                List.of(
                        new ItemStack(AdsDrillItems.BRASS_STABILIZER_CORE.get()),
                        new ItemStack(AdsDrillItems.STEEL_STABILIZER_CORE.get()),
                        new ItemStack(AdsDrillItems.NETHERITE_STABILIZER_CORE.get())
                ),
                mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                Component.translatable("adsdrill.jei.info.stabilizer_cores")
        );
        addAnvilTuningRecipes(registration);


        // --- Register JEI Information Pages ---
        ItemStack drillCore = new ItemStack(AdsDrillBlocks.DRILL_CORE.get());
        ItemStack ironDrillHead = new ItemStack(AdsDrillBlocks.IRON_ROTARY_DRILL_HEAD.get());
        ItemStack frameModule = new ItemStack(AdsDrillBlocks.FRAME_MODULE.get());
        ItemStack nodeFrame = new ItemStack(AdsDrillBlocks.NODE_FRAME.get());
        ItemStack artificialNode = new ItemStack(AdsDrillBlocks.ARTIFICIAL_NODE.get());
        ItemStack brassLocator = new ItemStack(AdsDrillItems.BRASS_NODE_LOCATOR.get());

        // Page 1: Introduction
        registration.addIngredientInfo(drillCore, VanillaTypes.ITEM_STACK,
                Component.translatable("adsdrill.jei.info.introduction.title"),
                Component.translatable("adsdrill.jei.info.introduction.1"),
                Component.translatable("adsdrill.jei.info.introduction.2"),
                Component.translatable("adsdrill.jei.info.introduction.3"),
                Component.translatable("adsdrill.jei.info.introduction.4")
        );

        // Page 2: Drill Assembly
        registration.addIngredientInfo(drillCore, VanillaTypes.ITEM_STACK,
                Component.translatable("adsdrill.jei.info.structure.title"),
                Component.translatable("adsdrill.jei.info.structure.1"),
                Component.translatable("adsdrill.jei.info.structure.2"),
                Component.translatable("adsdrill.jei.info.structure.3"),
                Component.translatable("adsdrill.jei.info.structure.4"),
                Component.translatable("adsdrill.jei.info.structure.5"),
                Component.translatable("adsdrill.jei.info.structure.error.loop"),
                Component.translatable("adsdrill.jei.info.structure.error.multiple_cores"),
                Component.translatable("adsdrill.jei.info.structure.error.limit"),
                Component.translatable("adsdrill.jei.info.structure.error.duplicate_processing")
        );

        // Page 3: Heat Management System
        registration.addIngredientInfo(drillCore, VanillaTypes.ITEM_STACK,
                Component.translatable("adsdrill.jei.info.heat.title"),
                Component.translatable("adsdrill.jei.info.heat.1"),
                Component.translatable("adsdrill.jei.info.heat.2"),
                Component.translatable("adsdrill.jei.info.heat.3"),
                Component.translatable("adsdrill.jei.info.heat.range.normal", String.format("%.1f", AdsDrillConfigs.SERVER.heatBoostStartThreshold.get() - 0.1)),
                Component.translatable("adsdrill.jei.info.heat.range.boost", AdsDrillConfigs.SERVER.heatBoostStartThreshold.get(), String.format("%.1f", AdsDrillConfigs.SERVER.heatOverloadStartThreshold.get() - 0.1), AdsDrillConfigs.SERVER.heatEfficiencyBonus.get()),
                Component.translatable("adsdrill.jei.info.heat.range.overload", AdsDrillConfigs.SERVER.heatOverloadStartThreshold.get()),
                Component.translatable("adsdrill.jei.info.heat.range.overheated", AdsDrillConfigs.SERVER.heatCooldownResetThreshold.get())
        );

        // Page 4: Mining Logic Explained
        registration.addIngredientInfo(ironDrillHead, VanillaTypes.ITEM_STACK,
                Component.translatable("adsdrill.jei.info.mining.title"),
                Component.translatable("adsdrill.jei.info.mining.1"),
                Component.translatable("adsdrill.jei.info.mining.step1_power"),
                Component.translatable("adsdrill.jei.info.mining.step1_detail", AdsDrillConfigs.SERVER.rotarySpeedDivisor.get(), AdsDrillConfigs.SERVER.laserMiningAmount.get()),
                Component.translatable("adsdrill.jei.info.mining.step2_effective_power"),
                Component.translatable("adsdrill.jei.info.mining.step2_detail"),
                Component.translatable("adsdrill.jei.info.mining.step3_progress"),
                Component.translatable("adsdrill.jei.info.mining.step3_detail"),
                Component.translatable("adsdrill.jei.info.mining.step4_cycle"),
                Component.translatable("adsdrill.jei.info.mining.step4_detail")
        );

        // Page 5: Module System & Priority
        registration.addIngredientInfo(frameModule, VanillaTypes.ITEM_STACK,
                Component.translatable("adsdrill.jei.info.modules.title"),
                Component.translatable("adsdrill.jei.info.modules.1"),
                Component.translatable("adsdrill.jei.info.modules.2"),
                Component.translatable("adsdrill.jei.info.modules.3"),
                Component.translatable("adsdrill.jei.info.modules.4")
        );

        // Page 6: Artificial Nodes & Quirks
        List<Component> quirkComponents = new ArrayList<>();
        quirkComponents.add(Component.translatable("adsdrill.jei.info.quirks.title"));
        quirkComponents.add(Component.translatable("adsdrill.jei.info.quirks.1"));
        quirkComponents.add(Component.translatable("adsdrill.jei.info.quirks.list_header"));
        for (Quirk quirk : Quirk.values()) {
            var config = AdsDrillConfigs.getQuirkConfig(quirk);
            if (config.chance() > 0) {
                quirkComponents.add(Component.translatable("adsdrill.jei.info.quirk." + quirk.getId() + ".with_chance", String.format("%.0f", config.chance() * 100)));
            } else {
                quirkComponents.add(Component.translatable("adsdrill.jei.info.quirk." + quirk.getId() + ".base"));
            }
        }
        registration.addIngredientInfo(artificialNode, VanillaTypes.ITEM_STACK, quirkComponents.toArray(new Component[0]));

        // Page 7: Advanced Node Crafting
        registration.addIngredientInfo(nodeFrame, VanillaTypes.ITEM_STACK,
                Component.translatable("adsdrill.jei.info.crafting.title"),
                Component.translatable("adsdrill.jei.info.crafting.1"),
                Component.translatable("adsdrill.jei.info.crafting.2"),
                Component.translatable("adsdrill.jei.info.crafting.sub.composition"),
                Component.translatable("adsdrill.jei.info.crafting.sub.yield"),
                Component.translatable("adsdrill.jei.info.crafting.3"),
                Component.translatable("adsdrill.jei.info.crafting.sub.stats"),
                Component.translatable("adsdrill.jei.info.crafting.4"),
                Component.translatable("adsdrill.jei.info.crafting.sub.fluid"),
                Component.translatable("adsdrill.jei.info.crafting.5"),
                Component.translatable("adsdrill.jei.info.crafting.sub.quirk_core"),
                Component.translatable("adsdrill.jei.info.crafting.sub.quirk_catalyst"),
                Component.translatable("adsdrill.jei.info.crafting.6")
        );

        // Page 8: Natural Ore Nodes
        registration.addIngredientInfo(brassLocator, VanillaTypes.ITEM_STACK,
                Component.translatable("adsdrill.jei.info.natural_nodes.title"),
                Component.translatable("adsdrill.jei.info.natural_nodes.1"),
                Component.translatable("adsdrill.jei.info.natural_nodes.2"),
                Component.translatable("adsdrill.jei.info.natural_nodes.3"),
                Component.translatable("adsdrill.jei.info.natural_nodes.4"),
                Component.translatable("adsdrill.jei.info.natural_nodes.5"),
                Component.translatable("adsdrill.jei.info.natural_nodes.6")
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(AdsDrillBlocks.NODE_FRAME.get()), NodeFrameCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(AdsDrillBlocks.FRAME_MODULE.get()), ModuleUpgradeCategory.TYPE);

        Stream.of(AdsDrillBlocks.IRON_ROTARY_DRILL_HEAD, AdsDrillBlocks.DIAMOND_ROTARY_DRILL_HEAD, AdsDrillBlocks.NETHERITE_ROTARY_DRILL_HEAD)
                .forEach(drill -> registration.addRecipeCatalyst(new ItemStack(drill.get()), DrillHeadUpgradeCategory.TYPE));

        registration.addRecipeCatalyst(new ItemStack(AdsDrillBlocks.DRILL_CORE.get()), DrillCoreUpgradeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(AdsDrillBlocks.EXPLOSIVE_DRILL_HEAD.get()), NodeCombinationCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(AdsDrillBlocks.LASER_DRILL_HEAD.get()), LaserDecompositionCategory.TYPE);
    }

    private void addAnvilTuningRecipes(IRecipeRegistration registration) {
        ItemStack locator = new ItemStack(AdsDrillItems.NETHERITE_NODE_LOCATOR.get());
        ItemStack ironOre = new ItemStack(Items.IRON_ORE);
        ItemStack tunedWithIron = locator.copy();
        CompoundTag nbt = new CompoundTag();
        nbt.putString("TargetOre", "minecraft:raw_iron");
        tunedWithIron.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

        // Wrap recipe in RecipeHolder
        RecipeHolder<SmithingRecipe> ironRecipe = new RecipeHolder<>(
                ResourceLocation.fromNamespaceAndPath(AdsDrillAddon.MOD_ID, "jei/anvil_tuning/ore_tuning"),
                new SmithingTransformRecipe(Ingredient.EMPTY, Ingredient.of(locator), Ingredient.of(ironOre), tunedWithIron)
        );

        ItemStack flint = new ItemStack(Items.FLINT);
        ItemStack clearedLocator = locator.copy();

        RecipeHolder<SmithingRecipe> clearRecipe = new RecipeHolder<>(
                ResourceLocation.fromNamespaceAndPath(AdsDrillAddon.MOD_ID, "jei/anvil_tuning/clear_tuning"),
                new SmithingTransformRecipe(Ingredient.EMPTY, Ingredient.of(tunedWithIron), Ingredient.of(flint), clearedLocator)
        );

        registration.addRecipes(RecipeTypes.SMITHING, List.of(ironRecipe, clearRecipe));

        registration.addIngredientInfo(
                new ItemStack(AdsDrillItems.NETHERITE_NODE_LOCATOR.get()),
                mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                Component.translatable("adsdrill.jei.info.anvil_tuning")
        );
    }
}