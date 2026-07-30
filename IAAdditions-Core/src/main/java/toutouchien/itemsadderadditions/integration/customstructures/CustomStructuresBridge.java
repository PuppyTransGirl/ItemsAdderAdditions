package toutouchien.itemsadderadditions.integration.customstructures;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Optional bridge for the Custom Structures plugin.
 *
 * <p>Custom Structures exposes spawned schematic bounds through its
 * {@code StructureSpawnEvent}, but has no API for looking up the structure
 * containing an arbitrary location. This bridge persists those bounds on every
 * chunk covered by the structure so advancement location predicates can query
 * them efficiently and the data survives restarts.</p>
 */
@NullMarked
public final class CustomStructuresBridge implements Listener {
    public static final String NAMESPACE = "customstructures";

    private static final String LOG_TAG = "CustomStructures";
    private static final String PLUGIN_NAME = "CustomStructures";
    private static final String SPAWN_EVENT_CLASS = "com.ryandw11.structure.api.StructureSpawnEvent";
    private static final NamespacedKey BOUNDS_KEY =
            new NamespacedKey("itemsadderadditions", "custom_structure_bounds");

    private final Method getStructure;
    private final Method getStructureName;
    private final Method getLocation;
    private final Method getMinimumPoint;
    private final Method getMaximumPoint;
    private final Method getRotation;

    private CustomStructuresBridge(Class<? extends Event> eventClass) throws ReflectiveOperationException {
        getStructure = eventClass.getMethod("getStructure");
        getStructureName = getStructure.getReturnType().getMethod("getName");
        getLocation = eventClass.getMethod("getLocation");
        getMinimumPoint = eventClass.getMethod("getMinimumPoint");
        getMaximumPoint = eventClass.getMethod("getMaximumPoint");
        getRotation = eventClass.getMethod("getRotation");
    }

    /**
     * Registers the optional event bridge when Custom Structures is installed.
     *
     * @return the registered listener, or {@code null} when the plugin/API is unavailable
     */
    @Nullable
    public static Listener register(Plugin owner) {
        Plugin customStructures = owner.getServer().getPluginManager().getPlugin(PLUGIN_NAME);
        if (customStructures == null || !customStructures.isEnabled()) return null;

        try {
            Class<?> rawEventClass = Class.forName(
                    SPAWN_EVENT_CLASS,
                    false,
                    customStructures.getClass().getClassLoader()
            );
            if (!Event.class.isAssignableFrom(rawEventClass)) {
                Log.warn(LOG_TAG, "{} is not a Bukkit event; advancement integration is disabled.",
                        SPAWN_EVENT_CLASS);
                return null;
            }

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) rawEventClass;
            CustomStructuresBridge bridge = new CustomStructuresBridge(eventClass);
            owner.getServer().getPluginManager().registerEvent(
                    eventClass,
                    bridge,
                    EventPriority.MONITOR,
                    (listener, event) -> ((CustomStructuresBridge) listener).onStructureSpawn(event),
                    owner,
                    true
            );
            Log.info(LOG_TAG, "Enabled advancement structure predicate integration.");
            return bridge;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Log.warn(LOG_TAG, "Could not enable advancement integration: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Tests whether {@code location} is inside a recorded Custom Structures
     * structure with the supplied name.
     */
    public static boolean isInStructure(Location location, String structureName) {
        World world = location.getWorld();
        if (world == null || structureName.isBlank()) return false;

        PersistentDataContainer data = location.getChunk().getPersistentDataContainer();
        String encoded = data.get(BOUNDS_KEY, PersistentDataType.STRING);
        if (encoded == null || encoded.isBlank()) return false;

        String expectedName = normalizeName(structureName);
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        for (StructureBounds bounds : decode(encoded)) {
            if (bounds.name().equals(expectedName) && bounds.contains(x, y, z)) return true;
        }
        return false;
    }

    private void onStructureSpawn(Event event) {
        try {
            Object structure = getStructure.invoke(event);
            Object rawName = getStructureName.invoke(structure);
            Object rawOrigin = getLocation.invoke(event);
            Object rawMinimum = getMinimumPoint.invoke(event);
            Object rawMaximum = getMaximumPoint.invoke(event);
            Object rawRotation = getRotation.invoke(event);
            if (!(rawName instanceof String name)
                    || !(rawOrigin instanceof Location origin)
                    || !(rawMinimum instanceof Location minimum)
                    || !(rawMaximum instanceof Location maximum)
                    || !(rawRotation instanceof Number rotation)) {
                Log.warn(LOG_TAG, "StructureSpawnEvent returned unexpected API values; ignoring it.");
                return;
            }
            storeBounds(name, origin, minimum, maximum, rotation.doubleValue());
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.warn(LOG_TAG, "Could not record spawned structure bounds: {}", e.getMessage());
        }
    }

    static void storeBounds(
            String structureName,
            Location origin,
            Location minimum,
            Location maximum,
            double rotationDegrees
    ) {
        World world = origin.getWorld();
        if (world == null
                || minimum.getWorld() == null
                || maximum.getWorld() == null
                || !world.equals(minimum.getWorld())
                || !world.equals(maximum.getWorld())) {
            return;
        }

        StructureBounds bounds = rotatedBounds(
                normalizeName(structureName), origin, minimum, maximum, rotationDegrees
        );
        int minChunkX = bounds.minX() >> 4;
        int maxChunkX = bounds.maxX() >> 4;
        int minChunkZ = bounds.minZ() >> 4;
        int maxChunkZ = bounds.maxZ() >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                PersistentDataContainer data = chunk.getPersistentDataContainer();
                List<StructureBounds> stored = new ArrayList<>();
                String current = data.get(BOUNDS_KEY, PersistentDataType.STRING);
                if (current != null && !current.isBlank()) stored.addAll(decode(current));
                if (!stored.contains(bounds)) {
                    stored.add(bounds);
                    data.set(BOUNDS_KEY, PersistentDataType.STRING, encode(stored));
                }
            }
        }
    }

