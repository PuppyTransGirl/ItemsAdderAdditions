package toutouchien.itemsadderadditions.recipes;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

/**
 * Resolves an ItemsAdder "namespace:id" or plain vanilla "minecraft:stone"
 * string into a Bukkit {@link RecipeChoice}.
 *
 * <p>For custom IA items we use {@link RecipeChoice.ExactChoice} on the
 * underlying ItemStack so NMS can match it exactly.
 * For vanilla items we use {@link RecipeChoice.ExactChoice} as well,
 * built from the resolved {@link ItemStack}.
 */
public final class RecipeItemResolver {
    private RecipeItemResolver() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * @param currentNamespace The namespace of the current YAML file,
     *                         used to normalize un-namespaced IDs
     * @param value            Raw string from YAML, e.g. {@code "myitems:ruby"},
     *                         {@code "ruby"} or {@code "minecraft:stone"}
     * @param caller           Human-readable caller label used in log messages
     * @return A {@link RecipeChoice}, or {@code null} if resolution failed
     */
    public static RecipeChoice resolve(String currentNamespace, String value, String caller) {
        if (value == null || value.isBlank()) return null;

        ItemStack item = NamespaceUtils.itemByID(currentNamespace, value);
        if (item == null) {
            Log.warn(caller, "Could not resolve item: " + value
                    + " (namespace: " + currentNamespace + ")");
            return null;
        }

        return new RecipeChoice.ExactChoice(item);
    }
}
