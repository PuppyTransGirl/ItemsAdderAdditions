package toutouchien.itemsadderadditions.feature.advancement;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.AdvancementDisplaySpec;
import toutouchien.itemsadderadditions.nms.api.AdvancementSpec;

import java.util.List;

@NullMarked
public final class AdvancementSpecBuilder {
    private AdvancementSpecBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static AdvancementSpec build(AdvancementDefinition def) {
        AdvancementDisplaySpec display = new AdvancementDisplaySpec(
                def.display().icon(),
                def.display().title(),
                def.display().description(),
                def.display().frame(),
                def.display().background(),
                def.display().showToast(),
                def.display().announceToChat(),
                def.display().hidden()
        );

        List<String> criteriaNames = def.criteria().stream()
                .map(AdvancementCriterionDefinition::name)
                .toList();
        if (criteriaNames.isEmpty() && def.isRoot()) {
            criteriaNames = List.of("root_trigger");
        }

        return new AdvancementSpec(
                def.key(),
                def.parent(),
                display,
                criteriaNames,
                def.isRoot(),
                def.rewards().experience(),
                def.rewards().loot(),
                def.rewards().recipes()
        );
    }

    public static List<AdvancementSpec> buildAll(List<AdvancementDefinition> defs) {
        return defs.stream().map(AdvancementSpecBuilder::build).toList();
    }
}
