package toutouchien.itemsadderadditions.feature.action;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Resolves the set of target entities for an action based on its targeting mode.
 *
 * <p>This class is responsible for a single concern: given an {@link ActionContext}
 * and a targeting configuration, return the entities that the action should run on.
 * Separating this logic from {@link ActionExecutor} keeps each class focused on one
 * responsibility (SOLID - SRP).
 *
 * <h3>Targeting modes</h3>
 * <table>
 *   <tr><th>Mode</th><th>Description</th></tr>
 *   <tr><td>{@code self}</td><td>Only the triggering player.</td></tr>
 *   <tr><td>{@code other}</td><td>Only the event's target entity (if any).</td></tr>
 *   <tr><td>{@code all}</td><td>Both the player and the target entity.</td></tr>
 *   <tr><td>{@code radius}</td><td>All entities within {@code targetRadius} blocks of the
 *       target / block / player.</td></tr>
 *   <tr><td>{@code in_sight}</td><td>The entity the player is looking at, up to
 *       {@code targetInSightDistance} blocks away.</td></tr>
 * </table>
 */
@NullMarked
public final class TargetResolver {
    private static final String TAG = "Action";

    private TargetResolver() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Resolves the set of entities that {@code action} should run on.
     *
     * @param context               the current action context
     * @param targetMode            the raw targeting mode string (e.g. {@code "self"}, {@code "radius"})
     * @param targetRadius          the radius used when {@code targetMode} is {@code "radius"}
     * @param targetInSightDistance the look-ahead distance used when {@code targetMode} is {@code "in_sight"}
     * @return the resolved set of entities; never {@code null}, but may be empty
     */
    public static Set<Entity> resolve(
            ActionContext context,
            String targetMode,
            double targetRadius,
            int targetInSightDistance
    ) {
        Set<Entity> entities = new HashSet<>();

        switch (targetMode.toLowerCase()) {
            case "self" -> {
                entities.add(context.player());
            }

            case "all" -> {
                entities.add(context.player());
                if (context.target() != null)
                    entities.add(context.target());
            }

            case "other" -> {
                if (context.target() != null)
                    entities.add(context.target());
            }

            case "radius" -> {
                if (targetRadius > 0) {
                    Location center = resolveRadiusCenter(context);
                    Collection<Entity> nearby = center.getNearbyEntities(targetRadius, targetRadius, targetRadius);
                    entities.addAll(nearby);
                }
            }

            case "in_sight" -> {
                if (targetInSightDistance > 0) {
                    Entity inSight = context.player().getTargetEntity(targetInSightDistance);
                    if (inSight != null)
                        entities.add(inSight);
                }
            }

            default -> Log.debug(TAG, "Unknown target mode '{}' - no entities resolved.", targetMode);
        }

        return entities;
    }

    /**
     * Determines the center point for radius-based targeting.
     * Priority: event target entity > event block > triggering player.
     */
    private static Location resolveRadiusCenter(ActionContext context) {
        if (context.target() != null)
            return context.target().getLocation();
        if (context.block() != null)
            return context.block().getLocation();
        return context.player().getLocation();
    }
}
