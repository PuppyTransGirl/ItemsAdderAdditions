package toutouchien.itemsadderadditions.utils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.components.SimpleComponentProperty;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ReflectionUtils {
    private ReflectionUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static @Nullable Class<?> getTypeArgument(@NonNull Class<?> clazz) {
        for (Type type : clazz.getGenericInterfaces()) {
            if (!(type instanceof ParameterizedType parameterizedType) ||
                    !parameterizedType.getRawType().equals(SimpleComponentProperty.class))
                continue;

            Type actualType = parameterizedType.getActualTypeArguments()[0];
            if (actualType instanceof Class<?> actualClass) {
                return actualClass;
            }
        }
        return null;
    }
}
