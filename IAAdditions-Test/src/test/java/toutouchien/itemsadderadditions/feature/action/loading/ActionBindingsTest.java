package toutouchien.itemsadderadditions.feature.action.loading;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.TriggerKey;
import toutouchien.itemsadderadditions.feature.action.TriggerType;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActionBindingsTest {
    private static ActionExecutor stubExecutor() {
        return new StubExecutor();
    }

    @BeforeEach
    void clearBindings() {
        ActionBindings.clear();
    }

    @Test
    void getUnknownIdReturnsEmpty() {
        List<ActionExecutor> result = ActionBindings.get("myns:item", TriggerType.ITEM_INTERACT);
        assertTrue(result.isEmpty());
    }

    @Test
    void addAndGetNonArgumentized() {
        ActionExecutor executor = stubExecutor();
        ActionBindings.add("myns:item", TriggerType.ITEM_INTERACT, executor);

        List<ActionExecutor> result = ActionBindings.get("myns:item", TriggerType.ITEM_INTERACT);
        assertEquals(1, result.size());
        assertSame(executor, result.get(0));
    }

    @Test
    void addAndGetWithExactArgument() {
        ActionExecutor executor = stubExecutor();
        ActionBindings.add("myns:item", TriggerKey.of(TriggerType.ITEM_INTERACT, "right"), executor);

        List<ActionExecutor> result = ActionBindings.get("myns:item", TriggerType.ITEM_INTERACT, "right");
        assertEquals(1, result.size());
        assertSame(executor, result.get(0));
    }

    @Test
    void wildcardExecutorReturnedForAnyArgument() {
        ActionExecutor wildcard = stubExecutor();
        ActionBindings.add("myns:item", TriggerType.ITEM_INTERACT, wildcard); // null argument = wildcard

        List<ActionExecutor> result = ActionBindings.get("myns:item", TriggerType.ITEM_INTERACT, "right");
        assertTrue(result.contains(wildcard));
    }

    @Test
    void exactAndWildcardBothReturned() {
        ActionExecutor exact = stubExecutor();
        ActionExecutor wildcard = stubExecutor();
        ActionBindings.add("myns:item", TriggerKey.of(TriggerType.ITEM_INTERACT, "right"), exact);
        ActionBindings.add("myns:item", TriggerType.ITEM_INTERACT, wildcard);

        List<ActionExecutor> result = ActionBindings.get("myns:item", TriggerType.ITEM_INTERACT, "right");
        assertEquals(2, result.size());
        assertTrue(result.contains(exact));
        assertTrue(result.contains(wildcard));
    }

    @Test
    void wrongArgumentDoesNotReturnExact() {
        ActionExecutor executor = stubExecutor();
        ActionBindings.add("myns:item", TriggerKey.of(TriggerType.ITEM_INTERACT, "left"), executor);

        List<ActionExecutor> result = ActionBindings.get("myns:item", TriggerType.ITEM_INTERACT, "right");
        assertTrue(result.isEmpty());
    }

    @Test
    void clearRemovesAllBindings() {
        ActionBindings.add("myns:item", TriggerType.ITEM_INTERACT, stubExecutor());
        ActionBindings.clear();

        assertTrue(ActionBindings.get("myns:item", TriggerType.ITEM_INTERACT).isEmpty());
    }

    @Test
    void hasReturnsTrueWhenBindingExists() {
        ActionBindings.add("myns:item", TriggerType.ITEM_INTERACT, stubExecutor());
        assertTrue(ActionBindings.has("myns:item", TriggerType.ITEM_INTERACT));
    }

    @Test
    void hasReturnsFalseWhenNoBinding() {
        assertFalse(ActionBindings.has("myns:item", TriggerType.ITEM_INTERACT));
    }

    @Test
    void rotationSuffixFallsBackToBaseId() {
        ActionExecutor executor = stubExecutor();
        ActionBindings.add("myns:block", TriggerType.BLOCK_INTERACT, executor);

        // "myns:block_north" has no direct binding but strips to "myns:block"
        List<ActionExecutor> result = ActionBindings.get("myns:block_north", TriggerType.BLOCK_INTERACT);
        assertEquals(1, result.size());
        assertSame(executor, result.get(0));
    }

    @Test
    void multipleExecutorsForSameTrigger() {
        ActionExecutor e1 = stubExecutor();
        ActionExecutor e2 = stubExecutor();
        ActionBindings.add("myns:item", TriggerType.ITEM_INTERACT, e1);
        ActionBindings.add("myns:item", TriggerType.ITEM_INTERACT, e2);

        List<ActionExecutor> result = ActionBindings.get("myns:item", TriggerType.ITEM_INTERACT);
        assertEquals(2, result.size());
    }

    @Test
    void differentItemsDontInterfere() {
        ActionExecutor e1 = stubExecutor();
        ActionExecutor e2 = stubExecutor();
        ActionBindings.add("myns:item_a", TriggerType.ITEM_INTERACT, e1);
        ActionBindings.add("myns:item_b", TriggerType.ITEM_INTERACT, e2);

        assertSame(e1, ActionBindings.get("myns:item_a", TriggerType.ITEM_INTERACT).get(0));
        assertSame(e2, ActionBindings.get("myns:item_b", TriggerType.ITEM_INTERACT).get(0));
    }

    @Action(key = "stub")
    private static class StubExecutor extends ActionExecutor {
        @Override
        protected void execute(ActionContext context) {
        }
    }
}
