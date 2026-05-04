package toutouchien.itemsadderadditions.behaviours;

import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.other.ItemCategory;

/**
 * Immutable snapshot of an item's identity, passed to
 * {@link BehaviourExecutor#onLoad} and {@link BehaviourExecutor#onUnload}.
 */
@NullMarked
public record BehaviourHost(String namespacedID, ItemCategory category, JavaPlugin plugin) {
    public BehaviourHost {
        if (!namespacedID.contains(":")) {
            throw new IllegalArgumentException("BehaviourHost requires a namespaced ID: " + namespacedID);
        }
    }

    /**
     * Returns the namespace portion of the namespaced ID.
     */
    public String namespace() {
        return NamespaceUtils.namespace(namespacedID);
    }

    /**
     * Returns the ID portion of the namespaced ID.
     */
    public String id() {
        return NamespaceUtils.id(namespacedID);
    }
}
