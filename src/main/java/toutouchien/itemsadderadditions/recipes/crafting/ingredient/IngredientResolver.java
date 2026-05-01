package toutouchien.itemsadderadditions.recipes.crafting.ingredient;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses ingredient entries from YAML into {@link ParsedIngredient} instances.
 *
 * <p>Accepted YAML forms for <b>char-keyed</b> (shaped / keyed-shapeless) ingredients:
 * <pre>
 * S: STICK
 * S: "#minecraft:planks"
 * S: "example:my_sword"
 *
 * S:
 *   item: POTION
 *   potion_type: minecraft:infested
 *   amount: 2
 *   damage: 5
 *   replacement: STICK
 *   ignore_durability: true
 * </pre>
 *
 * <p>Accepted YAML form for <b>list-style</b> (anonymous shapeless) ingredients:
 * <pre>
 * ingredients:
 *   - SNOWBALL
 *   - item: WATER_BUCKET
 *     replacement: BUCKET
 * </pre>
 *
 * <h3>Custom-item fast path</h3>
 * <p>When an ingredient resolves to an ItemsAdder custom item, its
 * {@link CustomStack#getNamespacedID()} is stored in
 * {@link ParsedIngredient#customNamespacedId()} alongside a precomputed
 * {@link String#hashCode()}.  {@code CraftingRecipeListener.testIngredient}
 * can then validate the slot via a fast
 * {@link CustomStack#byItemStack(ItemStack)} + integer hash comparison instead
 * of the expensive {@link RecipeChoice.ExactChoice#test} path (which calls
 * {@link ItemStack#isSimilar} and compares full {@code ItemMeta}).
 */
@NullMarked
public final class IngredientResolver {
    private static final String LOG_TAG = "IngredientResolver";

    // Synthetic char keys used when building a map from a list-style section.
    private static final char[] SYNTHETIC_KEYS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private IngredientResolver() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Resolves one char-keyed ingredient from a {@link ConfigurationSection}.
     *
     * @param namespace namespace used to resolve custom items
     * @param section   the {@code ingredients} config section
     * @param key       single-character key (e.g. {@code "P"})
     * @param recipeId  used in log messages only
     */
    @Nullable
    public static ParsedIngredient resolve(
            String namespace,
            ConfigurationSection section,
            String key,
            String recipeId
    ) {
        Object raw = section.get(key);
        return parseRawEntry(namespace, raw, key, recipeId);
    }

    /**
     * Resolves list-style ingredients (anonymous shapeless format).
     *
     * <p>Each list element may be a plain string or a map with {@code item} /
     * predicate keys. The returned map uses synthetic single-character keys so
     * the rest of the pipeline stays unchanged.
     *
     * @param namespace namespace used to resolve custom items
     * @param list      the raw YAML list under {@code ingredients}
     * @param recipeId  used in log messages only
     * @return ordered map (insertion-order preserved), or {@code null} on error
     */
    @Nullable
    public static Map<Character, ParsedIngredient> resolveList(
            String namespace,
            List<?> list,
            String recipeId
    ) {
        if (list.size() > SYNTHETIC_KEYS.length) {
            Log.warn(LOG_TAG,
                    "Recipe '{}' has {} list ingredients; max is {}.",
                    recipeId, list.size(), SYNTHETIC_KEYS.length);
            return null;
        }

        Map<Character, ParsedIngredient> result = new LinkedHashMap<>();
        boolean anyFailed = false;

        for (int i = 0; i < list.size(); i++) {
            char syntheticKey = SYNTHETIC_KEYS[i];
            Object raw = list.get(i);

            ParsedIngredient ingredient =
                    parseRawEntry(namespace, raw, String.valueOf(syntheticKey), recipeId);
            if (ingredient == null) {
                anyFailed = true;
            } else {
                result.put(syntheticKey, ingredient);
            }
        }

        return anyFailed ? null : result;
    }

    /**
     * Parses one raw YAML value (String, Map, or ConfigurationSection) into
     * a {@link ParsedIngredient}. Used by both {@link #resolve} and
     * {@link #resolveList}.
     */
    @Nullable
    private static ParsedIngredient parseRawEntry(
            String namespace,
            @Nullable Object raw,
            String keyLabel,
            String recipeId
    ) {
        String itemRef;
        int requiredAmount = 1;
        int damageAmount = 0;
        ItemStack replacement = null;
        boolean ignoreDurability = false;
        @Nullable String potionType = null;

        if (raw instanceof ConfigurationSection sub) {
            itemRef = sub.getString("item");
            if (itemRef == null) {
                Log.warn(LOG_TAG,
                        "Ingredient '{}' in recipe '{}' is missing 'item'.",
                        keyLabel, recipeId);
                return null;
            }
            requiredAmount = sub.getInt("amount", 1);
            damageAmount = sub.getInt("damage", 0);
            ignoreDurability = sub.getBoolean("ignore_durability", false);
            potionType = sub.getString("potion_type", null);

            String replacementRef = sub.getString("replacement");
            if (replacementRef != null) {
                replacement = resolveItem(namespace, replacementRef, recipeId,
                        "replacement for '" + keyLabel + "'");
                if (replacement == null) return null;
                damageAmount = 0;
            }

        } else if (raw instanceof Map<?, ?> map) {
            Object itemVal = map.get("item");
            if (!(itemVal instanceof String ref)) {
                Log.warn(LOG_TAG,
                        "Ingredient '{}' in recipe '{}' is missing 'item'.",
                        keyLabel, recipeId);
                return null;
            }
            itemRef = ref;

            if (map.get("amount") instanceof Number n) requiredAmount = n.intValue();
            if (map.get("damage") instanceof Number n) damageAmount = n.intValue();
            if (map.get("ignore_durability") instanceof Boolean b) ignoreDurability = b;
            if (map.get("potion_type") instanceof String pt) potionType = pt;

            if (map.get("replacement") instanceof String replacementRef) {
                replacement = resolveItem(namespace, replacementRef, recipeId,
                        "replacement for '" + keyLabel + "'");
                if (replacement == null) return null;
                damageAmount = 0;
            }

        } else if (raw instanceof String ref) {
            itemRef = ref;

        } else {
            Log.warn(LOG_TAG,
                    "Ingredient '{}' in recipe '{}' has an unsupported format.",
                    keyLabel, recipeId);
            return null;
        }

        return buildIngredient(
                namespace, itemRef, keyLabel, recipeId,
                requiredAmount, damageAmount, replacement, ignoreDurability, potionType);
    }

    /**
     * Resolves {@code itemRef} and assembles the final {@link ParsedIngredient}.
     *
     * <h3>Custom-item path</h3>
     * <p>When {@code itemRef} resolves to an ItemsAdder {@link CustomStack}, the
     * ingredient is built with {@link ParsedIngredient#customNamespacedId()} set
     * to {@link CustomStack#getNamespacedID()}.  The hash is derived automatically
     * in {@link ParsedIngredient}'s compact constructor.
     *
     * <p>At validation time ({@code CraftingRecipeListener.testIngredient}),
     * instead of calling {@link RecipeChoice.ExactChoice#test} (which internally
     * runs {@link ItemStack#isSimilar} - a full {@code ItemMeta} comparison),
     * the listener calls {@link CustomStack#byItemStack(ItemStack)} and compares
     * integer hashes first.  Full string equality is only checked on hash
     * collision (practically never for namespaced IDs).
     *
     * <h3>Vanilla path</h3>
     * <p>{@link ParsedIngredient#customNamespacedId()} is {@code null}, and
     * validation falls back to {@link ParsedIngredient#validationChoice()}.
     */
    @Nullable
    private static ParsedIngredient buildIngredient(
            String namespace,
            String itemRef,
            String keyLabel,
            String recipeId,
            int requiredAmount,
            int damageAmount,
            @Nullable ItemStack replacement,
            boolean ignoreDurability,
            @Nullable String potionType
    ) {
        if (itemRef.startsWith("#")) {
            RecipeChoice tagChoice = resolveTag(itemRef.substring(1), recipeId, keyLabel);
            if (tagChoice == null) return null;
            return new ParsedIngredient(
                    tagChoice, tagChoice,
                    requiredAmount, damageAmount, replacement,
                    ignoreDurability, potionType,
                    null, 0);   // tags are never custom items
        }

        CustomStack customStack = NamespaceUtils.customItemByID(namespace, itemRef);
        if (customStack != null) {
            RecipeChoice exactChoice =
                    new RecipeChoice.ExactChoice(customStack.getItemStack());

            RecipeChoice registrationChoice = new RecipeChoice.MaterialChoice(
                    customStack.getItemStack().getType());

            // Store the namespaced ID - hash is derived in the compact constructor
            String namespacedId = customStack.getNamespacedID();

            return new ParsedIngredient(
                    registrationChoice,
                    exactChoice,            // kept for ignoreDurability's ExactChoice test
                    requiredAmount, damageAmount, replacement,
                    ignoreDurability, potionType,
                    namespacedId, 0);       // hash computed automatically
        }

        Material mat = Material.matchMaterial(itemRef);
        if (mat != null) {
            RecipeChoice matChoice = new RecipeChoice.MaterialChoice(mat);
            return new ParsedIngredient(
                    matChoice, matChoice,
                    requiredAmount, damageAmount, replacement,
                    ignoreDurability, potionType,
                    null, 0);   // vanilla, no custom ID
        }

        Log.warn(LOG_TAG,
                "Ingredient '{}' in recipe '{}': could not resolve '{}'.",
                keyLabel, recipeId, itemRef);
        return null;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    @Nullable
    private static RecipeChoice resolveTag(
            String tagRef, String recipeId, String key
    ) {
        NamespacedKey tagKey = NamespacedKey.fromString(tagRef);
        if (tagKey == null) {
            Log.warn(LOG_TAG,
                    "Ingredient '{}' in recipe '{}': invalid tag key '#{}' .",
                    key, recipeId, tagRef);
            return null;
        }

        Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, tagKey, Material.class);
        if (tag == null)
            tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagKey, Material.class);

        if (tag == null || tag.getValues().isEmpty()) {
            Log.warn(LOG_TAG,
                    "Ingredient '{}' in recipe '{}': tag '#{}' is unknown or empty.",
                    key, recipeId, tagRef);
            return null;
        }

        return new RecipeChoice.MaterialChoice(new ArrayList<>(tag.getValues()));
    }

    @Nullable
    private static ItemStack resolveItem(
            String namespace, String ref, String recipeId, String context
    ) {
        CustomStack customStack = NamespaceUtils.customItemByID(namespace, ref);
        if (customStack != null) return customStack.getItemStack();

        Material mat = Material.matchMaterial(ref);
        if (mat != null) return new ItemStack(mat);

        Log.warn(LOG_TAG,
                "Could not resolve {} in recipe '{}': '{}'.",
                context, recipeId, ref);
        return null;
    }
}
