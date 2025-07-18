package com.adsd.adsdrill.registry;


import com.adsd.adsdrill.AdsDrillAddon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class AdsDrillTags {


    /**
     * 아이템 태그를 생성하는 헬퍼 메서드
     * @param name 태그의 경로 (예: "catalysts")
     * @return 생성된 TagKey<Item>
     */
    private static TagKey<Item> itemTag(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(AdsDrillAddon.MOD_ID, name));
    }

    /**
     * 블록 태그를 생성하는 헬퍼 메서드
     * @param name 태그의 경로
     * @return 생성된 TagKey<Block>
     */
    private static TagKey<Block> blockTag(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(AdsDrillAddon.MOD_ID, name));
    }

    public static final TagKey<Item> CATALYSTS = itemTag("catalysts");

    // 클래스가 로드될 때 모든 태그가 초기화되도록 하는 메서드
    public static void init() {
    }
}