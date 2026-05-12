package toutouchien.itemsadderadditions.feature.worldgen.populator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.worldgen.AbstractFurnitureLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Reads {@code blocks_populators} (or the legacy {@code worlds_populators}) section
 * from ItemsAdder item-definition YAML files and registers {@link FurniturePopulatorConfig}
 * instances, so furniture spawns during new-chunk generation.
 *
 * <h3>Minimal YAML example</h3>
 * <pre>{@code
 * blocks_populators:
 *   my_mushroom_populator:
 *     enabled: true
 *     furniture: "my_pack:big_mushroom"
 *     worlds: [world]
 *     biomes: [minecraft:forest]
 *     replaceable_blocks: [GRASS_BLOCK]
 *     vein_blocks:  1
 *     chunk_veins:  4
 *     chunk_chance: 20.0
 *     min_height:   60
 *     max_height:   80
 * }</pre>
 *
 * <p>Call {@link #clear} before each reload and {@link #loadAll} after ItemsAdder
 * has finished its own data loading.
 */
@NullMarked
public final class FurniturePopulatorLoader extends AbstractFurnitureLoader<FurniturePopulatorConfig> {
    /**
     * Global registry: entry-key → config. Rebuilt on each reload.
     */
    public static final Map<String, FurniturePopulatorConfig> REGISTRY = new HashMap<>();

    public static void clear() {
        FurniturePopulatorBehaviour.unregisterAll();
        REGISTRY.clear();
    }

    @Override
    protected String sectionName() {
        return "blocks_populators";
    }

    @Override
    protected String fallbackSectionName() {
        return "worlds_populators";
    }

    @Override
    protected String entryTypeName() {
        return "furniture populator";
    }

    @Override
    protected String tag() {
        return "FurniturePopulatorLoader";
    }

    @Override
    @Nullable
    protected FurniturePopulatorConfig buildConfig(
            String key, String furnitureId, ConfigurationSection entry, String filePath) {
        try {
            return new FurniturePopulatorConfig(
                    key, filePath,
                    entry.getStringList("worlds"),
                    furnitureId,
                    entry.getInt("vein_blocks", entry.getInt("amount", 1)),
                    entry.getInt("max_height", 80),
                    entry.getInt("min_height", 60),
                    entry.getInt("chunk_veins", entry.getInt("iterations", 4)),
                    entry.getDouble("chunk_chance", entry.getDouble("chance", -1.0)),
                    entry.getBoolean("allow_liquid_placement", false)
            );
        } catch (IllegalArgumentException e) {
            Log.warn(tag(), "Furniture populator '" + key + "' invalid: " + e.getMessage() + ". File: " + filePath);
            return null;
        }
    }

    @Override
    protected void loadMaterials(FurniturePopulatorConfig config, String key, ConfigurationSection entry, String filePath) {
        if (!entry.contains("replaceable_blocks")) return;
        for (String matName : entry.getStringList("replaceable_blocks")) {
            Material mat = parseMaterial(key, matName, filePath);
            if (mat != null) config.addReplaceableBlock(mat);
        }
    }

    @Override
    protected void addBiome(FurniturePopulatorConfig config, Biome biome) {
        config.addBiome(biome);
    }

    @Override
    protected void registerConfig(String key, FurniturePopulatorConfig config) {
        REGISTRY.put(key, config);
        Bukkit.getWorlds().forEach(world -> FurniturePopulatorBehaviour.register(config, world));
    }
}
