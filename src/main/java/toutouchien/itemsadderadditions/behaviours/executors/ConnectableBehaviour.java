package toutouchien.itemsadderadditions.behaviours.executors;

import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurniturePlacedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.behaviours.BehaviourExecutor;
import toutouchien.itemsadderadditions.behaviours.BehaviourHost;
import toutouchien.itemsadderadditions.behaviours.annotations.Behaviour;
import toutouchien.itemsadderadditions.utils.Log;

import java.util.*;

/**
 * Connectable behaviour - automatically swaps furniture variants based on
 * neighbouring same-type pieces, like vanilla stairs or crafting tables.
 *
 * <h2>Types</h2>
 *
 * <h3>{@code stair} (default)</h3>
 * Full staircase-style connection with eight shape variants:
 * {@code default}, {@code straight}, {@code left}, {@code right},
 * {@code outer} (outer corner), and {@code inner} (inner corner).
 * All six shape keys are required.
 *
 * <pre>{@code
 * behaviours:
 *   connectable:
 *     type: stair           # Optional - "stair" is the default
 *     default:  "ns:stair_end"
 *     straight: "ns:stair_straight"
 *     left:     "ns:stair_left"
 *     right:    "ns:stair_right"
 *     outer:    "ns:stair_outer"
 *     inner:    "ns:stair_inner"
 * }</pre>
 *
 * <h3>{@code table}</h3>
 * Four-variant connection like a crafting table. The variant is chosen based
 * on how many cardinal sides have a neighbour:
 * <ul>
 *   <li>{@code default}  - no neighbours</li>
 *   <li>{@code border}   - exactly one neighbour (end-cap); rotated toward the neighbour</li>
 *   <li>{@code corner}   - two adjacent neighbours (L-shape); rotated to face the open corner</li>
 *   <li>{@code middle}   - two opposite neighbours, or three/four neighbours (fully surrounded)</li>
 * </ul>
 * No facing is needed for {@code default} or {@code middle}; the piece is placed at 0°.
 *
 * <pre>{@code
 * behaviours:
 *   connectable:
 *     type: table
 *     default: "ns:table_single"
 *     border:  "ns:table_border"
 *     corner:  "ns:table_corner"
 *     middle:  "ns:table_middle"
 * }</pre>
 */
@NullMarked
@Behaviour(key = "connectable")
public final class ConnectableBehaviour extends BehaviourExecutor implements Listener {

    /**
     * Connection algorithm - {@code "stair"} (default) or {@code "table"}.
     */
    @Parameter(key = "type", type = String.class)
    @Nullable private String typeRaw;

    // Shared by both types
    @Parameter(key = "default", type = String.class, required = true)
    private String defaultFurniture;

    @Parameter(key = "straight", type = String.class)
    @Nullable private String straightFurniture;

    // Table-type only
    @Parameter(key = "middle", type = String.class)
    @Nullable private String middleFurniture;

    @Parameter(key = "border", type = String.class)
    @Nullable private String borderFurniture;

    @Parameter(key = "corner", type = String.class)
    @Nullable private String cornerFurniture;

    @Parameter(key = "end", type = String.class)
    @Nullable private String endFurniture;

    // Stair-type only
    @Parameter(key = "left", type = String.class)
    @Nullable private String leftFurniture;

    @Parameter(key = "right", type = String.class)
    @Nullable private String rightFurniture;

    @Parameter(key = "outer", type = String.class)
    @Nullable private String outerFurniture;

    @Parameter(key = "inner", type = String.class)
    @Nullable private String innerFurniture;

    private ConnectableType type = ConnectableType.STAIR;
    private BehaviourHost host;

    /**
     * Block locations currently being mutated by {@link #applyShape}.
     * Suppresses the re-entrant {@link FurniturePlacedEvent} that
     * {@link CustomFurniture#replaceFurniture} fires.
     */
    private final Set<Location> updating = new HashSet<>();

    /**
     * The true logical facing of each connectable, recorded at player-placement time
     * and never overwritten by shape updates.
     *
     * <p>This is necessary because {@link #applyShape} writes a display yaw onto the
     * entity (e.g. a corner model rotated 90°) that no longer encodes the original
     * facing direction. Without this map, {@link #deriveStairShape} reads the display
     * yaw and computes the wrong shape for neighbours evaluated after a mutation.
     */
    private final Map<Location, FacingDirection> canonicalFacing = new HashMap<>();

