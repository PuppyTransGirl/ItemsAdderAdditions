package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
public record AdvancementDefinition(
        NamespacedKey key,
        @Nullable NamespacedKey parent,
        AdvancementDisplayDefinition display,
        List<AdvancementCriterionDefinition> criteria,
        AdvancementRewardDefinition rewards,
        CompletionActions onComplete
) {
    public boolean isRoot() {
        return parent == null;
    }
}
