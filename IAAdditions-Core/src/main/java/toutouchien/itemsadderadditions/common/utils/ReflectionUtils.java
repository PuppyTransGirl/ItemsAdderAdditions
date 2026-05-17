package toutouchien.itemsadderadditions.common.utils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.feature.component.SimpleComponentProperty;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@NullMarked
public final class ReflectionUtils {
    private ReflectionUtils() {
        throw new IllegalStateException("Utility class");
    }

    @Nullable
    public static Class<?> getTypeArgument(Class<?> clazz) {
        for (Type type : clazz.getGenericInterfaces()) {
            if (!(type instanceof ParameterizedType parameterizedType)
                    || !parameterizedType.getRawType()
                    .equals(SimpleComponentProperty.class)) {
                continue;
            }

            Type actualType = parameterizedType.getActualTypeArguments()[0];
            if (actualType instanceof Class<?> actualClass) {
                return actualClass;
            }
        }

        return null;
    }
}
