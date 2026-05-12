package toutouchien.itemsadderadditions.common.inject;

import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.common.config.ConfigUtils;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Injects {@link Parameter}-annotated fields on any object from a YAML
 * {@link ConfigurationSection}.
 *
 * <h3>Sub-section paths</h3>
 * When {@link Parameter#path()} is non-empty, the injector resolves the value from
 * a nested sub-section rather than the root section. For example:
 * <pre>
 * {@literal @}Parameter(key = "name", path = "sound", type = String.class)
 * private String soundName;
 * </pre>
 * reads {@code section.sound.name}. If the sub-section is absent and the field is not
 * required, the field's default value is preserved silently.
 *
 * <h3>Min / max enforcement</h3>
 * When {@link Parameter#min()} {@code != } {@link Parameter#max()}, the injected
 * numeric value is clamped to {@code [min, max]} and a warning is logged if clamping
 * occurred.
 *
 * <h3>Superclass walk</h3>
 * Fields from the entire class hierarchy (up to but not including {@link Object})
 * are processed, so base-class parameters (e.g. {@code permission}, {@code delay}
 * on {@link toutouchien.itemsadderadditions.feature.action.ActionExecutor}) are always injected.
 */
@NullMarked
public final class ParameterInjector {
    private static final String SUBSYSTEM = "Config";

    private ParameterInjector() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Injects {@code @Parameter}-annotated fields of {@code target} from {@code section}.
     *
     * @param target   the object to inject into
     * @param section  the root YAML section ({@code null} treated as empty)
     * @param itemName the item's namespaced ID, used only in log messages
     * @return {@code true} if all required parameters were satisfied
     */
    public static boolean inject(Object target, @Nullable ConfigurationSection section, String itemName) {
        for (Field field : collectFields(target.getClass())) {
            Parameter param = field.getAnnotation(Parameter.class);
            if (param == null)
                continue;

            ConfigurationSection readFrom = resolveSection(section, param, itemName);

            // If the sub-section is absent and the field isn't required, leave the default.
            if (readFrom == null) {
                if (param.required() && !param.path().isEmpty()) {
                    Log.itemSkip(SUBSYSTEM, itemName,
                            "required sub-section '{}' is missing (needed by parameter '{}')",
                            param.path(), param.key());
                    return false;
                }
                continue;
            }

            Object raw = readFrom.get(param.key());
            Object value = ConfigUtils.coerceNumber(raw, param.type());

            try {
                field.setAccessible(true);

                if (param.type().isInstance(value)) {
                    field.set(target, enforceRange(value, param, itemName));
                    continue;
                }

                // Value is null or wrong type - leave default in place unless required.
                if (value == null && !param.required())
                    continue;

                // Value is present but wrong type - null the field so behaviour is predictable.
                field.set(target, null);

                if (value != null) {
                    Log.itemWarn(SUBSYSTEM, itemName,
                            "parameter '{}' has wrong type: expected {}, got {}",
                            qualifiedKey(param), param.type().getSimpleName(),
                            value.getClass().getSimpleName());
                }

                if (param.required()) {
                    Log.itemSkip(SUBSYSTEM, itemName,
                            "required parameter '{}' is missing or has the wrong type",
                            qualifiedKey(param));
                    return false;
                }

            } catch (ReflectiveOperationException e) {
                Log.error(SUBSYSTEM,
                        "Failed to inject field '" + field.getName() + "' on "
                                + target.getClass().getSimpleName() + " for item '" + itemName + "'", e);
                return false;
            }
        }

        return true;
    }

    /**
     * Resolves the section to read the parameter value from.
     * Returns {@code null} when the configured sub-section doesn't exist in YAML.
     */
    @Nullable
    private static ConfigurationSection resolveSection(
            @Nullable ConfigurationSection root,
            Parameter param,
            String itemName
    ) {
        if (param.path().isEmpty())
            return root;

        if (root == null)
            return null;

        ConfigurationSection sub = root.getConfigurationSection(param.path());
        return sub; // null when absent - caller decides whether to fail
    }

    /**
     * Clamps a numeric value to the range declared in {@code param} when
     * {@code min != max}.
     */
    private static Object enforceRange(Object value, Parameter param, String itemName) {
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
        Log.itemWarn("Config", itemName,
                "parameter '{}' value {} is out of range [{}, {}] - clamping to {}",
                qualifiedKey(param), d, lo, hi, clamped);

        return ConfigUtils.coerceNumber(clamped, param.type());
    }

    /**
     * Returns a human-readable key for log messages, e.g. {@code "sound.name"} when
     * {@code path = "sound", key = "name"}, or just {@code "name"} when path is empty.
     */
    private static String qualifiedKey(Parameter param) {
        return param.path().isEmpty() ? param.key() : param.path() + "." + param.key();
    }

    /**
     * Collects all declared fields from {@code clazz} and every superclass up to
     * (but not including) {@link Object}. Most-derived class fields are listed first.
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
