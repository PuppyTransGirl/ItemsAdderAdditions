package toutouchien.itemsadderadditions.actions;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.cooldown.CooldownBridge;
import toutouchien.itemsadderadditions.utils.Task;
import toutouchien.itemsadderadditions.utils.other.Keyed;
import toutouchien.itemsadderadditions.utils.other.Log;
import toutouchien.itemsadderadditions.utils.other.ParameterInjector;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@NullMarked
public abstract class ActionExecutor implements Keyed {
    @Parameter(key = "target", type = String.class) protected String target = "self";
    @Parameter(key = "target_radius", type = Double.class) protected double targetRadius = 0;
    @Parameter(key = "target_in_sight_distance", type = Integer.class) protected int targetInSightDistance;
    @Parameter(key = "permission", type = String.class) @Nullable private String permission;
    @Parameter(key = "delay", type = Integer.class) private int delay = 0;

    public final String key() {
        return annotation().key();
    }

    public final boolean isAllowedFor(TriggerType type) {
        TriggerType[] allowed = annotation().triggers();
        if (allowed.length == 0)
            return true;

        for (TriggerType t : allowed)
            if (t == type)
                return true;

        return false;
    }

    /**
     * Creates a fresh instance of this executor via no-arg reflection.
     * Each loaded item gets its own isolated, injectable copy.
     */
    public final ActionExecutor newInstance() {
        try {
            return getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "ActionExecutor subclass must expose a no-arg constructor: " + getClass().getName(), e);
        }
    }

    /**
     * Reads YAML configuration into this executor's fields.
     *
     * <p>The default implementation runs {@link ParameterInjector} over all
     * {@link Parameter}-annotated fields. Subclasses that need to read non-standard
     * structures (lists, polymorphic sections, etc.) should override this and call
     * {@code super.configure(configData, namespacedID)} first to inject base fields
     * ({@code permission}, {@code delay}).
     *
     * @param configData   the raw YAML value for this action's key (usually a
     *                     {@link ConfigurationSection}, but may be {@code null})
     * @param namespacedID the item's namespaced ID, used only in log messages
     * @return {@code true} if configuration succeeded and the executor should be loaded
     */
    public boolean configure(@Nullable Object configData, String namespacedID) {
        ConfigurationSection section = configData instanceof ConfigurationSection cs ? cs : null;
        return ParameterInjector.inject(this, section, namespacedID);
    }

    public final void run(ActionContext context) {
        Log.debug("Action", "Running action. target={}, delay={}, permission={}, player={}",
                target, delay, permission, context.player().getName());

        if (permission != null && !context.player().hasPermission(permission)) {
            Log.debug("Action", "Skipping: player lacks permission '{}'", permission);
            return;
        }

        ItemStack heldItem = context.heldItem();
        if (heldItem != null) {
            CustomStack customStack = CustomStack.byItemStack(heldItem);
            if (customStack != null) {
                int itemHash = customStack.getNamespacedID().hashCode();
                Log.debug("Action", "Held custom item detected: id={}, hash={}",
                        customStack.getNamespacedID(), itemHash);

                if (CooldownBridge.isOnCooldown(context.player(), itemHash)) {
                    Log.debug("Action", "Skipping: item is on cooldown for player {}",
                            context.player().getName());
                    return;
                }
            } else {
                Log.debug("Action", "Held item is not a custom stack");
            }
        } else {
            Log.debug("Action", "No held item");
        }

        Set<Entity> entities = new HashSet<>();

        if (target.equalsIgnoreCase("self")) {
            Log.debug("Action", "Resolving target mode 'self'");
            entities.add(context.player());
        }

        if (target.equalsIgnoreCase("all") && context.target() != null) {
            Log.debug("Action", "Resolving target mode 'all'");
            entities.add(context.player());
            entities.add(context.target());
        }

        if (target.equalsIgnoreCase("other")) {
            Log.debug("Action", "Resolving target mode 'other'");
            if (context.target() != null)
                entities.add(context.target());
        }

        if (target.equalsIgnoreCase("radius") && targetRadius != 0) {
            Location center;
            if (context.target() != null) {
                center = context.target().getLocation();
                Log.debug("Action", "Resolving radius center from target entity");
            } else if (context.block() != null) {
                center = context.block().getLocation();
                Log.debug("Action", "Resolving radius center from block");
            } else {
                center = context.player().getLocation();
                Log.debug("Action", "Resolving radius center from player");
            }

            var nearby = center.getNearbyEntities(targetRadius / 2, targetRadius / 2, targetRadius / 2);
            Log.debug("Action", "Found {} nearby entities within radius {}", nearby.size(), targetRadius);
            entities.addAll(nearby);
        }

        if (target.equalsIgnoreCase("in_sight") && targetInSightDistance != 0) {
            Log.debug("Action", "Resolving target mode 'in_sight' with distance {}",
                    targetInSightDistance);
            Entity targetEntity = context.player().getTargetEntity(targetInSightDistance);
            if (targetEntity != null) {
                Log.debug("Action", "Found target in sight: {}", targetEntity.getType());
                entities.add(targetEntity);
            } else {
                Log.debug("Action", "No target found in sight");
            }
        }

        Log.debug("Action", "Resolved {} target entity/entities", entities.size());

        for (Entity entity : entities) {
            Log.debug("Action", "Executing for entity {} (delay={}ms)", entity.getType(),
                    delay * 50L);

            if (delay <= 0) {
                context.runOn(entity);
                execute(context);
                Log.debug("Action", "Executed immediately for entity {}", entity.getType());
                continue;
            }

            Task.runDelayed(
                    task -> {
                        Log.debug("Action", "Delayed execution started for entity {}",
                                entity.getType());
                        context.runOn(entity);
                        execute(context);
                        Log.debug("Action", "Delayed execution finished for entity {}",
                                entity.getType());
                    },
                    ItemsAdderAdditions.instance(),
                    entity,
                    delay * 50L,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    protected abstract void execute(ActionContext context);

    private Action annotation() {
        Action a = getClass().getAnnotation(Action.class);
        if (a == null)
            throw new IllegalStateException("Missing @Action annotation on: " + getClass().getName());

        return a;
    }
}
