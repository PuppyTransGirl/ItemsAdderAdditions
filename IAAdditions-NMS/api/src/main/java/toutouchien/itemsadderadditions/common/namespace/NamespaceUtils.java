package toutouchien.itemsadderadditions.common.namespace;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import net.kyori.adventure.key.Key;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
public final class NamespaceUtils {
    private static final String LOG_TAG = "NamespaceUtils";

    private static volatile CustomTagRegistry customTagRegistry = CustomTagRegistry.empty();
    private static final Set<String> warnedAmbiguousTagReferences = ConcurrentHashMap.newKeySet();

    /**
     * Provider for {@code mmoitems:TYPE:ID} resolution; {@code null} when MMOItems is absent.
     */
    private static volatile @Nullable ItemProvider mmoitemsProvider = null;

    /**
     * Registers the provider used to resolve {@code mmoitems:TYPE:ID} strings.
     *
     * <p>Call once at plugin enable (after checking MMOItems is loaded) and
     * pass {@code null} on disable to release the reference.
     */
    public static void setMMOItemsProvider(@Nullable ItemProvider provider) {
        mmoitemsProvider = provider;
    }

    public static void setCustomTagRegistry(CustomTagRegistry registry) {
        customTagRegistry = registry;
        warnedAmbiguousTagReferences.clear();
    }

    public static void clearCustomTagRegistry() {
        setCustomTagRegistry(CustomTagRegistry.empty());
    }

    public static CustomTagRegistry customTagRegistry() {
        return customTagRegistry;
    }

    private static final String[] ROTATION_SUFFIXES = {
            "_north", "_south", "_east", "_west", "_up", "_down"
    };
    /**
     * Normalised namespaced-ID -> shared ItemStack for every vanilla Minecraft
     * item.  Built once by {@link #initVanillaCache()} at enable time and never
     * modified afterwards, so volatile reads are the only synchronisation cost.
     *
     * <p>Callers that need to mutate the returned stack must {@link ItemStack#clone()}
     * it themselves; callers that only read it (crafting checks, display, etc.)
     * can use the shared reference directly.
     */
    private static Map<String, ItemStack> vanillaItemCache = Map.of();

    /**
     * Normalised namespaced-ID -> pre-resolved {@link Key} for every vanilla item.
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
     * Resolves any item - vanilla Minecraft or custom IA - by ID.
     *
     * <p>For <strong>vanilla items</strong> the lookup hits {@link #vanillaItemCache},
     * a pre-built {@link Map} populated once at enable time.  This avoids the
     * {@link Key#key(String)} parse, the {@link Registry#get} traversal, and the
     * {@link ItemType#createItemStack()} allocation that the previous
     * implementation paid on every call.
     *
     * <p>The returned {@link ItemStack} for vanilla items is a <em>shared
     * reference</em>.  Callers that need to mutate it (quantity, meta, ...) must
     * {@link ItemStack#clone()} it first.
     *
     * @param currentNamespace namespace to prepend when {@code id} has none
     * @param id               raw item id as written in the YAML config
     */
    @Nullable
    public static ItemStack itemByID(@Nullable String currentNamespace, String id) {
        String normalizedId = normalizeItemID(currentNamespace, id);

        // Check for external integration namespaces (e.g. mmoitems:TYPE:ID).
        if (normalizedId.startsWith("mmoitems:")) {
            ItemProvider provider = mmoitemsProvider;
            if (provider == null || !provider.isAvailable()) return null;
            String rest = normalizedId.substring("mmoitems:".length());
            int colon = rest.indexOf(':');
            if (colon <= 0) return null;
            return provider.getItem(rest.substring(0, colon), rest.substring(colon + 1));
        }

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
        String lowerId = id.trim().toLowerCase(Locale.ROOT);

        if (lowerId.contains(":"))
            return lowerId; // Already contains namespace

        if (currentNamespace == null)
            return lowerId; // No namespace to prepend - caller's lookup will handle it

        return currentNamespace.toLowerCase(Locale.ROOT) + ":" + lowerId;
    }

    public static String normalizeNamespacedId(@Nullable String currentNamespace, String id) {
        return normalizeID(currentNamespace, id);
    }

    public static boolean isTagReference(String value) {
        return value.trim().startsWith("#");
    }

    public static String stripTagPrefix(String value) {
        String trimmed = value.trim();
        return trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
    }

