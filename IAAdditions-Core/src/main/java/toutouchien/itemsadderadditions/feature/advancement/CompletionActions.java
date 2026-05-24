package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public final class CompletionActions {
    public static final CompletionActions EMPTY = new CompletionActions(List.of());

    private final List<CompletionAction> actions;

    public CompletionActions(List<CompletionAction> actions) {
        this.actions = List.copyOf(actions);
    }

    public boolean isEmpty() {
        return actions.isEmpty();
    }

    public void execute(Player player) {
        for (CompletionAction action : actions) {
            action.execute(player);
        }
    }
}
