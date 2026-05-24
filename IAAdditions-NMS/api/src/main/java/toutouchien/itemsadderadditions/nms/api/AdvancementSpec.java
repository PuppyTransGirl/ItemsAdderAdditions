package toutouchien.itemsadderadditions.nms.api;

import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
public record AdvancementSpec(
        NamespacedKey key,
        @Nullable NamespacedKey parent,
        AdvancementDisplaySpec display,
        List<String> criteriaNames,
        boolean autoGrantRoot,
        int rewardExperience,
        List<String> rewardLoot,
        List<NamespacedKey> rewardRecipes
) {}
