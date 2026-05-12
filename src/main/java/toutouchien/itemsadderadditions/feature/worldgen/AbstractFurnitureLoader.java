package toutouchien.itemsadderadditions.feature.worldgen;

import dev.lone.itemsadder.api.CustomStack;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.loading.CategorizedConfigFile;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.Locale;

/**
 * Base class for furniture world-generation loaders.
 *
 * <p>Both {@link FurniturePopulatorLoader} and {@link FurnitureSurfaceDecoratorLoader}
 * share the same high-level pipeline:
 * <ol>
 *   <li>Scan all YAML files under the ItemsAdder contents directory.</li>
 *   <li>In each file, look for a named top-level section (e.g. {@code blocks_populators},
 *       {@code surface_decorators}).</li>
 *   <li>For each entry: validate the furniture ID, build a config object, resolve biomes
 *       and surface materials, register the config.</li>
 * </ol>
 *
 * <p>Subclasses declare which YAML section they read ({@link #sectionName()}) and
 * implement the type-specific steps ({@link #buildConfig}, {@link #loadMaterials},
 * {@link #registerConfig}).
 *
 * @param <C> the config type produced by this loader
 */
@NullMarked
public abstract class AbstractFurnitureLoader<C> {
    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Returns the primary YAML section key this loader reads.
     * For example {@code "blocks_populators"} or {@code "surface_decorators"}.
     */
    protected abstract String sectionName();

    /**
     * Returns a fallback section key to try when {@link #sectionName()} is absent, or
     * {@code null} if there is no fallback.
     */
    @Nullable
    protected String fallbackSectionName() {
        return null;
    }

    /**
     * Returns the human-readable name used in log messages (e.g. "furniture populator").
     */
    protected abstract String entryTypeName();

    /**
     * Returns the logging subsystem tag (e.g. {@code "FurniturePopulatorLoader"}).
     */
    protected abstract String tag();

    /**
     * Constructs the config object from the given YAML section.
     *
     * @param key         the entry's unique key within the section
     * @param furnitureId the validated furniture namespaced ID
     * @param entry       the YAML section for this entry
     * @param filePath    the source file path, used for error messages
     * @return the config, or {@code null} if construction failed
     */
    @Nullable
    protected abstract C buildConfig(String key, String furnitureId, ConfigurationSection entry, String filePath);

    /**
     * Loads type-specific materials from the entry section into the config.
     * Called after biomes have been loaded successfully.
     */
    protected abstract void loadMaterials(C config, String key, ConfigurationSection entry, String filePath);

    /**
     * Adds a biome to the config.
     * Called for each valid biome name found in the {@code biomes} list.
     */
    protected abstract void addBiome(C config, Biome biome);

    /**
     * Registers the fully-built config so it participates in world generation.
     */
    protected abstract void registerConfig(String key, C config);

    /**
     * Processes only the pre-filtered files supplied by {@link toutouchien.itemsadderadditions.common.loading.ConfigFileRegistry}.
     *
     * <p>This is the preferred entry point: files have already been read from disk and
     * parsed into {@link YamlConfiguration} objects exactly once, so there is no
     * additional I/O here.
     *
     * @param files files pre-filtered to this loader's category by the registry
     */
    public final void loadAll(java.util.List<CategorizedConfigFile> files) {
        Log.debug(tag(), "Processing {} YAML file(s) for {} entries...", files.size(), entryTypeName());
        for (CategorizedConfigFile ccf : files) {
            try {
                loadFile(ccf.yaml(), ccf.file().getPath());
            } catch (Exception e) {
                Log.error(tag(), "Failed to parse: " + ccf.file().getPath(), e);
            }
        }
    }

    private void loadFile(YamlConfiguration yaml, String filePath) {
        ConfigurationSection section = yaml.getConfigurationSection(sectionName());
        if (section == null && fallbackSectionName() != null)
            section = yaml.getConfigurationSection(fallbackSectionName());
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;

            String furnitureId = entry.getString("furniture");
            if (furnitureId == null || furnitureId.isBlank()) continue;

            loadEntry(key, furnitureId, entry, filePath);
        }
    }

    private void loadEntry(String key, String furnitureId, ConfigurationSection entry, String filePath) {
        if (!entry.getBoolean("enabled", true)) {
            Log.info(tag(), "Skipping disabled " + entryTypeName() + " '" + key + "'");
            return;
        }

        if (!CustomStack.isInRegistry(furnitureId)) {
            Log.warn(tag(), capitalize(entryTypeName()) + " '" + key
                    + "' references unknown furniture '" + furnitureId
                    + "' - skipping. File: " + filePath);
            return;
        }

        C config = buildConfig(key, furnitureId, entry, filePath);
        if (config == null) return;

        if (!loadBiomes(config, key, entry, filePath)) return;

        loadMaterials(config, key, entry, filePath);

        registerConfig(key, config);
        Log.info(tag(), "Registered " + entryTypeName() + " '" + key + "' → " + furnitureId);
    }

    /**
     * Reads the optional {@code biomes} string-list from {@code entry} and delegates
     * each name to {@link #addBiome}. Returns {@code false} if any name was invalid
     * (all entries must be valid, matching ItemsAdder's own validation behaviour).
     */
    private boolean loadBiomes(C config, String key, ConfigurationSection entry, String filePath) {
        if (!entry.contains("biomes")) return true;

        boolean ok = true;
        for (String biomeName : entry.getStringList("biomes")) {
            Key biomeKey = Key.key(biomeName.toLowerCase(Locale.ROOT));
            Biome biome = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.BIOME)
                    .get(biomeKey);

            if (biome == null) {
                Log.warn(tag(), capitalize(entryTypeName()) + " '" + key
                        + "': unknown biome '" + biomeName + "'. File: " + filePath);
                ok = false;
                continue;
            }

            addBiome(config, biome);
        }

        return ok;
    }

    /**
     * Resolves a material name to a {@link Material}, logging a warning on failure.
     *
     * @param key      the entry key (for log context)
     * @param matName  the raw material name from YAML
     * @param filePath the source file (for log context)
     * @return the material, or {@code null} if the name was unrecognised
     */
    @Nullable
    protected final Material parseMaterial(String key, String matName, String filePath) {
        try {
            return Material.valueOf(matName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Log.warn(tag(), capitalize(entryTypeName()) + " '" + key
                    + "': unknown material '" + matName + "'. File: " + filePath);
            return null;
        }
    }
}
