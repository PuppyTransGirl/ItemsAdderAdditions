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
import toutouchien.itemsadderadditions.common.utils.BiomeKeys;
import toutouchien.itemsadderadditions.integration.customstructures.CustomStructuresBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        List<Structure> structures,
        List<String> customStructures
) {
    @Nullable
    public static LocationPredicate parse(String namespace, @Nullable Object raw) {
        if (raw == null) return null;
        Object position = section(raw, "position");
        List<String> structureIds = readStringList(raw, "structures").isEmpty()
                ? readStringList(raw, "structure")
                : readStringList(raw, "structures");
        StructureFilters structureFilters = parseStructures(structureIds);
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
                structureFilters.vanilla(),
                structureFilters.custom()
        );
    }

    private static StructureFilters parseStructures(List<String> ids) {
        if (ids.isEmpty()) return StructureFilters.EMPTY;
        var registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE);
        List<Structure> vanilla = new ArrayList<>(ids.size());
        List<String> custom = new ArrayList<>();
        for (String id : ids) {
            String trimmed = id.trim();
            int separator = trimmed.indexOf(':');
            if (separator > 0 && separator < trimmed.length() - 1) {
                String namespace = trimmed.substring(0, separator).toLowerCase(Locale.ROOT);
                if (namespace.equals(CustomStructuresBridge.NAMESPACE)
                        || namespace.equals("custom_structures")) {
                    custom.add(trimmed.substring(separator + 1));
                    continue;
                }
            }

            String normalized = NamespaceUtils.normalizeMinecraftID(id);
            String[] parts = normalized.split(":", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) continue;
            Structure structure = registry.get(new NamespacedKey(parts[0], parts[1]));
            if (structure == null) {
                Log.warn("Advancement", "Unknown structure '{}' in location predicate, ignoring.", id);
                continue;
            }
            vanilla.add(structure);
        }
        return new StructureFilters(List.copyOf(vanilla), List.copyOf(custom));
    }

    public boolean matches(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;
        if (!dimensions.isEmpty() && dimensions.stream().map(NamespaceUtils::normalizeMinecraftID).noneMatch(dimension -> matchesDimension(world, dimension)))
            return false;
        if (worldName != null && !world.getName().equals(worldName)) return false;
        if (!biomes.isEmpty()) {
            String biome = BiomeKeys.asString(loc.getBlock().getBiome());
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
        if ((!structures.isEmpty() || !customStructures.isEmpty())
                && !isInAnyStructure(loc, world)) return false;
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
        for (String structure : customStructures) {
            if (CustomStructuresBridge.isInStructure(loc, structure)) return true;
        }
        return false;
    }

    private record StructureFilters(List<Structure> vanilla, List<String> custom) {
        private static final StructureFilters EMPTY = new StructureFilters(List.of(), List.of());
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
