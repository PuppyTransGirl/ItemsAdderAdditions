package toutouchien.itemsadderadditions.integration.valhalla;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Writes {@link ValhallaItemData} to an {@link ItemStack} via Bukkit PDC.
 *
 * <p>All PDC keys match what ValhallaMMO and ValhallaTrinkets read from items at runtime.
 * This class writes directly to the PDC using hardcoded namespaced keys; neither
 * ValhallaMMO nor ValhallaTrinkets needs to be loaded for writing to succeed.
 */
@NullMarked
public final class ValhallaItemApplier {
    private static final NamespacedKey KEY_ACTUAL_STATS =
            NamespacedKey.fromString("valhallammo:actual_stats");
    private static final NamespacedKey KEY_DEFAULT_STATS =
            NamespacedKey.fromString("valhallammo:default_stats");
    private static final NamespacedKey KEY_EQUIPMENT_CLASS =
            NamespacedKey.fromString("valhallammo:equipment_class");
    private static final NamespacedKey KEY_ITEM_FLAGS =
            NamespacedKey.fromString("valhallammo:item_flags");
    private static final NamespacedKey KEY_TRINKET_ID =
            NamespacedKey.fromString("valhallatrinkets:trinket_id");
    private static final NamespacedKey KEY_TRINKET_UNIQUE_ID =
            NamespacedKey.fromString("valhallatrinkets:trinket_unique_id");
    private static final NamespacedKey KEY_TRINKET_UNIQUE =
            NamespacedKey.fromString("valhallatrinkets:unique");

    private ValhallaItemApplier() {
    }

    /**
     * Applies all configured Valhalla PDC data to the item.
     *
     * @param itemStack the item to modify
     * @param data      the Valhalla data to write
     * @return the modified item stack
     */
    public static ItemStack apply(ItemStack itemStack, ValhallaItemData data) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!data.actualStats().isEmpty()) {
            pdc.set(KEY_ACTUAL_STATS, PersistentDataType.STRING, serializeStats(data.actualStats()));
        }

        if (!data.defaultStats().isEmpty()) {
            pdc.set(KEY_DEFAULT_STATS, PersistentDataType.STRING, serializeStats(data.defaultStats()));
        }

        if (data.equipmentClass() != null) {
            pdc.set(KEY_EQUIPMENT_CLASS, PersistentDataType.STRING, data.equipmentClass());
        }

        if (!data.itemFlags().isEmpty()) {
            pdc.set(KEY_ITEM_FLAGS, PersistentDataType.STRING, String.join(";", data.itemFlags()));
        }

        if (data.trinkets() != null) {
            applyTrinkets(pdc, data.trinkets());
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    static String serializeStats(List<ValhallaStatEntry> stats) {
        StringBuilder sb = new StringBuilder();
        for (ValhallaStatEntry entry : stats) {
            if (!sb.isEmpty()) sb.append(';');
            sb.append(entry.serialize());
        }

        return sb.toString();
    }

    private static void applyTrinkets(PersistentDataContainer pdc, ValhallaTrinketData trinkets) {
        if (trinkets.trinketId() != null) {
            pdc.set(KEY_TRINKET_ID, PersistentDataType.INTEGER, trinkets.trinketId());
        }

        if (trinkets.trinketUniqueId() != null) {
            pdc.set(KEY_TRINKET_UNIQUE_ID, PersistentDataType.INTEGER, trinkets.trinketUniqueId());
        }

        if (trinkets.unique() != null) {
            if (trinkets.unique()) {
                pdc.set(KEY_TRINKET_UNIQUE, PersistentDataType.BYTE, (byte) 1);
            } else {
                pdc.remove(KEY_TRINKET_UNIQUE);
            }
        }
    }
}
