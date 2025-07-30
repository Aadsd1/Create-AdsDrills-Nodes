package com.adsd.adsdrill.api;


import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.crafting.Quirk;
import com.google.common.base.Suppliers;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@ApiStatus.Experimental
public final class QuirkApi {

    private static final Supplier<EnumInvoker> ENUM_INVOKER = Suppliers.memoize(EnumInvoker::new);

    public static Quirk addQuirk(String name, String id, Quirk.Tier tier, Supplier<Item> catalyst, QuirkBehavior behavior) {
        if (Arrays.stream(Quirk.values()).anyMatch(e -> e.name().equalsIgnoreCase(name))) {
            AdsDrillAddon.LOGGER.warn("Quirk with name '{}' already exists. Skipping registration.", name);
            return null;
        }
        return ENUM_INVOKER.get().addEnum(name, id, tier, catalyst, behavior);
    }

    private static final class EnumInvoker {
        private static final String ENUM_CONSTANTS_FIELD = "enumConstants";
        private static final String ENUM_CONSTANT_DIRECTORY_FIELD = "enumConstantDirectory";

        private static final Constructor<?> QUIRK_CONSTRUCTOR;

        static {
            try {
                // Quirk enum에 정의된 모든 생성자를 가져옵니다.
                Constructor<?> foundConstructor = null;
                for (Constructor<?> constructor : Quirk.class.getDeclaredConstructors()) {
                    // 파라미터 중에 QuirkBehavior.class가 포함된 생성자를 찾습니다.
                    // 이것이 바로 애드온을 위해 만든 생성자입니다.
                    if (Arrays.asList(constructor.getParameterTypes()).contains(QuirkBehavior.class)) {
                        foundConstructor = constructor;
                        break;
                    }
                }

                if (foundConstructor == null) {
                    throw new NoSuchMethodException("Could not find a Quirk constructor that accepts a QuirkBehavior. API cannot function.");
                }

                foundConstructor.setAccessible(true);
                QUIRK_CONSTRUCTOR = foundConstructor;
            } catch (Exception e) {
                throw new RuntimeException("Could not find or access Quirk constructor for API. This may be due to a mod update.", e);
            }
        }

        private final Field constantsField;
        private final Field constantDirectoryField;

        private EnumInvoker() {
            try {
                this.constantsField = Class.class.getDeclaredField(ENUM_CONSTANTS_FIELD);
                this.constantDirectoryField = Class.class.getDeclaredField(ENUM_CONSTANT_DIRECTORY_FIELD);

                makeAccessible(this.constantsField, this.constantDirectoryField);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize EnumInvoker", e);
            }
        }

        private void makeAccessible(AccessibleObject... objects) {
            for (AccessibleObject object : objects) {
                object.setAccessible(true);
            }
        }

