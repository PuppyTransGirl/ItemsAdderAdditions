package toutouchien.itemsadderadditions.nms.api.component;

import org.bukkit.inventory.ItemStack;

/**
 * Result of applying a generic (codec-backed) component via NMS.
 */
public sealed interface GenericComponentResult {
    record Success(ItemStack itemStack) implements GenericComponentResult {}

    record Failure(String reason) implements GenericComponentResult {}

    static GenericComponentResult success(ItemStack itemStack) {
        return new Success(itemStack);
    }

    static GenericComponentResult failure(String reason) {
        return new Failure(reason);
    }
}
