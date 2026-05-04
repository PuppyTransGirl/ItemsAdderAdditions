package toutouchien.itemsadderadditions.utils.loading;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.utils.other.ItemCategory;
import toutouchien.itemsadderadditions.utils.other.Log;

/**
 * Shared traversal logic for systems that load data from every ItemsAdder item.
 *
 * <p>Action and behaviour loaders both follow the same lifecycle:
 * <ol>
 *   <li>clear the previous bindings</li>
 *   <li>walk every {@link CustomStack}</li>
 *   <li>derive the item's category and config context</li>
 *   <li>delegate the actual parsing to the concrete loader</li>
 *   <li>log a single summary at the end</li>
 * </ol>
 *
 * <p>This base class keeps that workflow in one place so new item-based loaders
 * can follow the same pattern without duplicating traversal code.
 */
@NullMarked
public abstract class AbstractItemsAdderItemLoader {
    private final String subsystem;
    private final String loadedUnit;

    protected AbstractItemsAdderItemLoader(String subsystem, String loadedUnit) {
        this.subsystem = subsystem;
        this.loadedUnit = loadedUnit;
    }

    /**
     * Clears previous state and parses every currently loaded ItemsAdder item.
     */
    public final void load() {
        beforeLoad();

        int total = 0;
        for (CustomStack customStack : ItemsAdder.getAllItems()) {
            total += loadItem(ItemLoadContext.of(customStack));
        }

        Log.loaded(subsystem, total, loadedUnit);
    }

    /**
     * Hook for subclasses to clear their bindings before the next load cycle.
     */
    protected abstract void beforeLoad();

    /**
     * Parses one item and returns the number of bindings registered for it.
     */
    protected abstract int loadItem(ItemLoadContext context);

    /**
     * Immutable snapshot of the current ItemsAdder item being parsed.
     */
    public record ItemLoadContext(
            FileConfiguration config,
            String itemId,
            String namespacedId,
            ItemCategory category
    ) {
        public static ItemLoadContext of(CustomStack customStack) {
            FileConfiguration config = customStack.getConfig();
            String itemId = customStack.getId();
            return new ItemLoadContext(
                    config,
                    itemId,
                    customStack.getNamespacedID(),
                    ItemCategory.determine(customStack, config, itemId)
            );
        }
    }
}
