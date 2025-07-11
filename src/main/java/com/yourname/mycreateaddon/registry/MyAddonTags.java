package com.yourname.mycreateaddon.registry;


import com.yourname.mycreateaddon.MyCreateAddon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Function;

public class MyAddonTags {

    // [!!! 핵심 수정: TagKey.create를 직접 사용합니다 !!!]

    /**
     * 아이템 태그를 생성하는 헬퍼 메서드
     * @param name 태그의 경로 (예: "catalysts")
     * @return 생성된 TagKey<Item>
     */
    private static TagKey<Item> itemTag(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, name));
    }

    /**
     * 블록 태그를 생성하는 헬퍼 메서드
     * @param name 태그의 경로
     * @return 생성된 TagKey<Block>
     */
    private static TagKey<Block> blockTag(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, name));
    }

    // 이제 우리 모드의 태그를 여기에 정의합니다.
    public static final TagKey<Item> CATALYSTS = itemTag("catalysts");

    // 클래스가 로드될 때 모든 태그가 초기화되도록 하는 메서드
    public static void init() {
        // 이 메서드는 클래스 로딩을 위해 존재하며, 내용은 비어있어도 됩니다.
    }
}