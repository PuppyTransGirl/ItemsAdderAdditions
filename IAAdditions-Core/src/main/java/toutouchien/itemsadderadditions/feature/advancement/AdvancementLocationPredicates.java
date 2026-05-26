package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import java.util.List;
import java.util.Map;

import static toutouchien.itemsadderadditions.feature.advancement.AdvancementPredicateSupport.*;

record LocationPredicate(
        List<String> dimensions,
        @Nullable String worldName,
        List<String> biomes,
        DoubleRange x,
        DoubleRange y,
        DoubleRange z,
        @Nullable BlockPredicate block,
        IntRange light,
        @Nullable Boolean canSeeSky,
        boolean unsupportedStructures
) {
    @Nullable
    public static LocationPredicate parse(@Nullable Object raw) {
        if (raw == null) return null;
        Object position = section(raw, "position");
        return new LocationPredicate(
                (readStringList(raw, "dimension").isEmpty() ? readStringList(raw, "dimensions") : readStringList(raw, "dimension")),
                emptyToNull(string(value(raw, "world"))),
                readStringList(raw, "biomes").isEmpty() ? readStringList(raw, "biome") : readStringList(raw, "biomes"),
                DoubleRange.parse(position != null ? position : raw, "x"),
                DoubleRange.parse(position != null ? position : raw, "y"),
                DoubleRange.parse(position != null ? position : raw, "z"),
                BlockPredicate.parse(sectionOrValue(raw, "block")),
                IntRange.parse(raw, "light"),
                bool(raw, "can_see_sky"),
                value(raw, "structure") != null || value(raw, "structures") != null
        );
    }

    public boolean matches(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;
        if (!dimensions.isEmpty() && dimensions.stream().map(NamespaceUtils::normalizeMinecraftID).noneMatch(dimension -> matchesDimension(world, dimension)))
            return false;
        if (worldName != null && !world.getName().equals(worldName)) return false;
        if (!biomes.isEmpty()) {
            String biome = loc.getBlock().getBiome().getKey().toString();
            boolean matched = false;
            for (String expected : biomes) {
                if (NamespaceUtils.normalizeMinecraftID(expected).equals(biome)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        if (!x.matches(loc.getX()) || !y.matches(loc.getY()) || !z.matches(loc.getZ())) return false;
        if (block != null && !block.matches(loc)) return false;
        if (!light.matches(loc.getBlock().getLightLevel())) return false;
        if (canSeeSky != null && canSeeSky != AdvancementPredicateSupport.canSeeSky(loc)) return false;
        return !unsupportedStructures;
    }
}

record BlockPredicate(List<String> blocks, Map<String, StringRange> states) {
    @Nullable
    public static BlockPredicate parse(@Nullable Object raw) {
        if (raw == null) return null;
        if (!isSection(raw)) {
            String blockId = string(raw);
            return blockId == null ? null : new BlockPredicate(List.of(normalizeMinecraftIdOrTag(blockId)), Map.of());
        }
        List<String> blocks = readStringList(raw, "blocks");
        if (blocks.isEmpty()) blocks = readStringList(raw, "block");
        if (blocks.isEmpty()) blocks = readStringList(raw, "id");
        Map<String, StringRange> states = parseStates(section(raw, "state"));
        return new BlockPredicate(blocks.stream().map(AdvancementPredicateSupport::normalizeMinecraftIdOrTag).toList(), states);
    }

    public boolean matches(Location loc) {
        Material type = loc.getBlock().getType();
        String actual = type.getKey().toString();
        if (!blocks.isEmpty()) {
            boolean matched = false;
            for (String block : blocks) {
                if (block.startsWith("#")) continue; // Block tags are not resolved here.
                if (block.equals(actual)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }

        if (!states.isEmpty()) {
            String blockData = loc.getBlock().getBlockData().getAsString(false);
            for (Map.Entry<String, StringRange> entry : states.entrySet()) {
                String current = blockStateValue(blockData, entry.getKey());
                if (current == null || !entry.getValue().matches(current)) return false;
            }
        }
        return true;
    }
}
