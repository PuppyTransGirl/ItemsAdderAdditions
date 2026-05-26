package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@NullMarked
public record AdvancementPlayerPredicate(
        List<String> types,
        Flags flags,
        Map<String, EffectPredicate> effects,
        Map<String, ItemPredicate> equipment,
        @Nullable LocationPredicate location,
        @Nullable MovementPredicate movement,
        @Nullable String team,
        @Nullable Integer periodicTick,
        PlayerSpecific playerSpecific
) {
    public static final AdvancementPlayerPredicate ANY = new AdvancementPlayerPredicate(
            List.of(), Flags.ANY, Map.of(), Map.of(), null, null, null, null, PlayerSpecific.ANY
    );

    public boolean matches(Player player) {
        if (!matchesType()) return false;
        if (!flags.matches(player)) return false;
        if (!matchesEffects(player)) return false;
        if (!matchesEquipment(player)) return false;
        if (location != null && !location.matches(player)) return false;
        if (movement != null && !movement.matches(player)) return false;
        if (team != null && !team.equals(playerTeam(player))) return false;
        if (periodicTick != null && periodicTick > 0 && player.getTicksLived() % periodicTick != 0) return false;
        return playerSpecific.matches(player);
    }

    public static AdvancementPlayerPredicate parse(String namespace, @Nullable ConfigurationSection sec) {
        if (sec == null) return ANY;

        List<String> types = readStringList(sec, "type").stream()
                .map(AdvancementPlayerPredicate::normalizeMinecraftIdOrTag)
                .toList();
        Flags flags = Flags.parse(sec.getConfigurationSection("flags"));
        Map<String, EffectPredicate> effects = parseEffects(sec.getConfigurationSection("effects"));
        Map<String, ItemPredicate> equipment = parseEquipment(namespace, sec.getConfigurationSection("equipment"));
        LocationPredicate location = LocationPredicate.parse(sec.getConfigurationSection("location"));
        MovementPredicate movement = MovementPredicate.parse(sec.getConfigurationSection("movement"));
        String team = emptyToNull(sec.getString("team"));
        Integer periodicTick = sec.contains("periodic_tick") ? sec.getInt("periodic_tick") : null;
        ConfigurationSection typeSpecificSec = sec.getConfigurationSection("type_specific");
        PlayerSpecific playerSpecific = PlayerSpecific.parse(typeSpecificSec != null ? typeSpecificSec : sec);

        return new AdvancementPlayerPredicate(
                types, flags, effects, equipment, location, movement, team, periodicTick, playerSpecific
        );
    }

    private boolean matchesType() {
        if (types.isEmpty()) return true;
        for (String type : types) {
            if (type.startsWith("#")) continue; // Entity type tags are not resolved by this lightweight predicate layer.
            if (type.equals("minecraft:player")) return true;
        }
        return false;
    }

    private boolean matchesEffects(Player player) {
        for (Map.Entry<String, EffectPredicate> entry : effects.entrySet()) {
            PotionEffect effect = null;
            for (PotionEffect active : player.getActivePotionEffects()) {
                if (active.getType().getKey().toString().equals(entry.getKey())) {
                    effect = active;
                    break;
                }
            }
            if (effect == null || !entry.getValue().matches(effect)) return false;
        }
        return true;
    }

    private boolean matchesEquipment(Player player) {
        PlayerInventory inv = player.getInventory();
        for (Map.Entry<String, ItemPredicate> entry : equipment.entrySet()) {
            ItemStack stack = switch (entry.getKey()) {
                case "mainhand" -> inv.getItemInMainHand();
                case "offhand" -> inv.getItemInOffHand();
                case "head" -> inv.getHelmet();
                case "chest" -> inv.getChestplate();
                case "legs" -> inv.getLeggings();
                case "feet" -> inv.getBoots();
                default -> null;
            };
            if (!entry.getValue().matches(stack)) return false;
        }
        return true;
    }

    @Nullable
    private static String playerTeam(Player player) {
        if (player.getScoreboard().getEntryTeam(player.getName()) == null) return null;
        return player.getScoreboard().getEntryTeam(player.getName()).getName();
    }

    private static Map<String, EffectPredicate> parseEffects(@Nullable ConfigurationSection sec) {
        if (sec == null) return Map.of();
        Map<String, EffectPredicate> effects = new LinkedHashMap<>();
        for (String key : sec.getKeys(false)) {
            ConfigurationSection effectSec = sec.getConfigurationSection(key);
            effects.put(NamespaceUtils.normalizeMinecraftID(key), EffectPredicate.parse(effectSec));
        }
        return Map.copyOf(effects);
    }

    private static Map<String, ItemPredicate> parseEquipment(String namespace, @Nullable ConfigurationSection sec) {
        if (sec == null) return Map.of();
        Map<String, ItemPredicate> equipment = new LinkedHashMap<>();
        for (String key : sec.getKeys(false)) {
            String slot = key.trim().toLowerCase(Locale.ROOT);
            if (!List.of("mainhand", "offhand", "head", "chest", "legs", "feet").contains(slot)) continue;
            equipment.put(slot, ItemPredicate.parse(namespace, sec, key));
        }
        return Map.copyOf(equipment);
    }

    private static String normalizeMinecraftIdOrTag(String raw) {
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("#")) return "#" + NamespaceUtils.normalizeMinecraftID(trimmed.substring(1));
        return NamespaceUtils.normalizeMinecraftID(trimmed);
    }

    @Nullable
    private static String emptyToNull(@Nullable String raw) {
        return raw == null || raw.isBlank() ? null : raw;
    }

    private static List<String> readStringList(ConfigurationSection sec, String path) {
        Object raw = sec.get(path);
        if (raw instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object value : list) {
                if (value != null) result.add(String.valueOf(value));
            }
            return result;
        }
        String single = sec.getString(path);
        return single == null || single.isBlank() ? List.of() : List.of(single);
    }

    public record Flags(
            @Nullable Boolean isBaby,
            @Nullable Boolean isOnFire,
            @Nullable Boolean isSneaking,
            @Nullable Boolean isSprinting,
            @Nullable Boolean isSwimming,
            @Nullable Boolean isOnGround,
            @Nullable Boolean isFlying,
            @Nullable Boolean isFallFlying
    ) {
        public static final Flags ANY = new Flags(null, null, null, null, null, null, null, null);

        public boolean matches(Player player) {
            if (isBaby != null) {
                // Players do not have baby/adult variants, so vanilla treats this as passing.
            }
            if (isOnFire != null && (player.getFireTicks() > 0) != isOnFire) return false;
            if (isSneaking != null && player.isSneaking() != isSneaking) return false;
            if (isSprinting != null && player.isSprinting() != isSprinting) return false;
            if (isSwimming != null && player.isSwimming() != isSwimming) return false;
            if (isOnGround != null && player.isOnGround() != isOnGround) return false;
            if (isFlying != null && (player.isFlying() || player.isGliding()) != isFlying) return false;
            return isFallFlying == null || player.isGliding() == isFallFlying;
        }

        public static Flags parse(@Nullable ConfigurationSection sec) {
            if (sec == null) return ANY;
            return new Flags(
                    bool(sec, "is_baby"),
                    bool(sec, "is_on_fire"),
                    bool(sec, "is_sneaking"),
                    bool(sec, "is_sprinting"),
                    bool(sec, "is_swimming"),
                    bool(sec, "is_on_ground"),
                    bool(sec, "is_flying"),
                    bool(sec, "is_fall_flying")
            );
        }
    }

    public record EffectPredicate(
            IntRange amplifier,
            IntRange duration,
            @Nullable Boolean ambient,
            @Nullable Boolean visible
    ) {
        public static EffectPredicate parse(@Nullable ConfigurationSection sec) {
            if (sec == null) return new EffectPredicate(IntRange.ANY, IntRange.ANY, null, null);
            return new EffectPredicate(
                    IntRange.parse(sec, "amplifier"),
                    IntRange.parse(sec, "duration"),
                    bool(sec, "ambient"),
                    bool(sec, "visible")
            );
        }

        public boolean matches(PotionEffect effect) {
            if (!amplifier.matches(effect.getAmplifier())) return false;
            if (!duration.matches(effect.getDuration())) return false;
            if (ambient != null && effect.isAmbient() != ambient) return false;
            return visible == null || effect.hasParticles() == visible;
        }
    }

    public record ItemPredicate(List<String> itemIds) {
        public static ItemPredicate parse(String namespace, ConfigurationSection parent, String key) {
            ConfigurationSection sec = parent.getConfigurationSection(key);
            if (sec == null) {
                String itemId = parent.getString(key);
                if (itemId == null || itemId.isBlank()) return new ItemPredicate(List.of());
                return new ItemPredicate(List.of(NamespaceUtils.normalizeItemID(namespace, itemId)));
            }

            List<String> ids = readStringList(sec, "items");
            if (ids.isEmpty()) ids = readStringList(sec, "item");
            return new ItemPredicate(ids.stream()
                    .map(id -> NamespaceUtils.normalizeItemID(namespace, id))
                    .toList());
        }

        public boolean matches(@Nullable ItemStack stack) {
            if (itemIds.isEmpty()) return true;
            String actual = NamespaceUtils.itemID(stack);
            return actual != null && itemIds.contains(actual);
        }
    }

    public record LocationPredicate(
            @Nullable String world,
            @Nullable String biome,
            DoubleRange x,
            DoubleRange y,
            DoubleRange z
    ) {
        @Nullable
        public static LocationPredicate parse(@Nullable ConfigurationSection sec) {
            if (sec == null) return null;
            return new LocationPredicate(
                    emptyToNull(sec.getString("world")),
                    NamespaceUtils.normalizeMinecraftIDNullable(sec.getString("biome")),
                    DoubleRange.parse(sec, "x"),
                    DoubleRange.parse(sec, "y"),
                    DoubleRange.parse(sec, "z")
            );
        }

        public boolean matches(Player player) {
            Location loc = player.getLocation();
            if (world != null && !loc.getWorld().getName().equals(world)) return false;
            if (biome != null && !loc.getBlock().getBiome().getKey().toString().equals(biome)) return false;
            return x.matches(loc.getX()) && y.matches(loc.getY()) && z.matches(loc.getZ());
        }
    }

    public record MovementPredicate(
            DoubleRange x,
            DoubleRange y,
            DoubleRange z,
            DoubleRange speed,
            DoubleRange horizontalSpeed,
            DoubleRange verticalSpeed,
            DoubleRange fallDistance
    ) {
        @Nullable
        public static MovementPredicate parse(@Nullable ConfigurationSection sec) {
            if (sec == null) return null;
            return new MovementPredicate(
                    DoubleRange.parse(sec, "x"),
                    DoubleRange.parse(sec, "y"),
                    DoubleRange.parse(sec, "z"),
                    DoubleRange.parse(sec, "speed"),
                    DoubleRange.parse(sec, "horizontal_speed"),
                    DoubleRange.parse(sec, "vertical_speed"),
                    DoubleRange.parse(sec, "fall_distance")
            );
        }

        public boolean matches(Player player) {
            Vector velocity = player.getVelocity();
            double vx = velocity.getX() * 20.0D;
            double vy = velocity.getY() * 20.0D;
            double vz = velocity.getZ() * 20.0D;
            double horizontal = Math.sqrt(vx * vx + vz * vz);
            double total = Math.sqrt(vx * vx + vy * vy + vz * vz);

            return x.matches(vx)
                    && y.matches(vy)
                    && z.matches(vz)
                    && speed.matches(total)
                    && horizontalSpeed.matches(horizontal)
                    && verticalSpeed.matches(Math.abs(vy))
                    && fallDistance.matches(player.getFallDistance());
        }
    }

    public record PlayerSpecific(
            boolean requiredPlayerType,
            Set<GameMode> gameModes,
            IntRange level,
            @Nullable FoodPredicate food,
            Map<NamespacedKey, AdvancementRequirement> advancements,
            Map<NamespacedKey, Boolean> recipes,
            List<StatPredicate> stats
    ) {
        public static final PlayerSpecific ANY = new PlayerSpecific(
                false, Set.of(), IntRange.ANY, null, Map.of(), Map.of(), List.of()
        );

        public static PlayerSpecific parse(@Nullable ConfigurationSection sec) {
            if (sec == null) return ANY;
            String type = NamespaceUtils.normalizeMinecraftID(sec.getString("type", "player"));
            boolean requiredPlayerType = sec.contains("type");
            if (requiredPlayerType && !type.equals("minecraft:player")) {
                return new PlayerSpecific(true, Set.of(), IntRange.NONE, null, Map.of(), Map.of(), List.of());
            }

            Set<GameMode> gameModes = parseGameModes(sec);
            IntRange level = IntRange.parse(sec, "level");
            FoodPredicate food = FoodPredicate.parse(sec.getConfigurationSection("food"));
            Map<NamespacedKey, AdvancementRequirement> advancements = parseAdvancements(sec.getConfigurationSection("advancements"));
            Map<NamespacedKey, Boolean> recipes = parseRecipes(sec.getConfigurationSection("recipes"));
            List<StatPredicate> stats = parseStats(sec);

            return new PlayerSpecific(requiredPlayerType, gameModes, level, food, advancements, recipes, stats);
        }

        public boolean matches(Player player) {
            if (requiredPlayerType) {
                // The predicate is attached to an actual Player instance, so minecraft:player has already passed.
            }
            if (!gameModes.isEmpty() && !gameModes.contains(player.getGameMode())) return false;
            if (!level.matches(player.getLevel())) return false;
            if (food != null && !food.matches(player)) return false;
            if (!matchesAdvancements(player)) return false;
            if (!matchesRecipes(player)) return false;
            return matchesStats(player);
        }

        private boolean matchesAdvancements(Player player) {
            for (Map.Entry<NamespacedKey, AdvancementRequirement> entry : advancements.entrySet()) {
                Advancement advancement = Bukkit.getAdvancement(entry.getKey());
                if (advancement == null) return false;
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                if (!entry.getValue().matches(progress)) return false;
            }
            return true;
        }

        private boolean matchesRecipes(Player player) {
            for (Map.Entry<NamespacedKey, Boolean> entry : recipes.entrySet()) {
                if (player.hasDiscoveredRecipe(entry.getKey()) != entry.getValue()) return false;
            }
            return true;
        }

        private boolean matchesStats(Player player) {
            for (StatPredicate stat : stats) {
                if (!stat.matches(player)) return false;
            }
            return true;
        }
    }

    public record FoodPredicate(IntRange level, DoubleRange saturation) {
        @Nullable
        public static FoodPredicate parse(@Nullable ConfigurationSection sec) {
            if (sec == null) return null;
            return new FoodPredicate(IntRange.parse(sec, "level"), DoubleRange.parse(sec, "saturation"));
        }

        public boolean matches(Player player) {
            return level.matches(player.getFoodLevel()) && saturation.matches(player.getSaturation());
        }
    }

    public interface AdvancementRequirement {
        boolean matches(AdvancementProgress progress);
    }

    public record AdvancementDoneRequirement(boolean done) implements AdvancementRequirement {
        @Override
        public boolean matches(AdvancementProgress progress) {
            return progress.isDone() == done;
        }
    }

    public record AdvancementCriteriaRequirement(Map<String, Boolean> criteria) implements AdvancementRequirement {
        @Override
        public boolean matches(AdvancementProgress progress) {
            for (Map.Entry<String, Boolean> entry : criteria.entrySet()) {
                boolean awarded = progress.getAwardedCriteria().contains(entry.getKey());
                if (awarded != entry.getValue()) return false;
            }
            return true;
        }
    }

    public record StatPredicate(String type, String stat, IntRange value) {
        public boolean matches(Player player) {
            Statistic statistic = statisticFor(type, stat);
            if (statistic == null) return false;

            int actual;
            String normalizedStat = NamespaceUtils.normalizeMinecraftID(stat);
            if (type.equals("minecraft:custom")) {
                actual = player.getStatistic(statistic);
            } else if (type.equals("minecraft:killed") || type.equals("minecraft:killed_by")) {
                EntityType entityType = entityType(normalizedStat);
                if (entityType == null) return false;
                actual = player.getStatistic(statistic, entityType);
            } else {
                Material material = material(normalizedStat);
                if (material == null) return false;
                actual = player.getStatistic(statistic, material);
            }
            return value.matches(actual);
        }
    }

    public record IntRange(@Nullable Integer min, @Nullable Integer max) {
        public static final IntRange ANY = new IntRange(null, null);
        public static final IntRange NONE = new IntRange(1, 0);

        public boolean matches(int value) {
            return (min == null || value >= min) && (max == null || value <= max);
        }

        public static IntRange parse(@Nullable ConfigurationSection sec, String path) {
            if (sec == null || !sec.contains(path)) return ANY;
            ConfigurationSection rangeSec = sec.getConfigurationSection(path);
            if (rangeSec != null) return new IntRange(
                    rangeSec.contains("min") ? rangeSec.getInt("min") : null,
                    rangeSec.contains("max") ? rangeSec.getInt("max") : null
            );
            int exact = sec.getInt(path);
            return new IntRange(exact, exact);
        }
    }

    public record DoubleRange(@Nullable Double min, @Nullable Double max) {
        public static final DoubleRange ANY = new DoubleRange(null, null);
        public static final DoubleRange NONE = new DoubleRange(1.0D, 0.0D);

        public boolean matches(double value) {
            return (min == null || value >= min) && (max == null || value <= max);
        }

        public static DoubleRange parse(@Nullable ConfigurationSection sec, String path) {
            if (sec == null || !sec.contains(path)) return ANY;
            ConfigurationSection rangeSec = sec.getConfigurationSection(path);
            if (rangeSec != null) return new DoubleRange(
                    rangeSec.contains("min") ? rangeSec.getDouble("min") : null,
                    rangeSec.contains("max") ? rangeSec.getDouble("max") : null
            );
            double exact = sec.getDouble(path);
            return new DoubleRange(exact, exact);
        }
    }

    @Nullable
    private static Boolean bool(ConfigurationSection sec, String path) {
        return sec.contains(path) ? sec.getBoolean(path) : null;
    }

    private static Set<GameMode> parseGameModes(ConfigurationSection sec) {
        List<String> values = readStringList(sec, "gamemode");
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

    private static Map<NamespacedKey, AdvancementRequirement> parseAdvancements(@Nullable ConfigurationSection sec) {
        if (sec == null) return Map.of();
        Map<NamespacedKey, AdvancementRequirement> result = new LinkedHashMap<>();
        for (String key : sec.getKeys(false)) {
            NamespacedKey namespacedKey = namespacedKey(key);
            if (namespacedKey == null) continue;
            ConfigurationSection criteriaSec = sec.getConfigurationSection(key);
            if (criteriaSec != null) {
                Map<String, Boolean> criteria = new LinkedHashMap<>();
                for (String criterion : criteriaSec.getKeys(false)) {
                    criteria.put(criterion, criteriaSec.getBoolean(criterion));
                }
                result.put(namespacedKey, new AdvancementCriteriaRequirement(Map.copyOf(criteria)));
            } else {
                result.put(namespacedKey, new AdvancementDoneRequirement(sec.getBoolean(key)));
            }
        }
        return Map.copyOf(result);
    }

    private static Map<NamespacedKey, Boolean> parseRecipes(@Nullable ConfigurationSection sec) {
        if (sec == null) return Map.of();
        Map<NamespacedKey, Boolean> result = new LinkedHashMap<>();
        for (String key : sec.getKeys(false)) {
            NamespacedKey namespacedKey = namespacedKey(key);
            if (namespacedKey != null) result.put(namespacedKey, sec.getBoolean(key));
        }
        return Map.copyOf(result);
    }

    private static List<StatPredicate> parseStats(ConfigurationSection sec) {
        Object raw = sec.get("stats");
        if (!(raw instanceof List<?> list)) return List.of();
        List<StatPredicate> result = new ArrayList<>();
        for (Object value : list) {
            if (!(value instanceof Map<?, ?> map)) continue;
            String type = string(map.get("type"));
            String stat = string(map.get("stat"));
            Object rawValue = map.get("value");
            IntRange range = IntRange.ANY;
            if (rawValue instanceof Number number) {
                int exact = number.intValue();
                range = new IntRange(exact, exact);
            } else if (rawValue instanceof Map<?, ?> rangeMap) {
                Integer min = rangeMap.get("min") instanceof Number minNumber ? minNumber.intValue() : null;
                Integer max = rangeMap.get("max") instanceof Number maxNumber ? maxNumber.intValue() : null;
                range = new IntRange(min, max);
            }
            if (type != null && stat != null) {
                result.add(new StatPredicate(NamespaceUtils.normalizeMinecraftID(type), stat, range));
            }
        }
        return List.copyOf(result);
    }

    @Nullable
    private static String string(@Nullable Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @Nullable
    private static Statistic statisticFor(String type, String stat) {
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
    private static Material material(String normalizedId) {
        String key = stripNamespace(normalizedId).toUpperCase(Locale.ROOT);
        return Material.matchMaterial(key);
    }

    @Nullable
    private static EntityType entityType(String normalizedId) {
        for (EntityType type : EntityType.values()) {
            if (type.getKey().toString().equals(normalizedId)) return type;
        }
        return null;
    }

    @Nullable
    private static NamespacedKey namespacedKey(String raw) {
        String normalized = raw.contains(":")
                ? raw.trim().toLowerCase(Locale.ROOT)
                : NamespaceUtils.normalizeMinecraftID(raw);
        String[] parts = normalized.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) return null;
        return new NamespacedKey(parts[0], parts[1]);
    }

    private static String stripNamespace(String raw) {
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        int index = lower.indexOf(':');
        return index >= 0 ? lower.substring(index + 1) : lower;
    }
}
