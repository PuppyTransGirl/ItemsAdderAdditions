package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseRemainder;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Format</th></tr>
 * <tr><td>(value)</td><td>String</td><td>Vanilla, ItemsAdder, or MMOItems item ID</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "use_remainder")
public final class UseRemainderComponent extends ComponentExecutor {
    private @Nullable ItemStack remainder;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof String raw)) {
            Log.itemWarn("Components", namespacedID,
                    "'use_remainder' must be a string item ID (vanilla, ItemsAdder, or mmoitems:TYPE:ID)");
            return false;
        }

        String namespace = NamespaceUtils.namespace(namespacedID);
        ItemStack resolved = NamespaceUtils.itemByID(namespace, raw.trim());
        if (resolved == null) {
            Log.itemWarn("Components", namespacedID, "'use_remainder' item '{}' not found.", raw);
            return false;
        }

        this.remainder = resolved.clone();
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        if (remainder != null) {
            itemStack.setData(DataComponentTypes.USE_REMAINDER, UseRemainder.useRemainder(remainder));
        }
        return itemStack;
    }
}
