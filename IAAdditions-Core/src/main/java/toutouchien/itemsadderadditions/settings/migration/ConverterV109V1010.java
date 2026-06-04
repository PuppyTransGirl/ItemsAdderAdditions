package toutouchien.itemsadderadditions.settings.migration;

import org.bukkit.configuration.file.FileConfiguration;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;

import java.util.Map;

/**
 * Migration from plugin version 1.0.9 -> 1.0.10.
 */
public final class ConverterV109V1010 {
    private static final String OLD_CONTACT_DAMAGE_KEY = "behaviours.contact_damage";
    private static final String NEW_CONTACT_EFFECT_KEY = "behaviours.contact_effect";

    private static final Map<String, Object> DEFAULTS = Map.of(
            "features.item_model_definitions", true,

            "worldguard.enabled", true,
            "worldguard.flags.storage_open", true,
            "worldguard.flags.contact_damage", true,
            "worldguard.flags.stackable_place", true,
            "worldguard.flags.bed_use", true,
            "worldguard.flags.custom_painting_place", true,
            "worldguard.flags.actions", true,

            "actions.open_trade_machine", true
    );

    private ConverterV109V1010() {
    }

    public static void run() {
        migrateContactDamageToggle();
        ConfigMigration.applyDefaults(DEFAULTS);
    }

    private static void migrateContactDamageToggle() {
        ItemsAdderAdditions plugin = ItemsAdderAdditions.instance();
        FileConfiguration config = plugin.getConfig();

        if (config.isSet(NEW_CONTACT_EFFECT_KEY)) {
            return;
        }

        Object value = config.isSet(OLD_CONTACT_DAMAGE_KEY)
                ? config.get(OLD_CONTACT_DAMAGE_KEY)
                : true;

        config.set(NEW_CONTACT_EFFECT_KEY, value);
        plugin.saveConfig();
    }
}
