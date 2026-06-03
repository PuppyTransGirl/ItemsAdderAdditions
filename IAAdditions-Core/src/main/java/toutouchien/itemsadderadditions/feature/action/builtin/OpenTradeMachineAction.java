package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;
import toutouchien.itemsadderadditions.integration.bridge.TradeMachineBridge;

/**
 * Opens an ItemsAdder trade machine for the player.
 *
 * <pre>{@code
 * open_trade_machine:
 *   trade_machine: "my_pack:shop"
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "open_trade_machine")
public final class OpenTradeMachineAction extends ActionExecutor {
    @Parameter(key = "trade_machine", type = String.class, required = true)
    private String tradeMachine;

    private String namespacedID = "";

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        this.namespacedID = namespacedID;
        return super.configure(configData, namespacedID);
    }

    @Override
    protected void execute(ActionContext context) {
        Entity runOn = context.runOn();
        if (!(runOn instanceof Player player))
            return;

        if (!TradeMachineBridge.isReady()) {
            Log.warn("Actions", "open_trade_machine: trade-machine support is not available");
            return;
        }

        String id = NamespaceUtils.normalizeItemID(NamespaceUtils.namespace(namespacedID), tradeMachine);
        if (!TradeMachineBridge.openTradeMachine(player, id))
            Log.warn("Actions", "open_trade_machine: no trade machine found for '{}'", id);
    }
}
