package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.contentvariant;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Coarse visual fill state for a storage inventory.
 *
 * <p>{@link #HALF} deliberately means "partially filled": it covers every state
 * between completely empty and every slot reaching its stack capacity. This gives resource-pack
 * authors three stable decorating states without making visuals flicker as stack
 * amounts change.</p>
 */
@NullMarked
public enum StorageFillState {
    EMPTY,
    HALF,
    FULL;

    public static StorageFillState fromContents(@Nullable ItemStack @Nullable [] contents) {
        if (contents == null || contents.length == 0) return EMPTY;

        boolean hasItems = false;
        boolean atCapacity = true;
        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                hasItems = true;
                if (item.getAmount() < item.getMaxStackSize()) atCapacity = false;
            } else {
                atCapacity = false;
            }
        }

        if (!hasItems) return EMPTY;
        return atCapacity ? FULL : HALF;
    }
}
