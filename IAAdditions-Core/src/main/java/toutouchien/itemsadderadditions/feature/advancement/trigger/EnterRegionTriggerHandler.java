package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;
import toutouchien.itemsadderadditions.integration.hook.worldguard.WorldGuardProtection;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Awards criteria when a player transitions from outside to inside a
 * WorldGuard region.
 */
@NullMarked
public final class EnterRegionTriggerHandler extends AbstractTriggerHandler {
    private final Function<Location, Set<String>> regionLookup;

    public EnterRegionTriggerHandler(AdvancementRegistry registry) {
        this(registry, WorldGuardProtection::regionIdsAt);
    }

    EnterRegionTriggerHandler(
            AdvancementRegistry registry,
            Function<Location, Set<String>> regionLookup
    ) {
        super(registry);
        this.regionLookup = regionLookup;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (sameBlock(event.getFrom(), event.getTo())) return;
        checkTransition(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        checkTransition(event.getPlayer(), event.getFrom(), event.getTo());
    }

    void checkTransition(Player player, Location from, Location to) {
        List<AdvancementCriterionDefinition> criteria =
                registry.criteriaByTrigger(RuntimeTrigger.ENTER_REGION);
        if (criteria.isEmpty() || to.getWorld() == null) return;

        Set<String> fromRegions = regionLookup.apply(from);
        Set<String> toRegions = regionLookup.apply(to);
        if (toRegions.isEmpty()) return;

        String worldName = to.getWorld().getName();
        for (AdvancementCriterionDefinition criterion : criteria) {
            if (!(criterion.conditions()
                    instanceof AdvancementConditions.EnterRegion(String regionId, String world))) {
                continue;
            }
            if (regionId.isBlank()
                    || !toRegions.contains(regionId)
                    || fromRegions.contains(regionId)) {
                continue;
            }
            if (world != null && !world.equals(worldName)) continue;
            award(player, advancementKeyFor(criterion), criterion.name());
        }
    }

    private static boolean sameBlock(Location from, Location to) {
        return from.getWorld() == to.getWorld()
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ();
    }
}
