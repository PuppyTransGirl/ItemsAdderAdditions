package toutouchien.itemsadderadditions.feature.advancement;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompletionActionsTest {
    @Test
    void empty_constant_isEmpty() {
        assertTrue(CompletionActions.EMPTY.isEmpty());
    }

    @Test
    void noActions_isEmpty() {
        assertTrue(new CompletionActions(List.of()).isEmpty());
    }

    @Test
    void withAction_isNotEmpty() {
        var actions = new CompletionActions(List.of(player -> {}));
        assertFalse(actions.isEmpty());
    }

    @Test
    void actions_listCopied_mutationSafe() {
        var mutable = new java.util.ArrayList<CompletionAction>();
        mutable.add(player -> {});
        var ca = new CompletionActions(mutable);
        assertFalse(ca.isEmpty());
        mutable.clear();
        assertFalse(ca.isEmpty());
    }
}
