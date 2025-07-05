package com.yourname.mycreateaddon.client;

import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.head.RotaryDrillHeadBlockEntity;
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

import java.awt.*;
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
        event.register(new ModelResourceLocation(MyAddonPartialModels.DIAMOND_DRILL_BODY_LOCATION,"standalone"));
        event.register(new ModelResourceLocation(MyAddonPartialModels.IRON_DRILL_BODY_LOCATION,"standalone"));
        event.register(new ModelResourceLocation(MyAddonPartialModels.DRILL_TIP_IRON_LOCATION,"standalone"));
        event.register(new ModelResourceLocation(MyAddonPartialModels.DRILL_TIP_DIAMOND_LOCATION,"standalone"));
        event.register(new ModelResourceLocation(MyAddonPartialModels.DRILL_TIP_GOLD_LOCATION,"standalone"));
        event.register(new ModelResourceLocation(MyAddonPartialModels.DRILL_TIP_EMERALD_LOCATION,"standalone"));

    }


    @SubscribeEvent
    public static void onModelRegistry(ModelEvent.RegisterGeometryLoaders event) {
        event.register(
                ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "ore_node_loader"),
                new OreNodeModelLoader()
        );
    }



    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, getter, pos, tintIndex) -> {
            if (getter == null || pos == null || !(getter.getBlockEntity(pos) instanceof OreNodeBlockEntity be)) {
                // 렌더링에 필요한 정보가 없으면 틴팅하지 않음
                return -1;
            }

            // [핵심 로직]
            // 틴트 인덱스가 1 또는 2인 경우, 우리가 직접 처리 (광맥 코어 색상)
            if (tintIndex == 1 || tintIndex == 2) {
                // 대표 광물의 기본 색상을 가져옵니다.
                ResourceLocation oreId = be.getRepresentativeOreItemId();
                int baseColor = ORE_COLORS.getOrDefault(oreId, DEFAULT_COLOR);

                if (tintIndex == 1) {
                    // tintIndex가 1이면 기본색을 그대로 반환
                    return baseColor;
                } else { // tintIndex가 2일 수밖에 없음
                    // tintIndex가 2이면 기본색을 더 밝게 만들어 반환
                    Color c = new Color(baseColor);
                    // HSB(색상, 채도, 밝기) 모델에서 밝기(Brightness)를 약간 올립니다.
                    float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
                    // 밝기를 0.0(검정) ~ 1.0(흰색) 사이에서 조절
                    hsb[2] = Math.min(1.0f, hsb[2] + 0.2f); // 20% 더 밝게
                    return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                }
            }
            // [핵심 로직]
            // 그 외의 틴트 인덱스(잔디의 0 등)는 바닐라 시스템에 위임하여 처리 (배경 색상)
            else {
                // BlockEntity에서 배경 블록 정보를 가져옵니다.
                ResourceLocation backgroundId = be.getBackgroundBlockId();
                Block backgroundBlock = BuiltInRegistries.BLOCK.get(backgroundId);

                if (backgroundBlock != Blocks.AIR) {
                    // 마인크래프트의 기본 BlockColors 인스턴스를 가져옵니다.
                    BlockColors blockColors = Minecraft.getInstance().getBlockColors();
                    BlockState backgroundState = backgroundBlock.defaultBlockState();

                    // 바닐라의 색상 처리기에 "원래 이 블록이라면 무슨 색이야?" 라고 물어보고, 그 결과를 반환합니다.
                    return blockColors.getColor(backgroundState, getter, pos, tintIndex);
                }
            }

            // 모든 조건에 해당하지 않으면 틴팅하지 않음
            return -1;

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