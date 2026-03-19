package toutouchien.itemsadderadditions.actions.executors;

import dev.lone.itemsadder.api.CustomStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.utils.ToastUtils;

import java.util.List;
import java.util.Locale;

/**
 * Shows an advancement-style toast notification using MiniMessage formatting.
 *
 * <p>The {@code item} field accepts either a vanilla Minecraft material key
 * (e.g. {@code "minecraft:diamond"}) or an ItemsAdder namespaced ID
 * (e.g. {@code "myplugin:my_item"}).
 *
 * <p>The {@code text} field supports multi-line input as a YAML list or a plain
 * string - both forms are accepted.
 *
 * <pre>{@code
 * toast:
 *   item:  "minecraft:diamond"    # required
 *   text:                         # required - list or plain string
 *     - "<yellow>Line one"
 *     - "<gray>Line two"
 *   frame: "goal"                 # optional - task | goal | challenge (default: goal)
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "toast")
public class ToastAction extends ActionExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Parameter(key = "item", type = String.class, required = true)
    private String item;

    @Parameter(key = "text", type = List.class, required = true)
    private List<?> text;

    @Parameter(key = "frame", type = String.class)
    private String frame = "goal";

    @Override
    protected void execute(ActionContext context) {
        if (text.isEmpty()) {
            ItemsAdderAdditions.instance().getSLF4JLogger().warn(
                "[Actions] toast: 'text' is missing or empty for item '{}'.",
                context.heldItem()
            );
            return;
        }

        String itemId = item.toLowerCase(Locale.ROOT);
        ItemStack itemStack = resolveItemStack(itemId);
        if (itemStack == null) {
            ItemsAdderAdditions.instance().getSLF4JLogger().warn(
                "[Actions] toast: unknown item '{}' - skipping.", itemId
            );
            return;
        }

        String joined = String.join("\n", text.stream().map(Object::toString).toList());
        Component title = MM.deserialize(joined);
        ToastUtils.sendToast(context.player(), itemStack, title, frame);
    }

    /**
     * Resolves the item ID to an {@link ItemStack}: tries ItemsAdder first, then
     * falls back to a vanilla {@link Material}.
     *
     * @return the resolved stack, or {@code null} if the ID is unknown
     */
    @Nullable
    private static ItemStack resolveItemStack(String itemId) {
        CustomStack custom = CustomStack.getInstance(itemId);
        if (custom != null)
            return custom.getItemStack();

        String materialName = itemId.replace("minecraft:", "").toUpperCase(Locale.ROOT);
        try {
            return ItemStack.of(Material.valueOf(materialName));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
