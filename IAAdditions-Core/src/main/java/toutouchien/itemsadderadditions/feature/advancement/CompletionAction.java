package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface CompletionAction {
    void execute(Player player);
}
