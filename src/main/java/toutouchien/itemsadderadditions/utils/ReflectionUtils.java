package toutouchien.itemsadderadditions.utils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.components.SimpleComponentProperty;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

@NullMarked
public final class ReflectionUtils {
    private static final MethodHandle ADVANCEMENT_PROGRESS_CTOR;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                    net.minecraft.advancements.AdvancementProgress.class,
                    MethodHandles.lookup()
            );

            ADVANCEMENT_PROGRESS_CTOR = lookup.findConstructor(
                    net.minecraft.advancements.AdvancementProgress.class,
                    MethodType.methodType(void.class, Map.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ADVANCEMENT_PROGRESS_CTOR", e);
        }
    }

    private ReflectionUtils() {
        throw new IllegalStateException("Utility class");
    }

    @Nullable
    public static Class<?> getTypeArgument(Class<?> clazz) {
        for (Type type : clazz.getGenericInterfaces()) {
            if (!(type instanceof ParameterizedType parameterizedType)
                    || !parameterizedType.getRawType().equals(SimpleComponentProperty.class)) {
                continue;
            }

            Type actualType = parameterizedType.getActualTypeArguments()[0];
            if (actualType instanceof Class<?> actualClass) {
                return actualClass;
            }
        }

        return null;
    }

    public static net.minecraft.advancements.AdvancementProgress newAdvancementProgress(
            Map<String, net.minecraft.advancements.CriterionProgress> criteria
    ) {
        try {
            return (net.minecraft.advancements.AdvancementProgress) ADVANCEMENT_PROGRESS_CTOR.invokeExact(criteria);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke AdvancementProgress constructor", e);
        }
    }
}
