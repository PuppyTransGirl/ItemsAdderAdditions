package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

/**
 * Immutable configuration for the visual "open variant" of a storage container -
 * the custom block, furniture, or plain custom-stack item displayed at the storage
 * location while at least one player has the GUI open.
 *
 * <p>The {@link FormType} is resolved automatically:
 * <ol>
 *   <li>{@link CustomStack#isBlock()} → {@link FormType#BLOCK}</li>
 *   <li>{@link CustomFurniture#getInstance} returns non-null → {@link FormType#FURNITURE}</li>
 *   <li>Otherwise → {@link FormType#ITEM_DISPLAY}: the open-variant is a plain custom stack
 *       (not a registered block or furniture). Its {@link CustomStack#getItemStack()} is
 *       written directly onto the existing furniture entity's {@link org.bukkit.entity.ItemDisplay}
 *       so that its model changes in-place, without any entity being removed or spawned.
 *       On close the original ItemStack is restored.</li>
 * </ol>
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
public record OpenVariantConfig(FormType type, String id) {
    /**
     * Resolves an {@link OpenVariantConfig} from a namespacedID string, auto-detecting
     * the {@link FormType}.
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

        FormType type;
        if (stack.isBlock()) {
            type = FormType.BLOCK;
            Log.debug("Storage",
                    "open_variant for '{}': '{}' → BLOCK (isBlock=true).",
                    ownerNamespacedId, stack.getNamespacedID());
        } else if (stack instanceof CustomFurniture) {
            type = FormType.FURNITURE;
            Log.debug("Storage",
                    "open_variant for '{}': '{}' → FURNITURE (stack instanceof CustomFurniture).",
                    ownerNamespacedId, stack.getNamespacedID());
        } else {
            type = FormType.ITEM_DISPLAY;
            Log.debug("Storage",
                    "open_variant for '{}': '{}' → ITEM_DISPLAY (plain custom stack, " +
                            "model will be swapped in-place on the furniture's ItemDisplay entity).",
                    ownerNamespacedId, stack.getNamespacedID());
        }

        return new OpenVariantConfig(type, stack.getNamespacedID());
    }

    /**
     * Convenience delegate to {@link FormType#isFurnitureBased()}.
     */
    public boolean isFurnitureBased() {
        return type.isFurnitureBased();
    }

    /**
     * Returns {@code true} when the open-variant is a plain custom stack whose model
     * will be swapped in-place on the existing furniture {@link org.bukkit.entity.ItemDisplay}
     * entity.
     */
    public boolean isItemDisplay() {
        return type == FormType.ITEM_DISPLAY;
    }

    public enum FormType {
        /**
         * A custom block registered through ItemsAdder.
         */
        BLOCK,
        /**
         * A custom furniture (single- or multi-entity, including complex furniture).
         */
        FURNITURE,
        /**
         * A plain custom stack (not a registered block or furniture).
         * At runtime the model is swapped in-place on the holder's existing
         * {@link org.bukkit.entity.ItemDisplay} entity; no entity is created or destroyed.
         */
        ITEM_DISPLAY;

        public boolean isFurnitureBased() {
            return this == FURNITURE;
        }
    }
}
