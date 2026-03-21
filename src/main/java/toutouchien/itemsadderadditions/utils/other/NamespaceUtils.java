package toutouchien.itemsadderadditions.utils.other;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NamespaceUtils {
    private NamespaceUtils() {
        throw new IllegalStateException("Utility class");
    }

    @Nullable
    public static ItemStack itemByID(String currentNamespace, String id) {
        CustomStack idCustomStack = CustomStack.getInstance(id);
        if (idCustomStack != null)
            return idCustomStack.getItemStack();

        CustomStack namespaceIDCustomStack = CustomStack.getInstance(currentNamespace + ":" + id);
        if (namespaceIDCustomStack != null)
            return namespaceIDCustomStack.getItemStack();

        String noNamespaceID = id.substring(id.indexOf(":") + 1);
        for (CustomStack customStack : ItemsAdder.getAllItems()) {
            if (customStack.getId().equals(noNamespaceID))
                return customStack.getItemStack();
        }

        ItemType minecraftItemType = Registry.ITEM.get(Key.key("minecraft:" + noNamespaceID));
        return minecraftItemType == null ? null : minecraftItemType.createItemStack();
    }

    @Nullable
    public static CustomStack customItemByID(String currentNamespace, String id) {
        CustomStack idCustomStack = CustomStack.getInstance(id);
        if (idCustomStack != null)
            return idCustomStack;

        CustomStack namespaceIDCustomStack = CustomStack.getInstance(currentNamespace + ":" + id);
        if (namespaceIDCustomStack != null)
            return namespaceIDCustomStack;

        String noNamespaceID = id.substring(id.indexOf(":") + 1);
        for (CustomStack customStack : ItemsAdder.getAllItems()) {
            if (customStack.getId().equals(noNamespaceID))
                return customStack;
        }

        return null;
    }
}
