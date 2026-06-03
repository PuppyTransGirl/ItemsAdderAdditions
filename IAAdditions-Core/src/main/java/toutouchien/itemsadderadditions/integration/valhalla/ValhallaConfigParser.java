package toutouchien.itemsadderadditions.integration.valhalla;

import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.*;

/**
 * Parses a {@code valhalla:} YAML section into {@link ValhallaItemData}.
 *
 * <h3>YAML structure</h3>
 * <pre>
 * items:
 *   my_item:
 *     valhalla:
 *       stats:
 *         - stat: GENERIC_MOVEMENT_SPEED
 *           amount: 0.3
 *           operation: ADD_SCALAR
 *           hidden: false
 *       equipment_class: TRINKET
 *       item_flags:
 *         - DISPLAY_ATTRIBUTES
 *       trinkets:
 *         trinket_id: 7
 *         trinket_unique_id: 529
 *         unique: true
 * </pre>
 *
 * <p>{@code stats} writes both {@code actual_stats} and {@code default_stats}.
 * Explicit {@code actual_stats} or {@code default_stats} override the respective field.
 */
@NullMarked
public final class ValhallaConfigParser {
    static final String LOG_TAG = "Valhalla";

    static final Set<String> VALID_OPERATIONS = Set.of("ADD_NUMBER", "ADD_SCALAR", "MULTIPLY_SCALAR_1");

    static final Set<String> VALID_EQUIPMENT_CLASSES = Set.of(
            "SWORD", "SPEAR", "BOW", "CROSSBOW", "TRIDENT", "MACE",
            "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS",
            "SHEARS", "FLINT_AND_STEEL", "FISHING_ROD", "ELYTRA",
            "PICKAXE", "AXE", "SHOVEL", "HOE", "SHIELD", "OTHER", "TRINKET"
    );

    static final Set<String> VALID_FLAGS = Set.of(
            "HIDE_TAGS", "HIDE_QUALITY", "DISPLAY_ATTRIBUTES", "HIDE_DURABILITY",
            "ATTRIBUTE_FOR_BOTH_HANDS", "ATTRIBUTE_FOR_HELMET", "INFINITY_EXPLOITABLE",
            "UNCRAFTABLE", "TEMPORARY_POTION_DISPLAY", "UNENCHANTABLE", "UNMENDABLE"
    );

    private ValhallaConfigParser() {
    }

    /**
     * Parses the {@code valhalla:} section directly.
     *
     * @param section      the {@code valhalla:} config section
     * @param namespacedId the item namespaced ID (used in log messages)
     * @return parsed data, or {@code null} if no valid data was found
     */
    @Nullable
    public static ValhallaItemData parse(ConfigurationSection section, String namespacedId) {
        boolean hasStatsKey = section.contains("stats");
        boolean hasActualKey = section.contains("actual_stats");
        boolean hasDefaultKey = section.contains("default_stats");

        List<ValhallaStatEntry> stats = hasStatsKey
                ? parseStatList(section.getList("stats"), "stats", namespacedId)
                : List.of();
        List<ValhallaStatEntry> actualStats = hasActualKey
                ? parseStatList(section.getList("actual_stats"), "actual_stats", namespacedId)
                : stats;
        List<ValhallaStatEntry> defaultStats = hasDefaultKey
                ? parseStatList(section.getList("default_stats"), "default_stats", namespacedId)
                : stats;

        String equipmentClass = parseEquipmentClass(section, namespacedId);
        List<String> itemFlags = parseFlags(section, namespacedId);
        List<ValhallaPermanentEffect> permanentEffects = parsePermanentEffects(section, namespacedId);
        ValhallaPermanentEffectCooldown permanentEffectCooldown = parsePermanentEffectCooldown(section, namespacedId);
        ValhallaTrinketData trinkets = parseTrinkets(section.getConfigurationSection("trinkets"), namespacedId);

        ValhallaItemData data = new ValhallaItemData(
                actualStats,
                defaultStats,
                equipmentClass,
                itemFlags,
                permanentEffects,
                permanentEffectCooldown,
                trinkets
        );
        return data.isEmpty() ? null : data;
    }

