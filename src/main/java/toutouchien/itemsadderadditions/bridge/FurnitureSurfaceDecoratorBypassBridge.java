package toutouchien.itemsadderadditions.bridge;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.utils.other.Log;

/**
 * Bridge called by {@link FurnitureSurfaceDecoratorBypassPatch_IA_4_0_16} at the
 * entry of {@code SurfaceDecoratorConfig.i(auo)}.
 *
 * <p>Identical strategy to {@link FurniturePopulatorBypassBridge}: locate the
 * underlying {@link YamlConfiguration} via reflection and null-out any
 * {@code surface_decorators} entry that carries a {@code furniture:} key, so IA's
 * loop never reaches it and never logs an "unknown block" error.
 */
@NullMarked
public final class FurnitureSurfaceDecoratorBypassBridge {
    private static final String TAG = "FurnitureSurfaceDecoratorBypass";
    private static final String SECTION = "surface_decorators";

    private FurnitureSurfaceDecoratorBypassBridge() {
        throw new IllegalStateException("Static class");
    }

    /**
     * Called from bytecode injected at the entry of
     * {@code SurfaceDecoratorConfig.i(auo)}.
     *
     * @param rawFile the {@code auo} instance typed as {@link Object}
     */
    public static void stripFurnitureKeys(Object rawFile) {
        try {
            doStrip(rawFile);
        } catch (Exception e) {
            Log.warn(TAG, "Failed to strip furniture keys from auo: "
                    + e.getMessage());
        }
    }

    private static void doStrip(Object rawFile) throws Exception {
        YamlConfiguration yaml = extractYaml(rawFile);
        if (yaml == null) {
            Log.warn(TAG, "Could not locate YamlConfiguration inside "
                    + rawFile.getClass().getName() + " - furniture keys not stripped");
            return;
        }

        ConfigurationSection section = yaml.getConfigurationSection(SECTION);
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;

            String furnitureId = entry.getString("furniture");
            if (furnitureId == null || furnitureId.isBlank()) continue;

            yaml.set(SECTION + "." + key, null);
            Log.debug(TAG, "Stripped surface_decorators key '" + key
                    + "' from IA's view (furniture=" + furnitureId + ")");
        }
    }

    private static YamlConfiguration extractYaml(Object rawFile) throws Exception {
        Class<?> cls = rawFile.getClass();
        while (cls != null && cls != Object.class) {
            for (java.lang.reflect.Field field : cls.getDeclaredFields()) {
                if (YamlConfiguration.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(rawFile);
                    if (value instanceof YamlConfiguration)
                        return (YamlConfiguration) value;
                }
            }
            cls = cls.getSuperclass();
        }
        return null;
    }
}
