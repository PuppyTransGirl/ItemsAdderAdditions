package toutouchien.itemsadderadditions.utils.other;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

/**
 * Describes what kind of ItemsAdder item a {@link CustomStack} represents.
 *
 * <p>Used by both
 * {@link toutouchien.itemsadderadditions.actions.loading.ActionLoader} and
 * {@link toutouchien.itemsadderadditions.behaviours.loading.BehaviourLoader}
 * to determine which events section and which trigger types to resolve for
 * a given item.
 */
public enum ItemCategory {
    /**
     * Entity-based furniture (has {@code behaviours.complex_furniture}).
     */
    COMPLEX_FURNITURE,

    /**
     * Placed decoration without an entity (has {@code behaviours.furniture}
     * or a {@code placed_furniture} events section).
     */
    FURNITURE,

    /**
     * A placeable custom block.
     */
    BLOCK,

    /**
     * A regular held / inventory item.
     */
    ITEM;

    /**
     * Infers the category of {@code customStack} from its YAML config.
     *
     * <p>Resolution order (first match wins):
     * <ol>
     *   <li>{@link #COMPLEX_FURNITURE} - {@link CustomStack#isComplexFurniture(ItemStack)} returns {@code true}</li>
     *   <li>{@link #FURNITURE}         - {@link CustomStack#isFurniture(ItemStack)} returns {@code true}</li>
     *   <li>{@link #BLOCK}             - {@link CustomStack#isBlock()} returns {@code true}</li>
     *   <li>{@link #ITEM}              - everything else</li>
     * </ol>
     */
    public static ItemCategory determine(CustomStack customStack, FileConfiguration config, String itemID) {
        ItemStack itemStack = customStack.getItemStack();
        if (CustomStack.isComplexFurniture(itemStack))
            return COMPLEX_FURNITURE;

        if (CustomStack.isFurniture(itemStack))
            return FURNITURE;

        if (customStack.isBlock())
            return BLOCK;

        return ITEM;
    }
}
