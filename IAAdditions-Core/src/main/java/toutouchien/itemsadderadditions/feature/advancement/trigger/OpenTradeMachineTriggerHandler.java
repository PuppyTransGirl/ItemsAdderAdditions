package toutouchien.itemsadderadditions.feature.advancement.trigger;

import dev.lone.itemsadder.api.Events.TradeMachineOpenEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

/**
 * Awards the {@link RuntimeTrigger#OPEN_TRADE_MACHINE} criterion when a player
 * opens a trade machine.
 *
 * <p>{@link TradeMachineOpenEvent} only exists in ItemsAdder 4.0.17 and up, so this
 * handler must never be loaded on older builds. {@code AdvancementRuntimeService}
 * gates its instantiation behind a version check.</p>
 */
@NullMarked
public final class OpenTradeMachineTriggerHandler extends AbstractTriggerHandler {
    public OpenTradeMachineTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onOpen(TradeMachineOpenEvent event) {
        String id = event.getNamespacedID();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.OPEN_TRADE_MACHINE)) {
            if (!(c.conditions() instanceof AdvancementConditions.OpenTradeMachine(String tradeMachineId))) continue;
            if (!matchesFurniture(id, tradeMachineId)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