    static List<ValhallaStatEntry> parseStatList(
            @Nullable List<?> rawList,
            String fieldName,
            String namespacedId
    ) {
        if (rawList == null || rawList.isEmpty()) return List.of();

        List<ValhallaStatEntry> entries = new ArrayList<>(rawList.size());
        Set<String> seen = new HashSet<>();

        for (int i = 0; i < rawList.size(); i++) {
            Object raw = rawList.get(i);
            if (!(raw instanceof Map<?, ?> map)) {
                Log.itemWarn(LOG_TAG, namespacedId,
                        "{}[{}]: expected a section with stat/amount/operation - skipping.", fieldName, i);
                continue;
            }

            String stat = readString(map, "stat");
            if (stat == null || stat.isBlank()) {
                Log.itemWarn(LOG_TAG, namespacedId, "{}[{}]: missing or blank 'stat' - skipping.", fieldName, i);
                continue;
            }
            stat = stat.toUpperCase(Locale.ROOT);

            if (stat.indexOf(':') >= 0 || stat.indexOf(';') >= 0) {
                Log.itemWarn(LOG_TAG, namespacedId, "{}[{}]: stat '{}' contains ':' or ';' - skipping.", fieldName, i, stat);
                continue;
            }

            if (!ValhallaKnownValues.STATS.contains(stat)) {
                Log.itemWarn(LOG_TAG, namespacedId, "{}[{}]: unknown stat '{}' - skipping.", fieldName, i, stat);
                continue;
            }

            if (!seen.add(stat)) {
                Log.itemWarn(LOG_TAG, namespacedId, "{}[{}]: duplicate stat '{}' - skipping.", fieldName, i, stat);
                continue;
            }

            Object rawAmount = map.get("amount");
            if (rawAmount == null) {
                Log.itemWarn(LOG_TAG, namespacedId, "{}[{}] stat '{}': missing 'amount' - skipping.", fieldName, i, stat);
                continue;
            }

            double amount;
            if (rawAmount instanceof Number n) {
                amount = n.doubleValue();
            } else {
                Log.itemWarn(LOG_TAG, namespacedId, "{}[{}] stat '{}': 'amount' must be a number - skipping.", fieldName, i, stat);
                continue;
            }

            if (!Double.isFinite(amount)) {
                Log.itemWarn(LOG_TAG, namespacedId, "{}[{}] stat '{}': 'amount' must be finite - skipping.", fieldName, i, stat);
                continue;
            }

            String operation = readString(map, "operation");
            if (operation == null || operation.isBlank()) {
                Log.itemWarn(LOG_TAG, namespacedId, "{}[{}] stat '{}': missing 'operation' - skipping.", fieldName, i, stat);
                continue;
            }
            operation = operation.toUpperCase(Locale.ROOT);
            if (!VALID_OPERATIONS.contains(operation)) {
                Log.itemWarn(LOG_TAG, namespacedId,
                        "{}[{}] stat '{}': unknown operation '{}' - valid: ADD_NUMBER, ADD_SCALAR, MULTIPLY_SCALAR_1 - skipping.",
                        fieldName, i, stat, operation);
                continue;
            }

            boolean hidden = false;
            Object rawHidden = map.get("hidden");
            if (rawHidden != null) {
                if (rawHidden instanceof Boolean b) {
                    hidden = b;
                } else {
                    Log.itemWarn(LOG_TAG, namespacedId,
                            "{}[{}] stat '{}': 'hidden' must be a boolean - defaulting to false.", fieldName, i, stat);
                }
            }

            entries.add(new ValhallaStatEntry(stat, amount, operation, hidden));
        }

        return List.copyOf(entries);
    }

    @Nullable
    private static String parseEquipmentClass(ConfigurationSection section, String namespacedId) {
        if (!section.contains("equipment_class")) return null;
        String value = section.getString("equipment_class");
        if (value == null || value.isBlank()) {
            Log.itemWarn(LOG_TAG, namespacedId, "equipment_class is blank - skipping.");
            return null;
        }

        String upper = value.toUpperCase(Locale.ROOT);
        if (!VALID_EQUIPMENT_CLASSES.contains(upper)) {
            Log.itemWarn(LOG_TAG, namespacedId,
                    "Unknown equipment_class '{}' - valid values: {} - skipping.",
                    value, String.join(", ", VALID_EQUIPMENT_CLASSES));
            return null;
        }
        return upper;
    }

    private static List<String> parseFlags(ConfigurationSection section, String namespacedId) {
        if (!section.contains("item_flags")) return List.of();
        List<String> raw = section.getStringList("item_flags");
        if (raw.isEmpty()) return List.of();

        List<String> flags = new ArrayList<>(raw.size());
        Set<String> seen = new HashSet<>();

        for (String entry : raw) {
            if (entry == null || entry.isBlank()) continue;
            String upper = entry.toUpperCase(Locale.ROOT);
            if (!VALID_FLAGS.contains(upper)) {
                Log.itemWarn(LOG_TAG, namespacedId, "Unknown item_flag '{}' - skipping.", entry);
                continue;
            }
            if (!seen.add(upper)) continue;
            flags.add(upper);
        }

        return List.copyOf(flags);
    }

