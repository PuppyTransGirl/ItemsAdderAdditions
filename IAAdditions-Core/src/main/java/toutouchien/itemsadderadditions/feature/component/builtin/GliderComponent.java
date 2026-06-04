package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.version.VersionUtils;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

/**
 * <p><strong>Minimum Minecraft Version:</strong> 1.21.2
 *
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Values</th></tr>
 * <tr><td>glider</td><td>Boolean</td><td>true, false</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "glider")
public final class GliderComponent extends ComponentExecutor {
    @Override
    public @Nullable VersionUtils minimumVersion() {
        return VersionUtils.v1_21_3;
    }

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (configData instanceof Boolean b) return b;
        return configData != null;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.GLIDER);
        return itemStack;
    }
}
