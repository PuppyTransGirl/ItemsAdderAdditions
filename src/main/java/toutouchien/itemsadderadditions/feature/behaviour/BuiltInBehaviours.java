package toutouchien.itemsadderadditions.feature.behaviour;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.*;

import java.util.List;

/**
 * Built-in behaviour prototypes registered by this plugin.
 */
@NullMarked
final class BuiltInBehaviours {
    private BuiltInBehaviours() {
    }

    static List<BehaviourExecutor> create() {
        return List.of(
                new BedBehaviour(),
                new ConnectableBehaviour(),
                new ContactDamageBehaviour(),
                new StackableBehaviour(),
                new StorageBehaviour(),
                new TextDisplayBehaviour()
        );
    }
}
