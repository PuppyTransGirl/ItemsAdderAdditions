package toutouchien.itemsadderadditions.bridge;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.List;

/**
 * Bridge called by {@link FurniturePopulatorBypassPatch_IA_4_0_16} at the
 * entry of {@code WorldGeneratorConfig.i(auo)}.
 *
 * <h3>What this does</h3>
 * <p>ItemsAdder's {@code WorldGeneratorConfig.i()} looks up each key in the
 * {@code blocks_populators} list and immediately errors if the section has no
 * {@code block:} key:
 * <pre>
 *   InternalCustomBlock block = internalCore.getCustomBlockItem(section.getString("block"));
 *   if (block == null) { Logger.error(…); continue; }
 * </pre>
 * Our furniture entries only carry {@code furniture:}, so IA would spam an error
 * for every one of them on every load.
 *
 * <p>This bridge is injected at the very start of that method. It receives the
 * raw {@code auo} as an {@link Object} (to avoid depending on the
 * obfuscated type at compile time) and uses reflection to:
 * <ol>
 *   <li>Retrieve the {@code blocks_populators} key list from the file.</li>
 *   <li>Remove every key whose section declares {@code furniture:} instead of
 *       {@code block:}.</li>
 * </ol>
 * Because IA reads the list by reference, the removal is visible to the rest of
 * {@code WorldGeneratorConfig.i()} without any further patching.
 *
 * <h3>Reflection strategy</h3>
 * <p>{@code auo} extends (or wraps) a Bukkit
 * {@link YamlConfiguration}. The obfuscated method {@code eS(String)} returns a
 * {@code List<String>} of keys for a given section - identical to
 * {@code getConfigurationSection(path).getKeys(false)}. We locate the underlying
 * {@link YamlConfiguration} via reflection (field scan for the first
 * YamlConfiguration-assignable field) so we can call standard Bukkit API without
 * referencing any obfuscated class.
 */
@NullMarked
public final class FurniturePopulatorBypassBridge {
    private static final String TAG = "FurniturePopulatorBypass";

    private FurniturePopulatorBypassBridge() {
        throw new IllegalStateException("Static class");
    }

    /**
     * Called from bytecode injected at the entry of
     * {@code WorldGeneratorConfig.i(auo)}.
     *
     * @param rawFile the {@code auo} instance, typed as
     *                {@link Object} to avoid a compile-time dependency on the
     *                obfuscated class
     */
    public static void stripFurnitureKeys(Object rawFile) {
        try {
            doStrip(rawFile);
        } catch (Exception e) {
            // Never crash IA's loading thread - just log and let it proceed.
            // The worst outcome is that IA logs its own "unknown block" errors
            // for the furniture entries, which is cosmetic.
            Log.warn(TAG, "Failed to strip furniture keys from auo: "
                    + e.getMessage());
        }
    }

    private static void doStrip(Object rawFile) throws Exception {
        // --- Step 1: obtain the underlying YamlConfiguration via reflection ---
        // auo keeps a YamlConfiguration in one of its fields.
        // We scan all declared fields (including super-classes) for the first
        // one that is assignable to YamlConfiguration.
        YamlConfiguration yaml = extractYaml(rawFile);
        if (yaml == null) {
            Log.warn(TAG, "Could not locate YamlConfiguration inside "
                    + rawFile.getClass().getName() + " - furniture keys not stripped");
            return;
        }

        // --- Step 2: locate the blocks_populators list (with IA's fallback) ---
        // eS("blocks_populators") returns the YAML keys list for that section.
        // We replicate the fallback: blocks_populators → worlds_populators.
        String sectionKey = findPopulatorsSection(yaml);
        if (sectionKey == null) return; // no populators at all - nothing to do

        ConfigurationSection section = yaml.getConfigurationSection(sectionKey);
        if (section == null) return;

        // --- Step 3: remove keys that belong to us ---
        // We use eS() / getKeys() rather than calling our own REGISTRY so that
        // this bridge works even before FurniturePopulatorLoader.loadAll() runs.
        // The only signal we need is the presence of "furniture:" in the section.
        //
        // auo.eS(path) returns the direct child-key list of that
        // section. On the raw YamlConfiguration that is simply getKeys(false).
        List<String> keys = yaml.getStringList(sectionKey); // fallback - see below

        // getStringList on a mapping section returns [] - use getKeys instead.
        // We need the mutable list that eS() will return to WorldGeneratorConfig.
        // auo.eS stores section keys in a List field; we cannot
        // reach that list directly, so we instead set the section's values to an
        // empty map for keys we want to hide. A simpler and fully supported
        // approach: set "block: __furniture_bypass__" so getCustomBlockItem()
        // returns null with a recognisable value, then suppress IA's error via
        // the patch. But the cleanest approach is the one below: delete the
        // section node entirely from the in-memory YAML, so eS() returns a list
        // that no longer contains our key.
        boolean stripped = false;
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;

            String furnitureId = entry.getString("furniture");
            if (furnitureId == null || furnitureId.isBlank()) continue;

            // Remove the node so that IA's eS() / fj() calls return nothing for it
            yaml.set(sectionKey + "." + key, null);
            Log.debug(TAG, "Stripped furniture key '" + key
                    + "' from IA's blocks_populators view (furniture=" + furnitureId + ")");
            stripped = true;
        }

        if (stripped) {
            Log.debug(TAG, "Furniture key stripping complete for "
                    + rawFile.getClass().getSimpleName());
        }
    }

    /**
     * Scans {@code rawFile}'s class hierarchy for a field of type
     * {@link YamlConfiguration} (or a subtype) and returns its value.
     *
     * @return the YamlConfiguration, or {@code null} if none found
     */
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

    /**
     * Returns {@code "blocks_populators"} if that section exists, falling back
     * to {@code "worlds_populators"}, or {@code null} if neither is present.
     */
    private static String findPopulatorsSection(YamlConfiguration yaml) {
        if (yaml.isConfigurationSection("blocks_populators"))
            return "blocks_populators";
        if (yaml.isConfigurationSection("worlds_populators"))
            return "worlds_populators";
        return null;
    }
}
