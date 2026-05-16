package toutouchien.itemsadderadditions.common.item;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.file.FileConfiguration;

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
        // Walk the variant_of chain up to 16 levels deep (guards against cycles).
        // Same-file parents (most common, includes template: true items) are resolved directly
        // from the FileConfiguration. Cross-file parents fall back to CustomStack.getInstance.
        String currentItemId = itemID;
        FileConfiguration currentConfig = config;
        String namespace = customStack.getNamespacedID().contains(":")
                ? customStack.getNamespacedID().substring(0, customStack.getNamespacedID().indexOf(':'))
                : "";

        for (int depth = 0; depth < 16; depth++) {
            String base = "items." + currentItemId;

            if (currentConfig.contains(base + ".behaviours.complex_furniture"))
                return COMPLEX_FURNITURE;

            if (currentConfig.contains(base + ".events.placed_furniture") || currentConfig.contains(base + ".behaviours.furniture"))
                return FURNITURE;

            String variantOf = currentConfig.getString(base + ".variant_of");
            if (variantOf == null || variantOf.isBlank()) break;

            String parentId = variantOf.contains(":") ? variantOf.substring(variantOf.indexOf(':') + 1) : variantOf;

            if (currentConfig.contains("items." + parentId)) {
                currentItemId = parentId;
            } else {
                String parentNsId = variantOf.contains(":") ? variantOf : namespace + ":" + variantOf;
                CustomStack parent = CustomStack.getInstance(parentNsId);
                if (parent == null) break;
                currentConfig = parent.getConfig();
                currentItemId = parent.getId();
            }
        }

        if (customStack.isBlock())
            return BLOCK;

        return ITEM;
    }
}
