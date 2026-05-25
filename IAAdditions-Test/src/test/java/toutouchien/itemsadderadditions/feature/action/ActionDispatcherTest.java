package toutouchien.itemsadderadditions.feature.action;

import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;
import toutouchien.itemsadderadditions.feature.action.loading.ActionBindings;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionDispatcherTest {
    private static ServerMock server;
    private PlayerMock player;

    @BeforeAll
    static void setupServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void teardownServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setup() {
        ActionBindings.clear();
        CountingAction.count = 0;
        player = server.addPlayer();
    }

    @AfterEach
    void cleanup() {
        ActionBindings.clear();
    }

    @Test
    void dispatchDoesNothingWhenNoBindingExists() {
        new ActionDispatcher().dispatch("ns:item", TriggerType.ITEM_INTERACT, context());

        assertEquals(0, CountingAction.count);
    }

    @Test
    void dispatchRunsMatchingExecutors() {
        ActionBindings.add("ns:item", TriggerType.ITEM_INTERACT, new CountingAction());
        ActionBindings.add("ns:item", TriggerType.ITEM_INTERACT, new CountingAction());

        new ActionDispatcher().dispatch("ns:item", TriggerType.ITEM_INTERACT, context());

        assertEquals(2, CountingAction.count);
    }

    @Test
    void dispatchUsesArgumentizedAndWildcardBindings() {
        ActionBindings.add("ns:item", TriggerKey.of(TriggerType.ITEM_INTERACT, "right"), new CountingAction());
        ActionBindings.add("ns:item", TriggerType.ITEM_INTERACT, new CountingAction());
        ActionBindings.add("ns:item", TriggerKey.of(TriggerType.ITEM_INTERACT, "left"), new CountingAction());

        new ActionDispatcher().dispatch("ns:item", TriggerType.ITEM_INTERACT, "right", context());

        assertEquals(2, CountingAction.count);
    }

    @Test
    void dispatchFallsBackFromRotatedIdToBaseBinding() {
        ActionBindings.add("ns:chair", TriggerType.BLOCK_INTERACT, new CountingAction());

        new ActionDispatcher().dispatch("ns:chair_north", TriggerType.BLOCK_INTERACT, context(TriggerType.BLOCK_INTERACT));

        assertEquals(1, CountingAction.count);
    }

    private ActionContext context() {
        return context(TriggerType.ITEM_INTERACT);
    }

    private ActionContext context(TriggerType type) {
        return ActionContext.create(player, type).build();
    }

    @Action(key = "count")
    private static final class CountingAction extends ActionExecutor {
        static int count;

        @Override
        protected void execute(ActionContext context) {
            count++;
        }
    }
}
