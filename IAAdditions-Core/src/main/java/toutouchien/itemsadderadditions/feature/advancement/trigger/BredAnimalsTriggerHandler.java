package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityBreedEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class BredAnimalsTriggerHandler extends AbstractTriggerHandler {
    public BredAnimalsTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) return;
        String entityType = event.getEntity().getType().getKey().toString();
        String motherType = event.getMother().getType().getKey().toString();
        String fatherType = event.getFather().getType().getKey().toString();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.BRED_ANIMALS)) {
            if (!(c.conditions() instanceof AdvancementConditions.BredAnimals(String type, String parentType, String partnerType)))
                continue;
            if (type != null && !type.equals(entityType)) continue;
            if (parentType != null && partnerType != null) {
                boolean option1 = parentType.equals(motherType) && partnerType.equals(fatherType);
                boolean option2 = parentType.equals(fatherType) && partnerType.equals(motherType);
                if (!option1 && !option2) continue;
            } else {
                if (parentType != null && !parentType.equals(motherType) && !parentType.equals(fatherType)) continue;
                if (partnerType != null && !partnerType.equals(motherType) && !partnerType.equals(fatherType)) continue;
            }
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
