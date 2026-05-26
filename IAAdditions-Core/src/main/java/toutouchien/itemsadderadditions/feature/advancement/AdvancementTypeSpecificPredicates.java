package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.*;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static toutouchien.itemsadderadditions.feature.advancement.AdvancementPredicateSupport.*;

record TypeSpecificPredicate(
        String type,
        @Nullable PlayerSpecific player,
        @Nullable SheepSpecific sheep,
        @Nullable SlimeSpecific slime,
        @Nullable RaiderSpecific raider,
        @Nullable FishingHookSpecific fishingHook
) {
    @Nullable
    public static TypeSpecificPredicate parse(String namespace, @Nullable Object typeSpecificRaw, Object parentRaw) {
        Object raw = typeSpecificRaw != null ? typeSpecificRaw : parentRaw;
        if (raw == null) return null;

        String type = NamespaceUtils.normalizeMinecraftID(string(value(raw, "type")) != null ? string(value(raw, "type")) : "minecraft:player");
        boolean explicitTypeSpecific = typeSpecificRaw != null && value(raw, "type") != null;
        boolean hasPlayerFields = hasAny(raw, "looking_at", "advancements", "gamemode", "level", "recipes", "stats", "input", "food");
        boolean hasOtherFields = hasAny(raw, "sheared", "size", "is_captain", "has_raid", "in_open_water");
        if (!explicitTypeSpecific && !hasPlayerFields && !hasOtherFields) return null;

        return switch (type) {
            case "minecraft:player" ->
                    new TypeSpecificPredicate(type, PlayerSpecific.parse(namespace, raw), null, null, null, null);
            case "minecraft:sheep" -> new TypeSpecificPredicate(type, null, SheepSpecific.parse(raw), null, null, null);
            case "minecraft:slime", "minecraft:magma_cube", "minecraft:cube_mob" ->
                    new TypeSpecificPredicate(type, null, null, SlimeSpecific.parse(raw), null, null);
            case "minecraft:raider" ->
                    new TypeSpecificPredicate(type, null, null, null, RaiderSpecific.parse(raw), null);
            case "minecraft:fishing_hook" ->
                    new TypeSpecificPredicate(type, null, null, null, null, FishingHookSpecific.parse(raw));
            default -> new TypeSpecificPredicate(type, null, null, null, null, null);
        };
    }

    public boolean matches(Entity entity, Location origin, Player contextPlayer) {
        return switch (type) {
            case "minecraft:player" ->
                    entity instanceof Player playerEntity && player != null && player.matches(playerEntity, origin, contextPlayer);
            case "minecraft:sheep" ->
                    entity instanceof Sheep sheepEntity && sheep != null && sheep.matches(sheepEntity);
            case "minecraft:slime", "minecraft:magma_cube", "minecraft:cube_mob" ->
                    entity instanceof Slime slimeEntity && slime != null && slime.matches(slimeEntity);
            case "minecraft:raider" -> raider != null && raider.matches(entity);
            case "minecraft:fishing_hook" -> fishingHook != null && fishingHook.matches(entity);
            default -> false;
        };
    }
}

