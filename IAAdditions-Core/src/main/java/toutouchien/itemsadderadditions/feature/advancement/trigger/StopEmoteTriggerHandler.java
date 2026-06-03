package toutouchien.itemsadderadditions.feature.advancement.trigger;

import dev.lone.itemsadder.api.Events.PlayerEmoteEndEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

/**
 * Awards the {@link RuntimeTrigger#STOP_EMOTE} criterion when a player's emote
 * stops or finishes.
 */
@NullMarked
public final class StopEmoteTriggerHandler extends AbstractTriggerHandler {
    public StopEmoteTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEmoteEnd(PlayerEmoteEndEvent event) {
        String emote = event.getEmoteName();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.STOP_EMOTE)) {
            if (!(c.conditions() instanceof AdvancementConditions.StopEmote(String expected))) continue;
            if (expected != null && !expected.equalsIgnoreCase(emote)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
