package toutouchien.itemsadderadditions.recipes;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

/**
 * Resolves an ItemsAdder "namespace:id" or plain vanilla "stone" / "minecraft:stone"
 * string into a Bukkit {@link RecipeChoice}.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>Try {@link NamespaceUtils#itemByID} with the file's own namespace
 *       (handles both custom IA items and already-namespaced vanilla ids)</li>
 *   <li>If the value has no namespace, retry with {@code minecraft:} so that
 *       bare names like {@code andesite} resolve to vanilla items.</li>
 * </ol>
 */
public final class RecipeItemResolver {
    private static final String MINECRAFT_NAMESPACE = "minecraft";

    private RecipeItemResolver() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * @param currentNamespace The namespace of the current YAML file,
     *                         used to normalize un-namespaced IDs
     * @param value            Raw string from YAML, e.g. {@code "myitems:ruby"},
     *                         {@code "andesite"} or {@code "minecraft:stone"}
     * @param caller           Hresolvuman-readable caller label used in log messages
     * @return A {@link RecipeChoice}, or {@code null} if resolution failed
     */
    public static RecipeChoice resolve(String currentNamespace, String value, String caller) {
        if (value == null || value.isBlank()) return null;

        // First attempt: resolve using the file's own namespace
        ItemStack item = NamespaceUtils.itemByID(currentNamespace, value);

        // Second attempt: if no namespace was given in the value,
        // retry with "minecraft:" so bare names like "andesite" work
        if (item == null && !value.contains(":")) {
            item = NamespaceUtils.itemByID(MINECRAFT_NAMESPACE, value);
        }

        if (item == null) {
            Log.warn(caller, "Could not resolve item: '" + value
                    + "' (namespace: " + currentNamespace + ")");
            return null;
        }

        return new RecipeChoice.ExactChoice(item);
    }
}
