package toutouchien.itemsadderadditions.feature.advancement;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import java.util.ArrayList;
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
        List<Structure> structures
) {
    @Nullable
    public static LocationPredicate parse(String namespace, @Nullable Object raw) {
        if (raw == null) return null;
        Object position = section(raw, "position");
        return new LocationPredicate(
                (readStringList(raw, "dimension").isEmpty() ? readStringList(raw, "dimensions") : readStringList(raw, "dimension")),
                emptyToNull(string(value(raw, "world"))),
                readStringList(raw, "biomes").isEmpty() ? readStringList(raw, "biome") : readStringList(raw, "biomes"),
                DoubleRange.parse(position != null ? position : raw, "x"),
                DoubleRange.parse(position != null ? position : raw, "y"),
                DoubleRange.parse(position != null ? position : raw, "z"),
                BlockPredicate.parse(namespace, sectionOrValue(raw, "block")),
                IntRange.parse(raw, "light"),
                bool(raw, "can_see_sky"),
                parseStructures(
                        readStringList(raw, "structures").isEmpty()
                                ? readStringList(raw, "structure")
                                : readStringList(raw, "structures")
                )
        );
    }

    private static List<Structure> parseStructures(List<String> ids) {
        if (ids.isEmpty()) return List.of();
        var registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE);
        List<Structure> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            String normalized = NamespaceUtils.normalizeMinecraftID(id);
            String[] parts = normalized.split(":", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) continue;
            Structure structure = registry.get(new NamespacedKey(parts[0], parts[1]));
            if (structure == null) {
                Log.warn("Advancement", "Unknown structure '{}' in location predicate, ignoring.", id);
                continue;
            }
            result.add(structure);
        }
        return List.copyOf(result);
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
        if (!structures.isEmpty() && !isInAnyStructure(loc, world)) return false;
        return true;
    }

    private boolean isInAnyStructure(Location loc, World world) {
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        org.bukkit.util.Vector point = loc.toVector();
        for (Structure structure : structures) {
            for (GeneratedStructure gs : world.getStructures(chunkX, chunkZ, structure)) {
                if (gs.getBoundingBox().contains(point)) return true;
            }
        }
        return false;
    }
}

record BlockPredicate(List<String> blocks, Map<String, StringRange> states) {
    @Nullable
    public static BlockPredicate parse(String namespace, @Nullable Object raw) {
        if (raw == null) return null;
        if (!isSection(raw)) {
            String blockId = string(raw);
            return blockId == null ? null : new BlockPredicate(List.of(normalizeBlockIdOrTag(namespace, blockId)), Map.of());
        }
        List<String> blocks = readStringList(raw, "blocks");
        if (blocks.isEmpty()) blocks = readStringList(raw, "block");
        if (blocks.isEmpty()) blocks = readStringList(raw, "id");
        Map<String, StringRange> states = parseStates(section(raw, "state"));
        return new BlockPredicate(blocks.stream().map(block -> normalizeBlockIdOrTag(namespace, block)).toList(), states);
    }

    public boolean matches(Location loc) {
        if (!blocks.isEmpty()) {
            boolean matched = false;
            for (String blockId : blocks) {
                if (NamespaceUtils.matchesBlockIDOrTag(loc.getBlock(), blockId)) {
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
