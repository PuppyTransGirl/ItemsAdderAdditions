package toutouchien.itemsadderadditions.feature.advancement;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.trigger.RuntimeTrigger;

@NullMarked
public record AdvancementCriterionDefinition(
        String name,
        RuntimeTrigger trigger,
        AdvancementConditions conditions
) {}
