package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import java.util.List;
import java.util.Map;

import static toutouchien.itemsadderadditions.feature.advancement.AdvancementPredicateSupport.*;

record ItemPredicate(
        List<String> itemIds,
        IntRange count,
        IntRange durability,
        Map<String, Object> components,
        List<EnchantmentPredicate> enchantments,
        boolean unsupportedPredicates
) {
    public static ItemPredicate parse(String namespace, @Nullable Object raw) {
        if (raw == null) return any();
        if (!isSection(raw)) {
            String itemId = string(raw);
            if (itemId == null || itemId.isBlank()) return any();
            return new ItemPredicate(
                    List.of(normalizeItemIdOrTag(namespace, itemId)),
                    IntRange.ANY,
                    IntRange.ANY,
                    Map.of(),
                    List.of(),
                    false
            );
        }

        List<String> ids = readStringList(raw, "items");
        if (ids.isEmpty()) ids = readStringList(raw, "item");
        return new ItemPredicate(
                ids.stream().map(id -> normalizeItemIdOrTag(namespace, id)).toList(),
                IntRange.parse(raw, "count"),
                IntRange.parse(raw, "durability"),
                mapOf(section(raw, "components")),
                parseEnchantments(raw),
                section(raw, "predicates") != null
        );
    }

    public static ItemPredicate any() {
        return new ItemPredicate(List.of(), IntRange.ANY, IntRange.ANY, Map.of(), List.of(), false);
    }

    public boolean matches(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return itemIds.isEmpty() && count.matches(0);

        if (!itemIds.isEmpty()) {
            String actual = NamespaceUtils.itemID(stack);
            boolean matched = false;
            for (String itemId : itemIds) {
                if (itemId.startsWith("#")) continue; // Item tags are not resolved in this lightweight layer.
                if (itemId.equals(actual)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }

        if (!count.matches(stack.getAmount())) return false;
        if (!durability.matches(remainingDurability(stack))) return false;
        if (!matchesComponents(stack)) return false;
        if (!matchesEnchantments(stack)) return false;
        return !unsupportedPredicates;
    }

    private boolean matchesComponents(ItemStack stack) {
        if (components.isEmpty()) return true;
        ItemMeta meta = stack.getItemMeta();
        for (Map.Entry<String, Object> entry : components.entrySet()) {
            String key = NamespaceUtils.normalizeMinecraftID(entry.getKey());
            Object expected = entry.getValue();
            switch (key) {
                case "minecraft:custom_model_data" -> {
                    if (meta == null || !meta.hasCustomModelData()) return false;
                    Integer customModelData = intObject(expected);
                    if (customModelData == null || meta.getCustomModelData() != customModelData) return false;
                }
                case "minecraft:custom_name" -> {
                    if (meta == null || !meta.hasDisplayName()) return false;
                    String expectedName = string(expected);
                    if (expectedName == null || !meta.getDisplayName().equals(expectedName)) return false;
                }
                case "minecraft:damage" -> {
                    Integer damage = intObject(expected);
                    if (!(meta instanceof Damageable damageable) || damage == null || damageable.getDamage() != damage)
                        return false;
                }
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean matchesEnchantments(ItemStack stack) {
        for (EnchantmentPredicate enchantment : enchantments) {
            if (!enchantment.matches(stack)) return false;
        }
        return true;
    }
}

record EnchantmentPredicate(String enchantmentId, IntRange levels) {
    public boolean matches(ItemStack stack) {
        NamespacedKey key = namespacedKey(enchantmentId);
        if (key == null) return false;
        Enchantment enchantment = Enchantment.getByKey(key);
        if (enchantment == null) return false;
        int level = stack.getEnchantmentLevel(enchantment);
        return level > 0 && levels.matches(level);
    }
}
