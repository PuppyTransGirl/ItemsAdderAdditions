package toutouchien.itemsadderadditions.integration.valhalla;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * All Valhalla-related PDC data parsed from one item's {@code valhalla:} config section.
 *
 * <p>{@link #actualStats} maps to {@code valhallammo:actual_stats},
 * {@link #defaultStats} to {@code valhallammo:default_stats},
 * {@link #equipmentClass} to {@code valhallammo:equipment_class},
 * {@link #itemFlags} to {@code valhallammo:item_flags},
 * {@link #permanentEffects} to {@code valhallammo:permanent_potion_effects},
 * {@link #permanentEffectCooldown} to {@code valhallammo:permanent_effects_cooldown_properties},
 * and {@link #trinkets} to the {@code valhallatrinkets:*} keys.
 */
@NullMarked
public record ValhallaItemData(
        List<ValhallaStatEntry> actualStats,
        List<ValhallaStatEntry> defaultStats,
        @Nullable String equipmentClass,
        List<String> itemFlags,
        List<ValhallaPermanentEffect> permanentEffects,
        @Nullable ValhallaPermanentEffectCooldown permanentEffectCooldown,
        @Nullable ValhallaTrinketData trinkets
) {
    public ValhallaItemData(
            List<ValhallaStatEntry> actualStats,
            List<ValhallaStatEntry> defaultStats,
            @Nullable String equipmentClass,
            List<String> itemFlags,
            @Nullable ValhallaTrinketData trinkets
    ) {
        this(actualStats, defaultStats, equipmentClass, itemFlags, List.of(), null, trinkets);
    }

    public boolean isEmpty() {
        return actualStats.isEmpty()
                && defaultStats.isEmpty()
                && equipmentClass == null
                && itemFlags.isEmpty()
                && permanentEffects.isEmpty()
                && permanentEffectCooldown == null
                && trinkets == null;
    }
}
