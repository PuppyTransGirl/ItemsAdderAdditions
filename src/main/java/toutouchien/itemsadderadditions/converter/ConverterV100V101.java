package toutouchien.itemsadderadditions.converter;

import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ConverterV100V101 {
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

    public static void run() {
        File dataFolder = ItemsAdderAdditions.instance().getDataFolder();
        File oldFolder = new File(dataFolder, "resourcepack/assets/itemsadder_additions/");
        try {
            FileUtils.deleteDirectory(oldFolder);
        } catch (IOException e) {
            // We don't care about it
        }

        FileConfiguration config = ItemsAdderAdditions.instance().getConfig();

        boolean dirty = false;
        for (Map.Entry<String, Object> entry : DEFAULTS.entrySet()) {
            if (!config.isSet(entry.getKey())) {
                config.set(entry.getKey(), entry.getValue());
                dirty = true;
            }
        }

        if (dirty) {
            ItemsAdderAdditions.instance().saveConfig();
        }
    }
}
