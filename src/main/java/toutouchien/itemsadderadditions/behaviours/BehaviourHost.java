package toutouchien.itemsadderadditions.behaviours;

import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.utils.other.ItemCategory;

/**
 * Immutable snapshot of an item's identity, passed to
 * {@link BehaviourExecutor#onLoad} and {@link BehaviourExecutor#onUnload}.
 *
 * <p>The host gives a behaviour executor everything it needs to:
 * <ul>
 *   <li>know which item it is attached to ({@link #namespacedID()})</li>
 *   <li>know what kind of item it is ({@link #category()}), so it can
 *       choose the right detection strategy (block adjacency, entity
 *       proximity, etc.)</li>
 *   <li>register Bukkit event listeners or schedule tasks ({@link #plugin()})</li>
 * </ul>
 */
@NullMarked
public record BehaviourHost(String namespacedID, ItemCategory category, JavaPlugin plugin) {
    /**
     * Returns the namespace portion of the namespaced ID (everything before {@code ':'}).
     * For example, {@code "my_pack:my_item"} -> {@code "my_pack"}.
     */
    public String namespace() {
        return namespacedID.substring(0, namespacedID.indexOf(':'));
    }

    /**
     * Returns the ID portion of the namespaced ID (everything after {@code ':'}).
     * For example, {@code "my_pack:my_item"} -> {@code "my_item"}.
     */
    public String id() {
        return namespacedID.substring(namespacedID.indexOf(':') + 1);
    }
}
