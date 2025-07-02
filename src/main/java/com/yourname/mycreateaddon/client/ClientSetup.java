package com.yourname.mycreateaddon.client;

import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlockEntity;
import com.yourname.mycreateaddon.etc.MyAddonPartialModels;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import com.yourname.mycreateaddon.content.kinetics.node.OreNodeModelLoader; // 새로 만들 파일
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.Map;

@EventBusSubscriber(modid = MyCreateAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    // [추가] 광물별 색상 정의 (나중에 별도 클래스로 분리 가능)
    private static final Map<ResourceLocation, Integer> ORE_COLORS = Map.of(
            BuiltInRegistries.ITEM.getKey(Items.RAW_IRON), 0xD8D8D8,
            BuiltInRegistries.ITEM.getKey(Items.RAW_COPPER), 0xD67D53,
            BuiltInRegistries.ITEM.getKey(Items.RAW_GOLD), 0xFCEE4B,
            BuiltInRegistries.ITEM.getKey(Items.COAL), 0x343434,
            BuiltInRegistries.ITEM.getKey(Items.LAPIS_LAZULI), 0x345EC3,
            BuiltInRegistries.ITEM.getKey(Items.DIAMOND), 0x4EECD8,
            BuiltInRegistries.ITEM.getKey(Items.EMERALD), 0x17DD62,
            BuiltInRegistries.ITEM.getKey(Items.REDSTONE), 0xFF0000
    );
    private static final int DEFAULT_COLOR = 0x808080; // 기본 색상 (회색)

    // FMLClientSetupEvent에서 PartialModel을 초기화합니다.
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(MyAddonPartialModels::init);
    }

    // 모델 등록 이벤트 핸들러는 그대로 유지합니다.
    @SubscribeEvent
    public static void onModelRegister(ModelEvent.RegisterAdditional event) {
        event.register(new ModelResourceLocation(MyAddonPartialModels.SHAFT_FOR_DRILL_LOCATION, "standalone"));
        event.register(new ModelResourceLocation(MyAddonPartialModels.SHAFT_FOR_MODULE_LOCATION, "standalone"));
        event.register(new ModelResourceLocation(MyAddonPartialModels.ROTARY_DRILL_HEAD_LOCATION,"standalone"));
    }


    @SubscribeEvent
    public static void onModelRegistry(ModelEvent.RegisterGeometryLoaders event) {
        event.register(
                ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "ore_node_loader"),
                new OreNodeModelLoader()
        );
    }


    // [추가] 블록 색상 핸들러 등록
    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        // [핵심 수정] BlockColors 인스턴스를 람다 밖에서 미리 가져오지 않습니다.

        event.register((state, getter, pos, tintIndex) -> {
            if (getter == null || pos == null) {
                return -1; // 문제가 있으면 틴팅 안 함
            }

            if (tintIndex == 1) {
                // 코어 모델 처리 (이전과 동일)
                if (getter.getBlockEntity(pos) instanceof OreNodeBlockEntity be) {
                    ResourceLocation oreId = be.getRepresentativeOreItemId();
                    return ORE_COLORS.getOrDefault(oreId, DEFAULT_COLOR);
                }
                return DEFAULT_COLOR;
            } else {
                // 배경 블록 처리
                if (getter.getBlockEntity(pos) instanceof OreNodeBlockEntity be) {
                    // [핵심 수정] BlockColors 인스턴스를 람다 안에서, 필요할 때 직접 가져옵니다.
                    BlockColors blockColors = Minecraft.getInstance().getBlockColors();

                    ResourceLocation backgroundId = be.getBackgroundBlockId();
                    Block backgroundBlock = BuiltInRegistries.BLOCK.get(backgroundId);

                    if (backgroundBlock != Blocks.AIR) {
                        BlockState backgroundState = backgroundBlock.defaultBlockState();
                        // 이제 blockColors는 절대 null이 아니므로 안전하게 호출할 수 있습니다.
                        return blockColors.getColor(backgroundState, getter, pos, tintIndex);
                    }
                }
                return -1; // 틴팅 안 함
            }
        }, MyAddonBlocks.ORE_NODE.get());
    }


    // [추가] 아이템 색상 핸들러 등록 (인벤토리 아이템용)
    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            // 아이템은 항상 기본 철 색상으로 표시
            return ORE_COLORS.getOrDefault(BuiltInRegistries.ITEM.getKey(Items.RAW_IRON), DEFAULT_COLOR);
        }, MyAddonBlocks.ORE_NODE.get());
    }


}