package com.yourname.mycreateaddon.datagen;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.yourname.mycreateaddon.MyCreateAddon;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = MyCreateAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class DataProvider {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();
        CreateRegistrate registrate = MyCreateAddon.registrate();

        // 서버 측 데이터 프로바이더 등록
        if (event.includeServer()) {
            // 아직 루트 테이블 프로바이더를 만들지 않았으므로, 필요하다면 아래 주석을 해제하고 클래스를 만들어야 합니다.
            // LootTableProvider loot = new LootTableProvider(output, Set.of(),
            //     List.of(new LootTableProvider.SubProviderEntry(MyAddonLootTableProvider::new, LootContextParamSets.BLOCK)),
            //     event.getLookupProvider()
            // );
            // gen.addProvider(true, loot);
        }


        if (event.includeClient()) {
            gen.addProvider(true, new MyAddonBlockStateProvider(output, helper));
            // 이 줄이 포함되어 있는지 확인합니다.
            gen.addProvider(true, new MyAddonItemModelProvider(output, helper));
        }
    }
}