    private static List<ValhallaPermanentEffect> parsePermanentEffects(ConfigurationSection section, String namespacedId) {
        List<ValhallaPermanentEffect> effects = new ArrayList<>();

        ConfigurationSection permanentEffectsSection = section.getConfigurationSection("permanent_effects");
        if (permanentEffectsSection != null) {
            effects.addAll(parsePermanentEffectList(permanentEffectsSection.getList("effects"),
                    "permanent_effects.effects", namespacedId));
        }

        if (section.contains("permanent_potion_effects")) {
            effects.addAll(parsePermanentEffectList(section.getList("permanent_potion_effects"),
                    "permanent_potion_effects", namespacedId));
        }

        return effects.isEmpty() ? List.of() : List.copyOf(effects);
    }

    static List<ValhallaPermanentEffect> parsePermanentEffectList(
            @Nullable List<?> rawList,
            String fieldName,
            String namespacedId
    ) {
        if (rawList == null || rawList.isEmpty()) return List.of();

        List<ValhallaPermanentEffect> effects = new ArrayList<>(rawList.size());
        for (int i = 0; i < rawList.size(); i++) {
            Object raw = rawList.get(i);
            if (!(raw instanceof Map<?, ?> map)) {
                Log.itemWarn(LOG_TAG, namespacedId,
                        "{}[{}]: expected a section with type/amplifier/duration/condition - skipping.",
                        fieldName, i);
                continue;
            }

            String effect = readString(map, "type");
            if (effect == null || effect.isBlank()) {
                effect = readString(map, "effect");
            }
            if (effect == null || effect.isBlank()) {
                Log.itemWarn(LOG_TAG, namespacedId, "{}[{}]: missing or blank 'type' - skipping.", fieldName, i);
                continue;
            }
            effect = effect.toUpperCase(Locale.ROOT);

            if (effect.indexOf(':') >= 0 || effect.indexOf(';') >= 0) {
                Log.itemWarn(LOG_TAG, namespacedId, "{}[{}]: type '{}' contains ':' or ';' - skipping.", fieldName, i, effect);
                continue;
            }

            if (!ValhallaKnownValues.PERMANENT_EFFECTS.contains(effect)
                    && !ValhallaKnownValues.STATS.contains(effect)) {
                Log.itemWarn(LOG_TAG, namespacedId, "{}[{}]: unknown permanent effect type '{}' - skipping.", fieldName, i, effect);
                continue;
            }

            Double amplifier = readDouble(map.get("amplifier"), fieldName + "[" + i + "] type '" + effect + "'", "amplifier", namespacedId);
            if (amplifier == null) continue;

            Integer duration = readInt(map.get("duration"), fieldName + "[" + i + "] type '" + effect + "'", "duration", namespacedId);
            if (duration == null) continue;
            if (duration <= 0) {
                Log.itemWarn(LOG_TAG, namespacedId, "{}[{}] type '{}': 'duration' must be greater than 0 - skipping.",
                        fieldName, i, effect);
                continue;
            }

            String condition = readString(map, "condition");
            if (condition == null || condition.isBlank()) condition = "constant";
            condition = condition.trim();
            if (condition.indexOf(':') >= 0 || condition.indexOf(';') >= 0) {
                Log.itemWarn(LOG_TAG, namespacedId, "{}[{}] type '{}': 'condition' contains ':' or ';' - skipping.",
                        fieldName, i, effect);
                continue;
            }

            effects.add(new ValhallaPermanentEffect(effect, amplifier, duration, condition));
        }

        return List.copyOf(effects);
    }

    @Nullable
    private static ValhallaPermanentEffectCooldown parsePermanentEffectCooldown(
            ConfigurationSection section,
            String namespacedId
    ) {
        ConfigurationSection cooldownSection = null;

        ConfigurationSection permanentEffects = section.getConfigurationSection("permanent_effects");
        if (permanentEffects != null) {
            cooldownSection = permanentEffects.getConfigurationSection("cooldown_properties");
        }

        ConfigurationSection aliasSection = section.getConfigurationSection("permanent_effects_cooldown_properties");
        if (aliasSection != null) cooldownSection = aliasSection;

        if (cooldownSection == null) return null;

        Boolean cdrAffected = readBoolean(cooldownSection.get("cdr_affected"));
        if (cdrAffected == null) cdrAffected = readBoolean(cooldownSection.get("cdrAffected"));
        if (cdrAffected == null) cdrAffected = false;

        Integer cooldown = readInt(cooldownSection.get("cooldown"),
                "permanent_effects_cooldown_properties", "cooldown", namespacedId);
        if (cooldown == null) return null;
        if (cooldown < 0) {
            Log.itemWarn(LOG_TAG, namespacedId,
                    "permanent_effects_cooldown_properties.cooldown must be non-negative - skipping.");
            return null;
        }

        return new ValhallaPermanentEffectCooldown(cdrAffected, cooldown);
    }

