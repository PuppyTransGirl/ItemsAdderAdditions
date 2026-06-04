package toutouchien.itemsadderadditions.feature.recipe.crafting.ingredient;

import dev.lone.itemsadder.api.CustomStack;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.set.RegistrySet;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.RecipeChoice;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.CustomTagType;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.integration.hook.MMOItemsHook;

import java.util.*;

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

        switch (raw) {
            case ConfigurationSection sub -> {
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
            }

            case Map<?, ?> map -> {
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
            }

            case String ref -> itemRef = ref;

            case null, default -> {
                Log.warn(LOG_TAG,
                        "Ingredient '{}' in recipe '{}' has an unsupported format.",
                        keyLabel, recipeId);
                return null;
            }
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
            String tagRef = itemRef.substring(1);

            // ItemsAdderAdditions custom ITEM tag wins over a same-named vanilla tag.
            String customTagId = NamespaceUtils.normalizeCustomTagId(namespace, tagRef);
            if (NamespaceUtils.customTagRegistry().hasTag(customTagId, CustomTagType.ITEM)) {
                return buildCustomTagIngredient(
                        namespace, customTagId, keyLabel, recipeId,
                        requiredAmount, damageAmount, replacement, ignoreDurability, potionType);
            }

            RecipeChoice tagChoice = resolveTag(tagRef, recipeId, keyLabel);
            if (tagChoice == null) return null;
            return new ParsedIngredient(
                    tagChoice, tagChoice,
                    requiredAmount, damageAmount, replacement,
                    ignoreDurability, potionType,
                    null, 0);   // vanilla tags are never custom items
        }

        if (itemRef.toLowerCase(Locale.ROOT).startsWith("mmoitems:")) {
            return buildMMOItemIngredient(itemRef, keyLabel, recipeId,
                    requiredAmount, damageAmount, replacement, ignoreDurability, potionType);
        }

        CustomStack customStack = NamespaceUtils.customItemByID(namespace, itemRef);
        if (customStack != null) {
            RecipeChoice exactChoice =
                    new RecipeChoice.ExactChoice(customStack.getItemStack());

            RecipeChoice registrationChoice = RecipeChoice.itemType(
                    customStack.getItemStack().getType().asItemType());

            // Store the namespaced ID - hash is derived in the compact constructor
            String namespacedId = customStack.getNamespacedID();

            return new ParsedIngredient(
                    registrationChoice,
                    exactChoice,            // kept for ignoreDurability's ExactChoice test
                    requiredAmount, damageAmount, replacement,
                    ignoreDurability, potionType,
                    namespacedId, 0);       // hash computed automatically
        }

        Material mat = NamespaceUtils.vanillaMaterial(itemRef);
        if (mat != null && mat.isItem()) {
            RecipeChoice matChoice = RecipeChoice.itemType(mat.asItemType());
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

    /**
     * Builds a {@link ParsedIngredient} for an {@code mmoitems:TYPE:ID} reference.
     *
     * <p>The registration and validation choice is a {@link RecipeChoice.MaterialChoice}
     * keyed on the MMOItem's base material.  At crafting time
     * {@code CraftingIngredientMatcher} identifies the slot by reading the
     * {@code MMOITEMS_ITEM_TYPE} / {@code MMOITEMS_ITEM_ID} NBT tags via
     * {@link MMOItemsHook#isMmoItem}.
     */
    @Nullable
    private static ParsedIngredient buildMMOItemIngredient(
            String itemRef,
            String keyLabel,
            String recipeId,
            int requiredAmount,
            int damageAmount,
            @Nullable ItemStack replacement,
            boolean ignoreDurability,
            @Nullable String potionType
    ) {
        if (!MMOItemsHook.INSTANCE.isAvailable()) {
            Log.warn(LOG_TAG,
                    "Ingredient '{}' in recipe '{}': MMOItems is not loaded.",
                    keyLabel, recipeId);
            return null;
        }

        String rest = itemRef.toLowerCase(Locale.ROOT).substring("mmoitems:".length());
        int colon = rest.indexOf(':');
        if (colon <= 0) {
            Log.warn(LOG_TAG,
                    "Ingredient '{}' in recipe '{}': invalid MMOItems reference '{}' - expected mmoitems:TYPE:ID.",
                    keyLabel, recipeId, itemRef);
            return null;
        }

        String type = rest.substring(0, colon);
        String id = rest.substring(colon + 1);

        ItemStack mmoStack = MMOItemsHook.INSTANCE.buildItemStack(type, id);
        if (mmoStack == null) {
            Log.warn(LOG_TAG,
                    "Ingredient '{}' in recipe '{}': MMOItems item '{}:{}' not found.",
                    keyLabel, recipeId, type, id);
            return null;
        }

        RecipeChoice materialChoice = RecipeChoice.itemType(mmoStack.getType().asItemType());
        String customId = "mmoitems:" + type + ":" + id;

        return new ParsedIngredient(
                materialChoice, materialChoice,
                requiredAmount, damageAmount, replacement,
                ignoreDurability, potionType,
                customId, 0);
    }

    /**
     * Builds a {@link ParsedIngredient} for an ItemsAdderAdditions custom ITEM
     * tag reference such as {@code #mypack:gems}.
     *
     * <p>The registration {@link RecipeChoice} is a {@link RecipeChoice.MaterialChoice}
     * over the base materials of every tag member, so Bukkit surfaces the recipe
     * for any plausible slot. The strict membership check (which rejects vanilla
     * items or custom items that merely share a base material) happens at craft
     * time in {@code CraftingIngredientMatcher} via
     * {@link NamespaceUtils#matchesItemIDOrTag}, gated by
     * {@link ParsedIngredient#customTagId()}.
     */
    @Nullable
    private static ParsedIngredient buildCustomTagIngredient(
            String namespace,
            String tagId,
            String keyLabel,
            String recipeId,
            int requiredAmount,
            int damageAmount,
            @Nullable ItemStack replacement,
            boolean ignoreDurability,
            @Nullable String potionType
    ) {
        List<String> members = NamespaceUtils.customTagRegistry().values(tagId, CustomTagType.ITEM);
        if (members.isEmpty()) {
            Log.warn(LOG_TAG,
                    "Ingredient '{}' in recipe '{}': custom tag '#{}' is empty.",
                    keyLabel, recipeId, tagId);
            return null;
        }

        EnumSet<Material> materials = EnumSet.noneOf(Material.class);
        for (String member : members) {
            ItemStack stack = NamespaceUtils.itemByID(namespace, member);
            if (stack == null || !stack.getType().isItem()) {
                Log.warn(LOG_TAG,
                        "Ingredient '{}' in recipe '{}': custom tag '#{}' member '{}' could not be resolved to an item.",
                        keyLabel, recipeId, tagId, member);
                continue;
            }
            materials.add(stack.getType());
        }

        if (materials.isEmpty()) {
            Log.warn(LOG_TAG,
                    "Ingredient '{}' in recipe '{}': custom tag '#{}' has no resolvable item materials.",
                    keyLabel, recipeId, tagId);
            return null;
        }

        RecipeChoice choice = new RecipeChoice.MaterialChoice(new ArrayList<>(materials));
        return new ParsedIngredient(
                choice, choice,
                requiredAmount, damageAmount, replacement,
                ignoreDurability, potionType,
                null, 0, tagId);
    }

    @Nullable
    private static RecipeChoice resolveTag(
            String tagRef, String recipeId, String key
    ) {
        String normalizedTag = NamespaceUtils.normalizeMinecraftID(tagRef);
        NamespacedKey tagKey = NamespacedKey.fromString(normalizedTag);
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

        List<ItemType> itemTypes = tag.getValues().stream()
                .filter(Material::isItem)
                .map(Material::asItemType)
                .toList();
        if (itemTypes.isEmpty()) {
            Log.warn(LOG_TAG,
                    "Ingredient '{}' in recipe '{}': tag '#{}' does not contain any item materials.",
                    key, recipeId, tagRef);
            return null;
        }
        return RecipeChoice.itemType(RegistrySet.keySetFromValues(RegistryKey.ITEM, itemTypes));
    }

    @Nullable
    private static ItemStack resolveItem(
            String namespace, String ref, String recipeId, String context
    ) {
        ItemStack item = NamespaceUtils.itemByID(namespace, ref);
        if (item != null) return item;

        Log.warn(LOG_TAG,
                "Could not resolve {} in recipe '{}': '{}'.",
                context, recipeId, ref);
        return null;
    }
}
