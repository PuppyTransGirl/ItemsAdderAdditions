package toutouchien.itemsadderadditions.utils;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class NamespaceUtils {
    private NamespaceUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Normalizes an ID by adding the current namespace if missing and converting to lowercase.
     * This ensures compatibility with Key.key() which requires lowercase characters.
     */
    public static String normalizeID(String currentNamespace, String id) {
        // Convert to lowercase to avoid Key parsing errors
        String lowerId = id.toLowerCase();
        String lowerNamespace = currentNamespace.toLowerCase();

        if (lowerId.contains(":"))
            return lowerId; // Already contains namespace

        return lowerNamespace + ":" + lowerId;
    }

    public static String namespace(String namespacedID) {
        return namespacedID.substring(0, namespacedID.indexOf(':'));
    }

    public static String id(String namespacedID) {
        return namespacedID.substring(namespacedID.indexOf(':') + 1);
    }

    @Nullable
    public static CustomStack customItemByID(String currentNamespace, String id) {
        String normalizedId = normalizeID(currentNamespace, id);

        CustomStack customStack = CustomStack.getInstance(normalizedId);
        if (customStack != null)
            return customStack;

        String path = Key.key(normalizedId).value();
        for (CustomStack stack : ItemsAdder.getAllItems()) {
            if (stack.getNamespacedID().equals(normalizedId) || stack.getId().equals(path))
                return stack;
        }

        return null;
    }

    @Nullable
    public static ItemStack itemByID(String currentNamespace, String id) {
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
