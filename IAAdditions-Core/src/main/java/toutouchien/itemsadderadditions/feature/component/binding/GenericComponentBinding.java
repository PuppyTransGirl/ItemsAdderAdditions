package toutouchien.itemsadderadditions.feature.component.binding;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.model.ComponentKey;
import toutouchien.itemsadderadditions.nms.api.component.ComponentValue;
import toutouchien.itemsadderadditions.nms.api.component.GenericComponentResult;
import toutouchien.itemsadderadditions.nms.api.component.INmsItemComponentHandler;

/**
 * A parsed generic component binding: a normalized key and the corresponding value tree.
 * Applied at item-modifier time through the NMS item component handler.
 */
@NullMarked
public record GenericComponentBinding(ComponentKey key, ComponentValue value) {
    private static final String SUBSYSTEM = "Components";

    public ItemStack apply(ItemStack itemStack, String itemId, INmsItemComponentHandler handler) {
        if (!handler.isSupported()) {
            Log.itemWarn(SUBSYSTEM, itemId,
                    "Generic component '{}' skipped: NMS generic components not supported on this server version.",
                    key.normalized());
            return itemStack;
        }

        GenericComponentResult result = handler.apply(itemStack, key.normalized(), value, itemId);
        return switch (result) {
            case GenericComponentResult.Success s -> s.itemStack();
            case GenericComponentResult.Failure f -> {
                Log.itemWarn(SUBSYSTEM, itemId,
                        "Generic component '{}' failed: {}",
                        key.normalized(), f.reason());
                yield itemStack;
            }
        };
    }
}
