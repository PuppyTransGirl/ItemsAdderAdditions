package toutouchien.itemsadderadditions.actions;

import dev.lone.itemsadder.api.CustomComplexFurniture;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;


/**
 * Immutable context object describing a single action trigger event.
 *
 * <p>Created by {@link toutouchien.itemsadderadditions.actions.ActionsListener} for every
 * qualifying Bukkit event and passed to each matching {@link ActionExecutor#run(ActionContext)}.
 *
 * <p>The {@link #runOn()} entity is the only mutable field: it is set by
 * {@link ActionExecutor} immediately before calling {@link ActionExecutor#execute(ActionContext)}
 * to indicate which entity in the resolved target set is currently being processed.
 *
 * <h3>Nullable fields</h3>
 * Not every trigger provides every piece of context. For example:
 * <ul>
 *   <li>{@link #block()} is non-null only when the trigger involves a block interaction.</li>
 *   <li>{@link #target()} is non-null only when the trigger involves a target entity.</li>
 *   <li>{@link #heldItem()} may be null when no item is held.</li>
 *   <li>{@link #eventArgument()} is non-null only for argumentized triggers (e.g. {@code interact}).</li>
 * </ul>
 *
 * @see ActionExecutor
 * @see toutouchien.itemsadderadditions.actions.TargetResolver
 */
@NullMarked
public final class ActionContext {
    private final Player player;
    private final TriggerType triggerType;
    @Nullable private final Block block;
    @Nullable private final Entity target;
    @Nullable private final CustomComplexFurniture complexFurniture;
    @Nullable private final ItemStack heldItem;
    /**
     * The event argument that qualified this trigger, if any
     * (e.g. {@code "right"}, {@code "left_shift"}, {@code "entity"} for interact triggers).
     * {@code null} when the trigger carries no argument.
     */
    @Nullable private final String eventArgument;

    private Entity runOn;

    private ActionContext(Builder builder) {
        this.player = builder.player;
        this.triggerType = builder.triggerType;
        this.block = builder.block;
        this.target = builder.target;
        this.complexFurniture = builder.complexFurniture;
        this.heldItem = builder.heldItem;
        this.eventArgument = builder.eventArgument;
    }

    public static Builder create(Player player, TriggerType type) {
        return new Builder(player, type);
    }

    public Player player() {
        return player;
    }

    public TriggerType triggerType() {
        return triggerType;
    }

    @Nullable
    public Block block() {
        return block;
    }

    @Nullable
    public Entity target() {
        return target;
    }

    @Nullable
    public CustomComplexFurniture complexFurniture() {
        return complexFurniture;
    }

    @Nullable
    public ItemStack heldItem() {
        return heldItem;
    }

    /**
     * The event argument that qualified this trigger, or {@code null} if none.
     *
     * @see TriggerKey
     */
    @Nullable
    public String eventArgument() {
        return eventArgument;
    }

    public Entity runOn() {
        return runOn;
    }

    void runOn(Entity runOn) {
        this.runOn = runOn;
    }

    public static final class Builder {
        private final Player player;
        private final TriggerType triggerType;
        @Nullable private Block block;
        @Nullable private Entity target;
        @Nullable private CustomComplexFurniture complexFurniture;
        @Nullable private ItemStack heldItem;
        @Nullable private String eventArgument;

        private Builder(Player player, TriggerType triggerType) {
            this.player = player;
            this.triggerType = triggerType;
        }

        public Builder block(@Nullable Block block) {
            this.block = block;
            return this;
        }

        public Builder target(@Nullable Entity target) {
            this.target = target;
            return this;
        }

        public Builder complexFurniture(@Nullable CustomComplexFurniture complexFurniture) {
            this.complexFurniture = complexFurniture;
            return this;
        }

        public Builder heldItem(@Nullable ItemStack item) {
            this.heldItem = item;
            return this;
        }

        public Builder eventArgument(@Nullable String argument) {
            this.eventArgument = argument;
            return this;
        }

        public ActionContext build() {
            return new ActionContext(this);
        }
    }
}
