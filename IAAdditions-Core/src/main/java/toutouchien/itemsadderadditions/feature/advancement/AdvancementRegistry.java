package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.feature.advancement.trigger.RuntimeTrigger;

import java.util.*;

@NullMarked
public final class AdvancementRegistry {
    private volatile Map<NamespacedKey, AdvancementDefinition> definitions = Map.of();

    public void setAll(List<AdvancementDefinition> defs) {
        Map<NamespacedKey, AdvancementDefinition> map = new LinkedHashMap<>();
        for (AdvancementDefinition def : defs) {
            if (map.containsKey(def.key())) {
                toutouchien.itemsadderadditions.common.logging.Log
                        .warn("AdvancementRegistry", "Duplicate advancement key: {} (last wins)", def.key());
            }
            map.put(def.key(), def);
        }
        this.definitions = Map.copyOf(map);
    }

    public void clear() {
        this.definitions = Map.of();
    }

    @Nullable
    public AdvancementDefinition get(NamespacedKey key) {
        return definitions.get(key);
    }

    public Collection<AdvancementDefinition> all() {
        return definitions.values();
    }

    public Set<NamespacedKey> keys() {
        return definitions.keySet();
    }

    public List<NamespacedKey> rootKeys() {
        return definitions.values().stream()
                .filter(AdvancementDefinition::isRoot)
                .map(AdvancementDefinition::key)
                .toList();
    }

    public List<NamespacedKey> hiddenKeys() {
        return definitions.values().stream()
                .filter(def -> def.display().hidden())
                .map(AdvancementDefinition::key)
                .toList();
    }

    public List<AdvancementCriterionDefinition> criteriaByTrigger(RuntimeTrigger trigger) {
        return definitions.values().stream()
                .flatMap(def -> def.criteria().stream())
                .filter(c -> c.trigger() == trigger)
                .toList();
    }
}
