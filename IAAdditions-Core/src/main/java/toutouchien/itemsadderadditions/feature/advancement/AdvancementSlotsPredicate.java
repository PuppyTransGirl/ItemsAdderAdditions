package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import static toutouchien.itemsadderadditions.feature.advancement.AdvancementPredicateSupport.*;

record SlotsPredicate(Map<String, ItemPredicate> slots) {
    @Nullable
    public static SlotsPredicate parse(String namespace, @Nullable Object raw) {
        if (raw == null) return null;
        Map<String, ItemPredicate> result = new LinkedHashMap<>();
        for (String key : keys(raw)) {
            result.put(key, ItemPredicate.parse(namespace, sectionOrValue(raw, key)));
        }
        return result.isEmpty() ? null : new SlotsPredicate(Map.copyOf(result));
    }

    public boolean matches(Player player) {
        for (Map.Entry<String, ItemPredicate> entry : slots.entrySet()) {
            if (!matchesSlotRange(player, entry.getKey(), entry.getValue())) return false;
        }
        return true;
    }
}
