package toutouchien.itemsadderadditions.utils;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NullMarked
public final class NamespaceUtils {
    private static final String LOG_TAG = "NamespaceUtils";

    /**
     * Holds two flat maps built once from {@link ItemsAdder#getAllItems()} so
     * that every subsequent lookup is O(1) instead of O(n).
     *
     * <ul>
     *   <li>{@code byNamespacedId} - keyed by {@link CustomStack#getNamespacedID()}
     *       (e.g. {@code "myns:my_sword"})</li>
     *   <li>{@code byPath} - keyed by {@link CustomStack#getId()}
     *       (e.g. {@code "my_sword"}, the part after the colon)</li>
     * </ul>
     * <p>
     * Both maps are replaced atomically on each reload; individual entries are
     * never mutated after the maps are published.
     */
    private static Map<String, CustomStack> cacheByNamespacedId = Map.of();
    private static Map<String, CustomStack> cacheByPath = Map.of();

    /**
     * Integer hash code snapshot taken when the cache was last built, keyed by
     * namespacedId. Used to detect stacks whose underlying ItemStack changed
     * between reloads so only the affected entries are logged as changed.
     *
     * <p>Comparison is done using {@link ItemStack#hashCode()}.
     */
    private static Map<String, Integer> cachedItemHashes = Map.of();

    private NamespaceUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Builds (or refreshes) the CustomStack cache from the live IA item list.
     *
     * <p>On the first call every item is inserted. On subsequent calls each
     * item's current {@link ItemStack} hash is compared with the hash stored
     * during the previous build; only entries whose ItemStack changed (or that
     * are completely new) are tracked as changed. Entries that no longer exist in IA
     * are removed.
     *
     * @param allItems the list returned by {@link ItemsAdder#getAllItems()}
     */
    public static void buildCache(List<CustomStack> allItems) {
        Map<String, Integer> previousHashes = cachedItemHashes;

        int allItemsSize = allItems.size();
        Map<String, CustomStack> newById = HashMap.newHashMap(allItemsSize);
        Map<String, CustomStack> newByPath = HashMap.newHashMap(allItemsSize);
        Map<String, Integer> newHashes = HashMap.newHashMap(allItemsSize);

        int added = 0;
        int changed = 0;
        int unchanged = 0;

        for (CustomStack stack : allItems) {
            String nsId = stack.getNamespacedID();
            String path = stack.getId();
            ItemStack currentItem = stack.getItemStack();
            int currentHash = currentItem.hashCode();

            Integer previousHash = previousHashes.get(nsId);
            if (previousHash == null) {
                // Brand-new entry
                added++;
            } else if (previousHash != currentHash) {
                // Existed before but its ItemStack hash changed
                changed++;
            } else {
                unchanged++;
            }

            newById.put(nsId, stack);
            newByPath.put(path, stack);
            newHashes.put(nsId, currentHash);
        }

        // Publish atomically (volatile write)
        cacheByNamespacedId = Map.copyOf(newById);
        cacheByPath = Map.copyOf(newByPath);
        cachedItemHashes = Map.copyOf(newHashes);

        Log.debug(LOG_TAG,
                "CustomStack cache built: {} entries ({} new, {} changed, {} unchanged).",
                allItemsSize, added, changed, unchanged);
    }

    /**
     * Clears all cached data. Call this on plugin disable so stale references
     * are not held across server reloads.
     */
    public static void invalidateCache() {
        cacheByNamespacedId = Map.of();
        cacheByPath = Map.of();
        cachedItemHashes = Map.of();
        Log.debug(LOG_TAG, "CustomStack cache cleared.");
    }

    /**
     * Normalizes an ID by adding the current namespace if missing and converting to lowercase.
     * This ensures compatibility with Key.key() which requires lowercase characters.
     *
     * @param currentNamespace namespace to prepend when {@code id} has none;
     *                         may be {@code null}, in which case a bare {@code id}
     *                         is returned as-is (lowercase).
     */
    public static String normalizeID(@Nullable String currentNamespace, String id) {
        String lowerId = id.toLowerCase();

        if (lowerId.contains(":"))
            return lowerId; // Already contains namespace

        if (currentNamespace == null)
            return lowerId; // No namespace to prepend - caller's lookup will handle it

        return currentNamespace.toLowerCase() + ":" + lowerId;
    }

    public static String namespace(String namespacedID) {
        return namespacedID.substring(0, namespacedID.indexOf(':'));
    }

    public static String id(String namespacedID) {
        return namespacedID.substring(namespacedID.indexOf(':') + 1);
    }

    /**
     * Resolves a {@link CustomStack} by ID using the pre-built cache.
     *
     * <p>Lookup order:
     * <ol>
     *   <li>Cache by fully-qualified namespaced ID (e.g. {@code "myns:my_sword"})</li>
     *   <li>Cache by bare path / id (e.g. {@code "my_sword"})</li>
     * </ol>
     *
     * <p>{@link ItemsAdder#getAllItems()} and {@link CustomStack#getInstance}
     * are never called on this hot path - all resolution happens in O(1) map
     * lookups against data built once during {@link #buildCache}.
     *
     * @param currentNamespace namespace to prepend when {@code id} has none
     * @param id               raw item id as written in the YAML config
     */
    @Nullable
    public static CustomStack customItemByID(@Nullable String currentNamespace, String id) {
        String normalizedId = normalizeID(currentNamespace, id);

        // 1. Try exact namespaced-id lookup
        CustomStack found = cacheByNamespacedId.get(normalizedId);
        if (found != null) return found;

        // 2. Try bare-path lookup (handles ids written without namespace)
        if (!normalizedId.contains(":")) {
            return cacheByPath.get(normalizedId);
        }

        // normalizedId has a colon → extract path and try path-keyed map
        String path = Key.key(normalizedId).value();
        return cacheByPath.get(path);
    }

    @Nullable
    public static ItemStack itemByID(@Nullable String currentNamespace, String id) {
        // First check if it's a Minecraft item
        String normalizedId = normalizeID("minecraft", id);
        Key key = Key.key(normalizedId);

        ItemType minecraftItemType = Registry.ITEM.get(key);
        if (minecraftItemType != null)
            return minecraftItemType.createItemStack();

        // If not a Minecraft item, check for custom item
        CustomStack customStack = customItemByID(currentNamespace, id);
        if (customStack != null)
            return customStack.getItemStack();

        return null;
    }
}