    private static StructureBounds rotatedBounds(
            String name,
            Location origin,
            Location minimum,
            Location maximum,
            double rotationDegrees
    ) {
        int minX = Math.min(minimum.getBlockX(), maximum.getBlockX());
        int maxX = Math.max(minimum.getBlockX(), maximum.getBlockX());
        int minY = Math.min(minimum.getBlockY(), maximum.getBlockY());
        int maxY = Math.max(minimum.getBlockY(), maximum.getBlockY());
        int minZ = Math.min(minimum.getBlockZ(), maximum.getBlockZ());
        int maxZ = Math.max(minimum.getBlockZ(), maximum.getBlockZ());

        double radians = Math.toRadians(-rotationDegrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        int originX = origin.getBlockX();
        int originZ = origin.getBlockZ();
        int rotatedMinX = Integer.MAX_VALUE;
        int rotatedMaxX = Integer.MIN_VALUE;
        int rotatedMinZ = Integer.MAX_VALUE;
        int rotatedMaxZ = Integer.MIN_VALUE;

        for (int x : new int[]{minX, maxX}) {
            for (int z : new int[]{minZ, maxZ}) {
                double rotatedX = cosine * (x - originX) - sine * (z - originZ) + originX;
                double rotatedZ = sine * (x - originX) + cosine * (z - originZ) + originZ;
                int blockX = blockCoordinate(rotatedX);
                int blockZ = blockCoordinate(rotatedZ);
                rotatedMinX = Math.min(rotatedMinX, blockX);
                rotatedMaxX = Math.max(rotatedMaxX, blockX);
                rotatedMinZ = Math.min(rotatedMinZ, blockZ);
                rotatedMaxZ = Math.max(rotatedMaxZ, blockZ);
            }
        }

        return new StructureBounds(
                name, rotatedMinX, minY, rotatedMinZ, rotatedMaxX, maxY, rotatedMaxZ
        );
    }

    private static int blockCoordinate(double value) {
        double nearestInteger = Math.rint(value);
        if (Math.abs(value - nearestInteger) < 1.0E-7D) return (int) nearestInteger;
        return (int) Math.floor(value);
    }

    private static String encode(List<StructureBounds> bounds) {
        StringBuilder result = new StringBuilder();
        for (StructureBounds entry : bounds) {
            if (!result.isEmpty()) result.append(';');
            result.append(Base64.getUrlEncoder().withoutPadding().encodeToString(
                            entry.name().getBytes(StandardCharsets.UTF_8)))
                    .append(',').append(entry.minX())
                    .append(',').append(entry.minY())
                    .append(',').append(entry.minZ())
                    .append(',').append(entry.maxX())
                    .append(',').append(entry.maxY())
                    .append(',').append(entry.maxZ());
        }
        return result.toString();
    }

    private static List<StructureBounds> decode(String encoded) {
        List<StructureBounds> result = new ArrayList<>();
        for (String entry : encoded.split(";")) {
            String[] parts = entry.split(",", 7);
            if (parts.length != 7) continue;
            try {
                String name = new String(
                        Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8
                );
                result.add(new StructureBounds(
                        normalizeName(name),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3]),
                        Integer.parseInt(parts[4]),
                        Integer.parseInt(parts[5]),
                        Integer.parseInt(parts[6])
                ));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed or stale entries without breaking predicates.
            }
        }
        return List.copyOf(result);
    }

    private static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private record StructureBounds(
            String name,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ
    ) {
        boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }
    }
}
