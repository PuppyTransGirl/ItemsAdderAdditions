package toutouchien.itemsadderadditions.feature.action.builtin;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.momirealms.antigrieflib.AntiGriefLib;
import net.momirealms.antigrieflib.Flag;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.utils.BlocksShape;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;

import java.util.List;

/**
 * Replaces the biome in a region around the executing entity.
 *
 * <pre>{@code
 * # Per-axis radii
 * replace_biome:
 *   biome: plains
 *   shape: CUBOID
 *   radius:
 *     x: 5
 *     y: 4
 *     z: 3
 *
 * # Uniform radius shorthand
 * replace_biome:
 *   biome: desert
 *   shape: SPHERE
 *   radius:
 *     blocks_from_center: 8
 *
 * # Flat cylinder (y: 0 = single layer)
 * replace_biome:
 *   biome: jungle
 *   shape: CYLINDER
 *   radius:
 *     x: 10
 *     y: 0
 *     z: 10
 * }</pre>
 *
 * <p>Biome names follow the vanilla registry key format (e.g. {@code plains},
 * {@code dark_forest}) with an optional {@code minecraft:} namespace prefix.
 * Shapes: {@code CUBOID}, {@code RHOMBUS}, {@code SPHERE}, {@code CYLINDER}.
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "replace_biome")
public final class ReplaceBiomeAction extends ActionExecutor {
    @Nullable private Biome biome;
    private BlocksShape shape = BlocksShape.SPHERE;
    private int radiusX = 5;
    private int radiusY = 5;
    private int radiusZ = 5;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!super.configure(configData, namespacedID)) return false;

        if (!(configData instanceof ConfigurationSection section)) {
            Log.warn("Actions", "replace_biome on '{}': config must be a section", namespacedID);
            return false;
        }

        String biomeName = section.getString("biome");
        if (biomeName == null) {
            Log.warn("Actions", "replace_biome on '{}': missing required key 'biome'", namespacedID);
            return false;
        }

        // Accept both plain keys ("plains", "dark_forest") and namespaced keys
        // ("minecraft:plains", "someplugin:custom_biome"). Key.key(string) parses
        // "namespace:value"; Key.key(MINECRAFT_NAMESPACE, value) is used as the
        // fallback when no colon is present so bare names default to minecraft:.
        Key biomeKey;
        String normalizedName = biomeName.toLowerCase();
        if (normalizedName.contains(":")) {
            biomeKey = Key.key(normalizedName);
        } else {
            biomeKey = Key.key(Key.MINECRAFT_NAMESPACE, normalizedName);
        }

        biome = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.BIOME)
                .get(biomeKey);

        if (biome == null) {
            Log.warn("Actions", "replace_biome on '{}': unknown biome '{}'. Use a vanilla key ('plains') or a namespaced key ('mymod:custom_biome')",
                    namespacedID, biomeName);
            return false;
        }

        String shapeName = section.getString("shape", "SPHERE");
        try {
            shape = BlocksShape.valueOf(shapeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            Log.warn("Actions", "replace_biome on '{}': unknown shape '{}'. Valid values: CUBOID, RHOMBUS, SPHERE, CYLINDER",
                    namespacedID, shapeName);
            return false;
        }

        ConfigurationSection radiusSection = section.getConfigurationSection("radius");
        if (radiusSection == null) {
            Log.warn("Actions", "replace_biome on '{}': missing 'radius' section", namespacedID);
            return false;
        }

        if (radiusSection.contains("blocks_from_center")) {
            int uniform = radiusSection.getInt("blocks_from_center");
            radiusX = uniform;
            radiusY = uniform;
            radiusZ = uniform;
        } else {
            radiusX = radiusSection.getInt("x", 5);
            radiusY = radiusSection.getInt("y", 5);
            radiusZ = radiusSection.getInt("z", 5);
        }

        if (radiusX < 0 || radiusY < 0 || radiusZ < 0) {
            Log.warn("Actions", "replace_biome on '{}': radius values must be >= 0", namespacedID);
            return false;
        }

        Log.debug("ReplaceBiome", "Configured: biome={}, shape={}, rx={}, ry={}, rz={}",
                biome.getKey(), shape, radiusX, radiusY, radiusZ);
        return true;
    }

    @Override
    protected void execute(ActionContext context) {
        if (biome == null) return;

        Location location = context.runOn().getLocation();
        Location center = location;
        Vector dir = shape.isDirectional()
                ? location.getDirection().normalize()
                : null;

        List<Location> targets = shape.collectBiomeQuanta(center, radiusX, radiusY, radiusZ, dir);
        AntiGriefLib antiGriefLib = ItemsAdderAdditions.instance().antiGriefLib();
        targets.removeIf(target -> !antiGriefLib.test(context.player(), Flag.PLACE, target));

        Log.debug("ReplaceBiome", "Replacing {} biome quanta with biome {} (shape={}, rx={}, ry={}, rz={})",
                targets.size(), biome.getKey(), shape, radiusX, radiusY, radiusZ);

        NmsManager.instance().handler().biome().setBiomes(center.getWorld(), targets, biome);
    }
}