record PlayerSpecific(
        @Nullable EntityPredicate lookingAt,
        Set<GameMode> gameModes,
        IntRange level,
        @Nullable FoodPredicate food,
        Map<NamespacedKey, AdvancementRequirement> advancements,
        Map<NamespacedKey, Boolean> recipes,
        List<StatPredicate> stats,
        @Nullable InputPredicate input
) {
    public static PlayerSpecific parse(String namespace, @Nullable Object raw) {
        if (raw == null)
            return new PlayerSpecific(null, Set.of(), IntRange.ANY, null, Map.of(), Map.of(), List.of(), null);
        return new PlayerSpecific(
                parseNullableEntityPredicate(namespace, section(raw, "looking_at")),
                parseGameModes(raw),
                IntRange.parse(raw, "level"),
                FoodPredicate.parse(section(raw, "food")),
                parseAdvancements(section(raw, "advancements")),
                parseRecipes(section(raw, "recipes")),
                parseStats(raw),
                InputPredicate.parse(section(raw, "input"))
        );
    }

    public boolean matches(Player player, Location origin, Player contextPlayer) {
        if (lookingAt != null) {
            Entity target = AdvancementPredicateSupport.lookingAt(player);
            if (target == null || !lookingAt.matches(target, origin, contextPlayer)) return false;
        }
        if (!gameModes.isEmpty() && !gameModes.contains(player.getGameMode())) return false;
        if (!level.matches(player.getLevel())) return false;
        if (food != null && !food.matches(player)) return false;
        if (!matchesAdvancements(player)) return false;
        if (!matchesRecipes(player)) return false;
        if (!matchesStats(player)) return false;
        return input == null || input.matches(player);
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

record FoodPredicate(IntRange level, DoubleRange saturation) {
    @Nullable
    public static FoodPredicate parse(@Nullable Object raw) {
        if (raw == null) return null;
        return new FoodPredicate(IntRange.parse(raw, "level"), DoubleRange.parse(raw, "saturation"));
    }

    public boolean matches(Player player) {
        return level.matches(player.getFoodLevel()) && saturation.matches(player.getSaturation());
    }
}

record InputPredicate(
        @Nullable Boolean forward,
        @Nullable Boolean backward,
        @Nullable Boolean left,
        @Nullable Boolean right,
        @Nullable Boolean jump,
        @Nullable Boolean sneak,
        @Nullable Boolean sprint
) {
    @Nullable
    public static InputPredicate parse(@Nullable Object raw) {
        if (raw == null) return null;
        return new InputPredicate(
                bool(raw, "forward"),
                bool(raw, "backward"),
                bool(raw, "left"),
                bool(raw, "right"),
                bool(raw, "jump"),
                bool(raw, "sneak"),
                bool(raw, "sprint")
        );
    }

    public boolean matches(Player player) {
        Object input = currentInput(player);
        if (forward != null && !matchesInput(input, "forward", forward)) return false;
        if (backward != null && !matchesInput(input, "backward", backward)) return false;
        if (left != null && !matchesInput(input, "left", left)) return false;
        if (right != null && !matchesInput(input, "right", right)) return false;
        if (jump != null && !matchesInput(input, "jump", jump)) return false;
        if (sneak != null) {
            boolean actual = input != null ? readInputBoolean(input, "sneak").orElse(player.isSneaking()) : player.isSneaking();
            if (actual != sneak) return false;
        }
        if (sprint != null) {
            boolean actual = input != null ? readInputBoolean(input, "sprint").orElse(player.isSprinting()) : player.isSprinting();
            if (actual != sprint) return false;
        }
        return true;
    }

    private boolean matchesInput(@Nullable Object input, String name, boolean expected) {
        return input != null && readInputBoolean(input, name).orElse(!expected) == expected;
    }
}

record SheepSpecific(@Nullable Boolean sheared) {
    public static SheepSpecific parse(Object raw) {
        return new SheepSpecific(bool(raw, "sheared"));
    }

    public boolean matches(Sheep sheep) {
        return sheared == null || sheep.isSheared() == sheared;
    }
}

record SlimeSpecific(IntRange size) {
    public static SlimeSpecific parse(Object raw) {
        return new SlimeSpecific(IntRange.parse(raw, "size"));
    }

    public boolean matches(Slime slime) {
        return size.matches(slime.getSize());
    }
}

record RaiderSpecific(@Nullable Boolean isCaptain, @Nullable Boolean hasRaid) {
    public static RaiderSpecific parse(Object raw) {
        return new RaiderSpecific(bool(raw, "is_captain"), bool(raw, "has_raid"));
    }

    public boolean matches(Entity entity) {
        if (isCaptain != null && reflectBoolean(entity, "isPatrolLeader").orElse(false) != isCaptain) return false;
        return hasRaid == null || reflectBoolean(entity, "hasRaid").orElse(false) == hasRaid;
    }
}

record FishingHookSpecific(@Nullable Boolean inOpenWater) {
    public static FishingHookSpecific parse(Object raw) {
        return new FishingHookSpecific(bool(raw, "in_open_water"));
    }

    public boolean matches(Entity entity) {
        return inOpenWater == null || reflectBoolean(entity, "isInOpenWater").orElse(false) == inOpenWater;
    }
}

interface AdvancementRequirement {
    boolean matches(AdvancementProgress progress);
}

record AdvancementDoneRequirement(boolean done) implements AdvancementRequirement {
    @Override
    public boolean matches(AdvancementProgress progress) {
        return progress.isDone() == done;
    }
}

record AdvancementCriteriaRequirement(Map<String, Boolean> criteria) implements AdvancementRequirement {
    @Override
    public boolean matches(AdvancementProgress progress) {
        for (Map.Entry<String, Boolean> entry : criteria.entrySet()) {
            boolean awarded = progress.getAwardedCriteria().contains(entry.getKey());
            if (awarded != entry.getValue()) return false;
        }
        return true;
    }
}

record StatPredicate(String type, String stat, IntRange value) {
    public boolean matches(Player player) {
        Statistic statistic = statisticFor(type, stat);
        if (statistic == null) return false;

        try {
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
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
