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
    private static final String[] ROTATION_SUFFIXES = {
            "_north", "_south", "_east", "_west", "_up", "_down"
    };
    /**
     * Normalised namespaced-ID → shared ItemStack for every vanilla Minecraft
     * item.  Built once by {@link #initVanillaCache()} at enable time and never
     * modified afterwards, so volatile reads are the only synchronisation cost.
     *
     * <p>Callers that need to mutate the returned stack must {@link ItemStack#clone()}
     * it themselves; callers that only read it (crafting checks, display, etc.)
     * can use the shared reference directly.
     */
    private static Map<String, ItemStack> vanillaItemCache = Map.of();

    /**
     * Normalised namespaced-ID → pre-resolved {@link Key} for every vanilla item.
     * Avoids the string-parse + allocation that {@link Key#key(String)} performs on
     * every call.
     */
    private static Map<String, Key> vanillaKeyCache = Map.of();

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
     *
     * <p>Keys are {@link String#intern() interned} during {@link #buildCache} so
     * all subsequent usages share the same instance, reducing heap pressure and
     * enabling identity comparisons.
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

    /**
     * Number of added + changed entries from the most recent {@link #buildCache}
     * call.  {@code 0} means every custom item is byte-for-byte identical to
     * the previous cycle - downstream systems that depend only on the custom
     * item set (e.g. packet-level painting variant maps) may safely skip their
     * own rebuild when this is {@code 0}.
     *
     * <p>Reset to {@code -1} before the first build so callers can distinguish
     * "never built" from "built and nothing changed".
     */
    private static volatile int lastDelta = -1;

    private NamespaceUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Builds the vanilla-item caches from {@link Registry#ITEM}.
     *
     * <p>This method must be called exactly once, during {@code onEnable}, before
     * any {@link #itemByID} calls are made.  Vanilla items never change between
     * reloads, so this cache is built once and never invalidated.
     *
     * <p>After this call:
     * <ul>
     *   <li>{@link #itemByID} for any Minecraft item pays only one {@link Map#get}
     *       instead of a {@link Key#key} parse + {@link Registry#get} + {@link ItemType#createItemStack()}.</li>
     *   <li>The returned {@link ItemStack} is a shared reference.  Clone it if you
     *       need to mutate it.</li>
     * </ul>
     */
    public static void initVanillaCache() {
        int size = 0;
        // Count first so we can size the maps correctly.
        for (ItemType ignored : Registry.ITEM) size++;

        Map<String, ItemStack> items = HashMap.newHashMap(size);
        Map<String, Key> keys = HashMap.newHashMap(size);

        for (ItemType type : Registry.ITEM) {
            Key key = type.key();
            // e.g. "minecraft:stone" - already lowercase by MC convention
            String nsId = key.asString().intern();
            keys.put(nsId, key);
            items.put(nsId, type.createItemStack());
        }

        vanillaItemCache = Map.copyOf(items);
        vanillaKeyCache = Map.copyOf(keys);

        Log.debug(LOG_TAG, "Vanilla item cache built: {} entries.", size);
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
     * <p>Keys stored in the maps are {@link String#intern() interned} so all
     * lookups after the first encounter share a single {@code String} instance,
     * reducing heap pressure and allowing identity ({@code ==}) comparisons.
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
            // Intern both keys so repeated lookups after this build reuse the
            // same String instances (fewer allocations, GC-friendlier).
            String nsId = stack.getNamespacedID().intern();
            String path = stack.getId().intern();
            ItemStack currentItem = stack.getItemStack();
            int currentHash = currentItem.hashCode();

            Integer previousHash = previousHashes.get(nsId);
            if (previousHash == null) {
                added++;
            } else if (previousHash != currentHash) {
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
        lastDelta = added + changed;

        Log.debug(LOG_TAG,
                "CustomStack cache built: {} entries ({} new, {} changed, {} unchanged).",
                allItemsSize, added, changed, unchanged);
    }

    /**
     * Returns the number of added + changed entries from the most recent
     * {@link #buildCache} call.
     *
     * <p>A return value of {@code 0} means every custom item is byte-for-byte
     * identical to the previous reload cycle; downstream systems that depend
     * only on the custom item set (e.g. {@code PacketListener.updateCache()},
     * {@code RegistryInjector.injectPaintingVariants()}) may safely skip their
     * own rebuild when this returns {@code 0}.
     *
     * <p>Returns {@code -1} if {@link #buildCache} has never been called.
     */
    public static int lastCacheDeltaSize() {
        return lastDelta;
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
     * Normalizes an ID by adding the current namespace if missing and converting
     * to lowercase.
     *
     * <p><strong>Performance note:</strong> {@code id.toLowerCase()} allocates a
     * new {@link String} on every call.  IDs read from YAML configs should be
     * lowercased <em>once</em> at parse / config-load time so that hot-path
     * callers already hold a lowercase string and this method can fast-path
     * through without the allocation.  The defensive toLowerCase() below is kept
     * for safety but becomes a no-op for pre-normalised inputs.
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

        // normalizedId has a colon → extract path and try path-keyed map.
        // Use the pre-resolved Key from the vanilla cache if available; fall back
        // to Key.key() only for truly unknown namespaced IDs.
        Key resolved = vanillaKeyCache.get(normalizedId);
        String path = (resolved != null) ? resolved.value() : Key.key(normalizedId).value();
        return cacheByPath.get(path);
    }

    /**
     * Resolves any item - vanilla Minecraft or custom IA - by ID.
     *
     * <p>For <strong>vanilla items</strong> the lookup hits {@link #vanillaItemCache},
     * a pre-built {@link Map} populated once at enable time.  This avoids the
     * {@link Key#key(String)} parse, the {@link Registry#get} traversal, and the
     * {@link ItemType#createItemStack()} allocation that the previous
     * implementation paid on every call.
     *
     * <p>The returned {@link ItemStack} for vanilla items is a <em>shared
     * reference</em>.  Callers that need to mutate it (quantity, meta, …) must
     * {@link ItemStack#clone()} it first.
     *
     * @param currentNamespace namespace to prepend when {@code id} has none
     * @param id               raw item id as written in the YAML config
     */
    @Nullable
    public static ItemStack itemByID(@Nullable String currentNamespace, String id) {
        // Normalise under "minecraft" to match keys stored in vanillaItemCache.
        String normalizedId = normalizeID("minecraft", id);

        // O(1) map lookup - no Key parse, no Registry scan, no ItemStack allocation.
        ItemStack vanillaItem = vanillaItemCache.get(normalizedId);
        if (vanillaItem != null)
            return vanillaItem; // shared reference; clone if mutation is needed

        // Fall through to the custom-item cache.
        CustomStack customStack = customItemByID(currentNamespace, id);
        if (customStack != null)
            return customStack.getItemStack();

        return null;
    }

    /**
     * Strips a directional rotation suffix from a namespaced ID, returning the
     * base block ID that was used when the behaviour/action was registered.
     *
     * <p>For example, {@code "myns:block_north"} and {@code "myns:block_down"} both
     * resolve to {@code "myns:block"}.  IDs with no recognised suffix are returned
     * unchanged.
     *
     * <p>Only the {@code id} part (after the colon) is inspected; the namespace is
     * always preserved as-is.
     *
     * @param namespacedId a fully-qualified namespaced ID such as {@code "myns:my_block_north"}
     * @return the base namespaced ID with the rotation suffix removed, or the
     * original string if no rotation suffix was found
     */
    public static String stripRotationSuffix(String namespacedId) {
        int colonIndex = namespacedId.indexOf(':');
        if (colonIndex < 0) {
            // Bare path (no namespace) - check the whole string
            for (String suffix : ROTATION_SUFFIXES) {
                if (namespacedId.endsWith(suffix) && namespacedId.length() > suffix.length()) {
                    return namespacedId.substring(0, namespacedId.length() - suffix.length());
                }
            }
            return namespacedId;
        }

        String idPart = namespacedId.substring(colonIndex + 1);
        for (String suffix : ROTATION_SUFFIXES) {
            if (idPart.endsWith(suffix) && idPart.length() > suffix.length()) {
                return namespacedId.substring(0, colonIndex + 1)
                        + idPart.substring(0, idPart.length() - suffix.length());
            }
        }
        return namespacedId;
    }

    /**
     * Returns {@code true} if {@code actual} identifies the same logical block as
     * {@code base}, accounting for directional rotation suffixes.
     *
     * <p>Specifically, this is equivalent to:
     * <pre>{@code stripRotationSuffix(actual).equals(base)}</pre>
     * but avoids allocating a new string when {@code actual} already equals {@code base}.
     *
     * <p>Typical usage in per-item event listeners:
     * <pre>{@code
     * // Instead of:
     * if (!cb.getNamespacedID().equals(namespacedID)) return;
     * // Use:
     * if (!NamespaceUtils.matchesWithRotation(cb.getNamespacedID(), namespacedID)) return;
     * }</pre>
     *
     * @param actual the namespaced ID observed at runtime (may carry a rotation suffix)
     * @param base   the registered base namespaced ID (no rotation suffix)
     * @return {@code true} if {@code actual} is {@code base} or a rotated variant of it
     */
    public static boolean matchesWithRotation(String actual, String base) {
        if (actual.equals(base)) return true;
        return stripRotationSuffix(actual).equals(base);
    }

}
