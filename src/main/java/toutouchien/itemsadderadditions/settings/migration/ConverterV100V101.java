package toutouchien.itemsadderadditions.settings.migration;

import org.apache.commons.io.FileUtils;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Migration from plugin version 1.0.0 → 1.0.1.
 *
 * <p>Removes the stale {@code resourcepack/assets/itemsadder_additions/} directory
 * (no longer needed after the asset pipeline was restructured) and backfills all
 * config keys introduced in this version.
 */
public final class ConverterV100V101 {
    private static final Map<String, Object> DEFAULTS = Map.ofEntries(
            // features
            Map.entry("features.creative_inventory_integration", true),

            // actions
            Map.entry("actions.actionbar", true),
            Map.entry("actions.clear_item", true),
            Map.entry("actions.message", true),
            Map.entry("actions.open_inventory", true),
            Map.entry("actions.play_animation", true),
            Map.entry("actions.play_emote", true),
            Map.entry("actions.shoot_fireball", true),
            Map.entry("actions.swing_hand", true),
            Map.entry("actions.teleport", true),
            Map.entry("actions.title", true),
            Map.entry("actions.toast", true),
            Map.entry("actions.veinminer", true),

            // behaviours
            Map.entry("behaviours.connectable", true),
            Map.entry("behaviours.contact_damage", true),
            Map.entry("behaviours.stackable", true),

            // update-checker
            Map.entry("update-checker.enabled", true),
            Map.entry("update-checker.on-join", true)
    );

    private ConverterV100V101() {
    }

    public static void run() {
        deleteObsoleteAssetDirectory();
        ConfigMigration.applyDefaults(DEFAULTS);
    }

    private static void deleteObsoleteAssetDirectory() {
        File obsolete = new File(
                ItemsAdderAdditions.instance().getDataFolder(),
                "resourcepack/assets/itemsadder_additions/"
        );
        try {
            FileUtils.deleteDirectory(obsolete);
        } catch (IOException ignored) {
            // Not critical - the directory may already be absent.
        }
    }
}
