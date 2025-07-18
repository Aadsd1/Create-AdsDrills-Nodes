package com.adsd.adsdrill.data.recipe;


import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.adsd.adsdrill.registry.AdsDrillBlocks;
import com.adsd.adsdrill.registry.AdsDrillItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.data.recipe.SequencedAssemblyRecipeGen;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import java.util.concurrent.CompletableFuture;



public class SequencialRecipeGen extends SequencedAssemblyRecipeGen {
    GeneratedRecipe

    DRILL_CORE=create("drill_core",b->b.require(AllBlocks.BRASS_CASING.asItem())
            .transitionTo(AdsDrillItems.INCOMPLETE_DRILL_CORE.get())
            .addOutput(AdsDrillBlocks.DRILL_CORE.get(),90)
            .addOutput(AllBlocks.COGWHEEL.get(),1)
            .addOutput(AllBlocks.MECHANICAL_BEARING.get(),2)
            .addOutput(AllBlocks.BRASS_CASING.get(),3)
            .addOutput(AllItems.BRASS_SHEET.get(),4)
            .loops(1)
            .addStep(DeployerApplicationRecipe::new,rb->rb.require(AllBlocks.GEARBOX.get()))
            .addStep(DeployerApplicationRecipe::new,rb->rb.require(AllBlocks.MECHANICAL_BEARING.get()))
            .addStep(DeployerApplicationRecipe::new,rb->rb.require(AllBlocks.COGWHEEL.get()))
    ),
    CORE_STEEL_UPGRADE=create("core_steel_upgrade",b->b.require(AdsDrillItems.STEEL_INGOT.get())
            .transitionTo(AdsDrillItems.INCOMPLETE_DRILL_CORE_STEEL_UPGRADE.get())
            .addOutput(AdsDrillItems.DRILL_CORE_STEEL_UPGRADE.get(),88)
            .addOutput(AdsDrillItems.RAW_STEEL_CHUNK.get(),3)
            .addOutput(AdsDrillItems.RAW_ROSE_GOLD_CHUNK.get(),3)
            .addOutput(AllItems.PRECISION_MECHANISM.get(),3)
            .addOutput(Items.IRON_INGOT,2)
            .addOutput(Items.IRON_NUGGET,1)
            .loops(2)
            .addStep(DeployerApplicationRecipe::new,rb->rb.require(AllTags.commonItemTag("plates/iron")))
            .addStep(DeployerApplicationRecipe::new,rb->rb.require(AdsDrillItems.ROSE_GOLD.get()))
            .addStep(DeployerApplicationRecipe::new,rb->rb.require(AllItems.PRECISION_MECHANISM.get()))
            .addStep(PressingRecipe::new,rb->rb)
    ),
    CORE_NETHERITE_UPGRADE=create("core_netherite_upgrade",b->b.require(AdsDrillItems.XOMV.get())
            .transitionTo(AdsDrillItems.INCOMPLETE_DRILL_CORE_NETHERITE_UPGRADE.get())
            .addOutput(AdsDrillItems.DRILL_CORE_NETHERITE_UPGRADE.get(),88)
            .addOutput(AdsDrillItems.XOMV.get(),3)
            .addOutput(Items.NETHERITE_INGOT,3)
            .addOutput(AllItems.PRECISION_MECHANISM.get(),2)
            .addOutput(Items.FLINT,1)
            .addOutput(AdsDrillItems.KOH_I_NOOR.get(),1)
            .addOutput(Items.DIAMOND,1)
            .addOutput(Items.NETHERITE_SCRAP,1)
            .loops(3)
            .addStep(DeployerApplicationRecipe::new,rb->rb.require(Items.NETHERITE_INGOT))
            .addStep(DeployerApplicationRecipe::new,rb->rb.require(AllItems.PRECISION_MECHANISM.get()))
            .addStep(DeployerApplicationRecipe::new,rb->rb.require(AdsDrillItems.KOH_I_NOOR.get()))
            .addStep(FillingRecipe::new,rb->rb.require(Fluids.LAVA,1000))
            .addStep(PressingRecipe::new,rb->rb)
    ),
    LASER_DRILL_HEAD=create("laser_drill_head",b->b.require(AllBlocks.MECHANICAL_DRILL.get())
            .transitionTo(AdsDrillItems.INCOMPLETE_LASER_DRILL_HEAD.get())
            .addOutput(AdsDrillBlocks.LASER_DRILL_HEAD.get(),90)
            .addOutput(Items.NETHERITE_INGOT,2)
            .addOutput(AdsDrillItems.RAW_ROSE_GOLD_CHUNK.get(),2)
            .addOutput(AdsDrillItems.RAW_STEEL_CHUNK.get(),2)
            .addOutput(AllItems.PRECISION_MECHANISM.get(),2)
            .addOutput(AllBlocks.MECHANICAL_DRILL.get(),1)
            .addOutput(Items.NETHERITE_SCRAP,1)
            .loops(3)
            .addStep(DeployerApplicationRecipe::new,rb->rb.require(Items.NETHERITE_INGOT))
            .addStep(DeployerApplicationRecipe::new,rb->rb.require(AdsDrillItems.STEEL_INGOT.get()))
            .addStep(DeployerApplicationRecipe::new,rb->rb.require(AdsDrillItems.ROSE_GOLD.get()))
            .addStep(DeployerApplicationRecipe::new,rb->rb.require(AllItems.PRECISION_MECHANISM.get()))
            .addStep(CuttingRecipe::new,rb->rb)
    );

    public SequencialRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }
}
