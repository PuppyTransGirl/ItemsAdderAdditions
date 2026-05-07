package toutouchien.itemsadderadditions.actions;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.FontImages.PlayerHudsHolderWrapper;
import dev.lone.itemsadder.api.FontImages.PlayerQuantityHudWrapper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.utils.Task;
import toutouchien.itemsadderadditions.utils.other.Keyed;
import toutouchien.itemsadderadditions.utils.other.Log;
import toutouchien.itemsadderadditions.utils.other.ParameterInjector;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Base class for all custom item actions.
 *
 * <h2>What is an action?</h2>
 * An action is a one-shot mechanic that fires when a specific event happens on a custom
 * item (e.g. right-click, kill, pickup). Unlike behaviours, actions are stateless - each
 * invocation is independent and no per-player state is tracked.
 *
 * <h2>Implementing an action</h2>
 * <ol>
 *   <li>Annotate the subclass with {@link Action @Action(key = "your_key")}.</li>
 *   <li>Declare a no-arg constructor (required by {@link #newInstance()}).</li>
 *   <li>Add {@link Parameter @Parameter}-annotated fields for any YAML parameters.</li>
 *   <li>Implement {@link #execute(ActionContext)} with the action-specific logic.</li>
 * </ol>
 *
 * <h2>YAML structure</h2>
 * <pre>
 * events:
 *   interact:
 *     your_key:          # matches @Action(key = …)
 *       some_param: 42   # injected into @Parameter fields
 * </pre>
 *
 * <h2>Shared parameters (available in every action)</h2>
 * <ul>
 *   <li>{@code target} - targeting mode: {@code self}, {@code other}, {@code all},
 *       {@code radius}, {@code in_sight} (default: {@code self})</li>
 *   <li>{@code target_radius} - radius in blocks; used when {@code target = radius}</li>
 *   <li>{@code target_in_sight_distance} - look-ahead distance; used when {@code target = in_sight}</li>
 *   <li>{@code permission} - permission node required to fire this action</li>
 *   <li>{@code delay} - delay in ticks before execution (default: 0)</li>
 * </ul>
 */
@NullMarked
public abstract class ActionExecutor implements Keyed {
    /**
     * Targeting mode. See {@link TargetResolver} for the full list of valid values.
     */
    @Parameter(key = "target", type = String.class)
    protected String target = "self";

    /**
     * Radius in blocks; used when {@code target = radius}.
     */
    @Parameter(key = "target_radius", type = Double.class)
    protected double targetRadius = 0;

    /**
     * Look-ahead in blocks; used when {@code target = in_sight}.
     */
    @Parameter(key = "target_in_sight_distance", type = Integer.class)
    protected int targetInSightDistance;

    /**
     * Optional permission node. The action is skipped when the player lacks this permission.
     */
    @Parameter(key = "permission", type = String.class)
    @Nullable private String permission;

    /**
     * Ticks to wait before executing. {@code 0} = run immediately.
     */
    @Parameter(key = "delay", type = Integer.class)
    private int delay = 0;

    /**
     * {@inheritDoc} Returns the YAML key from the {@link Action} annotation.
     */
    @Override
    public final String key() {
        return annotation().key();
    }

    /**
     * Creates a fresh, injectable instance via no-arg reflection.
     * Each item binding gets its own copy so field injection is isolated.
     *
     * @throws IllegalStateException if the subclass has no no-arg constructor
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
     * Reads YAML configuration into this executor's {@link Parameter}-annotated fields.
     *
     * <p>The default implementation delegates to {@link ParameterInjector}. Subclasses
     * that need to read non-standard structures (lists, polymorphic sections, etc.)
     * should override this and call {@code super.configure(...)} first to inject the
     * shared fields ({@code permission}, {@code delay}, etc.).
     *
     * @param configData   the raw YAML value for this action's key (typically a
     *                     {@link ConfigurationSection}, but may be {@code null})
     * @param namespacedID the item's namespaced ID; used in log messages only
     * @return {@code true} if configuration succeeded and this executor should be loaded
     */
    public boolean configure(@Nullable Object configData, String namespacedID) {
        ConfigurationSection section = configData instanceof ConfigurationSection cs ? cs : null;
        return ParameterInjector.inject(this, section, namespacedID);
    }

    /**
     * Returns {@code true} if this action may fire for the given trigger type.
     *
     * <p>Actions with an empty {@link Action#triggers()} array are allowed for every trigger type.
     */
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
     * Runs this action for the given context.
     *
     * <p>Execution pipeline:
     * <ol>
     *   <li>Check permission - abort if the player lacks it.</li>
     *   <li>Check cooldown / stat requirements on the held item - abort if blocked.</li>
     *   <li>Resolve target entities via {@link TargetResolver}.</li>
     *   <li>For each target: call {@link #execute(ActionContext)}, respecting {@code delay}.</li>
     * </ol>
     */
    @SuppressWarnings("java:S6916")
    public final void run(ActionContext context) {
        if (!isPermitted(context))
            return;

        if (!meetsItemRequirements(context))
            return;

        Set<Entity> targets = TargetResolver.resolve(context, target, targetRadius, targetInSightDistance);

        for (Entity entity : targets) {
            if (delay <= 0) {
                context.runOn(entity);
                execute(context);
            } else {
                Task.runDelayed(
                        task -> {
                            context.runOn(entity);
                            execute(context);
                        },
                        ItemsAdderAdditions.instance(),
                        entity,
                        delay * 50L,
                        TimeUnit.MILLISECONDS
                );
            }
        }
    }

    /**
     * Performs the action-specific logic.
     * Called once per resolved target entity after all shared checks pass.
     *
     * @param context the action context; {@link ActionContext#runOn()} holds the current target entity
     */
    protected abstract void execute(ActionContext context);

    /**
     * Returns {@code true} when no permission is required, or the player has the required permission.
     */
    private boolean isPermitted(ActionContext context) {
        if (permission == null)
            return true;

        boolean allowed = context.player().hasPermission(permission);
        if (!allowed)
            Log.debug("Action", "Skipping: player '{}' lacks permission '{}'",
                    context.player().getName(), permission);
        return allowed;
    }

    /**
     * Returns {@code true} when the held item's cooldown and stat requirements are satisfied,
     * or when no custom item is being held.
     */
    private boolean meetsItemRequirements(ActionContext context) {
        if (context.heldItem() == null)
            return true;

        CustomStack customStack = CustomStack.byItemStack(context.heldItem());
        if (customStack == null)
            return true;

        ItemStack itemStack = customStack.getItemStack();

        if (CustomStack.hasCooldown(context.player(), itemStack)) {
            Log.debug(
                    "Action",
                    "Skipping: '{}' is on cooldown for player '{}'",
                    customStack.getNamespacedID(),
                    context.player().getName()
            );
            return false;
        }

        if (!CustomStack.requiresStats(itemStack))
            return true;

        Map<String, String> requiredStats = CustomStack.getRequiredStats(itemStack);
        if (requiredStats == null || requiredStats.isEmpty())
            return true;

        for (Map.Entry<String, String> entry : requiredStats.entrySet()) {
            String statName = entry.getKey();
            String rule = entry.getValue();

            float playerValue = getPlayerStatValue(context.player(), statName, 0.0f);
            if (!matchesRule(playerValue, rule)) {
                Log.debug(
                        "Action",
                        "Skipping: stat requirement failed for '{}' (player '{}'): {} {} " +
                                "(actual: {})",
                        customStack.getNamespacedID(),
                        context.player().getName(),
                        statName,
                        rule,
                        playerValue
                );

                return false;
            }
        }

        return true;
    }

    private float getPlayerStatValue(Player player, String statName, float defaultValue) {
        PlayerHudsHolderWrapper huds = new PlayerHudsHolderWrapper(player);
        //noinspection deprecation
        PlayerQuantityHudWrapper statHud = new PlayerQuantityHudWrapper(huds, statName);

        if (!statHud.exists())
            return defaultValue;

        return statHud.getFloatValue();
    }

    private boolean matchesRule(float actualValue, @Nullable String rule) {
        if (rule == null || rule.length() < 2)
            return false;

        char operator = rule.charAt(0);

        final float expectedValue;
        try {
            expectedValue = Float.parseFloat(rule.substring(1));
        } catch (NumberFormatException e) {
            return false;
        }

        return switch (operator) {
            case '>' -> actualValue > expectedValue;
            case '<' -> actualValue < expectedValue;
            case '=' -> actualValue == expectedValue;
            default -> false;
        };
    }

    /**
     * Reads the {@link Action} annotation; throws if missing (indicates a programming mistake).
     */
    private Action annotation() {
        Action a = getClass().getAnnotation(Action.class);
        if (a == null)
            throw new IllegalStateException("Missing @Action annotation on: " + getClass().getName());
        return a;
    }
}
