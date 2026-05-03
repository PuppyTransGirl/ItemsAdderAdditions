package toutouchien.itemsadderadditions.worldgen;

import dev.lone.itemsadder.api.CustomStack;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.io.File;
import java.util.*;

/**
 * Reads {@code furniture:} entries from ItemsAdder item-definition YAML files
 * and registers {@link FurniturePopulatorConfig} instances.
 */
@NullMarked
public final class FurniturePopulatorLoader {
    /**
     * Global registry: entry-key → config. Rebuilt on each reload.
     */
    public static final Map<String, FurniturePopulatorConfig> REGISTRY =
            new HashMap<>();
    private static final String TAG = "FurniturePopulatorLoader";
    private static final String PRIMARY_SECTION = "blocks_populators";
    private static final String FALLBACK_SECTION = "worlds_populators";

    private FurniturePopulatorLoader() {
        throw new IllegalStateException("Static class");
    }

    public static void loadAll(File itemsAdderContentsDir) {
        if (!itemsAdderContentsDir.exists()) {
            Log.warn(TAG, "ItemsAdder contents directory not found: "
                    + itemsAdderContentsDir.getPath());
            return;
        }

        List<File> yamlFiles = collectYamlFiles(itemsAdderContentsDir);
        Log.info(TAG, "Scanning " + yamlFiles.size()
                + " YAML files for furniture populator entries…");

        for (File file : yamlFiles) {
            try {
                loadFile(YamlConfiguration.loadConfiguration(file), file.getPath());
            } catch (Exception e) {
                Log.error(TAG, "Failed to parse: " + file.getPath(), e);
            }
        }
    }

    public static void clear() {
        FurniturePopulatorBehaviour.unregisterAll();
        REGISTRY.clear();
    }

    private static void loadFile(YamlConfiguration yaml, String filePath) {
        ConfigurationSection populators =
                yaml.getConfigurationSection(PRIMARY_SECTION);
        if (populators == null) {
            populators = yaml.getConfigurationSection(FALLBACK_SECTION);
        }
        if (populators == null) {
            return;
        }

        for (String key : populators.getKeys(false)) {
            ConfigurationSection entry = populators.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }

            String furnitureId = entry.getString("furniture");
            if (furnitureId == null || furnitureId.isBlank()) {
                continue;
            }

            loadEntry(key, furnitureId, entry, filePath);
        }
    }

    private static void loadEntry(
            String key,
            String furnitureId,
            ConfigurationSection entry,
            String filePath
    ) {
        if (!entry.getBoolean("enabled", true)) {
            Log.info(TAG, "Skipping disabled furniture populator '" + key + "'");
            return;
        }

        if (!CustomStack.isInRegistry(furnitureId)) {
            Log.warn(TAG, "Furniture populator '" + key
                    + "' references unknown furniture '" + furnitureId
                    + "' - skipping. File: " + filePath);
            return;
        }

        FurniturePopulatorConfig config;
        try {
            config = new FurniturePopulatorConfig(
                    key,
                    filePath,
                    entry.getStringList("worlds"),
                    furnitureId,
                    entry.getInt("vein_blocks", entry.getInt("amount", 1)),
                    entry.getInt("max_height", 80),
                    entry.getInt("min_height", 60),
                    entry.getInt("chunk_veins", entry.getInt("iterations", 4)),
                    entry.getDouble(
                            "chunk_chance",
                            entry.getDouble("chance", -1.0)
                    ),
                    entry.getBoolean("allow_liquid_placement", false)
            );
        } catch (IllegalArgumentException e) {
            Log.warn(TAG, "Furniture populator '" + key + "' invalid: "
                    + e.getMessage() + ". File: " + filePath);
            return;
        }

        if (!loadBiomes(config, key, entry, filePath)) {
            return;
        }

        loadSurfaces(config, key, entry, filePath);

        REGISTRY.put(key, config);
        Log.info(TAG, "Registered furniture populator '" + key + "' → "
                + furnitureId);

        Bukkit.getWorlds().forEach(
                world -> FurniturePopulatorBehaviour.register(config, world));
    }

    private static boolean loadBiomes(
            FurniturePopulatorConfig config,
            String key,
            ConfigurationSection entry,
            String filePath
    ) {
        if (!entry.contains("biomes")) {
            return true;
        }

        boolean ok = true;
        for (String biomeName : entry.getStringList("biomes")) {
            Key biomeKey = Key.key(biomeName.toLowerCase(Locale.ROOT));
            Biome biome = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.BIOME)
                    .get(biomeKey);

            if (biome == null) {
                Log.warn(TAG, "Furniture populator '" + key
                        + "': unknown biome '" + biomeName
                        + "'. File: " + filePath);
                ok = false;
                continue;
            }

            config.addBiome(biome);
        }

        return ok;
    }

    private static void loadSurfaces(
            FurniturePopulatorConfig config,
            String key,
            ConfigurationSection entry,
            String filePath
    ) {
        if (!entry.contains("replaceable_blocks")) {
            return;
        }

        for (String matName : entry.getStringList("replaceable_blocks")) {
            try {
                config.addReplaceableBlock(
                        Material.valueOf(matName.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                Log.warn(TAG, "Furniture populator '" + key
                        + "': unknown material '" + matName
                        + "'. File: " + filePath);
            }
        }
    }

    private static List<File> collectYamlFiles(File dir) {
        List<File> result = new ArrayList<>();
        File[] children = dir.listFiles();
        if (children == null) {
            return result;
        }

        for (File child : children) {
            if (child.isDirectory()) {
                result.addAll(collectYamlFiles(child));
            } else if (child.getName().endsWith(".yml")) {
                result.add(child);
            }
        }

        return result;
    }
}