    @Override
    public boolean configure(Object configData, String namespacedID) {
        if (!super.configure(configData, namespacedID))
            return false;

        type = resolveType(typeRaw);

        if (type == ConnectableType.TABLE) {
            if (straightFurniture == null || middleFurniture == null || borderFurniture == null || cornerFurniture == null || endFurniture == null) {
                Log.itemSkip("Behaviours", namespacedID, "connectable (table) is missing required shape keys: straight, middle, border, corner, end");
                return false;
            }
        } else {
            if (straightFurniture == null || leftFurniture == null || rightFurniture == null
                    || outerFurniture == null || innerFurniture == null) {
                Log.itemSkip("Behaviours", namespacedID, "connectable (stair) is missing required shape keys: straight, left, right, outer, inner");
                return false;
            }
        }

        return true;
    }

    private static ConnectableType resolveType(@Nullable String raw) {
        if (raw == null)
            return ConnectableType.STAIR;

        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "table" -> ConnectableType.TABLE;
            default -> ConnectableType.STAIR;
        };
    }

    @Override
    protected void onLoad(BehaviourHost host) {
        this.host = host;
        Bukkit.getPluginManager().registerEvents(this, host.plugin());
    }

    @Override
    protected void onUnload(BehaviourHost host) {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onFurniturePlaced(FurniturePlacedEvent event) {
        CustomFurniture placed = event.getFurniture();
        if (placed == null || !isOwnFurniture(placed))
            return;

        Location loc = placed.getEntity().getLocation().toBlockLocation();
        if (updating.contains(loc)) return;

        if (type == ConnectableType.STAIR)
            canonicalFacing.put(loc, yawToFacing(placed.getEntity().getYaw()));

        Bukkit.getScheduler().runTask(host.plugin(), () -> {
            CustomFurniture current = CustomFurniture.byAlreadySpawned(loc.getBlock());
            if (current == null || !isOwnFurniture(current)) return;

            updateShapeAt(current);

            // Wait one more tick for replaceFurniture() to settle, then update neighbours.
            Bukkit.getScheduler().runTask(host.plugin(), () -> updateNeighboursOf(loc));
        });
    }

    @EventHandler
    public void onFurnitureRemoved(FurnitureBreakEvent event) {
        CustomFurniture removed = event.getFurniture();
        if (removed == null || !isOwnFurniture(removed))
            return;

        Location loc = removed.getEntity().getLocation().toBlockLocation();
        canonicalFacing.remove(loc);

        // Defer one tick so the entity is fully gone before neighbours recalculate.
        Bukkit.getScheduler().runTask(host.plugin(), () -> updateNeighboursOf(loc));
    }

    private void updateShapeAt(CustomFurniture target) {
        PlacementSpec spec = (type == ConnectableType.TABLE)
                ? deriveTableSpec(target)
                : deriveStairSpec(target);
        applyShape(target, spec);
    }

    /**
     * Table type - four variants based on which cardinal sides have neighbours:
     *
     * <table>
     *   <tr><th>Connected sides</th><th>Variant</th><th>Rotation</th></tr>
     *   <tr><td>None</td><td>{@code default}</td><td>0°</td></tr>
     *   <tr><td>1 side</td><td>{@code border}</td><td>facing away from neighbour</td></tr>
     *   <tr><td>2 adjacent (L)</td><td>{@code corner}</td><td>facing the open corner</td></tr>
     *   <tr><td>2 opposite / 3+ sides</td><td>{@code middle}</td><td>0°</td></tr>
     * </table>
     *
     * <p>Rotation follows the same {@link FacingDirection} -> yaw convention as the stair type.
     */
    private PlacementSpec deriveTableSpec(CustomFurniture self) {
        Location loc = self.getEntity().getLocation().toBlockLocation();

        boolean north = findConnectableAt(offsetLocation(loc, FacingDirection.NORTH, 1)) != null;
        boolean south = findConnectableAt(offsetLocation(loc, FacingDirection.SOUTH, 1)) != null;
        boolean west = findConnectableAt(offsetLocation(loc, FacingDirection.WEST, 1)) != null;
        boolean east = findConnectableAt(offsetLocation(loc, FacingDirection.EAST, 1)) != null;

        int count = (north ? 1 : 0) + (south ? 1 : 0) + (west ? 1 : 0) + (east ? 1 : 0);

        return switch (count) {
            case 0 -> new PlacementSpec(defaultFurniture, 0f);

            case 1 -> {
                // Border: the single open end faces away from the neighbour.
                // Rotate so the "front" of the model points toward the neighbour.
                float yaw = north ? yawOf(FacingDirection.NORTH)
                        : south ? yawOf(FacingDirection.SOUTH)
                        : west ? yawOf(FacingDirection.WEST)
                        : yawOf(FacingDirection.EAST);

                // When they have only one table in front of them
                if (north && !south && !west && !east)
                    yield new PlacementSpec(endFurniture, yaw);
                else if (!north && south && !west && !east)
                    yield new PlacementSpec(endFurniture, yaw);
                else if (!north && !south && west && !east)
                    yield new PlacementSpec(endFurniture, yaw);
                else if (!north && !south && !west && east)
                    yield new PlacementSpec(endFurniture, yaw);


                yield new PlacementSpec(borderFurniture, yaw);
            }

            case 2 -> {
                int opposite = (north && south) ? 1 : ((west && east) ? 0 : -1);
                if (opposite != -1) {
                    // Two opposite sides -> middle (no meaningful rotation).
                    yield new PlacementSpec(straightFurniture, 90F * opposite);
                }

                // Two adjacent sides -> corner.
                // Rotate so the model's "inner corner" faces the open diagonal.
                float yaw = (north && east) ? yawOf(FacingDirection.NORTH)
                        : (north && west) ? yawOf(FacingDirection.WEST)
                        : (south && west) ? yawOf(FacingDirection.SOUTH)
                        : yawOf(FacingDirection.EAST); // south && east
                yield new PlacementSpec(cornerFurniture, yaw);
            }

            case 3 -> {
                float yaw = 0F;
                if (north && south && !west && east)
                    yaw = -90F;
                else if (north && south && west && !east)
                    yaw = 90F;
                else if (north && !south && west && east)
                    yaw = 180F;


                yield new PlacementSpec(borderFurniture, yaw);
            }

            // 3 or 4 sides -> middle
            default -> {
                yield new PlacementSpec(middleFurniture, 0f);
            }
        };
    }

    private PlacementSpec deriveStairSpec(CustomFurniture self) {
        FacingDirection facing = facingOf(self);
        float baseYaw = yawOf(facing);
        StairShape shape = deriveStairShape(self, facing);

        return switch (shape) {
            case DEFAULT -> new PlacementSpec(defaultFurniture, baseYaw);
            case STRAIGHT -> new PlacementSpec(straightFurniture, baseYaw);
            case LEFT -> new PlacementSpec(leftFurniture, baseYaw);
            case RIGHT -> new PlacementSpec(rightFurniture, baseYaw);
            case OUTER_LEFT -> new PlacementSpec(outerFurniture, baseYaw);
            case OUTER_RIGHT -> new PlacementSpec(outerFurniture, normalizeYaw(baseYaw + 90));
            case INNER_LEFT -> new PlacementSpec(innerFurniture, baseYaw);
            case INNER_RIGHT -> new PlacementSpec(innerFurniture, normalizeYaw(baseYaw + 90));
        };
    }

    private StairShape deriveStairShape(CustomFurniture self, FacingDirection selfFacing) {
        Location selfLoc = self.getEntity().getLocation().toBlockLocation();

        // Front neighbour -> outer corner
        CustomFurniture front = findConnectableAt(offsetLocation(selfLoc, selfFacing, 1));
        if (front != null) {
            FacingDirection frontFacing = facingOf(front);
            if (isPerpendicular(selfFacing, frontFacing)
                    && canTakeShape(selfFacing, selfLoc, frontFacing.opposite())) {
                return frontFacing == selfFacing.counterClockWise()
                        ? StairShape.OUTER_LEFT
                        : StairShape.OUTER_RIGHT;
            }
        }

        // Back neighbour -> inner corner
        CustomFurniture back = findConnectableAt(offsetLocation(selfLoc, selfFacing.opposite(), 1));
        if (back != null) {
            FacingDirection backFacing = facingOf(back);
            if (isPerpendicular(selfFacing, backFacing)
                    && canTakeShape(selfFacing, selfLoc, backFacing)) {
                return backFacing == selfFacing.counterClockWise()
                        ? StairShape.INNER_LEFT
                        : StairShape.INNER_RIGHT;
            }
        }

        // Side neighbours -> straight or end-caps
        boolean hasLeft = findConnectableAt(offsetLocation(selfLoc, selfFacing.counterClockWise(), 1)) != null;
        boolean hasRight = findConnectableAt(offsetLocation(selfLoc, selfFacing.clockWise(), 1)) != null;

        if (hasLeft && hasRight)
            return StairShape.STRAIGHT;

        // CCW neighbour -> RIGHT cap
        if (hasLeft)
            return StairShape.RIGHT;

        // CW  neighbour -> LEFT cap
        if (hasRight)
            return StairShape.LEFT;

        return StairShape.DEFAULT;
    }

    /**
     * Equivalent of vanilla's {@code canTakeShape}: the block on the given face side
     * must NOT be another connectable with the same facing as {@code selfFacing}.
     */
    private boolean canTakeShape(FacingDirection selfFacing, Location selfLoc, FacingDirection face) {
        CustomFurniture neighbour = findConnectableAt(offsetLocation(selfLoc, face, 1));
        if (neighbour == null)
            return true;

        return facingOf(neighbour) != selfFacing;
    }

    private void updateNeighboursOf(Location snappedLoc) {
        for (FacingDirection dir : FacingDirection.values()) {
            CustomFurniture neighbour = findConnectableAt(offsetLocation(snappedLoc, dir, 1));
            if (neighbour != null)
                updateShapeAt(neighbour);
        }
    }

    private void applyShape(CustomFurniture current, PlacementSpec spec) {
        String targetID = spec.variantID();
        float currentYaw = normalizeYaw(current.getEntity().getYaw());
        float targetYaw = normalizeYaw(spec.yaw());

        boolean sameVariant = current.getNamespacedID().equals(targetID);
        boolean sameYaw = Math.abs(currentYaw - targetYaw) < 1f;
        if (sameVariant && sameYaw)
            return;

        Location loc = current.getEntity().getLocation().toBlockLocation();
        updating.add(loc);
        try {
            current.replaceFurniture(targetID);
            current.getEntity().setRotation(targetYaw, 0);
        } finally {
            Bukkit.getScheduler().runTask(host.plugin(), () -> updating.remove(loc));
        }
    }

    @Nullable
    private CustomFurniture findConnectableAt(Location loc) {
        CustomFurniture furniture = CustomFurniture.byAlreadySpawned(loc.getBlock());
        return (furniture != null && isOwnFurniture(furniture)) ? furniture : null;
    }

    private boolean isOwnFurniture(CustomFurniture furniture) {
        String id = furniture.getNamespacedID();
        if (id.equals(defaultFurniture))
            return true;

        if (type == ConnectableType.TABLE) {
            return id.equals(straightFurniture)
                    || id.equals(middleFurniture)
                    || id.equals(borderFurniture)
                    || id.equals(cornerFurniture)
                    || id.equals(endFurniture);
        }

        return id.equals(straightFurniture)
                || id.equals(leftFurniture)
                || id.equals(rightFurniture)
                || id.equals(outerFurniture)
                || id.equals(innerFurniture);
    }

    /**
     * Returns the canonical logical facing for this furniture.
     * Uses {@link #canonicalFacing} so display yaw mutations from {@link #applyShape}
     * never corrupt shape derivation.
     */
    private FacingDirection facingOf(CustomFurniture furniture) {
        Location loc = furniture.getEntity().getLocation().toBlockLocation();
        FacingDirection canonical = canonicalFacing.get(loc);
        if (canonical != null)
            return canonical;

        // Fallback for pieces placed before the plugin loaded.
        return yawToFacing(furniture.getEntity().getYaw());
    }

    private static FacingDirection yawToFacing(float yaw) {
        yaw = normalizeYaw(yaw);
        if (yaw >= 315 || yaw < 45)
            return FacingDirection.SOUTH;

        if (yaw < 135)
            return FacingDirection.WEST;

        if (yaw < 225)
            return FacingDirection.NORTH;

        return FacingDirection.EAST;
    }

    private static float yawOf(FacingDirection dir) {
        return switch (dir) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> 270f;
        };
    }

    private static float normalizeYaw(float yaw) {
        return ((yaw % 360) + 360) % 360;
    }

    private static boolean isPerpendicular(FacingDirection a, FacingDirection b) {
        boolean aIsNS = a == FacingDirection.NORTH || a == FacingDirection.SOUTH;
        boolean bIsNS = b == FacingDirection.NORTH || b == FacingDirection.SOUTH;
        return aIsNS != bIsNS;
    }

    private static Location offsetLocation(Location origin, FacingDirection dir, int distance) {
        Location loc = origin.clone();
        return switch (dir) {
            case NORTH -> loc.add(0, 0, -distance);
            case SOUTH -> loc.add(0, 0, distance);
            case WEST -> loc.add(-distance, 0, 0);
            case EAST -> loc.add(distance, 0, 0);
        };
    }

    private record PlacementSpec(@Nullable String variantID, float yaw) {}

    private enum ConnectableType {STAIR, TABLE}

    private enum FacingDirection {
        NORTH, SOUTH, WEST, EAST;

        FacingDirection opposite() {
            return switch (this) {
                case NORTH -> SOUTH;
                case SOUTH -> NORTH;
                case WEST -> EAST;
                case EAST -> WEST;
            };
        }

        FacingDirection counterClockWise() {
            return switch (this) {
                case NORTH -> WEST;
                case WEST -> SOUTH;
                case SOUTH -> EAST;
                case EAST -> NORTH;
            };
        }

        FacingDirection clockWise() {
            return counterClockWise().opposite();
        }
    }

    private enum StairShape {
        DEFAULT, STRAIGHT,
        LEFT, RIGHT,
        OUTER_LEFT, OUTER_RIGHT,
        INNER_LEFT, INNER_RIGHT
    }
}
