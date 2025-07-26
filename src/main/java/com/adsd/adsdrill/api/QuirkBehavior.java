package com.adsd.adsdrill.api;

import com.adsd.adsdrill.crafting.Quirk.QuirkContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * 애드온이 새로운 Quirk의 동작을 정의하기 위해 구현하는 인터페이스입니다.
 */
public interface QuirkBehavior {
    default void onAfterDropsCalculated(List<ItemStack> drops, QuirkContext context) {}
    default int onCalculateDrops(int originalCount, QuirkContext context) { return originalCount; }
    default int onCalculateFortune(int originalFortune) { return originalFortune; }
    default float onCalculateHardness(float originalHardness, QuirkContext context) { return originalHardness; }
    default float onCalculateRegeneration(float originalRegen, QuirkContext context) { return originalRegen; }
    default void onDrillCoreOverheat(QuirkContext context) {}
    default Optional<Item> onSelectItemToDrop(QuirkContext context) { return Optional.empty(); }
    default double onCalculateMiningAmount(double originalAmount, QuirkContext context) { return originalAmount; }
    default void onPeriodicTick(QuirkContext context) {}
    default void onYieldConsumed(QuirkContext context) {}
}