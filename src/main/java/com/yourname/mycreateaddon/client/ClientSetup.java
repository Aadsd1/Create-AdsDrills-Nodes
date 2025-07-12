package com.yourname.mycreateaddon.client;

import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import com.yourname.mycreateaddon.content.kinetics.node.OreNodeBlockEntity;
import com.yourname.mycreateaddon.etc.MyAddonPartialModels;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
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

        // [핵심] 드릴 코어 아이템에 커스텀 프로퍼티 등록
        // [핵심] 드릴 코어 아이템에 커스텀 프로퍼티 등록
        ItemProperties.register(
                MyAddonBlocks.DRILL_CORE.get().asItem(),
                ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "tier"),
                (stack, level, entity, seed) -> {
                    // 아이템 스택에서 BLOCK_ENTITY_DATA 컴포넌트를 가져옴
                    CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
                    if (blockEntityData == null) {
                        return 0; // NBT 없으면 기본값(BRASS)인 0 반환
                    }

                    // 컴포넌트에서 NBT 태그를 복사하여 읽기
                    CompoundTag nbt = blockEntityData.copyTag();
                    if (!nbt.contains("CoreTier")) {
                        return 0; // CoreTier 태그 없으면 기본값 0 반환
                    }

                    String tierName = nbt.getString("CoreTier");
                    try {
                        DrillCoreBlockEntity.Tier tier = DrillCoreBlockEntity.Tier.valueOf(tierName);

                        // [핵심 수정] 각 티어에 명시적인 값을 반환 (JSON과 일치시킴)
                        return switch (tier) {
                            case BRASS -> 0;
                            case STEEL -> 1;
                            case NETHERITE -> 2;
                        };
                    } catch (IllegalArgumentException e) {
                        return 0; // 잘못된 값이 저장되어 있으면 기본값 0 반환
                    }
                }
        );


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
        event.register(new ModelResourceLocation(MyAddonPartialModels.PUMP_HEAD_COG_LOCATION, "standalone"));
        event.register(new ModelResourceLocation(MyAddonPartialModels.NETHERITE_DRILL_BODY_LOCATION,"standalone"));
        event.register(new ModelResourceLocation(MyAddonPartialModels.NETHERITE_DRILL_TIP_LOCATION,"standalone"));

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
        BlockColor oreNodeColorHandler = (state, getter, pos, tintIndex) -> {
            if (getter == null || pos == null) {
                return -1;
            }
            if (getter.getBlockEntity(pos) instanceof OreNodeBlockEntity be) {
                // tintIndex 0: 배경 블록의 색상 (예: 잔디)
                if (tintIndex == 0) {
                    // [!!! 핵심 추가 !!!]
                    // 배경 블록의 상태를 가져와, 해당 블록의 컬러 핸들러를 직접 호출하여 색상을 얻습니다.
                    BlockState backgroundState = be.getBackgroundStateClient(); // BE에 이 메서드를 추가해야 합니다.
                    // Minecraft.getInstance().getBlockColors()를 통해 바닐라/모드 컬러 핸들러에 접근
                    return Minecraft.getInstance().getBlockColors().getColor(backgroundState, getter, pos, tintIndex);
                }
                // tintIndex 1 또는 2: 코어의 광물 색상
                if (tintIndex == 1 || tintIndex == 2) {
                    ResourceLocation oreId = be.getRepresentativeOreItemId();
                    int baseColor = ORE_COLORS.getOrDefault(oreId, DEFAULT_COLOR);

                    if (tintIndex == 1) {
                        return baseColor;
                    } else { // tintIndex == 2
                        Color c = new Color(baseColor);
                        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
                        hsb[2] = Math.min(1.0f, hsb[2] + 0.2f);
                        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                    }
                }
            }
            return -1; // 그 외의 경우는 색상 없음
        };

        event.register(oreNodeColorHandler,
                MyAddonBlocks.ORE_NODE.get(),
                MyAddonBlocks.ARTIFICIAL_NODE.get()
        );
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