package toutouchien.itemsadderadditions.integration.itemsadder;

import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Single ItemsAdder item modifier injection for the entire plugin.
 *
 * <p>Register contributors before calling {@link #register()}. Contributors are applied
 * in insertion order on every ItemsAdder item modifier call. Only one call to
 * {@code ItemsAdder.Advanced.injectItemModifier} is ever made by this plugin.</p>
 */
@NullMarked
public final class ItemModifierPipeline {
    private final ItemsAdderAdditions plugin;
    private final List<ItemModifierContributor> contributors = new ArrayList<>();
    private volatile boolean active = true;
    private boolean registered;

    public ItemModifierPipeline(ItemsAdderAdditions plugin) {
        this.plugin = plugin;
    }

    public synchronized void register() {
        if (registered) return;
        registered = true;
        ItemsAdder.Advanced.injectItemModifier(plugin, this::apply);
    }

    public void addContributor(ItemModifierContributor contributor) {
        contributors.add(Objects.requireNonNull(contributor, "contributor"));
    }

    public void shutdown() {
        active = false;
        contributors.clear();
    }

    ItemStack apply(String namespacedId, ItemStack itemStack) {
        if (!active) return itemStack;

        ItemStack current = itemStack;
        for (ItemModifierContributor contributor : contributors) {
            current = contributor.apply(namespacedId, current);
        }

        return current;
    }
}
