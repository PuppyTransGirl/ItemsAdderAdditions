package toutouchien.itemsadderadditions.feature.advancement.trigger;

import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class BreakBlockTriggerHandler extends AbstractTriggerHandler {
    public BreakBlockTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(CustomBlockBreakEvent event) {
        awardMatching(event.getPlayer(), event.getNamespacedID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVanillaBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String blockId = NamespaceUtils.blockID(block);
        // ItemsAdder fires its own custom block event; avoid awarding twice for the
        // underlying note block / mushroom block used as storage.
        if (!blockId.startsWith("minecraft:")) return;
        awardMatching(event.getPlayer(), block);
    }

    private void awardMatching(Player player, String actualId) {
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.BREAK_BLOCK)) {
            if (!(c.conditions() instanceof AdvancementConditions.BreakBlock(String id))) continue;
            if (!matchesContentId(actualId, id)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }

    private void awardMatching(Player player, Block block) {
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.BREAK_BLOCK)) {
            if (!(c.conditions() instanceof AdvancementConditions.BreakBlock(String id))) continue;
            if (!matchesBlock(block, id)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
