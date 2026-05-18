package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.MenuType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.Locale;

/**
 * Resolves the {@code inventory_type} config string to a {@link StorageInventorySpec}.
 *
 * <p>Functional types that have real Minecraft processing logic (furnace, blast_furnace,
 * smoker, brewing_stand) resolve to {@link StorageInventorySpec.Menu} backed by {@link MenuType},
 * which opens a working menu. Shaped-only types (dispenser, dropper, hopper) resolve to
 * {@link StorageInventorySpec.Typed} backed by {@link InventoryType}.
 *
 * <p>Returns {@code null} when the value is absent (chest-style rows are used instead)
 * or unsupported.
 */
@NullMarked
@SuppressWarnings("UnstableApiUsage")
public final class StorageInventoryTypes {
    private StorageInventoryTypes() {
    }

    @Nullable
    public static StorageInventorySpec resolve(@Nullable String name, String namespacedId) {
        if (name == null) return null;

        return switch (name.toLowerCase(Locale.ROOT)) {
            case "furnace" -> new StorageInventorySpec.Menu(MenuType.FURNACE);
            case "blast_furnace" -> new StorageInventorySpec.Menu(MenuType.BLAST_FURNACE);
            case "smoker" -> new StorageInventorySpec.Menu(MenuType.SMOKER);
            case "brewing_stand" -> new StorageInventorySpec.Menu(MenuType.BREWING_STAND);
            case "dispenser" -> new StorageInventorySpec.Typed(InventoryType.DISPENSER);
            case "dropper" -> new StorageInventorySpec.Typed(InventoryType.DROPPER);
            case "hopper" -> new StorageInventorySpec.Typed(InventoryType.HOPPER);
            default -> {
                Log.warn("Storage",
                        "storage '{}': unknown inventory_type '{}'. Valid values: furnace, blast_furnace, smoker, brewing_stand, dispenser, dropper, hopper.",
                        namespacedId, name);
                yield null;
            }
        };
    }
}
