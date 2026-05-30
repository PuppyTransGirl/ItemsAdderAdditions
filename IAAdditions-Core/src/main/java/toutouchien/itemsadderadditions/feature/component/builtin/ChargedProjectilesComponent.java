package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ChargedProjectiles;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Format</th></tr>
 * <tr><td>(value)</td><td>List&lt;String&gt;</td><td>Item IDs (vanilla, ItemsAdder, or mmoitems:TYPE:ID)</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "charged_projectiles")
public final class ChargedProjectilesComponent extends ComponentExecutor {
    private List<ItemStack> projectiles = List.of();

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof List<?> rawList)) {
            Log.itemWarn("Components", namespacedID, "'charged_projectiles' must be a list of item IDs");
            return false;
        }
        String namespace = NamespaceUtils.namespace(namespacedID);
        List<ItemStack> resolved = new ArrayList<>();
        for (Object entry : rawList) {
            if (!(entry instanceof String id)) continue;
            ItemStack item = NamespaceUtils.itemByID(namespace, id.trim());
            if (item == null) {
                Log.itemWarn("Components", namespacedID, "'charged_projectiles' item '{}' not found.", id);
                return false;
            }
            resolved.add(item.clone());
        }
        if (resolved.isEmpty()) {
            Log.itemWarn("Components", namespacedID, "'charged_projectiles' list must not be empty");
            return false;
        }
        this.projectiles = List.copyOf(resolved);
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectiles.chargedProjectiles(projectiles));
        return itemStack;
    }
}