    @Nullable
    private static ValhallaTrinketData parseTrinkets(
            @Nullable ConfigurationSection section,
            String namespacedId
    ) {
        if (section == null) return null;

        Integer trinketId = null;
        Integer trinketUniqueId = null;
        Boolean unique = null;
        Boolean unstackable = null;

        if (section.contains("trinket_id")) {
            trinketId = readStrictInt(section, "trinket_id", namespacedId);
        }

        if (section.contains("trinket_unique_id")) {
            trinketUniqueId = readStrictInt(section, "trinket_unique_id", namespacedId);
        }

        if (section.contains("unique")) {
            unique = readUnique(section.get("unique"), namespacedId);
        }

        if (section.contains("unstackable")) {
            unstackable = readBoolean(section.get("unstackable"));
            if (unstackable == null) {
                Log.itemWarn(LOG_TAG, namespacedId, "trinkets.unstackable must be true/false - skipping.");
            }
        }

        ValhallaTrinketData data = new ValhallaTrinketData(trinketId, trinketUniqueId, unique, unstackable);
        return data.isEmpty() ? null : data;
    }

    @Nullable
    private static Integer readStrictInt(ConfigurationSection section, String path, String namespacedId) {
        Object raw = section.get(path);
        if (!(raw instanceof Number n)) {
            Log.itemWarn(LOG_TAG, namespacedId, "trinkets.{} must be an integer - skipping.", path);
            return null;
        }

        double d = n.doubleValue();
        if (!Double.isFinite(d) || d % 1.0 != 0.0 || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
            Log.itemWarn(LOG_TAG, namespacedId, "trinkets.{} must be a whole 32-bit integer - skipping.", path);
            return null;
        }

        int value = n.intValue();
        if (value < 0) {
            Log.itemWarn(LOG_TAG, namespacedId, "trinkets.{} must be non-negative - skipping.", path);
            return null;
        }

        return value;
    }

    @Nullable
    private static Boolean readUnique(Object raw, String namespacedId) {
        switch (raw) {
            case Boolean b -> {
                return b;
            }

            case Number n -> {
                double d = n.doubleValue();
                if (Double.isFinite(d) && d == 1.0) return true;
                if (Double.isFinite(d) && d == 0.0) return false;
                Log.itemWarn(LOG_TAG, namespacedId, "trinkets.unique must be true/false, 1/0, or '1b'/'0b' - skipping.");
                return null;
            }

            case String s -> {
                String normalized = s.trim().toLowerCase(Locale.ROOT);
                return switch (normalized) {

                    case "true", "1", "1b" -> true;
                    case "false", "0", "0b" -> false;
                    default -> {
                        Log.itemWarn(LOG_TAG, namespacedId, "trinkets.unique must be true/false, 1/0, or '1b'/'0b' - skipping.");
                        yield null;
                    }
                };
            }

            default -> {
            }
        }

        Log.itemWarn(LOG_TAG, namespacedId, "trinkets.unique must be true/false, 1/0, or '1b'/'0b' - skipping.");
        return null;
    }

    @Nullable
    private static Double readDouble(@Nullable Object raw, String fieldLabel, String key, String namespacedId) {
        if (!(raw instanceof Number n)) {
            Log.itemWarn(LOG_TAG, namespacedId, "{}: '{}' must be a number - skipping.", fieldLabel, key);
            return null;
        }

        double value = n.doubleValue();
        if (!Double.isFinite(value)) {
            Log.itemWarn(LOG_TAG, namespacedId, "{}: '{}' must be finite - skipping.", fieldLabel, key);
            return null;
        }

        return value;
    }

    @Nullable
    private static Integer readInt(@Nullable Object raw, String fieldLabel, String key, String namespacedId) {
        if (!(raw instanceof Number n)) {
            Log.itemWarn(LOG_TAG, namespacedId, "{}: '{}' must be an integer - skipping.", fieldLabel, key);
            return null;
        }

        double d = n.doubleValue();
        if (!Double.isFinite(d) || d % 1.0 != 0.0 || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
            Log.itemWarn(LOG_TAG, namespacedId, "{}: '{}' must be a whole 32-bit integer - skipping.", fieldLabel, key);
            return null;
        }

        return n.intValue();
    }

    @Nullable
    private static Boolean readBoolean(@Nullable Object raw) {
        if (raw instanceof Boolean b) return b;
        if (raw instanceof String s) {
            return switch (s.trim().toLowerCase(Locale.ROOT)) {
                case "true" -> true;
                case "false" -> false;
                default -> null;
            };
        }
        return null;
    }

    @Nullable
    private static String readString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value instanceof String s ? s : null;
    }
}
