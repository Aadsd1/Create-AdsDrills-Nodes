package com.adsd.adsdrill.crafting;


import com.adsd.adsdrill.registry.AdsDrillBlocks;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.adsd.adsdrill.content.kinetics.module.GenericModuleBlock;
import com.adsd.adsdrill.registry.AdsDrillItems;
import net.minecraft.world.item.Item;

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
        addUpgrade(AdsDrillItems.MODULE_SPEED_UPGRADE.get(), AdsDrillBlocks.SPEED_MODULE);
        addUpgrade(AdsDrillItems.MODULE_EFFI_UPGRADE.get(), AdsDrillBlocks.EFFICIENCY_MODULE);
        addUpgrade(AdsDrillItems.MODULE_REIN_UPGRADE.get(), AdsDrillBlocks.REINFORCEMENT_MODULE);

        // 처리 모듈
        addUpgrade(AdsDrillItems.MODULE_FURNACE_UPGRADE.get(), AdsDrillBlocks.FURNACE_MODULE);
        addUpgrade(AdsDrillItems.MODULE_BF_UPGRADE.get(), AdsDrillBlocks.BLAST_FURNACE_MODULE);
        addUpgrade(AdsDrillItems.MODULE_CRUSH_UPGRADE.get(), AdsDrillBlocks.CRUSHER_MODULE);
        addUpgrade(AdsDrillItems.MODULE_WASH_UPGRADE.get(), AdsDrillBlocks.WASHER_MODULE);
        addUpgrade(AdsDrillItems.MODULE_COMP_UPGRADE.get(), AdsDrillBlocks.COMPACTOR_MODULE);

        // 유틸리티 및 시스템 모듈
        addUpgrade(AdsDrillItems.MODULE_HEATSINK_UPGRADE.get(), AdsDrillBlocks.HEATSINK_MODULE);
        addUpgrade(AdsDrillItems.MODULE_COOL_UPGRADE.get(), AdsDrillBlocks.COOLANT_MODULE);
        addUpgrade(AdsDrillItems.MODULE_FILTER_UPGRADE.get(), AdsDrillBlocks.FILTER_MODULE);
        addUpgrade(AdsDrillItems.MODULE_RESO_UPGRADE.get(), AdsDrillBlocks.RESONATOR_MODULE);
        addUpgrade(AdsDrillItems.MODULE_R_BRAKE_UPGRADE.get(), AdsDrillBlocks.REDSTONE_BRAKE_MODULE);
        addUpgrade(AdsDrillItems.MODULE_E_GEN_UPGRADE.get(), AdsDrillBlocks.KINETIC_DYNAMO_MODULE);

        // 버퍼 및 에너지 모듈
        addUpgrade(AdsDrillItems.MODULE_I_BUFFER_UPGRADE.get(), AdsDrillBlocks.ITEM_BUFFER_MODULE);
        addUpgrade(AdsDrillItems.MODULE_F_BUFFER_UPGRADE.get(), AdsDrillBlocks.FLUID_BUFFER_MODULE);
        addUpgrade(AdsDrillItems.MODULE_E_INPUT_UPGRADE.get(), AdsDrillBlocks.ENERGY_INPUT_MODULE);
        addUpgrade(AdsDrillItems.MODULE_E_BUFFER_UPGRADE.get(), AdsDrillBlocks.ENERGY_BUFFER_MODULE); // 예시
    }

    /**
     * 업그레이드 조합을 Map에 추가하는 헬퍼 메서드
     * @param upgradeItem 업그레이드에 사용할 아이템
     * @param resultModule 결과로 나올 모듈 블록
     */
    private static void addUpgrade(Item upgradeItem, BlockEntry<? extends GenericModuleBlock> resultModule) {
        UPGRADE_MAP.put(upgradeItem, resultModule);
    }
    public static Map<Item, BlockEntry<? extends GenericModuleBlock>> getUpgradeResultMap() {
        return UPGRADE_MAP;
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