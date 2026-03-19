package toutouchien.itemsadderadditions.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.annotations.Parameter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Injects {@link Parameter}-annotated fields on any object from a YAML
 * {@link ConfigurationSection}.
 *
 * <h3>Min / max enforcement</h3>
 * When a {@link Parameter} annotation declares {@code min != max}, the injected
 * numeric value is clamped to {@code [min, max]} and a warning is logged if the
 * configured value was out of range.
 *
 * <h3>Superclass walk</h3>
 * Fields from the entire class hierarchy (up to but not including {@link Object})
 * are processed, so base-class parameters (e.g. {@code permission}, {@code delay}
 * on {@link toutouchien.itemsadderadditions.actions.ActionExecutor}) are always injected.
 */
@SuppressWarnings("unused")
@NullMarked
public final class ParameterInjector {
    private ParameterInjector() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Injects {@code @Parameter}-annotated fields of {@code target} from {@code section}.
     *
     * @param target   any object whose declared fields (including inherited ones) may carry {@link Parameter}
     * @param section  the YAML section to read values from ({@code null} is treated as an empty section)
     * @param itemName the item's namespaced ID, used only in warning messages
     * @return {@code true} if all required parameters were satisfied, {@code false} otherwise
     */
    public static boolean inject(Object target, @Nullable ConfigurationSection section, String itemName) {
        for (Field field : collectFields(target.getClass())) {
            Parameter param = field.getAnnotation(Parameter.class);
            if (param == null) continue;

            Object raw = section != null ? section.get(param.key()) : null;
            Object value = ConfigUtils.coerceNumber(raw, param.type());

            try {
                field.setAccessible(true);

                if (param.type().isInstance(value)) {
                    field.set(target, enforceRange(value, param, field.getName(), itemName));
                    continue;
                }

                // Value is null or the wrong type - leave the field default in place.
                if (value == null && !param.required()) continue;

                // Value is present but wrong type - null the field so behaviour is predictable.
                field.set(target, null);

                if (value != null) {
                    ItemsAdderAdditions.instance().getSLF4JLogger().warn(
                        "[Config] Item '{}' - parameter '{}' has wrong type: expected {}, got {}",
                        itemName, param.key(),
                        param.type().getSimpleName(), value.getClass().getSimpleName()
                    );
                }

                if (param.required()) {
                    ItemsAdderAdditions.instance().getSLF4JLogger().warn(
                        "[Config] Item '{}' - required parameter '{}' is missing or invalid; skipping.",
                        itemName, param.key()
                    );
                    return false;
                }

            } catch (ReflectiveOperationException e) {
                ItemsAdderAdditions.instance().getSLF4JLogger().error(
                    "[Config] Failed to inject field '{}' on {}",
                    field.getName(), target.getClass().getSimpleName(), e
                );
                return false;
            }
        }

        return true;
    }

    /**
     * Clamps {@code value} to the {@code [min, max]} range declared on {@code param},
     * if the annotation has a non-trivial range (i.e. {@code min != max}).
     *
     * <p>Logs a warning when clamping occurs so the server admin can fix the config.
     *
     * @return the (possibly clamped) value - always the same type as the input
     */
    private static Object enforceRange(
            Object value,
            Parameter param,
            String fieldName,
            String itemName
    ) {
        if (param.min() == param.max())
            return value;

        if (!(value instanceof Number number))
            return value;

        double d = number.doubleValue();
        double lo = param.min();
        double hi = param.max();

        if (d >= lo && d <= hi)
            return value;

        double clamped = Math.clamp(d, lo, hi);
        ItemsAdderAdditions.instance().getSLF4JLogger().warn(
            "[Config] Item '{}' - parameter '{}' value {} is out of range [{}, {}]; clamping to {}.",
            itemName, fieldName, d, lo, hi, clamped
        );

        return ConfigUtils.coerceNumber(clamped, param.type());
    }

    /**
     * Collects all declared fields from {@code clazz} and every superclass up to
     * (but not including) {@link Object}.
     *
     * <p>Fields from the most-derived class are listed first so that subclass
     * declarations shadow superclass ones if they share a name (which should not
     * happen in practice, but is the safe ordering).
     */
    private static List<Field> collectFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> cursor = clazz;
        while (cursor != null && cursor != Object.class) {
            fields.addAll(Arrays.asList(cursor.getDeclaredFields()));
            cursor = cursor.getSuperclass();
        }

        return fields;
    }
}
