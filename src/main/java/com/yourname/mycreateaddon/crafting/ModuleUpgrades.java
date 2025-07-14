package com.yourname.mycreateaddon.crafting;


import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.yourname.mycreateaddon.content.kinetics.module.GenericModuleBlock;
import com.yourname.mycreateaddon.registry.MyAddonItems;
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
        addUpgrade(MyAddonItems.MODULE_SPEED_UPGRADE.get(), MyAddonBlocks.SPEED_MODULE);
        addUpgrade(MyAddonItems.MODULE_EFFI_UPGRADE.get(), MyAddonBlocks.EFFICIENCY_MODULE);
        addUpgrade(MyAddonItems.MODULE_REIN_UPGRADE.get(), MyAddonBlocks.REINFORCEMENT_MODULE);

        // 처리 모듈
        addUpgrade(MyAddonItems.MODULE_FURNACE_UPGRADE.get(), MyAddonBlocks.FURNACE_MODULE);
        addUpgrade(MyAddonItems.MODULE_BF_UPGRADE.get(), MyAddonBlocks.BLAST_FURNACE_MODULE);
        addUpgrade(MyAddonItems.MODULE_CRUSH_UPGRADE.get(), MyAddonBlocks.CRUSHER_MODULE);
        addUpgrade(MyAddonItems.MODULE_WASH_UPGRADE.get(), MyAddonBlocks.WASHER_MODULE);
        addUpgrade(MyAddonItems.MODULE_COMP_UPGRADE.get(), MyAddonBlocks.COMPACTOR_MODULE);

        // 유틸리티 및 시스템 모듈
        addUpgrade(MyAddonItems.MODULE_HEATSINK_UPGRADE.get(), MyAddonBlocks.HEATSINK_MODULE);
        addUpgrade(MyAddonItems.MODULE_COOL_UPGRADE.get(), MyAddonBlocks.COOLANT_MODULE);
        addUpgrade(MyAddonItems.MODULE_FILTER_UPGRADE.get(), MyAddonBlocks.FILTER_MODULE);
        addUpgrade(MyAddonItems.MODULE_RESO_UPGRADE.get(), MyAddonBlocks.RESONATOR_MODULE);
        addUpgrade(MyAddonItems.MODULE_R_BRAKE_UPGRADE.get(), MyAddonBlocks.REDSTONE_BRAKE_MODULE);
        addUpgrade(MyAddonItems.MODULE_E_GEN_UPGRADE.get(), MyAddonBlocks.KINETIC_DYNAMO_MODULE);

        // 버퍼 및 에너지 모듈
        addUpgrade(MyAddonItems.MODULE_I_BUFFER_UPGRADE.get(), MyAddonBlocks.ITEM_BUFFER_MODULE);
        addUpgrade(MyAddonItems.MODULE_F_BUFFER_UPGRADE.get(), MyAddonBlocks.FLUID_BUFFER_MODULE);
        addUpgrade(MyAddonItems.MODULE_E_INPUT_UPGRADE.get(), MyAddonBlocks.ENERGY_INPUT_MODULE);
        addUpgrade(MyAddonItems.MODULE_E_BUFFER_UPGRADE.get(), MyAddonBlocks.ENERGY_BUFFER_MODULE); // 예시
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