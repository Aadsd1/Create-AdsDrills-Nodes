package com.adsd.adsdrill.api;


import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.content.kinetics.module.IModuleBehavior;
import com.adsd.adsdrill.content.kinetics.module.ModuleType;
import com.google.common.base.Suppliers;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;



@ApiStatus.Experimental
public final class ModuleTypeApi {

    private static final Supplier<EnumInvoker> ENUM_INVOKER = Suppliers.memoize(EnumInvoker::new);

    public static ModuleType addModuleType(String name, float stressImpact, float speedBonus, float heatModifier, int itemCapacity, int fluidCapacity, int energyCapacity, IModuleBehavior behavior, boolean isPerformanceModule) {
        if (Arrays.stream(ModuleType.values()).anyMatch(e -> e.name().equalsIgnoreCase(name))) {
            AdsDrillAddon.LOGGER.warn("ModuleType with name '{}' already exists. Skipping registration.", name);
            return null;
        }
        return ENUM_INVOKER.get().addEnum(name, stressImpact, speedBonus, heatModifier, itemCapacity, fluidCapacity, energyCapacity, behavior, isPerformanceModule);
    }

    private static final class EnumInvoker {
        private static final String ENUM_CONSTANTS_FIELD = "enumConstants";
        private static final String ENUM_CONSTANT_DIRECTORY_FIELD = "enumConstantDirectory";

        private static final Constructor<?> MODULE_TYPE_CONSTRUCTOR;

        static {
            try {
                Constructor<?> constructor = ModuleType.class.getDeclaredConstructors()[0];
                constructor.setAccessible(true);
                MODULE_TYPE_CONSTRUCTOR = constructor;
            } catch (Exception e) {
                throw new RuntimeException("Could not find or access ModuleType constructor. This may be due to a mod update.", e);
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
                T[] previousValues = ((Class<T>) ModuleType.class).getEnumConstants();
                List<T> values = new ArrayList<>(Arrays.asList(previousValues));

                T newValue = makeEnum((Class<T>) ModuleType.class, enumName, values.size(), constructorArgs);
                values.add(newValue);

                setStaticFinalField(this.constantsField, toArray((Class<T>) ModuleType.class, values));
                setStaticFinalField(this.constantDirectoryField, null);

                AdsDrillAddon.LOGGER.info("Successfully added new enum constant: {}", enumName);
                return newValue;
            } catch (Exception e) {
                AdsDrillAddon.LOGGER.error("Failed to add new enum constant '{}' to {}", enumName, ModuleType.class.getSimpleName(), e);
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        private <T extends Enum<T>> T makeEnum(Class<T> enumType, String name, int ordinal, Object... constructorArgs) throws Exception {
            Object[] finalArgs = new Object[constructorArgs.length + 2];
            finalArgs[0] = name;
            finalArgs[1] = ordinal;
            System.arraycopy(constructorArgs, 0, finalArgs, 2, constructorArgs.length);

            return (T) MODULE_TYPE_CONSTRUCTOR.newInstance(finalArgs);
        }

        private static void setStaticFinalField(Field field, Object newValue) throws Exception {
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(ModuleType.class, newValue);
        }

        @SuppressWarnings("unchecked")
        private <T extends Enum<T>> T[] toArray(Class<T> enumType, List<T> list) {
            T[] array = (T[]) Array.newInstance(enumType, list.size());
            return list.toArray(array);
        }
    }
}