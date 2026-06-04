package toutouchien.itemsadderadditions.integration.hook.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;

@NullMarked
interface WorldGuardDelegate {
    boolean isEnabled();

    void registerFlags(JavaPlugin plugin, WorldGuardSettings settings, Collection<String> builtInActionKeys);

    boolean test(Player player, Location location, WorldGuardFlagKey key);

    boolean testAction(String actionKey, Player player, Location location);
}
