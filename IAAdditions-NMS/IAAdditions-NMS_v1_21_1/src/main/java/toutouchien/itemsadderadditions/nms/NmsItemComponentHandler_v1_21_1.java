package toutouchien.itemsadderadditions.nms;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.component.ComponentValue;
import toutouchien.itemsadderadditions.nms.api.component.GenericComponentResult;
import toutouchien.itemsadderadditions.nms.api.component.INmsItemComponentHandler;

@NullMarked
final class NmsItemComponentHandler_v1_21_1 implements INmsItemComponentHandler {
    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public GenericComponentResult apply(ItemStack itemStack, String normalizedKey, ComponentValue value, String itemId) {
        return GenericComponentResult.failure(
                "Generic NMS components require Minecraft 1.21.3 or higher. Current: 1.21.1"
        );
    }
}
