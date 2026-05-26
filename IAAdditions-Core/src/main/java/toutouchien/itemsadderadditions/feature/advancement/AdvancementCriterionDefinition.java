package toutouchien.itemsadderadditions.feature.advancement;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.trigger.RuntimeTrigger;

@NullMarked
public record AdvancementCriterionDefinition(
        String name,
        RuntimeTrigger trigger,
        AdvancementConditions conditions,
        AdvancementPlayerPredicate playerPredicate
) {
    public AdvancementCriterionDefinition(String name, RuntimeTrigger trigger, AdvancementConditions conditions) {
        this(name, trigger, conditions, AdvancementPlayerPredicate.ANY);
    }
}
