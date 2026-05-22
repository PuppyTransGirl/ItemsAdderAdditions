package toutouchien.itemsadderadditions.integration.hook;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class MMOItemsHook extends PluginHook {
    public static final MMOItemsHook INSTANCE = new MMOItemsHook();

    private MMOItemsHook() {
    }

    @Override
    public String pluginName() {
        return "MMOItems";
    }

    /**
     * Builds an {@link ItemStack} for the given MMOItems type and item ID.
     *
     * <p>Returns {@code null} when MMOItems is not loaded or the type/id
     * combination does not exist.
     *
     * @param type the MMOItems type name (e.g. {@code "sword"}) - case-insensitive
     * @param id   the item ID within that type (e.g. {@code "fire_sword"}) - case-insensitive
     */
    @Nullable
    public ItemStack buildItemStack(String type, String id) {
        if (!isAvailable()) return null;
        try {
            Type mmoType = MMOItems.plugin.getTypes().get(type.toUpperCase());
            if (mmoType == null) return null;
            return MMOItems.plugin.getItems().getItem(mmoType, id.toUpperCase());
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Returns {@code true} when {@code stack} is an MMOItems item of the given
     * type and ID.
     *
     * <p>Identification is performed by reading the {@code MMOITEMS_ITEM_TYPE}
     * and {@code MMOITEMS_ITEM_ID} NBT tags written by MMOItems.  Both
     * comparisons are case-insensitive so config values do not need to match
     * the exact case stored internally.
     *
     * @param stack the item to inspect
     * @param type  the expected type (e.g. {@code "sword"}) - case-insensitive
     * @param id    the expected item ID (e.g. {@code "fire_sword"}) - case-insensitive
     */
    public boolean isMmoItem(ItemStack stack, String type, String id) {
        if (!isAvailable()) return false;
        try {
            NBTItem nbt = NBTItem.get(stack);
            return type.equalsIgnoreCase(nbt.getString("MMOITEMS_ITEM_TYPE"))
                    && id.equalsIgnoreCase(nbt.getString("MMOITEMS_ITEM_ID"));
        } catch (Exception ignored) {
            return false;
        }
    }
}
