package toutouchien.itemsadderadditions.integration.hook.worldguard;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record WorldGuardSettings(
        boolean enabled,
        boolean storageOpen,
        boolean contactDamage,
        boolean stackablePlace,
        boolean bedUse,
        boolean customPaintingPlace,
        boolean actions
) {
    public static WorldGuardSettings defaults() {
        return new WorldGuardSettings(true, true, true, true, true, true, true);
    }

    public static WorldGuardSettings load(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("worldguard");
        if (section == null) {
            return defaults();
        }

        ConfigurationSection flags = section.getConfigurationSection("flags");
        return new WorldGuardSettings(
                section.getBoolean("enabled", true),
                flag(flags, "storage_open", true),
                flag(flags, "contact_damage", true),
                flag(flags, "stackable_place", true),
                flag(flags, "bed_use", true),
                flag(flags, "custom_painting_place", true),
                flag(flags, "actions", true)
        );
    }

    private static boolean flag(ConfigurationSection flags, String key, boolean fallback) {
        return flags == null || flags.getBoolean(key, fallback);
    }
}