        @SuppressWarnings("unchecked")
        private <T extends Enum<T>> T addEnum(String enumName, Object... constructorArgs) {
            try {
                T[] previousValues = ((Class<T>) Quirk.class).getEnumConstants();
                List<T> values = new ArrayList<>(Arrays.asList(previousValues));

                T newValue = makeEnum((Class<T>) Quirk.class, enumName, values.size(), constructorArgs);
                values.add(newValue);

                setStaticFinalField(this.constantsField, toArray((Class<T>) Quirk.class, values));
                setStaticFinalField(this.constantDirectoryField, null);

                AdsDrillAddon.LOGGER.info("Successfully added new Quirk enum constant: {}", enumName);
                return newValue;
            } catch (Exception e) {
                AdsDrillAddon.LOGGER.error("Failed to add new enum constant '{}' to {}", enumName, Quirk.class.getSimpleName(), e);
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        private <T extends Enum<T>> T makeEnum(Class<T> enumType, String name, int ordinal, Object... constructorArgs) throws Exception {
            Object[] finalArgs = new Object[constructorArgs.length + 2];
            finalArgs[0] = name;
            finalArgs[1] = ordinal;
            System.arraycopy(constructorArgs, 0, finalArgs, 2, constructorArgs.length);

            return (T) QUIRK_CONSTRUCTOR.newInstance(finalArgs);
        }

        private static void setStaticFinalField(Field field, Object newValue) throws Exception {
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(Quirk.class, newValue);
        }

        @SuppressWarnings("unchecked")
        private <T extends Enum<T>> T[] toArray(Class<T> enumType, List<T> list) {
            T[] array = (T[]) Array.newInstance(enumType, list.size());
            return list.toArray(array);
        }
    }
}

/*// Quirk.java 파일

// ... (기존 import문)
import org.jetbrains.annotations.Nullable;

public enum Quirk {
    // ... (기존의 모든 enum 상수 정의는 그대로 둡니다)
    // 예: STEADY_HANDS(...){...}, SIGNAL_AMPLIFICATION(...){...} 등

    // [신규] 애드온에 의해 동적으로 추가된 특성의 로직을 담을 필드
    @Nullable
    private final QuirkBehavior addonBehavior;

    // ... (기존 필드들: id, tier, catalyst)

    // 기존 생성자
    Quirk(String id, Tier tier, Supplier<Item> catalyst) {
        this.id = id;
        this.tier = tier;
        this.catalyst = catalyst;
        this.addonBehavior = null; // 기본 특성은 이 필드를 사용하지 않음
    }

    // [신규] 리플렉션을 통해 호출될, 애드온용 생성자
    // 이 생성자는 직접 호출되지 않으며, API를 통해서만 사용됩니다.
    private Quirk(String id, Tier tier, Supplier<Item> catalyst, QuirkBehavior behavior) {
        this.id = id;
        this.tier = tier;
        this.catalyst = catalyst;
        this.addonBehavior = behavior;
    }

    // ... (기존의 getId, getTier, getDisplayName 등은 그대로 둡니다)

    // [수정] 모든 로직 메서드를 수정하여, 애드온 로직이 있다면 그것을 먼저 실행하도록 변경
    public void onAfterDropsCalculated(List<ItemStack> drops, QuirkContext context) {
        if (addonBehavior != null) {
            addonBehavior.onAfterDropsCalculated(drops, context);
        }
    }
    public int onCalculateDrops(int originalCount, QuirkContext context) {
        if (addonBehavior != null) {
            return addonBehavior.onCalculateDrops(originalCount, context);
        }
        return originalCount;
    }
    // ... (onCalculateFortune, onCalculateHardness 등 모든 다른 로직 메서드에 대해 위와 동일한 패턴을 적용)
    public void onPeriodicTick(QuirkContext context) {
        if (addonBehavior != null) {
            addonBehavior.onPeriodicTick(context);
        }
    }

    // ... (나머지 Quirk 클래스 코드는 그대로 둡니다)
}

public enum Quirk {
    // STEADY_HANDS(...){...}, SIGNAL_AMPLIFICATION(...){...} 등
    // 기존의 모든 enum 상수 정의는 그대로 유지합니다.
    // ...

    // [신규/확인] 애드온 로직을 담을 필드
    @Nullable
    private final QuirkBehavior addonBehavior;

    // 기존 생성자
    Quirk(String id, Tier tier, Supplier<Item> catalyst) {
        this.id = id;
        this.tier = tier;
        this.catalyst = catalyst;
        this.addonBehavior = null;
    }

    // [신규/확인] 애드온 API가 사용할 새로운 private 생성자
    private Quirk(String id, Tier tier, Supplier<Item> catalyst, QuirkBehavior behavior) {
        this.id = id;
        this.tier = tier;
        this.catalyst = catalyst;
        this.addonBehavior = behavior;
    }

    // ... (getId, getTier 등 기존 getter는 그대로 유지) ...

    // [수정] 모든 로직 메서드에 addonBehavior 호출 로직 추가
    public void onAfterDropsCalculated(List<ItemStack> drops, QuirkContext context) {
        if (addonBehavior != null) addonBehavior.onAfterDropsCalculated(drops, context);
    }
    public int onCalculateDrops(int originalCount, QuirkContext context) {
        return addonBehavior != null ? addonBehavior.onCalculateDrops(originalCount, context) : originalCount;
    }
    public int onCalculateFortune(int originalFortune) {
        return addonBehavior != null ? addonBehavior.onCalculateFortune(originalFortune) : originalFortune;
    }
    public float onCalculateHardness(float originalHardness, QuirkContext context) {
        return addonBehavior != null ? addonBehavior.onCalculateHardness(originalHardness, context) : originalHardness;
    }
    public float onCalculateRegeneration(float originalRegen, QuirkContext context) {
        return addonBehavior != null ? addonBehavior.onCalculateRegeneration(originalRegen, context) : originalRegen;
    }
    public void onDrillCoreOverheat(QuirkContext context) {
        if (addonBehavior != null) addonBehavior.onDrillCoreOverheat(context);
    }
    public Optional<Item> onSelectItemToDrop(QuirkContext context) {
        return addonBehavior != null ? addonBehavior.onSelectItemToDrop(context) : Optional.empty();
    }
    public double onCalculateMiningAmount(double originalAmount, QuirkContext context) {
        return addonBehavior != null ? addonBehavior.onCalculateMiningAmount(originalAmount, context) : originalAmount;
    }
    public void onPeriodicTick(QuirkContext context) {
        if (addonBehavior != null) addonBehavior.onPeriodicTick(context);
    }
    public void onYieldConsumed(QuirkContext context) {
        if (addonBehavior != null) addonBehavior.onYieldConsumed(context);
    }

    // ... (나머지 Quirk 클래스 코드는 그대로 유지) ...
}

*/