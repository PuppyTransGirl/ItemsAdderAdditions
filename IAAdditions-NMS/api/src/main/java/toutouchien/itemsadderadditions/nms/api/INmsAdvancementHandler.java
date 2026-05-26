package toutouchien.itemsadderadditions.nms.api;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.List;

@NullMarked
public interface INmsAdvancementHandler {
    void registerAll(List<AdvancementSpec> specs);

    void unregisterAll(Collection<NamespacedKey> keys);

    default void replaceAll(Collection<NamespacedKey> oldKeys, List<AdvancementSpec> newSpecs) {
        unregisterAll(oldKeys);
        registerAll(newSpecs);
    }

    boolean award(Player player, NamespacedKey key, String criterionName);

    void onPlayerJoin(Player player, Collection<NamespacedKey> rootKeys);
}
