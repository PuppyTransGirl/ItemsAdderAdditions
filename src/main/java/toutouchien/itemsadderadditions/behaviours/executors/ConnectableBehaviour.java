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
import toutouchien.itemsadderadditions.behaviours.executors.connectable.*;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.Task;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@NullMarked
@Behaviour(key = "connectable")
public final class ConnectableBehaviour extends BehaviourExecutor implements Listener {
    private final Set<Location> updating = new HashSet<>();
    private final Map<Location, FacingDirection> canonicalFacing = new HashMap<>();
    @Parameter(key = "type", type = String.class, required = true) @Nullable private String typeRaw;
    @Parameter(key = "default", type = String.class) private String defaultFurniture;
    @Parameter(key = "straight", type = String.class) @Nullable private String straightFurniture;
    @Parameter(key = "middle", type = String.class) @Nullable private String middleFurniture;
    @Parameter(key = "border", type = String.class) @Nullable private String borderFurniture;
    @Parameter(key = "corner", type = String.class) @Nullable private String cornerFurniture;
    @Parameter(key = "end", type = String.class) @Nullable private String endFurniture;
    @Parameter(key = "left", type = String.class) @Nullable private String leftFurniture;
    @Parameter(key = "right", type = String.class) @Nullable private String rightFurniture;
    @Parameter(key = "outer", type = String.class) @Nullable private String outerFurniture;
    @Parameter(key = "inner", type = String.class) @Nullable private String innerFurniture;
    private ConnectableType type = ConnectableType.STAIR;

    private BehaviourHost host;
    private String namespacedID;

    private static float normalizeYaw(float yaw) {
        return ((yaw % 360) + 360) % 360;
    }

    @Nullable
    private String norm(String namespace, @Nullable String id, String suffix) {
        if (id == null)
            return suffix.isEmpty() ? namespacedID : namespacedID + "_" + suffix;

        return id.contains(":") ? id : namespace + ":" + id;
    }

    @Override
    public boolean configure(Object configData, String namespacedID) {
        if (!super.configure(configData, namespacedID)) return false;

        this.namespacedID = namespacedID;

        type = ConnectableType.from(typeRaw);
        String ns = NamespaceUtils.namespace(namespacedID);

        defaultFurniture = norm(ns, defaultFurniture, "");
        straightFurniture = norm(ns, straightFurniture, "straight");
        middleFurniture = norm(ns, middleFurniture, "middle");
        borderFurniture = norm(ns, borderFurniture, "border");
        cornerFurniture = norm(ns, cornerFurniture, "corner");
        endFurniture = norm(ns, endFurniture, "end");
        leftFurniture = norm(ns, leftFurniture, "left");
        rightFurniture = norm(ns, rightFurniture, "right");
        outerFurniture = norm(ns, outerFurniture, "outer");
        innerFurniture = norm(ns, innerFurniture, "inner");

        if (type == ConnectableType.TABLE) {
            if (straightFurniture == null || middleFurniture == null
                    || borderFurniture == null || cornerFurniture == null || endFurniture == null) {
                Log.itemSkip("Behaviours", namespacedID,
                        "connectable (table) is missing required shape keys: straight, middle, border, corner, end");
                return false;
            }
        } else {
            if (straightFurniture == null || leftFurniture == null || rightFurniture == null
                    || outerFurniture == null || innerFurniture == null) {
                Log.itemSkip("Behaviours", namespacedID,
                        "connectable (stair) is missing required shape keys: straight, left, right, outer, inner");
                return false;
            }
        }

        return true;
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
        if (placed == null || !isOwn(placed)) return;

        Location loc = placed.getEntity().getLocation().toBlockLocation();
        if (updating.contains(loc)) return;

        if (type == ConnectableType.STAIR)
            canonicalFacing.put(loc, FacingDirection.fromYaw(placed.getEntity().getYaw()));

        Task.sync(task -> {
            CustomFurniture current = CustomFurniture.byAlreadySpawned(loc.getBlock());
            if (current == null || !isOwn(current)) return;
            updateShapeAt(current);
            Task.sync(task1 -> updateNeighboursOf(loc), host.plugin());
        }, host.plugin());
    }

