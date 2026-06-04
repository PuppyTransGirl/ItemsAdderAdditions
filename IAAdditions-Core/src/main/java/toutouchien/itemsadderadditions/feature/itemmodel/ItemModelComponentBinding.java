package toutouchien.itemsadderadditions.feature.itemmodel;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.nms.api.component.ComponentValue;
import toutouchien.itemsadderadditions.nms.api.component.GenericComponentResult;
import toutouchien.itemsadderadditions.nms.api.component.INmsItemComponentHandler;

@NullMarked
record ItemModelComponentBinding(
        String itemModelId,
        @Nullable ComponentValue customModelData
) {
    ItemStack apply(ItemStack itemStack, String itemId, INmsItemComponentHandler handler) {
        if (!handler.isSupported()) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                    "item_model_definition components skipped: generic NMS components are not supported on this server version.");
            return itemStack;
        }

        ItemStack current = applyComponent(
                handler,
                itemStack,
                itemId,
                "minecraft:item_model",
                new ComponentValue.StringNode(itemModelId)
        );

        if (customModelData != null) {
            current = applyComponent(handler, current, itemId, "minecraft:custom_model_data", customModelData);
        }

        return current;
    }

    private static ItemStack applyComponent(
            INmsItemComponentHandler handler,
            ItemStack itemStack,
            String itemId,
            String key,
            ComponentValue value
    ) {
        GenericComponentResult result = handler.apply(itemStack, key, value, itemId);
        return switch (result) {
            case GenericComponentResult.Success success -> success.itemStack();
            case GenericComponentResult.Failure failure -> {
                Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                        "Generated component '{}' failed: {}", key, failure.reason());
                yield itemStack;
            }
        };
    }
}
