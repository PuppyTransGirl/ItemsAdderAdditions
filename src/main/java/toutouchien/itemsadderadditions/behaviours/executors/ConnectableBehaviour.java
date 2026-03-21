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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@NullMarked
@Behaviour(key = "connectable")
public final class ConnectableBehaviour extends BehaviourExecutor implements Listener {

    @Parameter(key = "default", type = String.class, required = true)
    private String defaultFurniture;

    @Parameter(key = "straight", type = String.class, required = true)
    private String straightFurniture;

    @Parameter(key = "left", type = String.class, required = true)
    private String leftFurniture;

    @Parameter(key = "right", type = String.class, required = true)
    private String rightFurniture;

    @Parameter(key = "outer", type = String.class, required = true)
    private String outerFurniture;

    @Parameter(key = "inner", type = String.class, required = true)
    private String innerFurniture;

    private BehaviourHost host;

    /**
     * Block locations currently being mutated by applyShape.
     * Used to suppress the re-entrant FurniturePlacedEvent that
     * replaceFurniture fires.
     */
    private final Set<Location> updating = new HashSet<>();

    /**
     * The true logical facing of each connectable, recorded at player-placement
     * time and never overwritten by shape updates.
     * <p>
     * This is necessary because applyShape writes a display yaw onto the entity
     * (e.g. a corner model rotated 90°) that no longer encodes the original
     * facing direction. Without this map, deriveShape reads the display yaw and
     * computes the wrong shape for neighbours evaluated after a mutation.
     */
    private final Map<Location, FacingDirection> canonicalFacing = new HashMap<>();

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
        if (placed == null) return;
        if (!isOwnFurniture(placed)) return;

        Location loc = placed.getEntity().getLocation().toBlockLocation();
        if (updating.contains(loc)) return;

        // Record the true logical facing from the raw placement yaw,
        // before any shape mutation can overwrite the entity's rotation
        canonicalFacing.put(loc, yawToFacing(placed.getEntity().getYaw()));

