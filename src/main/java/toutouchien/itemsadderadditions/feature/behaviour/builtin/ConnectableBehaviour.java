package toutouchien.itemsadderadditions.feature.behaviour.builtin;

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
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.common.util.Task;
import toutouchien.itemsadderadditions.feature.behaviour.BehaviourExecutor;
import toutouchien.itemsadderadditions.feature.behaviour.BehaviourHost;
import toutouchien.itemsadderadditions.feature.behaviour.annotation.Behaviour;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.connectable.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Makes furniture automatically change shape based on its neighbours,
 * similar to how vanilla fences, walls, or stairs connect to each other.
 *
 * <h3>Supported connection types ({@code type} parameter)</h3>
 * <ul>
 *   <li>{@code STAIR} - 8 variants: default, straight, left, right,
 *       outer_left, outer_right, inner_left, inner_right.</li>
 *   <li>{@code TABLE} - 5 variants: default (isolated), end, straight,
 *       corner, border (3-way), middle (4-way).</li>
 * </ul>
 *
 * <h3>Minimal YAML example (STAIR type)</h3>
 * <pre>{@code
 * behaviours:
 *   connectable:
 *     type: STAIR
 *     # Variant IDs default to "<self_id>_<suffix>".
 *     # Override any of them with a full "namespace:id" or a bare "id".
 *     straight: "my_pack:my_stair_straight"   # optional
 *     left:     "my_pack:my_stair_left"        # optional
 *     # ... etc.
 * }</pre>
 *
 * <h3>Variant ID resolution</h3>
 * For each variant key (e.g. {@code straight}), if no value is configured the ID
 * defaults to {@code "<base_id>_<suffix>"} (e.g. {@code "my_pack:my_stair_straight"}).
 * If a bare ID without a colon is provided, the base item's namespace is prepended.
 */
@NullMarked
@Behaviour(key = "connectable")
public final class ConnectableBehaviour extends BehaviourExecutor implements Listener {
    /**
     * Locations currently being updated to avoid recursive re-entry.
     */
    private final Set<Location> updating = new HashSet<>();

    /**
     * Canonical facing at placement time, keyed by block-snapped location.
     * Needed because {@link CustomFurniture#getEntity()}.{@code getYaw()} may drift
     * after shape-swap replacements.
     */
    private final Map<Location, FacingDirection> canonicalFacing = new HashMap<>();

    // Raw YAML parameters - resolved into full namespaced IDs in configure().
    @Parameter(key = "type", type = String.class, required = true) @Nullable private String typeRaw;
    @Parameter(key = "default", type = String.class) @Nullable private String defaultVariant;
    @Parameter(key = "straight", type = String.class) @Nullable private String straightVariant;
    @Parameter(key = "middle", type = String.class) @Nullable private String middleVariant;
    @Parameter(key = "border", type = String.class) @Nullable private String borderVariant;
    @Parameter(key = "corner", type = String.class) @Nullable private String cornerVariant;
    @Parameter(key = "end", type = String.class) @Nullable private String endVariant;
    @Parameter(key = "left", type = String.class) @Nullable private String leftVariant;
    @Parameter(key = "right", type = String.class) @Nullable private String rightVariant;
    @Parameter(key = "outer", type = String.class) @Nullable private String outerVariant;
    @Parameter(key = "inner", type = String.class) @Nullable private String innerVariant;

    // Resolved values set in configure()
    private ConnectableType type = ConnectableType.STAIR;
    private String namespacedID = "";

    private BehaviourHost host;

    private static float normalizeYaw(float yaw) {
        return ((yaw % 360) + 360) % 360;
    }

    @Override
    public boolean configure(Object configData, String namespacedID) {
        if (!super.configure(configData, namespacedID)) return false;

        this.namespacedID = namespacedID;
        this.type = ConnectableType.from(typeRaw);

        String namespace = NamespaceUtils.namespace(namespacedID);
        String baseId = NamespaceUtils.id(namespacedID);

        // Resolve each variant: if null → default suffix; if bare id → prepend namespace.
        defaultVariant = resolveVariant(namespace, defaultVariant, "");
        straightVariant = resolveVariant(namespace, straightVariant, "straight");
        middleVariant = resolveVariant(namespace, middleVariant, "middle");
        borderVariant = resolveVariant(namespace, borderVariant, "border");
        cornerVariant = resolveVariant(namespace, cornerVariant, "corner");
        endVariant = resolveVariant(namespace, endVariant, "end");
        leftVariant = resolveVariant(namespace, leftVariant, "left");
        rightVariant = resolveVariant(namespace, rightVariant, "right");
        outerVariant = resolveVariant(namespace, outerVariant, "outer");
        innerVariant = resolveVariant(namespace, innerVariant, "inner");

        return validateRequiredVariants(namespacedID);
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
        if (placed == null || !isOwnVariant(placed)) return;

        Location loc = placed.getEntity().getLocation().toBlockLocation();
        if (updating.contains(loc)) return;

        if (type == ConnectableType.STAIR)
            canonicalFacing.put(loc, FacingDirection.fromYaw(placed.getEntity().getYaw()));

        Task.sync(task -> {
            CustomFurniture current = CustomFurniture.byAlreadySpawned(loc.getBlock());
            if (current == null || !isOwnVariant(current)) return;
            updateShapeAt(current);
            Task.sync(task1 -> updateNeighboursOf(loc), host.plugin());
        }, host.plugin());
    }

