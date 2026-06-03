package toutouchien.itemsadderadditions.feature.action;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BuiltInActionsTest {
    @Test
    void createReturnsAllBuiltInActionKeys() {
        List<ActionExecutor> actions = BuiltInActions.create();
        Set<String> keys = actions.stream().map(ActionExecutor::key).collect(Collectors.toSet());

        assertEquals(17, actions.size());
        assertEquals(Set.of(
                "actionbar",
                "clear_item",
                "ignite",
                "message",
                "mythic_mobs_skill",
                "open_inventory",
                "open_trade_machine",
                "play_animation",
                "play_emote",
                "replace_biome",
                "replace_item",
                "shoot_fireball",
                "swing_hand",
                "teleport",
                "title",
                "toast",
                "veinminer"
        ), keys);
    }

    @Test
    void createReturnsFreshInstancesEachTime() {
        List<ActionExecutor> first = BuiltInActions.create();
        List<ActionExecutor> second = BuiltInActions.create();

        assertEquals(first.stream().map(ActionExecutor::key).toList(), second.stream().map(ActionExecutor::key).toList());
        for (int i = 0; i < first.size(); i++) {
            assertNotSame(first.get(i), second.get(i));
            assertEquals(first.get(i).getClass(), second.get(i).getClass());
        }
    }

    @Test
    void returnedListIsImmutable() {
        List<ActionExecutor> actions = BuiltInActions.create();

        assertThrows(UnsupportedOperationException.class, () -> actions.add(actions.getFirst()));
    }
}
