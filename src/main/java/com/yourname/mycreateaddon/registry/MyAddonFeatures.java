package com.yourname.mycreateaddon.registry;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.worldgen.OreNodeConfiguration;
import com.yourname.mycreateaddon.content.worldgen.OreNodeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import com.tterrag.registrate.util.entry.RegistryEntry;//

public class MyAddonFeatures {
    private static final CreateRegistrate REGISTRATE = MyCreateAddon.registrate();



    public static final RegistryEntry<Feature<?>, OreNodeFeature> ORE_NODE_FEATURE = REGISTRATE
            .generic("ore_node", Registries.FEATURE, () -> new OreNodeFeature(OreNodeConfiguration.CODEC))
            .register();



    public static void register() {
        // 클래스가 로드되면서 등록이 실행됩니다.
    }
}