package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.Location;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

import static toutouchien.itemsadderadditions.feature.advancement.AdvancementPredicateSupport.*;

record IntRange(@Nullable Integer min, @Nullable Integer max) {
    public static final IntRange ANY = new IntRange(null, null);
    public static final IntRange NONE = new IntRange(1, 0);

    public boolean matches(int value) {
        return (min == null || value >= min) && (max == null || value <= max);
    }

    public static IntRange parse(@Nullable Object parent, String path) {
        if (parent == null) return ANY;
        return parseValue(value(parent, path));
    }

    public static IntRange parseValue(@Nullable Object raw) {
        if (raw == null) return ANY;
        if (raw instanceof Number number) {
            int exact = number.intValue();
            return new IntRange(exact, exact);
        }
        if (raw instanceof String stringValue && !stringValue.isBlank()) {
            try {
                int exact = Integer.parseInt(stringValue.trim());
                return new IntRange(exact, exact);
            } catch (NumberFormatException ignored) {
                return ANY;
            }
        }
        return new IntRange(intObject(value(raw, "min")), intObject(value(raw, "max")));
    }
}

record DoubleRange(@Nullable Double min, @Nullable Double max) {
    public static final DoubleRange ANY = new DoubleRange(null, null);
    public static final DoubleRange NONE = new DoubleRange(1.0D, 0.0D);

    public boolean matches(double value) {
        return (min == null || value >= min) && (max == null || value <= max);
    }

    public static DoubleRange parse(@Nullable Object parent, String path) {
        if (parent == null) return ANY;
        return parseValue(value(parent, path));
    }

    public static DoubleRange parseValue(@Nullable Object raw) {
        if (raw == null) return ANY;
        if (raw instanceof Number number) {
            double exact = number.doubleValue();
            return new DoubleRange(exact, exact);
        }
        if (raw instanceof String stringValue && !stringValue.isBlank()) {
            try {
                double exact = Double.parseDouble(stringValue.trim());
                return new DoubleRange(exact, exact);
            } catch (NumberFormatException ignored) {
                return ANY;
            }
        }
        return new DoubleRange(doubleObject(value(raw, "min")), doubleObject(value(raw, "max")));
    }
}

record DistancePredicate(
        DoubleRange absolute,
        DoubleRange horizontal,
        DoubleRange x,
        DoubleRange y,
        DoubleRange z
) {
    @Nullable
    public static DistancePredicate parse(@Nullable Object raw) {
        if (raw == null) return null;
        return new DistancePredicate(
                DoubleRange.parse(raw, "absolute"),
                DoubleRange.parse(raw, "horizontal"),
                DoubleRange.parse(raw, "x"),
                DoubleRange.parse(raw, "y"),
                DoubleRange.parse(raw, "z")
        );
    }

    public boolean matches(Location origin, Location target) {
        if (origin.getWorld() == null || target.getWorld() == null || !origin.getWorld().equals(target.getWorld()))
            return false;
        double dx = Math.abs(target.getX() - origin.getX());
        double dy = Math.abs(target.getY() - origin.getY());
        double dz = Math.abs(target.getZ() - origin.getZ());
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double absoluteDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return absolute.matches(absoluteDistance)
                && horizontal.matches(horizontalDistance)
                && x.matches(dx)
                && y.matches(dy)
                && z.matches(dz);
    }
}

record MovementPredicate(
        DoubleRange x,
        DoubleRange y,
        DoubleRange z,
        DoubleRange speed,
        DoubleRange horizontalSpeed,
        DoubleRange verticalSpeed,
        DoubleRange fallDistance
) {
    @Nullable
    public static MovementPredicate parse(@Nullable Object raw) {
        if (raw == null) return null;
        return new MovementPredicate(
                DoubleRange.parse(raw, "x"),
                DoubleRange.parse(raw, "y"),
                DoubleRange.parse(raw, "z"),
                DoubleRange.parse(raw, "speed"),
                DoubleRange.parse(raw, "horizontal_speed"),
                DoubleRange.parse(raw, "vertical_speed"),
                DoubleRange.parse(raw, "fall_distance")
        );
    }

    public boolean matches(Entity entity) {
        Vector velocity = entity.getVelocity();
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
                && fallDistance.matches(entity.getFallDistance());
    }
}

record Flags(
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

    public boolean matches(Entity entity) {
        if (isBaby != null && entity instanceof Ageable ageable && ageable.isAdult() == isBaby) return false;
        if (isOnFire != null && (entity.getFireTicks() > 0) != isOnFire) return false;
        if (isSneaking != null && entity.isSneaking() != isSneaking) return false;
        if (isSprinting != null && reflectBoolean(entity, "isSprinting").orElse(false) != isSprinting) return false;
        if (isSwimming != null && reflectBoolean(entity, "isSwimming").orElse(false) != isSwimming) return false;
        if (isOnGround != null && entity.isOnGround() != isOnGround) return false;
        if (isFlying != null) {
            boolean flying = entity instanceof Player player && player.isFlying();
            boolean gliding = entity instanceof LivingEntity living && living.isGliding();
            if ((flying || gliding) != isFlying) return false;
        }
        return isFallFlying == null || (entity instanceof LivingEntity living && living.isGliding()) == isFallFlying;
    }

    public static Flags parse(@Nullable Object raw) {
        if (raw == null) return ANY;
        return new Flags(
                bool(raw, "is_baby"),
                bool(raw, "is_on_fire"),
                bool(raw, "is_sneaking"),
                bool(raw, "is_sprinting"),
                bool(raw, "is_swimming"),
                bool(raw, "is_on_ground"),
                bool(raw, "is_flying"),
                bool(raw, "is_fall_flying")
        );
    }
}

record EffectPredicate(
        IntRange amplifier,
        IntRange duration,
        @Nullable Boolean ambient,
        @Nullable Boolean visible
) {
    public static EffectPredicate parse(@Nullable Object raw) {
        if (raw == null) return new EffectPredicate(IntRange.ANY, IntRange.ANY, null, null);
        return new EffectPredicate(
                IntRange.parse(raw, "amplifier"),
                IntRange.parse(raw, "duration"),
                bool(raw, "ambient"),
                bool(raw, "visible")
        );
    }

    public boolean matches(PotionEffect effect) {
        if (!amplifier.matches(effect.getAmplifier())) return false;
        if (!duration.matches(effect.getDuration())) return false;
        if (ambient != null && effect.isAmbient() != ambient) return false;
        return visible == null || effect.hasParticles() == visible;
    }
}

record StringRange(@Nullable String exact, @Nullable String min, @Nullable String max) {
    public boolean matches(String value) {
        if (exact != null) return value.equals(exact);
        if (min != null && compareStateValues(value, min) < 0) return false;
        return max == null || compareStateValues(value, max) <= 0;
    }
}
