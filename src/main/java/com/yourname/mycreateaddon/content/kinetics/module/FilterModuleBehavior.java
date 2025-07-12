package com.yourname.mycreateaddon.content.kinetics.module;


import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * 필터 모듈의 아이템 필터링 로직을 처리하는 행동 클래스입니다.
 */
public class FilterModuleBehavior implements IModuleBehavior {

    @Override
    public List<ItemStack> processItem(GenericModuleBlockEntity moduleBE, ItemStack stack, DrillCoreBlockEntity core) {
        ItemStack filterStack = moduleBE.getFilter();
        if (filterStack.isEmpty()) {
            return Collections.singletonList(stack); // 필터가 없으면 모든 아이템 통과
        }

        FilterItemStack filter = FilterItemStack.of(filterStack);
        if (filter.test(core.getLevel(), stack)) {
            return Collections.singletonList(stack); // 필터 조건 통과
        } else {
            return Collections.emptyList(); // 필터 조건 불만족 (아이템 파괴)
        }
    }
}