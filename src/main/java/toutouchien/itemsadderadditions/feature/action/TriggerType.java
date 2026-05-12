package toutouchien.itemsadderadditions.feature.action;

/**
 * Enumerates every event type that can fire an {@link ActionExecutor}.
 *
 * <p>Trigger types are grouped by the kind of item that triggers them:
 * blocks, items, furniture, and complex furniture (multi-block barrier models).
 *
 * <p>Some trigger types are <em>argumentized</em> - they carry an additional
 * string qualifier (e.g. {@code "right"}, {@code "left_shift"}, {@code "entity"})
 * that allows the YAML author to distinguish between specific interaction variants.
 * See {@link TriggerKey} and {@link toutouchien.itemsadderadditions.feature.action.loading.ActionLoader}
 * for details on how argumentized triggers are parsed.
 */
public enum TriggerType {
    // Block
    /**
     * Right-click on a block.
     */
    BLOCK_INTERACT,
    /**
     * A placed custom block is broken.
     */
    PLACED_BLOCK_BREAK,

    // Item - Interact
    /**
     * Right-click while holding a custom item (both hands).
     */
    ITEM_INTERACT,
    /**
     * Right-click while holding a custom item in main hand only.
     */
    ITEM_INTERACT_MAINHAND,
    /**
     * Right-click while holding a custom item in offhand only.
     */
    ITEM_INTERACT_OFFHAND,

    // Item - Block & Combat
    /**
     * Break any block while holding a custom item.
     */
    ITEM_BREAK_BLOCK,
    /**
     * Attack an entity while holding a custom item.
     */
    ITEM_ATTACK,
    /**
     * Kill an entity while holding a custom item.
     */
    ITEM_KILL,

    // Item - Inventory
    /**
     * Drop this custom item.
     */
    ITEM_DROP,
    /**
     * Pick up this custom item.
     */
    ITEM_PICKUP,
    /**
     * Move this item into the main-hand slot.
     */
    ITEM_HELD,
    /**
     * Move this item into the off-hand slot.
     */
    ITEM_HELD_OFFHAND,
    /**
     * Move this item out of the main-hand slot.
     */
    ITEM_UNHELD,
    /**
     * Move this item out of the off-hand slot.
     */
    ITEM_UNHELD_OFFHAND,
    /**
     * Item reaches 0 durability and disappears.
     */
    ITEM_BREAK,

    // Item - Consumable
    /**
     * Eat this custom item.
     */
    ITEM_EAT,
    /**
     * Drink this custom item (potion, honey bottle, milk bucket…).
     */
    ITEM_DRINK,

    // Item - Ranged
    /**
     * Shoot a bow (item must be a BOW).
     */
    ITEM_BOW_SHOT,
    /**
     * Fire a gun (item must have the gun behaviour).
     */
    ITEM_GUN_SHOT,
    /**
     * Attempt to fire a gun with no ammo.
     */
    ITEM_GUN_NO_AMMO,
    /**
     * Reload a gun.
     */
    ITEM_GUN_RELOAD,
    /**
     * Throw this item (ARROW, SNOWBALL, ENDER_PEARL…).
     */
    ITEM_THROW,
    /**
     * Thrown item lands on the ground.
     */
    ITEM_HIT_GROUND,
    /**
     * Thrown item hits an entity.
     */
    ITEM_HIT_ENTITY,

    // Item - Books
    /**
     * Write in this book (item must be a WRITABLE_BOOK).
     */
    ITEM_BOOK_WRITE,
    /**
     * Read this book (item must be a WRITTEN_BOOK).
     */
    ITEM_BOOK_READ,

    // Item - Fishing
    /**
     * Cast the fishing rod.
     */
    ITEM_FISHING_START,
    /**
     * A fish or item is caught.
     */
    ITEM_FISHING_CAUGHT,
    /**
     * Reel in with nothing on the hook.
     */
    ITEM_FISHING_FAILED,
    /**
     * Fishing cancelled (rod reeled in before any result).
     */
    ITEM_FISHING_CANCEL,
    /**
     * A fish bites the hook.
     */
    ITEM_FISHING_BITE,
    /**
     * The hook lands on the ground instead of water.
     */
    ITEM_FISHING_IN_GROUND,

    // Item - Buckets
    /**
     * Empty a water / milk bucket.
     */
    ITEM_BUCKET_EMPTY,
    /**
     * Fill a bucket.
     */
    ITEM_BUCKET_FILL,

    // Furniture
    FURNITURE_INTERACT,
    FURNITURE_INTERACT_MAINHAND,
    FURNITURE_INTERACT_OFFHAND,
    FURNITURE_ATTACK,
    FURNITURE_KILL,
    FURNITURE_DROP,
    FURNITURE_PICKUP,
    FURNITURE_EAT,
    FURNITURE_DRINK,
    FURNITURE_BOW_SHOT,
    FURNITURE_GUN_SHOT,
    FURNITURE_GUN_NO_AMMO,
    FURNITURE_GUN_RELOAD,
    FURNITURE_BOOK_WRITE,
    FURNITURE_BOOK_READ,
    FURNITURE_FISHING_START,
    FURNITURE_FISHING_CAUGHT,
    FURNITURE_FISHING_FAILED,
    FURNITURE_FISHING_CANCEL,
    FURNITURE_FISHING_BITE,
    FURNITURE_FISHING_IN_GROUND,
    FURNITURE_WEAR,
    FURNITURE_UNWEAR,
    FURNITURE_HELD,
    FURNITURE_HELD_OFFHAND,
    FURNITURE_UNHELD,
    FURNITURE_UNHELD_OFFHAND,
    FURNITURE_THROW,
    FURNITURE_HIT_GROUND,
    FURNITURE_HIT_ENTITY,
    FURNITURE_BREAK,
    FURNITURE_BUCKET_EMPTY,
    FURNITURE_BUCKET_FILL,

    // Complex Furniture
    COMPLEX_FURNITURE_INTERACT,
}
