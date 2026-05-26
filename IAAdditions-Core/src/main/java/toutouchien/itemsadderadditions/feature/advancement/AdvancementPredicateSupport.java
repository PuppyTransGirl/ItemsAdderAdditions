package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import java.lang.reflect.Method;
import java.util.*;

final class AdvancementPredicateSupport {
    private AdvancementPredicateSupport() {
    }

    @Nullable
    static Object unwrapPredicateCondition(@Nullable Object raw) {
        if (raw == null) return null;
        Object condition = value(raw, "condition");
        if (condition == null) return raw;

        String normalizedCondition = NamespaceUtils.normalizeMinecraftID(String.valueOf(condition));
        return switch (normalizedCondition) {
            case "minecraft:entity_properties" -> value(raw, "predicate");
            case "minecraft:location_check" -> {
                Object predicate = value(raw, "predicate");
                if (predicate == null) yield raw;
                Map<String, Object> wrapped = new LinkedHashMap<>();
                wrapped.put("location", predicate);
                yield wrapped;
            }
            default -> value(raw, "predicate") != null ? value(raw, "predicate") : raw;
        };
    }

    @Nullable
    static EntityPredicate parseNullableEntityPredicate(String namespace, @Nullable Object raw) {
        if (raw == null) return null;
        Object normalized = unwrapPredicateCondition(raw);
        return normalized == null ? null : EntityPredicate.parse(namespace, normalized);
    }

    static Map<String, EffectPredicate> parseEffects(@Nullable Object raw) {
        if (raw == null) return Map.of();
        Map<String, EffectPredicate> effects = new LinkedHashMap<>();
        for (String key : keys(raw)) {
            effects.put(NamespaceUtils.normalizeMinecraftID(key), EffectPredicate.parse(sectionOrValue(raw, key)));
        }
        return Map.copyOf(effects);
    }

    static Map<String, ItemPredicate> parseEquipment(String namespace, @Nullable Object raw) {
        if (raw == null) return Map.of();
        Map<String, ItemPredicate> equipment = new LinkedHashMap<>();
        for (String key : keys(raw)) {
            String slot = key.trim().toLowerCase(Locale.ROOT);
            if (!List.of("mainhand", "offhand", "head", "chest", "legs", "feet", "body").contains(slot)) continue;
            equipment.put(slot, ItemPredicate.parse(namespace, sectionOrValue(raw, key)));
        }
        return Map.copyOf(equipment);
    }

    static List<EnchantmentPredicate> parseEnchantments(Object raw) {
        Object enchantmentsRaw = value(raw, "enchantments");
        if (!(enchantmentsRaw instanceof List<?> list)) return List.of();
        List<EnchantmentPredicate> result = new ArrayList<>();
        for (Object entry : list) {
            String enchantment = firstNonBlankString(value(entry, "enchantment"), value(entry, "id"));
            if (enchantment == null) continue;
            result.add(new EnchantmentPredicate(NamespaceUtils.normalizeMinecraftID(enchantment), IntRange.parse(entry, "levels")));
        }
        return List.copyOf(result);
    }

    static Map<String, StringRange> parseStates(@Nullable Object raw) {
        if (raw == null) return Map.of();
        Map<String, StringRange> result = new LinkedHashMap<>();
        for (String key : keys(raw)) {
            Object value = sectionOrValue(raw, key);
            if (isSection(value)) {
                result.put(key, new StringRange(null, string(value(value, "min")), string(value(value, "max"))));
            } else {
                result.put(key, new StringRange(string(value), null, null));
            }
        }
        return Map.copyOf(result);
    }

