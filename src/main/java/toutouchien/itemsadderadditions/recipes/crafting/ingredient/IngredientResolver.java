package toutouchien.itemsadderadditions.recipes.crafting.ingredient;

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
import java.util.List;

/**
 * Parses a single ingredient entry from YAML into a {@link ParsedIngredient}.
 *
 * <p>Accepted YAML forms:
 * <pre>
 * # shorthand - no predicates
 * S: STICK
 * S: "#minecraft:planks"
 * S: "example:my_sword"
 *
 * # long-form - with optional predicates
 * S:
 *   item: "#minecraft:planks"
 *   amount: 2            # stack must have ≥2; the extra is consumed
 *   damage: 5            # deal 5 durability damage after craft
 *   replacement: STICK   # place this back after craft (overrides damage)
 *   ignore_durability: true  # accept the item at any durability level
 * </pre>
 *
 * <h3>ignore_durability</h3>
 * <p>Only available in long-form. When {@code true}:
 * <ul>
 *   <li>For <em>custom IA items</em> (resolved via {@link NamespaceUtils}):
 *       the Bukkit recipe is registered with a {@link RecipeChoice.MaterialChoice}
 *       so Bukkit shows the crafting result for any-durability item. The listener
 *       then validates the slot using the original
 *       {@link RecipeChoice.ExactChoice} after stripping the current damage,
 *       which keeps vanilla items of the same material from being accepted.</li>
 *   <li>For <em>vanilla materials</em> and <em>tags</em>
 *       ({@link RecipeChoice.MaterialChoice} is already in use): no change
 *       is needed; the flag is recorded in {@link ParsedIngredient} so the
 *       listener still strips damage before testing (harmless but consistent).</li>
 * </ul>
 */
@NullMarked
public final class IngredientResolver {

    private static final String LOG_TAG = "IngredientResolver";

    private IngredientResolver() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * @param namespace The current IA namespace (used to resolve custom items).
     * @param section   The {@code ingredients} config section.
     * @param key       The single-character key (e.g. {@code "P"}).
     * @param recipeId  Used in log messages only.
     * @return A resolved {@link ParsedIngredient}, or {@code null} on failure.
     */
    @Nullable
    public static ParsedIngredient resolve(
            String namespace,
            ConfigurationSection section,
            String key,
            String recipeId
    ) {
        String itemRef;
        int requiredAmount = 1;
        int damageAmount = 0;
        ItemStack replacement = null;
        boolean ignoreDurability = false;

        Object raw = section.get(key);

        if (raw instanceof ConfigurationSection sub) {
            itemRef = sub.getString("item");
            if (itemRef == null) {
                Log.warn(LOG_TAG,
                        "Ingredient '{}' in recipe '{}' is missing 'item'.",
                        key, recipeId);
                return null;
            }

            requiredAmount = sub.getInt("amount", 1);
            damageAmount = sub.getInt("damage", 0);
            ignoreDurability = sub.getBoolean("ignore_durability", false);

            String replacementRef = sub.getString("replacement");
            if (replacementRef != null) {
                replacement = resolveItem(namespace, replacementRef, recipeId,
                        "replacement for '" + key + "'");
                if (replacement == null) return null;
                damageAmount = 0; // replacement takes precedence
            }
        } else {
            itemRef = section.getString(key);
            if (itemRef == null) {
                Log.warn(LOG_TAG,
                        "Ingredient '{}' in recipe '{}' has an unsupported format.",
                        key, recipeId);
                return null;
            }
        }

        // Resolve the base choice (used for validation in the listener).
        RecipeChoice validationChoice = resolveChoice(namespace, itemRef, recipeId, key);
        if (validationChoice == null) return null;

        // The registration choice is what Bukkit actually uses for recipe matching.
        // For custom items with ignore_durability we upgrade to MaterialChoice so
        // Bukkit will show the result regardless of current tool durability.
        RecipeChoice registrationChoice = validationChoice;
        if (ignoreDurability && validationChoice instanceof RecipeChoice.ExactChoice exact) {
            // ExactChoice holds custom IA items whose meta includes damage → use
            // the item's base material so Bukkit accepts any durability level.
            Material mat = exact.getChoices().get(0).getType();
            registrationChoice = new RecipeChoice.MaterialChoice(mat);
        }
        // For MaterialChoice (tags / vanilla) ignore_durability is a no-op at
        // registration time; the listener still strips damage before testing.

        return new ParsedIngredient(
                registrationChoice,
                validationChoice,
                requiredAmount,
                damageAmount,
                replacement,
                ignoreDurability);
    }

    @Nullable
    private static RecipeChoice resolveChoice(
            String namespace, String itemRef, String recipeId, String key
    ) {
        if (itemRef.startsWith("#")) {
            return resolveTag(itemRef.substring(1), recipeId, key);
        }

        // Try as custom IA item first
        ItemStack custom = NamespaceUtils.itemByID(namespace, itemRef);
        if (custom != null) {
            return new RecipeChoice.ExactChoice(custom);
        }

        // Fall back to vanilla material
        Material mat = Material.matchMaterial(itemRef);
        if (mat != null) {
            return new RecipeChoice.MaterialChoice(mat);
        }

        Log.warn(LOG_TAG,
                "Ingredient '{}' in recipe '{}': could not resolve '{}'.",
                key, recipeId, itemRef);
        return null;
    }

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

        // Try item registry first, then block registry
        Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, tagKey, Material.class);
        if (tag == null) {
            tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagKey, Material.class);
        }

        if (tag == null || tag.getValues().isEmpty()) {
            Log.warn(LOG_TAG,
                    "Ingredient '{}' in recipe '{}': tag '#{}' is unknown or empty.",
                    key, recipeId, tagRef);
            return null;
        }

        List<Material> materials = new ArrayList<>(tag.getValues());
        return new RecipeChoice.MaterialChoice(materials);
    }

    @Nullable
    private static ItemStack resolveItem(
            String namespace, String ref, String recipeId, String context
    ) {
        ItemStack custom = NamespaceUtils.itemByID(namespace, ref);
        if (custom != null) return custom;

        Material mat = Material.matchMaterial(ref);
        if (mat != null) return new ItemStack(mat);

        Log.warn(LOG_TAG,
                "Could not resolve {} in recipe '{}': '{}'.",
                context, recipeId, ref);
        return null;
    }
}
