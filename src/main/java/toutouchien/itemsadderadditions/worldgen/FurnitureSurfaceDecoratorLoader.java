package toutouchien.itemsadderadditions.worldgen;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Reads the {@code surface_decorators} section from ItemsAdder item-definition YAML files
 * and registers {@link FurnitureSurfaceDecoratorConfig} instances, so furniture appears
 * on the surface of newly generated terrain.
 *
 * <h3>Minimal YAML example</h3>
 * <pre>{@code
 * surface_decorators:
 *   my_flower_decorator:
 *     enabled: true
 *     furniture: "my_pack:small_flower"
 *     worlds: [world]
 *     biomes: [minecraft:plains]
 *     bottom_blocks: [GRASS_BLOCK]
 *     amount:       4
 *     chunk_chance: 50.0
 *     min_height:   60
 *     max_height:   120
 *     allow_liquid_surface:   false
 *     allow_liquid_placement: false
 * }</pre>
 *
 * <p>Call {@link #clear} before each reload and {@link #loadAll} after ItemsAdder
 * has finished its own data loading.
 */
@NullMarked
public final class FurnitureSurfaceDecoratorLoader extends AbstractFurnitureLoader<FurnitureSurfaceDecoratorConfig> {
    /**
     * Global registry: entry-key → config. Rebuilt on each reload.
     */
    public static final Map<String, FurnitureSurfaceDecoratorConfig> REGISTRY = new HashMap<>();

    public static void clear() {
        FurnitureSurfaceDecoratorBehaviour.unregisterAll();
        REGISTRY.clear();
    }

    @Override
    protected String sectionName() {
        return "surface_decorators";
    }

    @Override
    protected String entryTypeName() {
        return "furniture surface decorator";
    }

    @Override
    protected String tag() {
        return "FurnitureSurfaceDecoratorLoader";
    }

    @Override
    @Nullable
    protected FurnitureSurfaceDecoratorConfig buildConfig(
            String key, String furnitureId, ConfigurationSection entry, String filePath) {
        try {
            return new FurnitureSurfaceDecoratorConfig(
                    key, filePath,
                    entry.getStringList("worlds"),
                    furnitureId,
                    entry.getInt("amount", 4),
                    entry.getInt("max_height", 80),
                    entry.getInt("min_height", 60),
                    entry.getDouble("chunk_chance", entry.getDouble("chance", -1.0)),
                    entry.getBoolean("allow_liquid_surface", false),
                    entry.getBoolean("allow_liquid_placement", false)
            );
        } catch (IllegalArgumentException e) {
            Log.warn(tag(), "Furniture surface decorator '" + key + "' invalid: " + e.getMessage() + ". File: " + filePath);
            return null;
        }
    }

    @Override
    protected void loadMaterials(FurnitureSurfaceDecoratorConfig config, String key, ConfigurationSection entry, String filePath) {
        if (!entry.contains("bottom_blocks")) return;
        for (String matName : entry.getStringList("bottom_blocks")) {
            Material mat = parseMaterial(key, matName, filePath);
            if (mat != null) config.addBottomBlock(mat);
        }
    }

    @Override
    protected void addBiome(FurnitureSurfaceDecoratorConfig config, Biome biome) {
        config.addBiome(biome);
    }

    @Override
    protected void registerConfig(String key, FurnitureSurfaceDecoratorConfig config) {
        REGISTRY.put(key, config);
        Bukkit.getWorlds().forEach(world -> FurnitureSurfaceDecoratorBehaviour.register(config, world));
    }
}
