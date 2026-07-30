package toutouchien.itemsadderadditions.common.item;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Describes what kind of ItemsAdder item a {@link CustomStack} represents.
 *
 * <p>Used by both
 * {@link toutouchien.itemsadderadditions.feature.action.loading.ActionLoader} and
 * {@link toutouchien.itemsadderadditions.feature.behaviour.loading.BehaviourLoader}
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
     *   <li>{@link #COMPLEX_FURNITURE} - has {@code behaviours.complex_furniture}</li>
     *   <li>{@link #FURNITURE}         - has {@code events.placed_furniture} or {@code behaviours.furniture}</li>
     *   <li>{@link #BLOCK}             - {@link CustomStack#isBlock()} returns {@code true}</li>
     *   <li>{@link #ITEM}              - everything else</li>
     * </ol>
     */
    public static ItemCategory determine(CustomStack customStack, FileConfiguration config, String itemID) {
        List<ItemVariantResolver.ResolvedItemConfig> chain =
                ItemVariantResolver.chainFromSelf(config, itemID, customStack.getNamespacedID());

        for (ItemVariantResolver.ResolvedItemConfig node : chain) {
            if (node.section().contains("behaviours.complex_furniture"))
                return COMPLEX_FURNITURE;

            if (node.section().contains("events.placed_furniture") || node.section().contains("behaviours.furniture"))
                return FURNITURE;
        }

        if (customStack.isBlock())
            return BLOCK;

        return ITEM;
    }
}