    public static boolean isCustomTagReference(String value) {
        if (!isTagReference(value)) return false;
        String id = stripTagPrefix(value).toLowerCase(Locale.ROOT);
        if (!id.contains(":")) return false;
        for (CustomTagType type : CustomTagType.values()) {
            if (customTagRegistry.hasTag(id, type)) return true;
        }
        return false;
    }

    public static boolean isCustomTagReference(String value, CustomTagType expectedType) {
        if (!isTagReference(value)) return false;
        String id = stripTagPrefix(value).toLowerCase(Locale.ROOT);
        return id.contains(":") && customTagRegistry.hasTag(id, expectedType);
    }

    public static boolean isVanillaTagReference(String value) {
        if (!isTagReference(value)) return false;
        return stripTagPrefix(value).toLowerCase(Locale.ROOT).startsWith("minecraft:");
    }

    public static String normalizeCustomTagId(@Nullable String currentNamespace, String rawIdOrReference) {
        return normalizeID(currentNamespace, stripTagPrefix(rawIdOrReference));
    }

    public static String normalizeCustomTagReference(
            @Nullable String currentNamespace,
            String rawReference,
            CustomTagType expectedType
    ) {
        return "#" + normalizeCustomTagId(currentNamespace, rawReference);
    }

    /**
     * Normalizes an item ID exactly once at config-load time.
     *
     * <p>Lookup order for bare IDs is intentionally the same as most YAML files
     * expect: ItemsAdder item in the current namespace first, then vanilla
     * {@code minecraft:*}. IDs that already contain a namespace are only
     * lowercased, which keeps external formats such as
     * {@code mmoitems:TYPE:ID} compatible.
     *
     * @param currentNamespace namespace to use for bare ItemsAdder IDs
     * @param id raw item ID from config
     * @return canonical item ID used by runtime triggers
     */
    public static String normalizeItemID(@Nullable String currentNamespace, String id) {
        String lowerId = id.trim().toLowerCase(Locale.ROOT);
        if (lowerId.isBlank()) return lowerId;

        // Keep already-qualified IDs as-is apart from casing. This preserves
        // external integrations such as mmoitems:type:id.
        if (lowerId.contains(":")) return lowerId;

        CustomStack customStack = customItemByID(currentNamespace, lowerId);
        if (customStack != null) return customStack.getNamespacedID();

        return normalizeID("minecraft", lowerId);
    }

    @Nullable
    public static String normalizeItemIDNullable(@Nullable String currentNamespace, @Nullable String id) {
        return id == null ? null : normalizeItemID(currentNamespace, id);
    }

