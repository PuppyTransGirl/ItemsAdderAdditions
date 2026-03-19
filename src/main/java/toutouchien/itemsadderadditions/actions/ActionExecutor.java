package toutouchien.itemsadderadditions.actions;


import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.utils.Keyed;

@SuppressWarnings("unused")
@NullMarked
public abstract class ActionExecutor implements Keyed {
    public final String key() {
        return annotation().key();
    }

    public final boolean isAllowedFor(TriggerType type) {
        TriggerType[] allowed = annotation().triggers();
        if (allowed.length == 0)
            return true; // empty = unrestricted

        for (TriggerType t : allowed) {
            if (t == type)
                return true;
        }

        return false;
    }

    /**
     * Creates a fresh instance of this executor (used when loading items
     * so each item gets its own injected copy, avoiding shared state).
     */
    public final ActionExecutor newInstance() {
        try {
            return getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "ActionExecutor subclass must expose a no-arg constructor: " + getClass().getName(), e);
        }
    }

    @Parameter(key = "permission", type = String.class)
    @Nullable private String permission;

    @Parameter(key = "delay", type = Integer.class)
    private int delay = 0;

    public final void run(ActionContext context) {
        if (permission != null && !context.player().hasPermission(permission))
            return;

        if (delay <= 0) {
            execute(context);
            return;
        }

        Bukkit.getScheduler().runTaskLater(
                ItemsAdderAdditions.instance(),
                () -> execute(context),
                delay
        );
    }

    protected abstract void execute(ActionContext context);

    private Action annotation() {
        Action a = getClass().getAnnotation(Action.class);
        if (a == null)
            throw new IllegalStateException("Missing @Action annotation on: " + getClass().getName());
        return a;
    }
}
