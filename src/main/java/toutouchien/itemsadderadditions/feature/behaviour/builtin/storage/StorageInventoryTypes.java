package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import org.bukkit.event.inventory.InventoryType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.Locale;
import java.util.Set;

/**
 * Resolves the {@code inventory_type} config string to a Bukkit {@link InventoryType}.
 *
 * <p>Only inventory types that can meaningfully store items are accepted:
 * {@code furnace}, {@code blast_furnace}, {@code smoker}, {@code brewing_stand},
 * {@code dispenser}, {@code dropper}, {@code hopper}.
 *
 * <p>Returns {@code null} when the value is absent (chest-style rows are used instead)
 * or unsupported.</p>
 */
@NullMarked
public final class StorageInventoryTypes {
    private static final Set<InventoryType> ALLOWED = Set.of(
            InventoryType.FURNACE,
            InventoryType.BLAST_FURNACE,
            InventoryType.SMOKER,
            InventoryType.BREWING,
            InventoryType.DISPENSER,
            InventoryType.DROPPER,
            InventoryType.HOPPER
    );

    private StorageInventoryTypes() {
    }

    @Nullable
    public static InventoryType resolve(@Nullable String name, String namespacedId) {
        if (name == null) return null;

        // "brewing_stand" is the user-facing alias; Bukkit enum name is "BREWING"
        String normalized = name.toUpperCase(Locale.ROOT).replace("BREWING_STAND", "BREWING");

        InventoryType type;
        try {
            type = InventoryType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            Log.warn("Storage",
                    "storage '{}': unknown inventory_type '{}'. Valid values: furnace, blast_furnace, smoker, brewing_stand, dispenser, dropper, hopper.",
                    namespacedId, name);
            return null;
        }

        if (!ALLOWED.contains(type)) {
            Log.warn("Storage",
                    "storage '{}': inventory_type '{}' is not a storage inventory. Valid values: furnace, blast_furnace, smoker, brewing_stand, dispenser, dropper, hopper.",
                    namespacedId, name);
            return null;
        }

        return type;
    }
}
