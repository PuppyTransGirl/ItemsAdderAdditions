package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.Locale;

@NullMarked
public final class StorageTypes {
    private StorageTypes() {
    }

    public static StorageType resolve(String typeName, String namespacedId) {
        try {
            return StorageType.valueOf(typeName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            Log.warn("Storage", "Unknown type '{}' for '{}'. Defaulting to STORAGE.", typeName, namespacedId);
            return StorageType.STORAGE;
        }
    }
}
