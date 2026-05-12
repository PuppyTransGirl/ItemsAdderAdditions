package toutouchien.itemsadderadditions.feature.action.builtin;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.common.utils.TextRenderer;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;
import toutouchien.itemsadderadditions.nms.api.NmsManager;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Shows an advancement-style toast notification using MiniMessage formatting.
 *
 * <p>{@code icon} accepts a vanilla material key (e.g. {@code "minecraft:diamond"})
 * or an ItemsAdder namespaced ID. {@code text} accepts a plain string or a YAML list
 * whose lines are joined with {@code \n}.
 *
 * <pre>{@code
 * toast:
 *   icon:  "minecraft:diamond"
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
    @Parameter(key = "icon", type = String.class, required = true)
    private String icon;

    @Parameter(key = "frame", type = String.class)
    private String frame = "goal";

    /**
     * Resolved at configure-time from a plain string or list.
     */
    private String text = "";

    private String namespacedID;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        this.namespacedID = namespacedID;

        if (!super.configure(configData, namespacedID))
            return false;

        if (!(configData instanceof ConfigurationSection section))
            return false;

        Object raw = section.get("text");
        switch (raw) {
            case List<?> list -> text = list.stream().map(Object::toString).collect(Collectors.joining("\n"));
            case String s -> text = s;
            case null, default -> {
                Log.itemSkip("Actions", namespacedID, "toast: 'text' is missing or not a string/list");
                return false;
            }
        }

        if (text.isBlank()) {
            Log.itemSkip("Actions", namespacedID, "toast: 'text' is blank");
            return false;
        }

        return true;
    }

    @Override
    protected void execute(ActionContext context) {
        Entity runOn = context.runOn();
        if (!(runOn instanceof Player player))
            return;

        String itemID = icon.toLowerCase(Locale.ROOT);
        ItemStack itemStack = NamespaceUtils.itemByID(namespacedID, itemID);
        if (itemStack == null) {
            Log.warn("Actions", "toast: unknown item '{}' - check the item key in your config", itemID);
            return;
        }

        Component title = TextRenderer.render(player, text);
        NmsManager.instance().handler().toasts().sendToast(player, itemStack, title, frame);
    }
}
