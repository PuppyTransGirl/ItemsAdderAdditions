package toutouchien.itemsadderadditions.integration.protection;

import net.momirealms.antigrieflib.AntiGriefLib;
import net.momirealms.antigrieflib.Flag;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.integration.worldguard.WorldGuardProtectionChecks;

/**
 * Applies every installed protection integration before a persistent storage is opened.
 */
@NullMarked
public final class StorageProtectionChecks {
    private StorageProtectionChecks() {
    }

    public static boolean canOpenStorage(
            Player player,
            Location location,
            AntiGriefLib antiGriefLib
    ) {
        return WorldGuardProtectionChecks.canOpenStorage(player, location)
                && antiGriefLib.test(player, Flag.OPEN_CONTAINER, location);
    }
}
