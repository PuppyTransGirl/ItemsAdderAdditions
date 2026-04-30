package toutouchien.itemsadderadditions.behaviours.executors.storage;

import dev.lone.itemsadder.api.CustomStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

/**
 * Immutable configuration for the visual "open variant" of a storage container -
 * the custom block or furniture displayed at the storage location while at least
 * one player has the GUI open.
 *
 * <p>The {@link FormType} is resolved automatically via {@link CustomStack#isBlock()},
 * so no {@code type} field is required in the YAML. The ID is also normalised by
 * {@link NamespaceUtils#customItemByID}, so the namespace may be omitted when it
 * matches the owning item's namespace.
 *
 * <p>Example YAML:
 * <pre>
 * open_variant: "mynamespace:my_chest_open"
 * # namespace optional when it matches the owning item:
 * open_variant: "my_chest_open"
 * </pre>
 */
@NullMarked
public record OpenVariantConfig(FormType type, String id) {
    /**
     * Resolves an {@link OpenVariantConfig} from a namespacedID string, auto-detecting
     * the {@link FormType} via {@link CustomStack#isBlock()}.
     *
     * <p>The ID is normalised through {@link NamespaceUtils#customItemByID}: if it
     * contains no namespace, the owning item's namespace is prepended automatically.
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

        FormType type = stack.isBlock() ? FormType.BLOCK : FormType.FURNITURE;
        Log.debug(
                "Storage",
                "open_variant for '{}': resolved '{}' as {}.",
                ownerNamespacedId,
                id,
                type
        );
        return new OpenVariantConfig(type, stack.getNamespacedID());
    }

    /**
     * Convenience delegate to {@link FormType#isFurnitureBased()}.
     */
    public boolean isFurnitureBased() {
        return type.isFurnitureBased();
    }

    public enum FormType {
        /**
         * A custom block registered through ItemsAdder (not a vanilla block).
         */
        BLOCK,
        /**
         * A custom furniture (single- or multi-entity, including complex furniture).
         */
        FURNITURE;

        public boolean isFurnitureBased() {
            return this == FURNITURE;
        }
    }
}
