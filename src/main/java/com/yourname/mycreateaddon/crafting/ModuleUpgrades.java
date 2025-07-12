package com.yourname.mycreateaddon.crafting;


import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 프레임 모듈의 업그레이드 조합법을 관리하는 클래스입니다.
 */
public class ModuleUpgrades {

    // 업그레이드 아이템과 결과 모듈을 매핑하는 Map
    private static final Map<Item, BlockEntry<? extends GenericModuleBlock>> UPGRADE_MAP = new HashMap<>();

    /**
     * 모듈 업그레이드 레시피를 등록합니다. 이 메서드는 FMLCommonSetupEvent에서 호출되어야 합니다.
     */
    public static void register() {
        // 성능 모듈
        addUpgrade(Items.COPPER_INGOT, MyAddonBlocks.SPEED_MODULE);
        addUpgrade(AllItems.PROPELLER.get(), MyAddonBlocks.EFFICIENCY_MODULE);
        addUpgrade(Items.IRON_BLOCK, MyAddonBlocks.REINFORCEMENT_MODULE);

        // 처리 모듈
        addUpgrade(Items.FURNACE, MyAddonBlocks.FURNACE_MODULE);
        addUpgrade(Items.BLAST_FURNACE, MyAddonBlocks.BLAST_FURNACE_MODULE);
        addUpgrade(AllBlocks.CRUSHING_WHEEL.asItem(), MyAddonBlocks.CRUSHER_MODULE);
        addUpgrade(Items.WATER_BUCKET, MyAddonBlocks.WASHER_MODULE);
        addUpgrade(Items.PISTON, MyAddonBlocks.COMPACTOR_MODULE);

        // 유틸리티 및 시스템 모듈
        addUpgrade(Items.ICE, MyAddonBlocks.HEATSINK_MODULE);
        addUpgrade(Items.BLUE_ICE, MyAddonBlocks.COOLANT_MODULE);
        addUpgrade(AllItems.FILTER.get(), MyAddonBlocks.FILTER_MODULE);
        addUpgrade(Items.ECHO_SHARD, MyAddonBlocks.RESONATOR_MODULE);
        addUpgrade(Items.LEVER, MyAddonBlocks.REDSTONE_BRAKE_MODULE);
        addUpgrade(AllItems.ELECTRON_TUBE.get(), MyAddonBlocks.KINETIC_DYNAMO_MODULE);

        // 버퍼 및 에너지 모듈
        addUpgrade(Items.CHEST, MyAddonBlocks.ITEM_BUFFER_MODULE);
        addUpgrade(Items.BUCKET, MyAddonBlocks.FLUID_BUFFER_MODULE);
        addUpgrade(AllItems.ELECTRON_TUBE.get(), MyAddonBlocks.ENERGY_INPUT_MODULE);
        addUpgrade(AllBlocks.BRASS_CASING.asItem(), MyAddonBlocks.ENERGY_BUFFER_MODULE); // 예시
    }

    /**
     * 업그레이드 조합을 Map에 추가하는 헬퍼 메서드
     * @param upgradeItem 업그레이드에 사용할 아이템
     * @param resultModule 결과로 나올 모듈 블록
     */
    private static void addUpgrade(Item upgradeItem, BlockEntry<? extends GenericModuleBlock> resultModule) {
        UPGRADE_MAP.put(upgradeItem, resultModule);
    }

    /**
     * 주어진 아이템으로 업그레이드할 수 있는 모듈을 반환합니다.
     * @param item 확인할 아이템
     * @return 업그레이드 가능한 모듈 BlockEntry, 없으면 null
     */
    @Nullable
    public static BlockEntry<? extends GenericModuleBlock> getUpgradeResult(Item item) {
        return UPGRADE_MAP.get(item);
    }
}