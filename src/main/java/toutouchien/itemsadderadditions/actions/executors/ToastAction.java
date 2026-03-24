package toutouchien.itemsadderadditions.actions.executors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.utils.Log;
import toutouchien.itemsadderadditions.utils.ToastUtils;
import toutouchien.itemsadderadditions.utils.other.NamespaceUtils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Shows an advancement-style toast notification using MiniMessage formatting.
 *
 * <p>{@code item} accepts a vanilla material key (e.g. {@code "minecraft:diamond"})
 * or an ItemsAdder namespaced ID. {@code text} accepts a plain string or a YAML list
 * whose lines are joined with {@code \n}.
 *
 * <pre>{@code
 * toast:
 *   item:  "minecraft:diamond"
 *   text:
 *     - "<yellow>Line one"
 *     - "<gray>Line two"
 *   frame: "goal"   # task | goal | challenge (default: goal)
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "toast")
public final class ToastAction extends ActionExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Parameter(key = "item", type = String.class, required = true)
    private String item;

    @Parameter(key = "frame", type = String.class)
    private String frame = "goal";

    /** Resolved at configure-time from a plain string or list. */
    private String text = "";

    private String namespacedID;

    @Override
    public boolean configure(Object configData, String namespacedID) {
        this.namespacedID = namespacedID;

        if (!super.configure(configData, namespacedID))
            return false;

        if (!(configData instanceof ConfigurationSection section))
            return false;

        Object raw = section.get("text");
        if (raw instanceof List<?> list) {
            text = list.stream().map(Object::toString).collect(Collectors.joining("\n"));
        } else if (raw instanceof String s) {
            text = s;
        } else {
            Log.itemSkip("Actions", namespacedID, "toast: 'text' is missing or not a string/list");
            return false;
        }

        if (text.isBlank()) {
            Log.itemSkip("Actions", namespacedID, "toast: 'text' is blank");
            return false;
        }

        return true;
    }

    @Override
    protected void execute(ActionContext context) {
        String itemID = item.toLowerCase(Locale.ROOT);
        ItemStack itemStack = NamespaceUtils.itemByID(namespacedID, itemID);
        if (itemStack == null) {
            Log.warn("Actions", "toast: unknown item '{}' - check the item key in your config", itemID);
            return;
        }

        Component title = MM.deserialize(text);
        ToastUtils.sendToast(context.player(), itemStack, title, frame);
    }
}
