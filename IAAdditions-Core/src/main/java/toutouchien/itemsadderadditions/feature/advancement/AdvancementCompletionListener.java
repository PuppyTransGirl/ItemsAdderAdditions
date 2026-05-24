package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class AdvancementCompletionListener implements Listener {
    private final AdvancementRegistry registry;

    public AdvancementCompletionListener(AdvancementRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDone(PlayerAdvancementDoneEvent event) {
        NamespacedKey key = event.getAdvancement().getKey();
        AdvancementDefinition def = registry.get(key);
        if (def == null) return;
        if (def.onComplete().isEmpty()) return;
        def.onComplete().execute(event.getPlayer());
    }
}
