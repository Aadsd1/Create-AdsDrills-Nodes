package com.yourname.mycreateaddon.content.kinetics.module;


import com.yourname.mycreateaddon.content.kinetics.base.IResourceAccessor;
import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * 각 모듈 타입의 고유한 행동을 정의하는 전략 인터페이스입니다.
 * GenericModuleBlockEntity는 이 인터페이스의 구현체에 로직을 위임합니다.
 */
public interface IModuleBehavior {

    /**
     * 모듈이 아이템을 처리하는 로직을 정의합니다. (처리/필터 모듈용)
     * @param moduleBE 행동을 호출한 모듈의 BlockEntity
     * @param stack 처리할 아이템
     * @param core 드릴 코어 인스턴스
     * @return 처리 결과 아이템 리스트
     */
    default List<ItemStack> processItem(GenericModuleBlockEntity moduleBE, ItemStack stack, DrillCoreBlockEntity core) {
        return Collections.singletonList(stack);
    }

    /**
     * 모듈이 내부 버퍼 전체를 대상으로 대량 처리를 수행합니다. (압축 모듈용)
     * @param moduleBE 행동을 호출한 모듈의 BlockEntity
     * @param coreResources 드릴 구조의 자원 접근자
     * @return 작업 성공 여부
     */
    default boolean processBulk(GenericModuleBlockEntity moduleBE, IResourceAccessor coreResources) {
        return false;
    }

    /**
     * 드릴 코어의 매 틱마다 호출되어 시스템에 직접 관여합니다. (냉각, 발전 등)
     * @param moduleBE 행동을 호출한 모듈의 BlockEntity
     * @param core 드릴 코어 인스턴스
     */
    default void onCoreTick(GenericModuleBlockEntity moduleBE, DrillCoreBlockEntity core) {
    }

    /**
     * 이 모듈이 사용하는 레시피 타입을 반환합니다.
     * @return 레시피 타입, 없으면 null
     */
    @Nullable
    default RecipeType<?> getRecipeType() {
        return null;
    }

    /**
     * 처리 모듈의 작동 전제 조건을 확인합니다.
     * @param moduleBE 행동을 호출한 모듈의 BlockEntity
     * @param core 드릴 코어 인스턴스
     * @return 조건 충족 여부
     */
    default boolean checkProcessingPreconditions(GenericModuleBlockEntity moduleBE, DrillCoreBlockEntity core) {
        return true;
    }

    /**
     * 처리 모듈이 자원을 소모하는 로직을 정의합니다.
     * @param moduleBE 행동을 호출한 모듈의 BlockEntity
     * @param core 드릴 코어 인스턴스
     */
    default void consumeResources(GenericModuleBlockEntity moduleBE, DrillCoreBlockEntity core) {
    }

    /**
     * 처리 모듈이 시각/청각 효과를 재생합니다.
     * @param moduleBE 행동을 호출한 모듈의 BlockEntity
     * @param level 월드
     * @param modulePos 모듈 위치
     */
    default void playEffects(GenericModuleBlockEntity moduleBE, Level level, BlockPos modulePos) {
    }
}