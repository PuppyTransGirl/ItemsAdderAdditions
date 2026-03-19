package toutouchien.itemsadderadditions.actions;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;


@NullMarked
public final class ActionContext {
    private final Player player;
    private final TriggerType triggerType;
    @Nullable private final Block block;
    @Nullable private final Entity entity;
    @Nullable private final ItemStack heldItem;
    /**
     * The event argument that qualified this trigger, if any
     * (e.g. {@code "right"}, {@code "left_shift"}, {@code "entity"} for interact triggers).
     * {@code null} when the trigger carries no argument.
     */
    @Nullable private final String eventArgument;

    private ActionContext(Builder builder) {
        this.player = builder.player;
        this.triggerType = builder.triggerType;
        this.block = builder.block;
        this.entity = builder.entity;
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
    public Entity entity() {
        return entity;
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

    public static final class Builder {
        private final Player player;
        private final TriggerType triggerType;
        @Nullable private Block block;
        @Nullable private Entity entity;
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

        public Builder entity(@Nullable Entity entity) {
            this.entity = entity;
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
