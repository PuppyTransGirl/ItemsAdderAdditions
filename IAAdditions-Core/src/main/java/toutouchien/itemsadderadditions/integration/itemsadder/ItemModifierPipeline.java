package toutouchien.itemsadderadditions.integration.itemsadder;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Single ItemsAdder item modifier injection for the entire plugin.
 *
 * <p>Register contributors before the plugin {@code onEnable} injects this pipeline into
 * ItemsAdder. Contributors are applied in insertion order on every ItemsAdder item modifier call.</p>
 */
@NullMarked
public final class ItemModifierPipeline {
    private final List<ItemModifierContributor> contributors = new ArrayList<>();
    private volatile boolean active = true;

    public void addContributor(ItemModifierContributor contributor) {
        contributors.add(Objects.requireNonNull(contributor, "contributor"));
    }

    public void shutdown() {
        active = false;
        contributors.clear();
    }

    public ItemStack apply(String namespacedId, ItemStack itemStack) {
        if (!active) return itemStack;

        ItemStack current = itemStack;
        for (ItemModifierContributor contributor : contributors) {
            current = contributor.apply(namespacedId, current);
        }

        return current;
    }
}
