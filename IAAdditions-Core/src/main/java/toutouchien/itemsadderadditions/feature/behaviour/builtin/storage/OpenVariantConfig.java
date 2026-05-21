package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

/**
 * Immutable configuration for the visual "open variant" of a storage container -
 * the custom block, furniture, or plain custom-stack item displayed at the storage
 * location while at least one player has the GUI open.
 *
 * <p>No {@code type} field is required in the YAML. The ID is also normalised by
 * {@link NamespaceUtils#customItemByID}, so the namespace may be omitted when it
 * matches the owning item's namespace.
 *
 * <p>Example YAML:
 * <pre>
 * open_variant: "mynamespace:my_chest_open"
 * # namespace optional when it matches the owning item:
 * open_variant: "my_chest_open"
 * # plain custom stack (not a registered block or furniture):
 * open_variant: "mynamespace:birch_office_table_open"
 * </pre>
 */
@NullMarked
public record OpenVariantConfig(ItemCategory category, String id) {
    /**
     * Resolves an {@link OpenVariantConfig} from a namespacedID string, auto-detecting
     * the {@link ItemCategory}.
     *
     * <p><strong>Must be called after ItemsAdder has finished loading</strong>
     * (i.e. from {@code onLoad} / {@code ItemsAdderLoadDataEvent}, not {@code onEnable}).
     *
     * @param id                the namespacedID of the open-form item
     * @param ownerNamespacedId the namespacedID of the owning storage item
     * @return a fully resolved config, or {@code null} if the ID is blank or unrecognised
     */
    @Nullable
    public static OpenVariantConfig resolve(@Nullable String id, String ownerNamespacedId) {
        if (id == null)
            return null;

        if (id.isBlank()) {
            Log.warn(
                    "Storage",
                    "open_variant for '{}': value is blank - expected a namespacedID string.",
                    ownerNamespacedId
            );
            return null;
        }

        String namespace = NamespaceUtils.namespace(ownerNamespacedId);
        CustomStack stack = NamespaceUtils.customItemByID(namespace, id);

        if (stack == null) {
            Log.warn(
                    "Storage",
                    "open_variant for '{}': '{}' is not a recognised custom item. " +
                            "Make sure ItemsAdder has finished loading before this behaviour is initialised.",
                    ownerNamespacedId,
                    id
            );
            return null;
        }

        ItemCategory category = ItemCategory.determine(stack, stack.getConfig(), stack.getId());
        Log.debug("Storage",
                "open_variant for '{}': '{}' -> {} (determined via ItemCategory).",
                ownerNamespacedId, stack.getNamespacedID(), category);

        return new OpenVariantConfig(category, stack.getNamespacedID());
    }

    public boolean isFurnitureBased() {
        return category == ItemCategory.FURNITURE || category == ItemCategory.COMPLEX_FURNITURE;
    }

    /**
     * Returns {@code true} when the open-variant is a plain custom item.
     */
    public boolean isItem() {
        return category == ItemCategory.ITEM;
    }
}
