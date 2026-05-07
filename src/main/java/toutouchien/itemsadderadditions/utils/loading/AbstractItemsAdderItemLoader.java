package toutouchien.itemsadderadditions.utils.loading;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.utils.other.ItemCategory;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.List;

/**
 * Shared traversal logic for systems that load data from every ItemsAdder item.
 *
 * <h3>Optimized reload flow (preferred)</h3>
 * The caller (typically {@code ItemsAdderLoadListener}) builds the item list
 * once and shares it with every loader:
 *
 * <pre>{@code
 * List<CustomStack> allItems = ItemsAdder.getAllItems();
 * actionsManager.reload(allItems);
 * behavioursManager.reload(allItems);
 * }</pre>
 *
 * This eliminates redundant {@link ItemsAdder#getAllItems()} calls and enables the
 * base class to apply item-level <em>pre-filtering</em> via {@link #requiredItemSection()},
 * so subclasses only process items that contain their relevant YAML section.
 *
 * <h3>Item-level pre-filtering</h3>
 * Subclasses may override {@link #requiredItemSection()} to declare the top-level
 * item key they need. For example, if a loader only reads the {@code behaviours:}
 * block, it returns {@code "behaviours"} and the base class silently skips all
 * items that don't have that block — without the subclass ever seeing those items.
 *
 * <h3>Legacy / standalone use</h3>
 * The no-arg {@link #load()} method remains for backward compatibility and for
 * tests; it calls {@link ItemsAdder#getAllItems()} internally.
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
     * Checks whether the item's config contains the YAML path
     * {@code items.<id>.<section>} without creating any intermediate objects.
     */
    private static boolean hasSection(CustomStack customStack, String section) {
        return customStack.getConfig().contains("items." + customStack.getId() + "." + section);
    }

    /**
     * Clears previous state and parses items from the supplied pre-built list.
     *
     * <p>Use this overload in production: the list is fetched once by the caller
     * and shared across all item loaders, eliminating duplicate API calls.
     *
     * <p>If {@link #requiredItemSection()} is non-null, only items that contain
     * {@code items.<id>.<requiredSection>} in their config are passed to
     * {@link #loadItem}, skipping the rest in O(1) per item.
     *
     * @param items pre-fetched result of {@link ItemsAdder#getAllItems()}
     */
    public final void load(List<CustomStack> items) {
        beforeLoad();

        String requiredSection = requiredItemSection();
        int total = 0;

        for (CustomStack customStack : items) {
            if (requiredSection != null && !hasSection(customStack, requiredSection)) {
                continue; // Fast pre-filter: skip items that can't match.
            }
            total += loadItem(ItemLoadContext.of(customStack));
        }

        Log.loaded(subsystem, total, loadedUnit);
    }

    /**
     * Clears previous state and parses every currently loaded ItemsAdder item.
     *
     * <p>Prefer {@link #load(List)} when multiple loaders run in the same cycle —
     * it avoids a redundant {@link ItemsAdder#getAllItems()} call per loader.
     */
    public final void load() {
        load(ItemsAdder.getAllItems());
    }

    /**
     * The YAML section key that must exist under {@code items.<id>} for an item
     * to be eligible. Return {@code null} to disable pre-filtering and visit every
     * item regardless.
     *
     * <h3>Example</h3>
     * A behaviour loader that reads {@code items.<id>.behaviours} should return
     * {@code "behaviours"}. Items without that block are skipped before
     * {@link #loadItem} is ever called.
     */
    @Nullable
    protected String requiredItemSection() {
        return null;
    }

    /**
     * Parses one item and returns the number of bindings registered for it.
     */
    protected abstract int loadItem(ItemLoadContext context);

    /**
     * Hook called once at the start of each load cycle so subclasses can tear
     * down state from the previous cycle before new data is registered.
     */
    protected abstract void beforeLoad();

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