    @EventHandler
    public void onFurnitureRemoved(FurnitureBreakEvent event) {
        CustomFurniture removed = event.getFurniture();
        if (removed == null || !isOwn(removed)) return;

        Location loc = removed.getEntity().getLocation().toBlockLocation();
        canonicalFacing.remove(loc);
        Task.sync(task -> updateNeighboursOf(loc), host.plugin());
    }

    private void updateShapeAt(CustomFurniture target) {
        PlacementSpec spec = (type == ConnectableType.TABLE)
                ? TableShapeDeriver.derive(target,
                defaultFurniture, endFurniture, straightFurniture,
                cornerFurniture, borderFurniture, middleFurniture,
                this::findConnectableAt)
                : deriveStairSpec(target);
        applyShape(target, spec);
    }

    private PlacementSpec deriveStairSpec(CustomFurniture self) {
        FacingDirection facing = facingOf(self);
        StairShape shape = StairShapeDeriver.derive(self, facing, this::findConnectableAt, canonicalFacing);
        float base = facing.toYaw();
        float norm90 = normalizeYaw(base + 90);

        return switch (shape) {
            case DEFAULT -> new PlacementSpec(defaultFurniture, base);
            case STRAIGHT -> new PlacementSpec(straightFurniture, base);
            case LEFT -> new PlacementSpec(leftFurniture, base);
            case RIGHT -> new PlacementSpec(rightFurniture, base);
            case OUTER_LEFT -> new PlacementSpec(outerFurniture, base);
            case OUTER_RIGHT -> new PlacementSpec(outerFurniture, norm90);
            case INNER_LEFT -> new PlacementSpec(innerFurniture, base);
            case INNER_RIGHT -> new PlacementSpec(innerFurniture, norm90);
        };
    }

    private void applyShape(CustomFurniture current, PlacementSpec spec) {
        float currentYaw = normalizeYaw(current.getEntity().getYaw());
        float targetYaw = normalizeYaw(spec.yaw());
        boolean sameVariant = current.getNamespacedID().equals(spec.variantID());
        boolean sameYaw = Math.abs(currentYaw - targetYaw) < 1f;
        if (sameVariant && sameYaw) return;

        Location loc = current.getEntity().getLocation().toBlockLocation();
        updating.add(loc);
        try {
            current.replaceFurniture(spec.variantID());
            current.getEntity().setRotation(targetYaw, 0);
        } finally {
            Task.sync(task -> updating.remove(loc), host.plugin());
        }
    }

    private void updateNeighboursOf(Location loc) {
        for (FacingDirection dir : FacingDirection.values()) {
            CustomFurniture n = findConnectableAt(dir.offset(loc));
            if (n != null) updateShapeAt(n);
        }
    }

    @Nullable
    private CustomFurniture findConnectableAt(Location loc) {
        CustomFurniture f = CustomFurniture.byAlreadySpawned(loc.getBlock());
        return (f != null && isOwn(f)) ? f : null;
    }

    private boolean isOwn(CustomFurniture f) {
        String id = f.getNamespacedID();
        if (id.equals(defaultFurniture)) return true;
        if (type == ConnectableType.TABLE)
            return id.equals(straightFurniture) || id.equals(middleFurniture)
                    || id.equals(borderFurniture) || id.equals(cornerFurniture)
                    || id.equals(endFurniture);
        return id.equals(straightFurniture) || id.equals(leftFurniture)
                || id.equals(rightFurniture) || id.equals(outerFurniture)
                || id.equals(innerFurniture);
    }

    private FacingDirection facingOf(CustomFurniture f) {
        Location loc = f.getEntity().getLocation().toBlockLocation();
        FacingDirection c = canonicalFacing.get(loc);
        return c != null ? c : FacingDirection.fromYaw(f.getEntity().getYaw());
    }
}
