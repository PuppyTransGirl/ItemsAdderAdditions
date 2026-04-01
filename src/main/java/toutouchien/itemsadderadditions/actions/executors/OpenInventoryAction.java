package toutouchien.itemsadderadditions.actions.executors;

import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.utils.VersionUtils;
import toutouchien.itemsadderadditions.utils.other.Log;
import toutouchien.itemsadderadditions.utils.other.PlaceholderAPIUtils;

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
    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Parameter(key = "type", type = String.class, required = true)
    private String type;

    @Parameter(key = "title", type = String.class)
    @Nullable private String title;

    @Override
    protected void execute(ActionContext context) {
        Entity runOn = context.runOn();
        if (!(runOn instanceof HumanEntity human))
            return;

        // Setting the title before 1.21.4 is unstable
        if (VersionUtils.isHigherThanOrEquals(VersionUtils.v1_21_4))
            openInventoryNew(human);
        else
            openInventoryOld(human);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void openInventoryNew(HumanEntity human) {
        Component menuTitle = null;

        if (title != null) {
            String input = human instanceof Player player ? PlaceholderAPIUtils.parsePlaceholders(player, title) : title;
            menuTitle = FontImageWrapper.replaceFontImages(MM.deserialize(input));
        }

        switch (type.toLowerCase(Locale.ROOT)) {
            case "anvil" -> MenuType.ANVIL.create(human, menuTitle).open();
            case "cartography", "cartography_table" -> MenuType.CARTOGRAPHY_TABLE.create(human, menuTitle).open();
            case "crafting_table", "workbench" -> MenuType.CRAFTING.create(human, menuTitle).open();
            case "enchanting", "enchanting_table" -> MenuType.ENCHANTMENT.create(human, menuTitle).open();
            case "ender_chest" -> human.openInventory(human.getEnderChest());
            case "grindstone" -> MenuType.GRINDSTONE.create(human, menuTitle).open();
            case "loom" -> MenuType.LOOM.create(human, menuTitle).open();
            case "smithing", "smithing_table" -> MenuType.SMITHING.create(human, menuTitle).open();
            case "stonecutter" -> MenuType.STONECUTTER.create(human, menuTitle).open();
            default -> Log.warn(
                    "Actions",
                    "open_inventory: unsupported type '{}' - valid types: anvil, cartography_table, crafting_table, enchanting_table, ender_chest, grindstone, loom, smithing_table, stonecutter",
                    type
            );
        }
    }

    @SuppressWarnings("deprecation")
    private void openInventoryOld(HumanEntity human) {
        switch (type.toLowerCase(Locale.ROOT)) {
            case "anvil" -> human.openAnvil(human.getLocation(), true);
            case "cartography", "cartography_table" -> human.openCartographyTable(human.getLocation(), true);
            case "crafting_table", "workbench" -> human.openWorkbench(human.getLocation(), true);
            case "enchanting", "enchanting_table" -> human.openEnchanting(human.getLocation(), true);
            case "ender_chest" -> human.openInventory(human.getEnderChest());
            case "grindstone" -> human.openGrindstone(human.getLocation(), true);
            case "loom" -> human.openLoom(human.getLocation(), true);
            case "smithing", "smithing_table" -> human.openSmithingTable(human.getLocation(), true);
            case "stonecutter" -> human.openStonecutter(human.getLocation(), true);
            default -> Log.warn(
                    "Actions",
                    "open_inventory: unsupported type '{}' - valid types: anvil, cartography_table, crafting_table, enchanting_table, ender_chest, grindstone, loom, smithing_table, stonecutter",
                    type
            );
        }
    }
}
