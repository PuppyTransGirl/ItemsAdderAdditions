package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.LodestoneTracker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Default</th></tr>
 * <tr><td>tracked</td><td>Boolean</td><td>true</td></tr>
 * <tr><td>world</td><td>String</td><td>null (world name; omit to create unlinked tracker)</td></tr>
 * <tr><td>x</td><td>Integer</td><td>0</td></tr>
 * <tr><td>y</td><td>Integer</td><td>0</td></tr>
 * <tr><td>z</td><td>Integer</td><td>0</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "lodestone_tracker")
public final class LodestoneTrackerComponent extends ComponentExecutor {
    private boolean tracked = true;
    private @Nullable String worldName;
    private int x, y, z;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section)) {
            Log.itemWarn("Components", namespacedID, "'lodestone_tracker' must be a configuration section");
            return false;
        }
        this.tracked = section.getBoolean("tracked", true);
        this.worldName = section.getString("world");
        this.x = section.getInt("x", 0);
        this.y = section.getInt("y", 0);
        this.z = section.getInt("z", 0);
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        Location location = null;
        if (worldName != null) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                Log.warn("Components", "'lodestone_tracker' world '{}' not found for '{}' — applying without location.", worldName, namespacedID);
            } else {
                location = new Location(world, x, y, z);
            }
        }
        itemStack.setData(DataComponentTypes.LODESTONE_TRACKER, LodestoneTracker.lodestoneTracker(location, tracked));
        return itemStack;
    }
}
