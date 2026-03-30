package toutouchien.itemsadderadditions.actions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.utils.other.Keyed;
import toutouchien.itemsadderadditions.utils.other.ParameterInjector;

import java.util.HashSet;
import java.util.Set;

@NullMarked
public abstract class ActionExecutor implements Keyed {

    @Parameter(key = "permission", type = String.class)
    @Nullable private String permission;

    @Parameter(key = "delay", type = Integer.class)
    private int delay = 0;

    @Parameter(key = "target", type = String.class)
    protected String target = "self";

    @Parameter(key = "target_radius", type = Double.class)
    protected double targetRadius = 0;

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
        if (permission != null && !context.player().hasPermission(permission))
            return;

        Set<Entity> entities = new HashSet<>();
        entities.add(context.player());

        if (target.equalsIgnoreCase("all") && context.target() != null)
            entities.add(context.target());

        if (target.equalsIgnoreCase("other")) {
            entities.clear();
            if (context.target() != null)
                entities.add(context.target());
        }

        if (target.equalsIgnoreCase("radius") && targetRadius != 0) {
            entities.clear();
            Location center;
            if (context.target() != null)
                center = context.target().getLocation();
            else if (context.block() != null)
                center = context.block().getLocation();
            else
                center = context.player().getLocation();

            entities.addAll(center.getNearbyEntities(targetRadius / 2, targetRadius / 2, targetRadius / 2));
        }

        for (Entity entity : entities) {
            context.runOn(entity);

            if (delay <= 0) {
                execute(context);
                continue;
            }

            Bukkit.getScheduler().runTaskLater(
                    ItemsAdderAdditions.instance(),
                    () -> execute(context),
                    delay
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
