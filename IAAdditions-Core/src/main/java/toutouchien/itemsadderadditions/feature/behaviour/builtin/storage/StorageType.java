package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

/**
 * Defines the behaviour mode of a {@link toutouchien.itemsadderadditions.feature.behaviour.builtin.StorageBehaviour} instance.
 *
 * <ul>
 *   <li>{@link #STORAGE}  - Shared container; inventory is saved to the block/furniture location.</li>
 *   <li>{@link #SHULKER}  - Items persist inside the block item; contents are restored when re-placed.</li>
 *   <li>{@link #DISPOSAL} - Trash can; all items placed inside are deleted when the GUI is closed.</li>
 * </ul>
 */
public enum StorageType {
    /**
     * Shared container. The inventory is saved to the block or furniture entity via PDC and
     * every player who opens the block sees the same contents.
     */
    STORAGE,

    /**
     * Shulker-box style storage. The inventory is serialised into the dropped {@link org.bukkit.inventory.ItemStack}'s
     * {@link org.bukkit.persistence.PersistentDataContainer} when the block/furniture is broken, and
     * restored into the world PDC when the item is placed again.
     */
    SHULKER,

    /**
     * Disposal / trash can. Any items placed inside this container are silently discarded when the
     * player closes the GUI. Nothing is ever written to persistent storage.
     */
    DISPOSAL
}
