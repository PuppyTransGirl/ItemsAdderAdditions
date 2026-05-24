package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class PermissionTriggerHandler extends AbstractTriggerHandler {
    public PermissionTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        checkPermissions(event.getPlayer());
    }

    public void checkPermissions(Player player) {
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.PERMISSION)) {
            if (!(c.conditions() instanceof AdvancementConditions.Permission(String node))) continue;
            if (!player.hasPermission(node)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