    /**
     * Normalizes an item ID or tag reference. Loaded custom IAA tags win; item
     * references that are not custom tags keep the existing vanilla-tag fallback.
     */
    public static String normalizeItemIDOrTag(@Nullable String currentNamespace, String id) {
        String trimmed = id.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("#")) {
            return normalizeReferenceOrVanillaTag(currentNamespace, trimmed, CustomTagType.ITEM);
        }
        return normalizeItemID(currentNamespace, trimmed);
    }

    @Nullable
    public static String normalizeItemIDOrTagNullable(@Nullable String currentNamespace, @Nullable String id) {
        return id == null ? null : normalizeItemIDOrTag(currentNamespace, id);
    }

    /**
     * Normalizes a block reference for predicates/triggers. Bare vanilla block
     * material names resolve to {@code minecraft:*}; other bare IDs resolve under
     * the current ItemsAdder namespace. Loaded custom IAA tags win; block
     * references that are not custom tags keep the existing vanilla-tag fallback.
     */
    public static String normalizeBlockIDOrTag(@Nullable String currentNamespace, String id) {
        String trimmed = id.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("#")) {
            return normalizeReferenceOrVanillaTag(currentNamespace, trimmed, CustomTagType.BLOCK);
        }
        return normalizeBlockID(currentNamespace, trimmed);
    }

    @Nullable
    public static String normalizeBlockIDOrTagNullable(@Nullable String currentNamespace, @Nullable String id) {
        return id == null ? null : normalizeBlockIDOrTag(currentNamespace, id);
    }

    public static String normalizeFurnitureIDOrTag(@Nullable String currentNamespace, String id) {
        String trimmed = id.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("#")) {
            return normalizeCustomTagReference(currentNamespace, trimmed, CustomTagType.FURNITURE);
        }
        return normalizeID(currentNamespace, trimmed);
    }

    @Nullable
    public static String normalizeFurnitureIDOrTagNullable(@Nullable String currentNamespace, @Nullable String id) {
        return id == null ? null : normalizeFurnitureIDOrTag(currentNamespace, id);
    }

    public static String normalizeRecipeIDOrTag(@Nullable String currentNamespace, String id) {
        String trimmed = id.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("#")) {
            return normalizeCustomTagReference(currentNamespace, trimmed, CustomTagType.RECIPE);
        }
        if (trimmed.isBlank()) return trimmed;
        if (trimmed.contains(":")) return normalizeID(currentNamespace, trimmed);

        // Preserve legacy direct recipe semantics: bare recipe conditions compare
        // against Bukkit's key path as well as full namespaced keys at runtime.
        return trimmed;
    }

    @Nullable
    public static String normalizeRecipeIDOrTagNullable(@Nullable String currentNamespace, @Nullable String id) {
        return id == null ? null : normalizeRecipeIDOrTag(currentNamespace, id);
    }

    /**
     * Normalizes a block reference without tag support.
     */
    public static String normalizeBlockID(@Nullable String currentNamespace, String id) {
        String lowerId = id.trim().toLowerCase(Locale.ROOT);
        if (lowerId.isBlank()) return lowerId;
        if (lowerId.contains(":")) return lowerId;

        Material material = vanillaMaterial(lowerId);
        if (material != null && material.isBlock()) {
            return material.getKey().toString();
        }

        return normalizeID(currentNamespace, lowerId);
    }

    @Nullable
    public static String normalizeBlockIDNullable(@Nullable String currentNamespace, @Nullable String id) {
        return id == null ? null : normalizeBlockID(currentNamespace, id);
    }

    /**
     * Normalizes vanilla registry IDs such as block, biome, entity, effect, or
     * recipe keys. Bare IDs are resolved under {@code minecraft}.
     */
    public static String normalizeMinecraftID(String id) {
        String lowerId = id.trim().toLowerCase(Locale.ROOT);
        if (lowerId.isBlank()) return lowerId;
        return normalizeID("minecraft", lowerId);
    }

    @Nullable
    public static String normalizeMinecraftIDNullable(@Nullable String id) {
        return id == null ? null : normalizeMinecraftID(id);
    }

    @Nullable
    public static String normalizeContentIdForCustomTagValue(
            @Nullable String currentNamespace,
            String raw,
            CustomTagType type
    ) {
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        if (lower.isBlank() || lower.startsWith("#")) return null;

        return switch (type) {
            case ITEM -> normalizeItemTagValue(currentNamespace, lower);
            case BLOCK -> normalizeBlockTagValue(currentNamespace, lower);
            case FURNITURE -> validNamespacedOrNull(normalizeID(currentNamespace, lower));
            case RECIPE -> normalizeRecipeTagValue(currentNamespace, lower);
        };
    }

    private static String normalizeReferenceOrVanillaTag(
            @Nullable String currentNamespace,
            String rawReference,
            CustomTagType type
    ) {
        String customReference = normalizeCustomTagReference(currentNamespace, rawReference, type);
        String customId = stripTagPrefix(customReference);
        if (customTagRegistry.hasTag(customId, type)) {
            warnAmbiguousCustomVanillaTag(customId, type);
            return customReference;
        }

        return "#" + normalizeMinecraftID(stripTagPrefix(rawReference));
    }

    @Nullable
    private static String normalizeItemTagValue(@Nullable String currentNamespace, String lower) {
        if (lower.startsWith("mmoitems:")) {
            return isValidMMOItemsId(lower) ? lower : null;
        }

        if (lower.contains(":")) {
            return isValidNamespacedId(lower) ? lower : null;
        }

        CustomStack customStack = customItemByID(currentNamespace, lower);
        if (customStack != null) return customStack.getNamespacedID();

        Material material = vanillaMaterial(lower);
        if (material != null && material.isItem()) {
            return material.getKey().toString();
        }

        return validNamespacedOrNull(normalizeID(currentNamespace, lower));
    }

    @Nullable
    private static String normalizeBlockTagValue(@Nullable String currentNamespace, String lower) {
        if (lower.contains(":")) {
            return isValidNamespacedId(lower) ? lower : null;
        }

        Material material = vanillaMaterial(lower);
        if (material != null && material.isBlock()) {
            return material.getKey().toString();
        }

        return validNamespacedOrNull(normalizeID(currentNamespace, lower));
    }

    @Nullable
    private static String normalizeRecipeTagValue(@Nullable String currentNamespace, String lower) {
        if (lower.contains(":")) {
            return isValidNamespacedId(lower) ? lower : null;
        }
        return validNamespacedOrNull(normalizeID(currentNamespace, lower));
    }

    @Nullable
    private static String validNamespacedOrNull(String id) {
        return isValidNamespacedId(id) ? id : null;
    }

    public static boolean isValidNamespacedId(String id) {
        String lower = id.trim().toLowerCase(Locale.ROOT);
        if (lower.isBlank()) return false;
        if (!lower.contains(":")) return false;
        if (lower.indexOf(':') != lower.lastIndexOf(':')) return false;
        return NamespacedKey.fromString(lower) != null;
    }

    private static boolean isValidMMOItemsId(String id) {
        String rest = id.substring("mmoitems:".length());
        int colon = rest.indexOf(':');
        return colon > 0
                && colon < rest.length() - 1
                && rest.indexOf(':', colon + 1) < 0
                && isValidPathSegment(rest.substring(0, colon))
                && isValidPathSegment(rest.substring(colon + 1));
    }

    private static boolean isValidPathSegment(String value) {
        if (value.isBlank()) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.' || c == '/') {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * Returns the canonical ID of an ItemStack for runtime comparisons.
     *
     * <p>ItemsAdder items are returned as {@code namespace:id}, MMOItems are
     * delegated to the registered provider and returned as
     * {@code mmoitems:type:id}, and vanilla items are returned as
     * {@code minecraft:item}.
     */
    @Nullable
    public static String itemID(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        try {
            CustomStack customStack = CustomStack.byItemStack(item);
            if (customStack != null) return customStack.getNamespacedID();
        } catch (RuntimeException ignored) {
            // ItemsAdder may throw while its runtime is not bootstrapped (unit tests / early startup).
        }

        ItemProvider provider = mmoitemsProvider;
        if (provider != null && provider.isAvailable()) {
            String externalId = provider.getItemId(item);
            if (externalId != null && !externalId.isBlank()) {
                return externalId.toLowerCase(Locale.ROOT);
            }
        }

        return item.getType().getKey().toString();
    }

    /**
     * Matches an ItemStack against a normalized item ID, including MMOItems when
     * a provider is registered.
     */
    public static boolean matchesItemID(@Nullable ItemStack item, String normalizedId) {
        if (item == null || item.getType().isAir()) return false;

        String lowerId = normalizedId.trim().toLowerCase(Locale.ROOT);
        if (lowerId.startsWith("mmoitems:")) {
            ItemProvider provider = mmoitemsProvider;
            if (provider == null || !provider.isAvailable()) return false;

            String rest = lowerId.substring("mmoitems:".length());
            int colon = rest.indexOf(':');
            if (colon <= 0) return false;

            return provider.matchesItem(item, rest.substring(0, colon), rest.substring(colon + 1));
        }

        String itemId = itemID(item);
        return lowerId.equals(itemId);
    }

    /**
     * Matches an ItemStack against an exact item ID or a vanilla item tag
     * reference such as {@code #minecraft:planks}.
     */
    public static boolean matchesItemIDOrTag(@Nullable ItemStack item, String normalizedIdOrTag) {
        if (item == null || item.getType().isAir()) return false;

        String expected = normalizedIdOrTag.trim().toLowerCase(Locale.ROOT);
        if (expected.startsWith("#")) {
            String tagId = stripTagPrefix(expected);
            if (customTagRegistry.hasTag(tagId, CustomTagType.ITEM)) {
                String itemId = itemID(item);
                return itemId != null && customTagContainsContent(tagId, CustomTagType.ITEM, itemId);
            }
            return matchesMaterialTag(item.getType(), expected.substring(1), Tag.REGISTRY_ITEMS);
        }

        return matchesItemID(item, expected);
    }

    /**
     * Returns the canonical ID of a placed block. ItemsAdder custom blocks keep
     * their IA namespaced ID; vanilla blocks return their Minecraft key.
     */
    public static String blockID(Block block) {
        try {
            CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
            if (customBlock != null) return customBlock.getNamespacedID();
        } catch (RuntimeException ignored) {
            // ItemsAdder can throw while not fully loaded; fall back to vanilla.
        }

        return block.getType().getKey().toString();
    }

    /**
     * Matches a placed block against an ItemsAdder block ID, a vanilla block ID,
     * or a vanilla block tag reference such as {@code #minecraft:logs}.
     */
    public static boolean matchesBlockIDOrTag(Block block, String normalizedIdOrTag) {
        String expected = normalizedIdOrTag.trim().toLowerCase(Locale.ROOT);
        if (expected.startsWith("#")) {
            String tagId = stripTagPrefix(expected);
            if (customTagRegistry.hasTag(tagId, CustomTagType.BLOCK)) {
                return customTagContainsContent(tagId, CustomTagType.BLOCK, blockID(block));
            }
            return matchesMaterialTag(block.getType(), expected.substring(1), Tag.REGISTRY_BLOCKS);
        }

        return matchesContentID(blockID(block), expected);
    }

    public static boolean matchesFurnitureIDOrTag(String actualFurnitureId, String normalizedIdOrTag) {
        return matchesContentIDOrTag(actualFurnitureId, normalizedIdOrTag, CustomTagType.FURNITURE);
    }

    public static boolean matchesRecipeIDOrTag(String actualRecipeKey, String normalizedIdOrTag) {
        String actual = actualRecipeKey.trim().toLowerCase(Locale.ROOT);
        String expected = normalizedIdOrTag.trim().toLowerCase(Locale.ROOT);
        if (expected.isBlank()) return true;

        if (expected.startsWith("#")) {
            String tagId = stripTagPrefix(expected);
            return customTagRegistry.hasTag(tagId, CustomTagType.RECIPE)
                    && customTagRegistry.contains(tagId, CustomTagType.RECIPE, actual);
        }

        if (actual.equals(expected)) return true;

        int colon = actual.indexOf(':');
        return colon >= 0 && expected.indexOf(':') < 0 && actual.substring(colon + 1).equals(expected);
    }

    /**
     * Matches two already-normalized content IDs. Rotation suffixes are accepted
     * for ItemsAdder block/furniture IDs but never for Minecraft registry IDs.
     */
    public static boolean matchesContentID(String actual, String expected) {
        String lowerActual = actual.trim().toLowerCase(Locale.ROOT);
        String lowerExpected = expected.trim().toLowerCase(Locale.ROOT);
        if (lowerActual.startsWith("minecraft:") || lowerExpected.startsWith("minecraft:")) {
            return lowerActual.equals(lowerExpected);
        }
        return matchesWithRotation(lowerActual, lowerExpected);
    }

    public static boolean matchesContentIDOrTag(String actualId, String expectedIdOrTag, CustomTagType expectedType) {
        String actual = actualId.trim().toLowerCase(Locale.ROOT);
        String expected = expectedIdOrTag.trim().toLowerCase(Locale.ROOT);

        if (!expected.startsWith("#")) {
            return expectedType == CustomTagType.RECIPE
                    ? matchesRecipeIDOrTag(actual, expected)
                    : matchesContentID(actual, expected);
        }

        String tagId = stripTagPrefix(expected);
        if (customTagRegistry.hasTag(tagId, expectedType)) {
            return customTagContainsContent(tagId, expectedType, actual);
        }

        if (expectedType == CustomTagType.ITEM || expectedType == CustomTagType.BLOCK) {
            return matchesMinecraftIDOrTag(actual, expected);
        }

        return false;
    }


    /**
     * Matches an already-normalized Minecraft item/block ID against either an
     * exact ID or a vanilla registry tag. This is useful for systems that only
     * carry the observed ID at dispatch time.
     */
    public static boolean matchesMinecraftIDOrTag(String actualId, String expectedIdOrTag) {
        String actual = actualId.trim().toLowerCase(Locale.ROOT);
        String expected = expectedIdOrTag.trim().toLowerCase(Locale.ROOT);
        if (!expected.startsWith("#")) return matchesContentID(actual, expected);
        if (!actual.startsWith("minecraft:")) return false;

        Material material = vanillaMaterial(actual);
        if (material == null) return false;

        String tag = expected.substring(1);
        return (material.isItem() && matchesMaterialTag(material, tag, Tag.REGISTRY_ITEMS))
                || (material.isBlock() && matchesMaterialTag(material, tag, Tag.REGISTRY_BLOCKS));
    }

    private static boolean customTagContainsContent(String tagId, CustomTagType type, String actualId) {
        String actual = actualId.trim().toLowerCase(Locale.ROOT);
        if (customTagRegistry.contains(tagId, type, actual)) return true;

        if (type == CustomTagType.BLOCK || type == CustomTagType.FURNITURE) {
            String base = stripRotationSuffix(actual);
            return !base.equals(actual) && customTagRegistry.contains(tagId, type, base);
        }

        return false;
    }

    /**
     * Resolves a vanilla material from either {@code stone} or
     * {@code minecraft:stone}. Non-Minecraft namespaces return {@code null}.
     */
    @Nullable
    public static Material vanillaMaterial(String raw) {
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        if (lower.isBlank()) return null;
        if (lower.contains(":")) {
            int colon = lower.indexOf(':');
            if (!lower.substring(0, colon).equals("minecraft")) return null;
            lower = lower.substring(colon + 1);
        }
        return Material.matchMaterial(lower.toUpperCase(Locale.ROOT));
    }

    private static boolean matchesMaterialTag(Material material, String rawTag, String registry) {
        NamespacedKey key = minecraftKey(rawTag);
        if (key == null) return false;

        Tag<Material> tag = Bukkit.getTag(registry, key, Material.class);
        return tag != null && tag.isTagged(material);
    }

    private static void warnAmbiguousCustomVanillaTag(String tagId, CustomTagType type) {
        if ((type != CustomTagType.ITEM && type != CustomTagType.BLOCK) || !tagId.startsWith("minecraft:")) {
            return;
        }

        String warningKey = type + ":" + tagId;
        if (!warnedAmbiguousTagReferences.add(warningKey)) {
            return;
        }

        try {
            NamespacedKey key = minecraftKey(tagId);
            if (key == null) return;
            String registry = type == CustomTagType.ITEM ? Tag.REGISTRY_ITEMS : Tag.REGISTRY_BLOCKS;
            Tag<Material> tag = Bukkit.getTag(registry, key, Material.class);
            if (tag == null) return;

            Log.warn(LOG_TAG,
                    "Custom {} tag '#{}' also exists as a vanilla Bukkit tag; using the ItemsAdderAdditions custom tag in this context.",
                    type, tagId);
        } catch (RuntimeException ignored) {
            // Bukkit may be unavailable in isolated unit tests.
        }
    }

    @Nullable
    private static NamespacedKey minecraftKey(String raw) {
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        if (lower.isBlank()) return null;
        return lower.contains(":")
                ? NamespacedKey.fromString(lower)
                : new NamespacedKey("minecraft", lower);
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

        // normalizedId has a colon -> extract path and try path-keyed map.
        // Use the pre-resolved Key from the vanilla cache if available; fall back
        // to Key.key() only for truly unknown namespaced IDs.
        Key resolved = vanillaKeyCache.get(normalizedId);
        String path;
        if (resolved != null) {
            path = resolved.value();
        } else {
            int colon = normalizedId.indexOf(':');
            if (colon < 0 || colon == normalizedId.length() - 1) return null;
            path = normalizedId.substring(colon + 1);
        }
        return cacheByPath.get(path);
    }

    /**
     * Resolves an item for a specific external integration namespace.
     *
     * <p>Implementations are registered once on plugin enable via
     * {@link #setMMOItemsProvider} and called whenever {@link #itemByID}
     * encounters a matching namespace prefix.
     */
    @FunctionalInterface
    public interface ItemProvider {
        /**
         * @param type the type segment of the namespaced ID, already lowercased
         * @param id   the item-ID segment, already lowercased
         * @return the resolved stack, or {@code null} if the item does not exist
         */
        @Nullable ItemStack getItem(String type, String id);

        /**
         * Returns this provider's canonical ID for an item, or {@code null} when
         * the item does not belong to the integration.
         */
        default @Nullable String getItemId(ItemStack item) {
            return null;
        }

        /**
         * Returns whether {@code item} is exactly the external item represented
         * by {@code type:id}. Providers can override this to use native metadata
         * checks instead of comparing display ItemStacks.
         */
        default boolean matchesItem(ItemStack item, String type, String id) {
            String itemId = getItemId(item);
            return itemId != null && itemId.equalsIgnoreCase("mmoitems:" + type + ":" + id);
        }

        /**
         * Allows callers to fail open when the backing plugin is absent.
         */
        default boolean isAvailable() {
            return true;
        }
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