        Bukkit.getScheduler().runTask(host.plugin(), () -> {
            CustomFurniture current = CustomFurniture.byAlreadySpawned(loc.getBlock());
            if (current == null || !isOwnFurniture(current)) return;

            updateShapeAt(current);

            // Wait one more tick for replaceFurniture() inside updateShapeAt to settle, then update neighbours
            Bukkit.getScheduler().runTask(host.plugin(), () -> updateNeighboursOf(loc));
        });
    }

    @EventHandler
    public void onFurnitureRemoved(FurnitureBreakEvent event) {
        CustomFurniture removed = event.getFurniture();
        if (removed == null) return;
        if (!isOwnFurniture(removed)) return;

        Location loc = removed.getEntity().getLocation().toBlockLocation();

        // Clean up the canonical facing entry
        canonicalFacing.remove(loc);

        // Defer by one tick so the entity is fully gone before
        // neighbours recalculate (otherwise they still see it)
        Bukkit.getScheduler().runTask(host.plugin(), () -> updateNeighboursOf(loc));
    }

    private void updateShapeAt(CustomFurniture target) {
        PlacementSpec spec = deriveSpec(target);
        applyShape(target, spec);
    }

    private ConnectionShape deriveShape(CustomFurniture self) {
        FacingDirection selfFacing = facingOf(self);
        Location selfLoc = self.getEntity().getLocation().toBlockLocation();

        // Front neighbour -> outer corner
        CustomFurniture front = findConnectableAt(offsetLocation(selfLoc, selfFacing, 1));
        if (front != null) {
            FacingDirection frontFacing = facingOf(front);
            if (isPerpendicular(selfFacing, frontFacing) && canTakeShape(selfFacing, selfLoc, frontFacing.opposite())) {
                return frontFacing == selfFacing.counterClockWise()
                        ? ConnectionShape.OUTER_LEFT
                        : ConnectionShape.OUTER_RIGHT;
            }
        }

        // Back neighbour -> inner corner
        CustomFurniture back = findConnectableAt(offsetLocation(selfLoc, selfFacing.opposite(), 1));
        if (back != null) {
            FacingDirection backFacing = facingOf(back);
            if (isPerpendicular(selfFacing, backFacing) && canTakeShape(selfFacing, selfLoc, backFacing)) {
                    return backFacing == selfFacing.counterClockWise()
                            ? ConnectionShape.INNER_LEFT
                            : ConnectionShape.INNER_RIGHT;
                }

        }

        // Side neighbours -> straight or end-caps
        boolean hasLeft = findConnectableAt(offsetLocation(selfLoc, selfFacing.counterClockWise(), 1)) != null;
        boolean hasRight = findConnectableAt(offsetLocation(selfLoc, selfFacing.clockWise(), 1)) != null;

        if (hasLeft && hasRight)
            return ConnectionShape.STRAIGHT;

        // neighbour on CCW side -> RIGHT cap
        if (hasLeft)
            return ConnectionShape.RIGHT;

        // neighbour on CW side -> LEFT cap
        if (hasRight)
            return ConnectionShape.LEFT;

        return ConnectionShape.DEFAULT;
    }

    /**
     * Equivalent of vanilla's canTakeShape:
     * The block on the given face side must NOT be another connectable with the same facing.
     */
    private boolean canTakeShape(FacingDirection selfFacing, Location selfLoc, FacingDirection face) {
        CustomFurniture neighbour = findConnectableAt(offsetLocation(selfLoc, face, 1));
        if (neighbour == null)
            return true;

        FacingDirection neighbourFacing = facingOf(neighbour);
        return neighbourFacing != selfFacing;
    }

    private void updateNeighboursOf(Location snappedLoc) {
        for (FacingDirection dir : FacingDirection.values()) {
            Location neighbourLoc = offsetLocation(snappedLoc, dir, 1);
            CustomFurniture neighbour = findConnectableAt(neighbourLoc);
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

    private static Location offsetLocation(Location origin, FacingDirection dir, int distance) {
        Location loc = origin.clone();
        return switch (dir) {
            case NORTH -> loc.add(0, 0, -distance);
            case SOUTH -> loc.add(0, 0, distance);
            case WEST -> loc.add(-distance, 0, 0);
            case EAST -> loc.add(distance, 0, 0);
        };
    }

    @Nullable
    private CustomFurniture findConnectableAt(Location loc) {
        CustomFurniture furniture = CustomFurniture.byAlreadySpawned(loc.getBlock());
        if (furniture != null && isOwnFurniture(furniture))
            return furniture;

        return null;
    }

    private boolean isOwnFurniture(CustomFurniture furniture) {
        String id = furniture.getNamespacedID();
        return id.equals(defaultFurniture)
                || id.equals(straightFurniture)
                || id.equals(leftFurniture)
                || id.equals(rightFurniture)
                || id.equals(outerFurniture)
                || id.equals(innerFurniture);
    }

    private record PlacementSpec(String variantID, float yaw) {}

    private PlacementSpec deriveSpec(CustomFurniture self) {
        FacingDirection facing = facingOf(self);
        float baseYaw = yawOf(facing);
        ConnectionShape shape = deriveShape(self);

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

    /**
     * Returns the canonical logical facing for this furniture.
     * Uses the placement-time facing from the canonicalFacing map so that
     * display yaw mutations from applyShape never corrupt shape derivation.
     */
    private FacingDirection facingOf(CustomFurniture furniture) {
        Location loc = furniture.getEntity().getLocation().toBlockLocation();
        FacingDirection canonical = canonicalFacing.get(loc);
        if (canonical != null)
            return canonical;

        // Fallback for pieces that predate this map (placed before plugin load)
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

    private enum ConnectionShape {
        DEFAULT, STRAIGHT,
        LEFT, RIGHT,
        OUTER_LEFT, OUTER_RIGHT,
        INNER_LEFT, INNER_RIGHT
    }
}