    @EventHandler
    public void onFurnitureRemoved(FurnitureBreakEvent event) {
        CustomFurniture removed = event.getFurniture();
        if (removed == null || !isOwnVariant(removed)) return;

        Location loc = removed.getEntity().getLocation().toBlockLocation();
        canonicalFacing.remove(loc);
        Task.sync(task -> updateNeighboursOf(loc), host.plugin());
    }

    private void updateShapeAt(CustomFurniture target) {
        PlacementSpec spec = (type == ConnectableType.TABLE)
                ? TableShapeDeriver.derive(target,
                defaultVariant, endVariant, straightVariant,
                cornerVariant, borderVariant, middleVariant,
                this::findConnectableAt)
                : deriveStairSpec(target);
        applyShape(target, spec);
    }

    private PlacementSpec deriveStairSpec(CustomFurniture self) {
        FacingDirection facing = facingOf(self);
        StairShape shape = StairShapeDeriver.derive(self, facing, this::findConnectableAt, canonicalFacing);
        float base = facing.toYaw();
        float base90 = normalizeYaw(base + 90);

        return switch (shape) {
            case DEFAULT -> new PlacementSpec(defaultVariant, base);
            case STRAIGHT -> new PlacementSpec(straightVariant, base);
            case LEFT -> new PlacementSpec(leftVariant, base);
            case RIGHT -> new PlacementSpec(rightVariant, base);
            case OUTER_LEFT -> new PlacementSpec(outerVariant, base);
            case OUTER_RIGHT -> new PlacementSpec(outerVariant, base90);
            case INNER_LEFT -> new PlacementSpec(innerVariant, base);
            case INNER_RIGHT -> new PlacementSpec(innerVariant, base90);
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
        return (f != null && isOwnVariant(f)) ? f : null;
    }

    /**
     * Returns {@code true} if {@code f}'s namespaced ID is one of this behaviour's variants.
     */
    private boolean isOwnVariant(CustomFurniture f) {
        String id = f.getNamespacedID();
        if (id.equals(defaultVariant)) return true;
        if (type == ConnectableType.TABLE)
            return id.equals(straightVariant) || id.equals(middleVariant)
                    || id.equals(borderVariant) || id.equals(cornerVariant)
                    || id.equals(endVariant);
        return id.equals(straightVariant) || id.equals(leftVariant)
                || id.equals(rightVariant) || id.equals(outerVariant)
                || id.equals(innerVariant);
    }

    private FacingDirection facingOf(CustomFurniture f) {
        Location loc = f.getEntity().getLocation().toBlockLocation();
        FacingDirection canonical = canonicalFacing.get(loc);
        return canonical != null ? canonical : FacingDirection.fromYaw(f.getEntity().getYaw());
    }

    /**
     * Resolves a variant ID from the raw YAML value.
     *
     * <ul>
     *   <li>{@code null} → {@code "<namespace>:<baseId>_<suffix>"} (or just the base ID when suffix is empty)</li>
     *   <li>Contains {@code ":"} → used as-is (fully qualified)</li>
     *   <li>No {@code ":"} → {@code "<namespace>:<rawValue>"}</li>
     * </ul>
     */
    private String resolveVariant(String namespace, @Nullable String rawValue, String suffix) {
        if (rawValue == null) {
            String baseId = NamespaceUtils.id(namespacedID);
            return suffix.isEmpty()
                    ? namespacedID
                    : namespace + ":" + baseId + "_" + suffix;
        }

        return rawValue.contains(":") ? rawValue : namespace + ":" + rawValue;
    }

    /**
     * Validates that all required variant fields are present for the configured type.
     * Logs a skip warning and returns {@code false} if any are missing.
     */
    private boolean validateRequiredVariants(String itemName) {
        if (type == ConnectableType.TABLE) {
            if (straightVariant == null || middleVariant == null
                    || borderVariant == null || cornerVariant == null || endVariant == null) {
                Log.itemSkip("Behaviours", itemName,
                        "connectable (table) is missing required shape keys: "
                                + "straight, middle, border, corner, end");
                return false;
            }
        } else {
            if (straightVariant == null || leftVariant == null || rightVariant == null
                    || outerVariant == null || innerVariant == null) {
                Log.itemSkip("Behaviours", itemName,
                        "connectable (stair) is missing required shape keys: "
                                + "straight, left, right, outer, inner");
                return false;
            }
        }
        return true;
    }
}
