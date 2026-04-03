package toutouchien.itemsadderadditions.converter;

import org.bukkit.configuration.file.FileConfiguration;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;

import java.util.Map;

public class ConverterV101V102 {
    private static final Map<String, Object> DEFAULTS = Map.ofEntries(
            // actions
            Map.entry("actions.ignite", true),
            Map.entry("actions.mythic_mobs_skill", true)
    );

    public static void run() {
        FileConfiguration config = ItemsAdderAdditions.instance().getConfig();

        boolean dirty = false;
        for (Map.Entry<String, Object> entry : DEFAULTS.entrySet()) {
            if (!config.isSet(entry.getKey())) {
                config.set(entry.getKey(), entry.getValue());
                dirty = true;
            }
        }

        if (dirty)
            ItemsAdderAdditions.instance().saveConfig();
    }
}
