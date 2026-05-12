package toutouchien.itemsadderadditions.feature.action;

import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.action.listener.*;

import java.util.List;

/**
 * Factory for all Bukkit listeners used by the actions system.
 *
 * <p>Keeping registration here gives the plugin one stable entry point while the
 * event handlers themselves stay split by responsibility.</p>
 */
@NullMarked
public final class ActionsListener {
    private ActionsListener() {
    }

    public static List<Listener> createAll() {
        ActionDispatcher dispatcher = new ActionDispatcher();
        return List.of(
                new ComplexFurnitureActionListener(dispatcher),
                new FurnitureActionListener(dispatcher),
                new BlockActionListener(dispatcher),
                new ItemInteractionActionListener(dispatcher),
                new ItemCombatInventoryActionListener(dispatcher),
                new ItemUseProjectileActionListener(dispatcher),
                new MiscItemActionListener(dispatcher)
        );
    }
}
