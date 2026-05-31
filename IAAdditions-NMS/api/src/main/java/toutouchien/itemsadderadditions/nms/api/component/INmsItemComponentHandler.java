package toutouchien.itemsadderadditions.nms.api.component;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * NMS-backed handler for generic item components resolved through Minecraft's
 * DATA_COMPONENT_TYPE registry and Mojang codecs.
 *
 * <p>Implementations in version-specific modules use real NMS classes.
 * The core module sees only this interface, ComponentValue, and GenericComponentResult.
 */
@NullMarked
public interface INmsItemComponentHandler {
    /**
     * Whether this version supports the generic NMS component pipeline.
     * Returns false for versions where the codec/registry API is not safe to use.
     */
    boolean isSupported();

    /**
     * Looks up the DataComponentType for {@code normalizedKey}, parses {@code value}
     * through the component's Mojang codec, and returns the modified item stack.
     *
     * <p>{@code normalizedKey} must already be fully-qualified (e.g. "minecraft:custom_data").
     *
     * @param itemStack     the Bukkit item stack to modify
     * @param normalizedKey fully-qualified component key (e.g. "minecraft:custom_data")
     * @param value         the parsed YAML value tree
     * @param itemId        the item's namespaced ID, used only in error messages
     * @return Success with modified stack, or Failure with a human-readable reason
     */
    GenericComponentResult apply(ItemStack itemStack, String normalizedKey, ComponentValue value, String itemId);
}