    static Set<GameMode> parseGameModes(Object raw) {
        List<String> values = readStringList(raw, "gamemode");
        if (values.isEmpty()) return Set.of();
        Set<GameMode> modes = EnumSet.noneOf(GameMode.class);
        for (String value : values) {
            try {
                modes.add(GameMode.valueOf(stripNamespace(value).toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Set.copyOf(modes);
    }

    static Map<NamespacedKey, AdvancementRequirement> parseAdvancements(@Nullable Object raw) {
        if (raw == null) return Map.of();
        Map<NamespacedKey, AdvancementRequirement> result = new LinkedHashMap<>();
        for (String key : keys(raw)) {
            NamespacedKey namespacedKey = namespacedKey(key);
            if (namespacedKey == null) continue;
            Object criteriaRaw = sectionOrValue(raw, key);
            if (isSection(criteriaRaw)) {
                Map<String, Boolean> criteria = new LinkedHashMap<>();
                for (String criterion : keys(criteriaRaw)) {
                    Boolean expected = bool(criteriaRaw, criterion);
                    if (expected != null) criteria.put(criterion, expected);
                }
                result.put(namespacedKey, new AdvancementCriteriaRequirement(Map.copyOf(criteria)));
            } else {
                Boolean expected = booleanObject(criteriaRaw);
                if (expected != null) result.put(namespacedKey, new AdvancementDoneRequirement(expected));
            }
        }
        return Map.copyOf(result);
    }

    static Map<NamespacedKey, Boolean> parseRecipes(@Nullable Object raw) {
        if (raw == null) return Map.of();
        Map<NamespacedKey, Boolean> result = new LinkedHashMap<>();
        for (String key : keys(raw)) {
            NamespacedKey namespacedKey = namespacedKey(key);
            Boolean expected = bool(raw, key);
            if (namespacedKey != null && expected != null) result.put(namespacedKey, expected);
        }
        return Map.copyOf(result);
    }

    static List<StatPredicate> parseStats(Object raw) {
        Object statsRaw = value(raw, "stats");
        if (!(statsRaw instanceof List<?> list)) return List.of();
        List<StatPredicate> result = new ArrayList<>();
        for (Object entry : list) {
            String type = firstNonBlankString(value(entry, "type"));
            String stat = firstNonBlankString(value(entry, "stat"));
            IntRange range = IntRange.parse(entry, "value");
            if (type != null && stat != null) {
                result.add(new StatPredicate(NamespaceUtils.normalizeMinecraftID(type), stat, range));
            }
        }
        return List.copyOf(result);
    }

    @Nullable
    static Entity lookingAt(Player player) {
        Location eye = player.getEyeLocation();
        RayTraceResult result = player.getWorld().rayTraceEntities(
                eye,
                eye.getDirection(),
                100.0D,
                0.3D,
                entity -> !entity.equals(player) && player.hasLineOfSight(entity)
        );
        return result == null ? null : result.getHitEntity();
    }

    static boolean matchesSlotRange(Player player, String rawRange, ItemPredicate predicate) {
        PlayerInventory inv = player.getInventory();
        String range = rawRange.trim().toLowerCase(Locale.ROOT);

        ItemStack named = switch (range) {
            case "mainhand", "weapon.mainhand" -> inv.getItemInMainHand();
            case "offhand", "weapon.offhand" -> inv.getItemInOffHand();
            case "head", "armor.head" -> inv.getHelmet();
            case "chest", "armor.chest" -> inv.getChestplate();
            case "legs", "armor.legs" -> inv.getLeggings();
            case "feet", "armor.feet" -> inv.getBoots();
            default -> null;
        };
        if (named != null || List.of("mainhand", "weapon.mainhand", "offhand", "weapon.offhand", "head", "armor.head", "chest", "armor.chest", "legs", "armor.legs", "feet", "armor.feet").contains(range)) {
            return predicate.matches(named);
        }

        int[] bounds = slotBounds(range);
        if (bounds == null) return false;
        ItemStack[] contents = inv.getContents();
        int min = Math.max(0, bounds[0]);
        int max = Math.min(contents.length - 1, bounds[1]);
        for (int i = min; i <= max; i++) {
            if (predicate.matches(contents[i])) return true;
        }
        return false;
    }

    @Nullable
    static int[] slotBounds(String rawRange) {
        String range = rawRange;
        if (range.equals("*") || range.equals("inventory.*") || range.equals("container.*")) return new int[]{0, 40};
        if (range.startsWith("hotbar.")) range = range.substring("hotbar.".length());
        else if (range.startsWith("container.")) range = range.substring("container.".length());
        else if (range.startsWith("inventory.")) {
            String suffix = range.substring("inventory.".length());
            int[] parsed = parseRange(suffix);
            if (parsed == null) return null;
            return new int[]{parsed[0] + 9, parsed[1] + 9};
        }
        return parseRange(range);
    }

    @Nullable
    static int[] parseRange(String raw) {
        try {
            if (raw.contains("-")) {
                String[] parts = raw.split("-", 2);
                return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
            }
            int exact = Integer.parseInt(raw);
            return new int[]{exact, exact};
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static boolean matchesLimitedNbt(Entity entity, String nbt) {
        String normalized = nbt.trim();
        if (normalized.isBlank()) return true;
        if (normalized.contains("Tags")) {
            for (String tag : entity.getScoreboardTags()) {
                if (normalized.contains('"' + tag + '"') || normalized.contains("'" + tag + "'")) return true;
            }
            return false;
        }
        if (normalized.contains("CustomName") && entity.getCustomName() == null) return false;
        return false;
    }

    static int remainingDurability(ItemStack stack) {
        short maxDurability = stack.getType().getMaxDurability();
        if (maxDurability <= 0) return 0;
        ItemMeta meta = stack.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return maxDurability;
        return Math.max(0, maxDurability - damageable.getDamage());
    }

    @Nullable
    static ItemStack itemFromMethod(Object object, String methodName) {
        try {
            Method method = object.getClass().getMethod(methodName);
            Object value = method.invoke(object);
            return value instanceof ItemStack itemStack ? itemStack : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Nullable
    static Object currentInput(Player player) {
        try {
            Method method = player.getClass().getMethod("getCurrentInput");
            return method.invoke(player);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    static Optional<Boolean> readInputBoolean(Object input, String name) {
        for (String methodName : List.of("is" + capitalize(name), name)) {
            Optional<Boolean> result = reflectBoolean(input, methodName);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    static Optional<Boolean> reflectBoolean(Object object, String methodName) {
        try {
            Method method = object.getClass().getMethod(methodName);
            Object value = method.invoke(object);
            return value instanceof Boolean bool ? Optional.of(bool) : Optional.empty();
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    @Nullable
    static String entityTeam(Entity entity) {
        if (Bukkit.getScoreboardManager() == null) return null;
        String entry = entity instanceof Player player ? player.getName() : entity.getUniqueId().toString();
        if (Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(entry) == null) return null;
        return Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(entry).getName();
    }

    static Location blockRelativeLocation(Location base, double down) {
        Location clone = base.clone();
        clone.setY(clone.getY() - down);
        return clone;
    }

    static boolean matchesDimension(World world, String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> world.getEnvironment() == World.Environment.NORMAL;
            case "minecraft:the_nether" -> world.getEnvironment() == World.Environment.NETHER;
            case "minecraft:the_end" -> world.getEnvironment() == World.Environment.THE_END;
            default -> world.getName().equals(stripNamespace(dimension));
        };
    }

    static boolean canSeeSky(Location loc) {
        World world = loc.getWorld();
        return world != null && world.getHighestBlockYAt(loc) <= loc.getBlockY();
    }

    @Nullable
    static String blockStateValue(String blockData, String key) {
        int bracket = blockData.indexOf('[');
        int end = blockData.indexOf(']');
        if (bracket < 0 || end <= bracket) return null;
        String[] entries = blockData.substring(bracket + 1, end).split(",");
        for (String entry : entries) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2 && parts[0].equals(key)) return parts[1];
        }
        return null;
    }

    static int compareStateValues(String left, String right) {
        try {
            return Double.compare(Double.parseDouble(left), Double.parseDouble(right));
        } catch (NumberFormatException ignored) {
            return left.compareTo(right);
        }
    }

    @Nullable
    static Statistic statisticFor(String type, String stat) {
        try {
            return switch (type) {
                case "minecraft:custom" -> Statistic.valueOf(stripNamespace(stat).toUpperCase(Locale.ROOT));
                case "minecraft:crafted" -> Statistic.CRAFT_ITEM;
                case "minecraft:used" -> Statistic.USE_ITEM;
                case "minecraft:broken" -> Statistic.BREAK_ITEM;
                case "minecraft:mined" -> Statistic.MINE_BLOCK;
                case "minecraft:killed" -> Statistic.KILL_ENTITY;
                case "minecraft:picked_up" -> Statistic.PICKUP;
                case "minecraft:dropped" -> Statistic.DROP;
                case "minecraft:killed_by" -> Statistic.ENTITY_KILLED_BY;
                default -> null;
            };
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Nullable
    static Material material(String normalizedId) {
        String key = stripNamespace(normalizedId).toUpperCase(Locale.ROOT);
        return Material.matchMaterial(key);
    }

    @Nullable
    static EntityType entityType(String normalizedId) {
        for (EntityType type : EntityType.values()) {
            if (type.getKey().toString().equals(normalizedId)) return type;
        }
        return null;
    }

    @Nullable
    static NamespacedKey namespacedKey(String raw) {
        String normalized = raw.contains(":")
                ? raw.trim().toLowerCase(Locale.ROOT)
                : NamespaceUtils.normalizeMinecraftID(raw);
        String[] parts = normalized.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) return null;
        return new NamespacedKey(parts[0], parts[1]);
    }


    static String normalizeItemIdOrTag(String namespace, String raw) {
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("#")) return "#" + NamespaceUtils.normalizeMinecraftID(trimmed.substring(1));
        return NamespaceUtils.normalizeItemID(namespace, trimmed);
    }

    static String normalizeMinecraftIdOrTag(String raw) {
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("#")) return "#" + NamespaceUtils.normalizeMinecraftID(trimmed.substring(1));
        return NamespaceUtils.normalizeMinecraftID(trimmed);
    }

    static String stripNamespace(String raw) {
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        int index = lower.indexOf(':');
        return index >= 0 ? lower.substring(index + 1) : lower;
    }

    static String capitalize(String raw) {
        if (raw.isEmpty()) return raw;
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    static boolean hasAny(Object raw, String... keys) {
        for (String key : keys) {
            if (value(raw, key) != null) return true;
        }
        return false;
    }

    @Nullable
    static Boolean bool(Object raw, String path) {
        return booleanObject(value(raw, path));
    }

    @Nullable
    static Boolean booleanObject(@Nullable Object raw) {
        if (raw instanceof Boolean bool) return bool;
        if (raw instanceof String stringValue && !stringValue.isBlank()) return Boolean.parseBoolean(stringValue);
        return null;
    }

    @Nullable
    static Integer intObject(@Nullable Object raw) {
        if (raw instanceof Number number) return number.intValue();
        if (raw instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Integer.parseInt(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @Nullable
    static Double doubleObject(@Nullable Object raw) {
        if (raw instanceof Number number) return number.doubleValue();
        if (raw instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Double.parseDouble(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @Nullable
    static String string(@Nullable Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }

    @Nullable
    static String emptyToNull(@Nullable String raw) {
        return raw == null || raw.isBlank() ? null : raw;
    }

    @Nullable
    static String firstNonBlankString(@Nullable Object... values) {
        for (Object value : values) {
            String string = string(value);
            if (string != null && !string.isBlank()) return string;
        }
        return null;
    }

    static List<String> readStringList(Object raw, String path) {
        Object value = value(raw, path);
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object entry : list) {
                if (entry != null) result.add(String.valueOf(entry));
            }
            return result;
        }
        if (value instanceof Collection<?> collection) {
            List<String> result = new ArrayList<>();
            for (Object entry : collection) {
                if (entry != null) result.add(String.valueOf(entry));
            }
            return result;
        }
        String single = string(value);
        return single == null || single.isBlank() ? List.of() : List.of(single);
    }

    static boolean isSection(@Nullable Object raw) {
        return raw instanceof ConfigurationSection || raw instanceof Map<?, ?>;
    }

    static Map<String, Object> mapOf(@Nullable Object raw) {
        if (raw == null) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : keys(raw)) {
            Object value = sectionOrValue(raw, key);
            if (value != null) result.put(key, value);
        }
        return Map.copyOf(result);
    }

    @Nullable
    static Object section(Object raw, String key) {
        if (raw instanceof ConfigurationSection sec) {
            // Prefer direct child keys over Bukkit's path lookup. Some vanilla
            // advancement predicate keys intentionally contain dots, e.g.
            // "weapon.mainhand", "inventory.0", or advancement ids.
            Object direct = sec.getValues(false).get(key);
            if (isSection(direct)) return direct;

            ConfigurationSection child = sec.getConfigurationSection(key);
            return child;
        }
        if (raw instanceof Map<?, ?> map) {
            Object value = map.get(key);
            return isSection(value) ? value : null;
        }
        return null;
    }

    @Nullable
    static Object value(Object raw, String key) {
        if (raw instanceof ConfigurationSection sec) {
            // Prefer direct child keys over Bukkit's path lookup. Dotted keys are
            // common in vanilla predicates and must not be split as config paths.
            Map<String, Object> directValues = sec.getValues(false);
            if (directValues.containsKey(key)) return directValues.get(key);
            return sec.get(key);
        }
        if (raw instanceof Map<?, ?> map) return map.get(key);
        return null;
    }

    @Nullable
    static Object sectionOrValue(Object raw, String key) {
        Object section = section(raw, key);
        return section != null ? section : value(raw, key);
    }

    static Set<String> keys(Object raw) {
        if (raw instanceof ConfigurationSection sec) return sec.getKeys(false);
        if (raw instanceof Map<?, ?> map) {
            Set<String> keys = new java.util.LinkedHashSet<>();
            for (Object key : map.keySet()) {
                if (key != null) keys.add(String.valueOf(key));
            }
            return keys;
        }
        return Set.of();
    }
}
