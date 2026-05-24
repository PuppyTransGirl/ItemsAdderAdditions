package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public record AdvancementRewardDefinition(
        int experience,
        List<String> loot,
        List<NamespacedKey> recipes
) {
    public static final AdvancementRewardDefinition EMPTY =
            new AdvancementRewardDefinition(0, List.of(), List.of());

    public boolean isEmpty() {
        return experience == 0 && loot.isEmpty() && recipes.isEmpty();
    }
}
