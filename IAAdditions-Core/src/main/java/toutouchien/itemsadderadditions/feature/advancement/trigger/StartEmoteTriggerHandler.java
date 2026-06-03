package toutouchien.itemsadderadditions.feature.advancement.trigger;

import dev.lone.itemsadder.api.Events.PlayerEmotePlayEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

/**
 * Awards the {@link RuntimeTrigger#START_EMOTE} criterion when a player starts
 * playing an emote.
 */
@NullMarked
public final class StartEmoteTriggerHandler extends AbstractTriggerHandler {
    public StartEmoteTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEmote(PlayerEmotePlayEvent event) {
        String emote = event.getEmoteName();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.START_EMOTE)) {
            if (!(c.conditions() instanceof AdvancementConditions.StartEmote(String expected))) continue;
            if (expected != null && !expected.equalsIgnoreCase(emote)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
