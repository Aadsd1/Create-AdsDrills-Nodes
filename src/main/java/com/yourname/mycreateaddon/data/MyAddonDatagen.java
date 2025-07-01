package com.yourname.mycreateaddon.data;


import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.RegistrateDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

import static com.yourname.mycreateaddon.MyCreateAddon.MOD_ID;
import static com.yourname.mycreateaddon.MyCreateAddon.REGISTRATE;

@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class MyAddonDatagen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event){
        DataGenerator generator = event.getGenerator();

        addCustomLang(REGISTRATE);

        generator.addProvider(
                true,
                new RegistrateDataProvider(REGISTRATE,MOD_ID, event)
        );
    }

    public static void addCustomLang(CreateRegistrate registrate) {
        registrate.addRawLang("goggle.mycreateaddon.drill_core.header", "Drill Assembly");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.valid", "Operational (%s Modules)");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.invalid", "Structure Invalid");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.reason.loop_detected", "Error: Structural loop detected.");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.reason.multiple_cores", "Error: Multiple cores in one assembly.");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.reason.too_many_modules", "Error: Module limit exceeded (%s).");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.speed_bonus", "Speed Bonus");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.stress_impact", "Added Stress");
        registrate.addRawLang("goggle.mycreateaddon.drill_core.reason.head_missing", "Warning: Drill Head missing.");
        registrate.addRawLang("goggle.mycreateaddon.ore_node.header", "Ore Vein");
        registrate.addRawLang("goggle.mycreateaddon.ore_node.yield", "Remaining Yield: %s");
        registrate.addRawLang("goggle.mycreateaddon.ore_node.composition", "Composition:");
    }
}
