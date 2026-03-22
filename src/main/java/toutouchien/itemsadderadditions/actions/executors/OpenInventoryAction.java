package toutouchien.itemsadderadditions.actions.executors;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.utils.Log;

import java.util.Locale;

/**
 * Opens a vanilla inventory GUI for the player.
 *
 * <p>Supported types (case-insensitive): {@code anvil}, {@code cartography_table},
 * {@code crafting_table}, {@code enchanting_table}, {@code ender_chest},
 * {@code grindstone}, {@code loom}, {@code smithing_table}, {@code stonecutter}.
 *
 * <pre>{@code
 * open_inventory:
 *   type: "stonecutter"
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "open_inventory")
public final class OpenInventoryAction extends ActionExecutor {
    @Parameter(key = "type", type = String.class, required = true)
    private String type;

    /**
     * Note on deprecated API usage:
     * The Paper API introduced non-deprecated alternatives for opening the
     * workbench/enchanting GUIs in 1.21.4+.
     * <p>
     * However, ItemsAdder supports Minecraft server
     * versions from 1.20.6 up to 1.21.11. Those older server versions (pre-1.21.4)
     * do not have the newer non-deprecated methods, so we must call the older,
     * deprecated methods (`Player#openWorkbench` and `Player#openEnchanting`) to remain
     * compatible across the full supported range.
     * <p>
     * Using deprecated methods here is an intentional compatibility decision:
     * - It ensures the addon runs on servers where the newer API doesn't exist.
     * - When/if ItemsAdder drops support for < 1.21.4, these calls should be updated
     * to the non-deprecated API and the deprecation suppressions can be removed.
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void execute(ActionContext context) {
        Player player = context.player();

        switch (type.toLowerCase(Locale.ROOT)) {
            case "anvil" -> player.openAnvil(player.getLocation(), true);
            case "cartography", "cartography_table" -> player.openCartographyTable(player.getLocation(), true);
            case "crafting_table", "workbench" -> player.openWorkbench(player.getLocation(), true);
            case "enchanting", "enchanting_table" -> player.openEnchanting(player.getLocation(), true);
            case "ender_chest" -> player.openInventory(player.getEnderChest());
            case "grindstone" -> player.openGrindstone(player.getLocation(), true);
            case "loom" -> player.openLoom(player.getLocation(), true);
            case "smithing", "smithing_table" -> player.openSmithingTable(player.getLocation(), true);
            case "stonecutter" -> player.openStonecutter(player.getLocation(), true);
            default -> Log.warn("Actions", "open_inventory: unsupported type '{}' - valid types: anvil, cartography_table, crafting_table, enchanting_table, ender_chest, grindstone, loom, smithing_table, stonecutter", type);
        }
    }
}
