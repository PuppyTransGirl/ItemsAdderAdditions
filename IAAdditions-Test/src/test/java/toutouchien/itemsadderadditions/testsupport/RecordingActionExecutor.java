package toutouchien.itemsadderadditions.testsupport;

import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * Test-only action executor that records every context it is run with.
 * No trigger restriction, so it fires for whichever {@code TriggerKey} it is bound to.
 */
@Action(key = "recording")
public class RecordingActionExecutor extends ActionExecutor {
    public final List<ActionContext> contexts = new ArrayList<>();

    @Override
    protected void execute(ActionContext context) {
        contexts.add(context);
    }

    public int count() {
        return contexts.size();
    }

    public ActionContext last() {
        return contexts.get(contexts.size() - 1);
    }
}
