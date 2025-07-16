package com.yourname.mycreateaddon.data.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.data.recipe.MechanicalCraftingRecipeBuilder;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import com.yourname.mycreateaddon.registry.MyAddonItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class AnyCraftRecipeGen extends RecipeProvider {


    public AnyCraftRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(MyAddonItems.RAW_ROSE_GOLD_CHUNK.get()),RecipeCategory.MISC,MyAddonItems.ROSE_GOLD.get(),0.7f,200)
                .unlockedBy("has_rose_gold_chunk",has(MyAddonItems.RAW_ROSE_GOLD_CHUNK.get()))
                .save(recipeOutput);

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(MyAddonItems.RAW_STEEL_CHUNK.get()),RecipeCategory.MISC,MyAddonItems.STEEL_INGOT.get(),0.7f,200)
                .unlockedBy("has_steel_chunk",has(MyAddonItems.RAW_STEEL_CHUNK.get()))
                .save(recipeOutput);

        MechanicalCraftingRecipeBuilder.shapedRecipe(MyAddonBlocks.DIAMOND_ROTARY_DRILL_HEAD.get(), 1)
                .patternLine("  A  ")
                .patternLine(" ABA ")
                .patternLine("ABCBA")
                .patternLine(" ABA ")
                .patternLine("  A  ")
                .key('A', Items.DIAMOND)
                .key('B', Items.DIAMOND_BLOCK)
                .key('C', MyAddonBlocks.IRON_ROTARY_DRILL_HEAD.get())
                .build(recipeOutput);

        MechanicalCraftingRecipeBuilder.shapedRecipe(MyAddonBlocks.NETHERITE_ROTARY_DRILL_HEAD.get(),1)
                .patternLine("ACBCA")
                .patternLine("CBABC")
                .patternLine("BADAB")
                .patternLine("CBABC")
                .patternLine("ACBCA")
                .key('A',Items.NETHERITE_INGOT)
                .key('B',Items.DIAMOND_BLOCK)
                .key('C', MyAddonItems.STEEL_INGOT.get())
                .key('D',MyAddonBlocks.DIAMOND_ROTARY_DRILL_HEAD.get())
                .build(recipeOutput);

        MechanicalCraftingRecipeBuilder.shapedRecipe(MyAddonBlocks.EXPLOSIVE_DRILL_HEAD.get(),1)
                .patternLine("ABA")
                .patternLine("ACA")
                .patternLine("EDF")
                .patternLine("AGA")
                .key('A', AllItems.IRON_SHEET.get())
                .key('B',Items.STONE_SLAB)
                .key('C',AllItems.ELECTRON_TUBE.get())
                .key('D',Items.WATER_BUCKET)
                .key('E',Items.DISPENSER)
                .key('F',Items.REDSTONE_BLOCK)
                .key('G', AllBlocks.DEPLOYER.get())
                .build(recipeOutput);

        MechanicalCraftingRecipeBuilder.shapedRecipe(MyAddonBlocks.NODE_FRAME.get(),1)
                .patternLine("AAAAA")
                .patternLine("ABCBA")
                .patternLine("ADEFA")
                .patternLine("ABGBA")
                .patternLine("AAAAA")
                .key('A',MyAddonItems.STEEL_INGOT.get())
                .key('B',AllBlocks.SHAFT.get())
                .key('C',MyAddonItems.ROSE_GOLD.get())
                .key('D',MyAddonItems.XOMV.get())
                .key('E',AllBlocks.BASIN.get())
                .key('F',MyAddonItems.THUNDER_STONE.get())
                .key('G',MyAddonItems.IVORY_CRYSTAL.get())
                .build(recipeOutput);

        MechanicalCraftingRecipeBuilder.shapedRecipe(MyAddonItems.BRASS_STABILIZER_CORE.get(),1)
                .patternLine("AAAA")
                .patternLine("ABCA")
                .patternLine("ADEA")
                .patternLine("AAAA")
                .key('A',AllItems.BRASS_SHEET.get())
                .key('B',Items.DIAMOND_BLOCK)
                .key('C',AllBlocks.COGWHEEL.get())
                .key('D',Items.PUFFERFISH)
                .key('E',Items.GOLDEN_APPLE)
                .build(recipeOutput);

        MechanicalCraftingRecipeBuilder.shapedRecipe(MyAddonItems.STEEL_STABILIZER_CORE.get(),1)
                .patternLine("AAAA")
                .patternLine("ABCA")
                .patternLine("ADEA")
                .patternLine("AAAA")
                .key('A',MyAddonItems.STEEL_INGOT.get())
                .key('B',MyAddonItems.IVORY_CRYSTAL.get())
                .key('C',MyAddonItems.XOMV.get())
                .key('D',MyAddonItems.THE_FOSSIL.get())
                .key('E',MyAddonItems.SILKY_JEWEL.get())
                .build(recipeOutput);

        MechanicalCraftingRecipeBuilder.shapedRecipe(MyAddonItems.NETHERITE_STABILIZER_CORE.get(),1)
                .patternLine("AAAA")
                .patternLine("FBCF")
                .patternLine("FDEF")
                .patternLine("AAAA")
                .key('A',Items.NETHERITE_INGOT)
                .key('B',MyAddonItems.ROSE_GOLD.get())
                .key('C',AllItems.PRECISION_MECHANISM.get())
                .key('D',MyAddonItems.KOH_I_NOOR.get())
                .key('E',MyAddonItems.THE_FOSSIL.get())
                .key('F',MyAddonItems.STEEL_INGOT.get())
                .build(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonBlocks.IRON_ROTARY_DRILL_HEAD.get())
                .pattern(" A ")
                .pattern("AAA")
                .pattern("ABA")
                .define('A',Items.IRON_BLOCK)
                .define('B',AllBlocks.MECHANICAL_DRILL.get())
                .unlockedBy("has_drill",has(AllBlocks.MECHANICAL_DRILL.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonBlocks.PUMP_HEAD.get())
                .pattern("ABC")
                .pattern("BDB")
                .pattern("EBA")
                .define('A',AllBlocks.COGWHEEL.get())
                .define('B',AllItems.COPPER_SHEET.get())
                .define('C',MyAddonItems.STEEL_INGOT.get())
                .define('D',AllBlocks.MECHANICAL_PUMP.get())
                .define('E',AllBlocks.HOSE_PULLEY.get())
                .unlockedBy("has_steel_ingot",has(MyAddonItems.STEEL_INGOT.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonBlocks.HYDRAULIC_DRILL_HEAD.get())
                .pattern("ABC")
                .pattern("DED")
                .pattern("CBA")
                .define('A',AllBlocks.COGWHEEL.get())
                .define('B',MyAddonItems.STEEL_INGOT.get())
                .define('C',MyAddonItems.ULTRAMARINE.get())
                .define('D',Items.DISPENSER)
                .define('E',MyAddonBlocks.IRON_ROTARY_DRILL_HEAD.get())
                .unlockedBy("has_first_drill",has(MyAddonBlocks.IRON_ROTARY_DRILL_HEAD.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonBlocks.FRAME_MODULE.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A',Items.OBSIDIAN)
                .define('B',AllItems.BRASS_SHEET)
                .define('C',AllBlocks.SHAFT.get())
                .define('D',AllBlocks.GEARBOX.get())
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, MyAddonItems.BRASS_NODE_LOCATOR.get())
                .pattern(" C ")
                .pattern("ABA")
                .pattern(" D ")
                .define('A', AllItems.BRASS_INGOT.get())
                .define('B', AllItems.ELECTRON_TUBE.get())
                .define('C', AllItems.BRASS_SHEET.get())
                .define('D', Items.COMPASS)
                .unlockedBy("has_electron_tube", has(AllItems.ELECTRON_TUBE.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, MyAddonItems.STEEL_NODE_LOCATOR.get())
                .pattern(" C ")
                .pattern("ABA")
                .pattern(" D ")
                .define('A', MyAddonItems.STEEL_INGOT.get())
                .define('B', MyAddonItems.BRASS_NODE_LOCATOR.get())
                .define('C', AllItems.PRECISION_MECHANISM.get())
                .define('D', MyAddonItems.ROSE_GOLD.get())
                .unlockedBy("has_brass_locator", has(MyAddonItems.BRASS_NODE_LOCATOR.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, MyAddonItems.NETHERITE_NODE_LOCATOR.get())
                .pattern(" C ")
                .pattern("ABA")
                .pattern(" D ")
                .define('A', Items.NETHERITE_INGOT)
                .define('B', MyAddonItems.STEEL_NODE_LOCATOR.get())
                .define('C', MyAddonItems.THUNDER_STONE.get())
                .define('D', Items.LODESTONE)
                .unlockedBy("has_steel_locator", has(MyAddonItems.STEEL_NODE_LOCATOR.get()))
                .save(recipeOutput);


        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_SPEED_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDE")
                .pattern("ABA")
                .define('A',Items.OBSIDIAN)
                .define('B',AllItems.BRASS_SHEET.get())
                .define('C',MyAddonItems.STEEL_INGOT.get())
                .define('D',Items.FEATHER)
                .define('E',MyAddonItems.CINNABAR.get())
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_REIN_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A',Items.OBSIDIAN)
                .define('B',AllItems.IRON_SHEET.get())
                .define('C',AllBlocks.COGWHEEL.get())
                .define('D',AllBlocks.LARGE_COGWHEEL.get())
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_EFFI_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDE")
                .pattern("AFA")
                .define('A',Items.OBSIDIAN)
                .define('B',MyAddonItems.ROSE_GOLD.get())
                .define('C',MyAddonBlocks.REINFORCEMENT_MODULE.get())
                .define('D',MyAddonItems.STEEL_INGOT.get())
                .define('E',MyAddonBlocks.HEATSINK_MODULE.get())
                .define('F',MyAddonItems.SILKY_JEWEL.get())
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_HEATSINK_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A',Items.OBSIDIAN)
                .define('B',AllItems.BRASS_SHEET.get())
                .define('C',Items.PACKED_ICE)
                .define('D',Items.WATER_BUCKET)
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_I_BUFFER_UPGRADE.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A',Items.OBSIDIAN)
                .define('B',AllItems.BRASS_SHEET.get())
                .define('C',Items.CHEST)
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_F_BUFFER_UPGRADE.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A',Items.OBSIDIAN)
                .define('B',AllItems.BRASS_SHEET.get())
                .define('C',AllBlocks.FLUID_TANK.get())
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_R_BRAKE_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A',Items.OBSIDIAN)
                .define('B',AllItems.BRASS_SHEET.get())
                .define('C',Items.PISTON)
                .define('D',Items.REDSTONE_TORCH)
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_FURNACE_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("AEA")
                .define('A',Items.OBSIDIAN)
                .define('B',Items.FURNACE)
                .define('C',AllItems.BRASS_SHEET.get())
                .define('D',MyAddonItems.STEEL_INGOT.get())
                .define('E',AllBlocks.BLAZE_BURNER.get())
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_BF_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("AEA")
                .define('A',Items.OBSIDIAN)
                .define('B',Items.BLAST_FURNACE)
                .define('C',MyAddonItems.ROSE_GOLD.get())
                .define('D',MyAddonBlocks.FURNACE_MODULE.get())
                .define('E',AllItems.BLAZE_CAKE.get())
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_CRUSH_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A',Items.OBSIDIAN)
                .define('B',MyAddonItems.STEEL_INGOT.get())
                .define('C',AllBlocks.CRUSHING_WHEEL.get())
                .define('D',AllBlocks.CHUTE.get())
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_WASH_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDE")
                .pattern("ABA")
                .define('A',Items.OBSIDIAN)
                .define('B',AllItems.BRASS_SHEET.get())
                .define('C',AllBlocks.ENCASED_FAN.get())
                .define('D',Items.IRON_BARS)
                .define('E',Items.WATER_BUCKET)
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_COOL_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A',Items.OBSIDIAN)
                .define('B',MyAddonItems.ULTRAMARINE.get())
                .define('C',Items.BLUE_ICE)
                .define('D',Items.WATER_BUCKET)
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_COMP_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDE")
                .pattern("AFA")
                .define('A',Items.OBSIDIAN)
                .define('B',AllBlocks.MECHANICAL_PRESS.get())
                .define('C',Items.PISTON)
                .define('D',Items.ANVIL)
                .define('E',MyAddonItems.THUNDER_STONE.get())
                .define('F',AllBlocks.BASIN.get())
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_FILTER_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A',Items.OBSIDIAN)
                .define('B',Items.OBSERVER)
                .define('C',AllItems.BRASS_SHEET.get())
                .define('D',AllBlocks.DEPOT.get())
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_E_INPUT_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDE")
                .pattern("ABA")
                .define('A',Items.OBSIDIAN)
                .define('B',AllItems.BRASS_SHEET.get())
                .define('C',AllItems.PRECISION_MECHANISM.get())
                .define('D',MyAddonItems.THUNDER_STONE.get())
                .define('E',MyAddonItems.ROSE_GOLD.get())
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_E_BUFFER_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDE")
                .pattern("ABA")
                .define('A',Items.OBSIDIAN)
                .define('B',AllItems.BRASS_SHEET.get())
                .define('C',Items.REPEATER)
                .define('D',MyAddonItems.THUNDER_STONE.get())
                .define('E',MyAddonItems.CINNABAR.get())
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_E_GEN_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDE")
                .pattern("ABA")
                .define('A',Items.OBSIDIAN)
                .define('B',AllItems.BRASS_SHEET.get())
                .define('C',AllBlocks.WINDMILL_BEARING.get())
                .define('D',MyAddonItems.THUNDER_STONE.get())
                .define('E',MyAddonItems.CINNABAR.get())
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_RESO_UPGRADE.get())
                .pattern("ABA")
                .pattern("CDE")
                .pattern("AFA")
                .define('A',Items.OBSIDIAN)
                .define('B',AllItems.BRASS_SHEET.get())
                .define('C',MyAddonItems.IVORY_CRYSTAL.get())
                .define('D',MyAddonItems.KOH_I_NOOR.get())
                .define('E',MyAddonItems.STEEL_INGOT.get())
                .define('F',AllItems.ATTRIBUTE_FILTER.get())
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.MODULE_UPGRADE_REMOVER.get())
                .pattern("ABC")
                .pattern("BCB")
                .pattern("CBA")
                .define('A',Items.OBSIDIAN)
                .define('B',Items.IRON_NUGGET)
                .define('C',Items.FLINT)
                .unlockedBy("has_brass_ingot",has(AllItems.BRASS_INGOT.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,MyAddonItems.NODE_DESIGNATOR.get())
                .pattern("ABA")
                .pattern("BCA")
                .pattern("AAD")
                .define('A',AllItems.IRON_SHEET.get())
                .define('B',Items.REDSTONE_BLOCK)
                .define('C',Items.GLASS)
                .define('D',AllBlocks.REDSTONE_LINK.get())
                .unlockedBy("has_laser_drill",has(MyAddonBlocks.LASER_DRILL_HEAD.get()))
                .save(recipeOutput);

    }

}