package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static toutouchien.itemsadderadditions.feature.advancement.AdvancementPredicateSupport.*;

record SlotsPredicate(Map<String, ItemPredicate> slots) {
    @Nullable
    public static SlotsPredicate parse(String namespace, @Nullable Object raw) {
        if (raw == null) return null;
        Map<String, ItemPredicate> result = new LinkedHashMap<>();
        collectSlots(namespace, raw, "", result);
        return result.isEmpty() ? null : new SlotsPredicate(Map.copyOf(result));
    }

    private static void collectSlots(String namespace, Object raw, String prefix, Map<String, ItemPredicate> result) {
        for (String key : keys(raw)) {
            Object child = sectionOrValue(raw, key);
            if (child == null) continue;

            String slotKey = prefix.isBlank() ? key : prefix + "." + key;

            // Bukkit's YAML configuration treats dots as path separators when it
            // builds MemorySections. A YAML key like "inventory.0" can therefore
            // arrive here as { inventory: { 0: ... } }. Flatten those grouped
            // slot sections back into vanilla's dotted slot names before parsing
            // their item predicate.
            if (isSection(child) && isSlotGroup(key) && !looksLikeItemPredicate(child)) {
                collectSlots(namespace, child, slotKey, result);
                continue;
            }

            result.put(slotKey, ItemPredicate.parse(namespace, child));
        }
    }

    private static boolean isSlotGroup(String key) {
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("weapon")
                || normalized.equals("armor")
                || normalized.equals("inventory")
                || normalized.equals("container")
                || normalized.equals("hotbar");
    }

    private static boolean looksLikeItemPredicate(Object raw) {
        return value(raw, "items") != null
                || value(raw, "item") != null
                || value(raw, "count") != null
                || value(raw, "durability") != null
                || section(raw, "components") != null
                || section(raw, "enchantments") != null
                || section(raw, "predicates") != null;
    }

    public boolean matches(Player player) {
        for (Map.Entry<String, ItemPredicate> entry : slots.entrySet()) {
            if (!matchesSlotRange(player, entry.getKey(), entry.getValue())) return false;
        }
        return true;
    }
}
