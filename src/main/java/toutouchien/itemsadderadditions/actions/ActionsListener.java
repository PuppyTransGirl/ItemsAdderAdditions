package toutouchien.itemsadderadditions.actions;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomEntity;
import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.actions.loading.ActionBindings;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
@NullMarked
public final class ActionsListener implements Listener {
    /**
     * Maps a launched projectile's UUID to the namespaced ID of the custom item
     * that was held when it was thrown. Needed because ProjectileHitEvent does
     * not carry the original item.
     */
    private final Map<UUID, String> projectileItems = new ConcurrentHashMap<>();

    /**
     * Maps a {@link PlayerInteractEvent} to an event argument string used as the
     * qualifier in {@link TriggerKey}.
     *
     * <table>
     *   <tr><th>Bukkit Action</th><th>Sneaking</th><th>Argument</th></tr>
     *   <tr><td>RIGHT_CLICK_AIR / RIGHT_CLICK_BLOCK</td><td>No</td><td>{@code "right"}</td></tr>
     *   <tr><td>RIGHT_CLICK_AIR / RIGHT_CLICK_BLOCK</td><td>Yes</td><td>{@code "right_shift"}</td></tr>
     *   <tr><td>LEFT_CLICK_AIR / LEFT_CLICK_BLOCK</td><td>No</td><td>{@code "left"}</td></tr>
     *   <tr><td>LEFT_CLICK_AIR / LEFT_CLICK_BLOCK</td><td>Yes</td><td>{@code "left_shift"}</td></tr>
     * </table>
     *
     * @return the argument string, or {@code null} if the action is not mappable
     */
    @Nullable
    private static String resolveInteractArgument(PlayerInteractEvent event) {
        boolean shift = event.getPlayer().isSneaking();
        return switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> shift ? "right_shift" : "right";
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> shift ? "left_shift" : "left";
            default -> null;
        };
    }

    @EventHandler(ignoreCancelled = true)
    public void onComplexFurnitureInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        if (block.getType() != Material.BARRIER)
            return;

        Location location = block.getLocation();
        Collection<Entity> nearbyEntities = block.getWorld().getNearbyEntities(
                location, 0.1D, 0.1D, 0.1D,
                entity -> entity.getType() == EntityType.ARMOR_STAND
        );
        if (nearbyEntities.isEmpty())
            return;

        CustomEntity customEntity = null;
        for (Entity entity : nearbyEntities) {
            customEntity = CustomEntity.byAlreadySpawned(entity);
            if (customEntity != null)
                break;
        }

        if (customEntity == null)
            return;

        Player player = event.getPlayer();
        String namespacedID = customEntity.getNamespacedID();
        Entity entity = customEntity.getEntity();
        ItemStack held = player.getInventory().getItemInMainHand();

        dispatch(
                namespacedID,
                TriggerType.COMPLEX_FURNITURE_INTERACT,
                ActionContext.create(player, TriggerType.COMPLEX_FURNITURE_INTERACT)
                        .complexFurniture(entity)
                        .heldItem(held)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnitureInteract(FurnitureInteractEvent event) {
        CustomFurniture furniture = event.getFurniture();
        if (furniture == null)
            return;

        Player player = event.getPlayer();
        String namespacedID = furniture.getNamespacedID();
        ItemStack held = player.getInventory().getItemInMainHand();

        // FurnitureInteractEvent does not expose a click action, so we default to "right"
        String argument = "right";

        dispatch(
                namespacedID,
                TriggerType.FURNITURE_INTERACT,
                argument,
                ActionContext.create(player, TriggerType.FURNITURE_INTERACT)
                        .heldItem(held)
                        .eventArgument(argument)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();
        CustomStack customStack = item == null ? null : CustomStack.byItemStack(item);
        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(event.getClickedBlock());

        if (customStack != null) {
            dispatch(
                    customStack.getNamespacedID(),
                    TriggerType.BLOCK_INTERACT,
                    ActionContext.create(event.getPlayer(), TriggerType.BLOCK_INTERACT)
                            .block(event.getClickedBlock())
                            .heldItem(event.getPlayer().getInventory().getItemInMainHand())
                            .build()
            );
        }

        if (customBlock != null) {
            dispatch(
                    customBlock.getNamespacedID(),
                    TriggerType.BLOCK_INTERACT,
                    ActionContext.create(event.getPlayer(), TriggerType.BLOCK_INTERACT)
                            .block(event.getClickedBlock())
                            .heldItem(event.getPlayer().getInventory().getItemInMainHand())
                            .build()
            );
        }
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent event) {
        String argument = resolveInteractArgument(event);
        if (argument == null)
            return; // PHYSICAL or unmappable - not an interact trigger

        // PlayerInteractEvent is fired as cancelled when vanilla has nothing to do
        // (e.g. interacting with air), so we cannot use ignoreCancelled = true.
        // We do skip it when the player actually clicked a block and the block
        // interaction was explicitly denied, which means the click did nothing.
        if (!isInteractAllowed(event))
            return;

        ItemStack item = event.getItem();
        if (item == null)
            return;

        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null)
            return;

        Player player = event.getPlayer();
        String namespacedID = customStack.getNamespacedID();
        Block clicked = event.getClickedBlock();

        ActionContext.Builder base = ActionContext.create(player, TriggerType.ITEM_INTERACT)
                .block(clicked)
                .heldItem(item)
                .eventArgument(argument);

        dispatch(namespacedID, TriggerType.ITEM_INTERACT, argument, base.build());

        TriggerType handType = event.getHand() == EquipmentSlot.HAND
                ? TriggerType.ITEM_INTERACT_MAINHAND
                : TriggerType.ITEM_INTERACT_OFFHAND;

        dispatch(
                namespacedID,
                handType,
                argument,
                ActionContext.create(player, handType)
                        .block(clicked)
                        .heldItem(item)
                        .eventArgument(argument)
                        .build()
        );
    }

    /**
     * Returns {@code true} when the interact event should be processed.
     * Air-clicks are always allowed. Block-clicks are allowed unless the block
     * interaction was explicitly denied by another listener.
     */
    private static boolean isInteractAllowed(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.RIGHT_CLICK_AIR)
            return true;
        return event.useInteractedBlock() != Event.Result.DENY;
    }

    /**
     * Handles right-clicking directly on an entity while holding a custom item.
     * This fires the interact trigger with argument {@code "entity"}.
     */
    @EventHandler(ignoreCancelled = true)
    public void onItemInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();

        ItemStack item = hand == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null)
            return;

        String namespacedID = customStack.getNamespacedID();
        String argument = "entity";

        ActionContext.Builder base = ActionContext.create(player, TriggerType.ITEM_INTERACT)
                .target(event.getRightClicked())
                .heldItem(item)
                .eventArgument(argument);

        // Generic interact
        dispatch(namespacedID, TriggerType.ITEM_INTERACT, argument, base.build());

        // Hand-specific
        TriggerType handType = hand == EquipmentSlot.HAND
                ? TriggerType.ITEM_INTERACT_MAINHAND
                : TriggerType.ITEM_INTERACT_OFFHAND;

        dispatch(
                namespacedID,
                handType,
                argument,
                ActionContext.create(player, handType)
                        .target(event.getRightClicked())
                        .heldItem(item)
                        .eventArgument(argument)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        CustomStack cs = CustomStack.byItemStack(tool);
        if (cs == null)
            return;

        dispatch(
                cs.getNamespacedID(),
                TriggerType.ITEM_BREAK_BLOCK,
                ActionContext.create(player, TriggerType.ITEM_BREAK_BLOCK)
                        .block(event.getBlock())
                        .heldItem(tool)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player))
            return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        CustomStack cs = CustomStack.byItemStack(tool);
        if (cs == null)
            return;

        dispatch(
                cs.getNamespacedID(),
                TriggerType.ITEM_ATTACK,
                ActionContext.create(player, TriggerType.ITEM_ATTACK)
                        .target(event.getEntity())
                        .heldItem(tool)
                        .build()
        );
    }

    @EventHandler
    public void onItemKill(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null)
            return;

        ItemStack tool = killer.getInventory().getItemInMainHand();
        CustomStack cs = CustomStack.byItemStack(tool);
        if (cs == null)
            return;

        dispatch(
                cs.getNamespacedID(),
                TriggerType.ITEM_KILL,
                ActionContext.create(killer, TriggerType.ITEM_KILL)
                        .target(dead)
                        .heldItem(tool)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        CustomStack cs = CustomStack.byItemStack(item);
        if (cs == null)
            return;

        dispatch(cs.getNamespacedID(),
                TriggerType.ITEM_DROP,
                ActionContext.create(event.getPlayer(), TriggerType.ITEM_DROP)
                        .heldItem(item)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        ItemStack item = event.getItem().getItemStack();
        CustomStack cs = CustomStack.byItemStack(item);
        if (cs == null)
            return;

        dispatch(
                cs.getNamespacedID(),
                TriggerType.ITEM_PICKUP,
                ActionContext.create(player, TriggerType.ITEM_PICKUP)
                        .heldItem(item)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        // Item leaving main hand
        ItemStack leaving = player.getInventory().getItem(event.getPreviousSlot());
        if (leaving != null && !leaving.isEmpty()) {
            CustomStack cs = CustomStack.byItemStack(leaving);
            if (cs != null) {
                dispatch(cs.getNamespacedID(), TriggerType.ITEM_UNHELD,
                        ActionContext.create(player, TriggerType.ITEM_UNHELD)
                                .heldItem(leaving).build());
            }
        }

        // Item entering main hand
        ItemStack entering = player.getInventory().getItem(event.getNewSlot());
        if (entering != null && !entering.isEmpty()) {
            CustomStack cs = CustomStack.byItemStack(entering);
            if (cs != null) {
                dispatch(
                        cs.getNamespacedID(),
                        TriggerType.ITEM_HELD,
                        ActionContext.create(player, TriggerType.ITEM_HELD)
                                .heldItem(entering)
                                .build()
                );
            }
        }
    }

    /**
     * Covers held_offhand / unheld_offhand via the F-key swap.
     * Items moved into the offhand slot directly through the inventory
     * screen are not currently detected.
     */
    @EventHandler(ignoreCancelled = true)
    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        // Main-hand item - moving to offhand
        ItemStack toOffhand = event.getMainHandItem();
        if (!toOffhand.isEmpty()) {
            CustomStack cs = CustomStack.byItemStack(toOffhand);
            if (cs != null) {
                dispatch(
                        cs.getNamespacedID(),
                        TriggerType.ITEM_HELD_OFFHAND,
                        ActionContext.create(player, TriggerType.ITEM_HELD_OFFHAND)
                                .heldItem(toOffhand)
                                .build()
                );
            }
        }

        // Offhand item - moving to main hand (leaving offhand)
        ItemStack fromOffhand = event.getOffHandItem();
        if (!fromOffhand.isEmpty()) {
            CustomStack cs = CustomStack.byItemStack(fromOffhand);
            if (cs != null) {
                dispatch(cs.getNamespacedID(),
                        TriggerType.ITEM_UNHELD_OFFHAND,
                        ActionContext.create(player, TriggerType.ITEM_UNHELD_OFFHAND)
                                .heldItem(fromOffhand)
                                .build()
                );
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent event) {
        ItemStack item = event.getBrokenItem();
        CustomStack cs = CustomStack.byItemStack(item);
        if (cs == null)
            return;

        dispatch(
                cs.getNamespacedID(),
                TriggerType.ITEM_BREAK,
                ActionContext.create(event.getPlayer(), TriggerType.ITEM_BREAK)
                        .heldItem(item)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        CustomStack cs = CustomStack.byItemStack(item);
        if (cs == null)
            return;

        TriggerType type = item.getData(DataComponentTypes.CONSUMABLE).animation() == ItemUseAnimation.DRINK
                ? TriggerType.ITEM_DRINK
                : TriggerType.ITEM_EAT;

        dispatch(
                cs.getNamespacedID(),
                type,
                ActionContext.create(event.getPlayer(), type)
                        .heldItem(item)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onBowShot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        ItemStack bow = event.getBow();
        if (bow == null)
            return;

        CustomStack cs = CustomStack.byItemStack(bow);
        if (cs == null)
            return;

        dispatch(
                cs.getNamespacedID(),
                TriggerType.ITEM_BOW_SHOT,
                ActionContext.create(player, TriggerType.ITEM_BOW_SHOT)
                        .heldItem(bow)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player))
            return;

        ItemStack item = player.getInventory().getItemInMainHand();
        CustomStack cs = CustomStack.byItemStack(item);
        if (cs == null)
            return;

        // Track which item launched this projectile so the hit handler can resolve it.
        projectileItems.put(event.getEntity().getUniqueId(), cs.getNamespacedID());

        dispatch(
                cs.getNamespacedID(),
                TriggerType.ITEM_THROW,
                ActionContext.create(player, TriggerType.ITEM_THROW)
                        .heldItem(item)
                        .build()
        );
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        String itemID = projectileItems.remove(event.getEntity().getUniqueId());
        if (itemID == null)
            return;

        if (!(event.getEntity().getShooter() instanceof Player player))
            return;

        Entity hitEntity = event.getHitEntity();
        TriggerType type = hitEntity != null
                ? TriggerType.ITEM_HIT_ENTITY
                : TriggerType.ITEM_HIT_GROUND;

        dispatch(itemID, type,
                ActionContext.create(player, type)
                        .target(hitEntity)
                        .block(event.getHitBlock())
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onBookWrite(PlayerEditBookEvent event) {
        int slot = event.getSlot();

        ItemStack book = slot == -1
                ? event.getPlayer().getInventory().getItemInOffHand()
                : event.getPlayer().getInventory().getItem(slot);

        if (book == null)
            return;

        CustomStack cs = CustomStack.byItemStack(book);
        if (cs == null)
            return;

        dispatch(
                cs.getNamespacedID(),
                TriggerType.ITEM_BOOK_WRITE,
                ActionContext.create(event.getPlayer(), TriggerType.ITEM_BOOK_WRITE)
                        .heldItem(book)
                        .build()
        );
    }

    /**
     * Fires when a player right-clicks with a WRITTEN_BOOK (opening it counts as reading).
     * This reuses PlayerInteractEvent, so it only fires once the client opens the book GUI.
     */
    @EventHandler(ignoreCancelled = true)
    public void onBookRead(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.WRITTEN_BOOK)
            return;

        CustomStack cs = CustomStack.byItemStack(item);
        if (cs == null)
            return;

        dispatch(
                cs.getNamespacedID(),
                TriggerType.ITEM_BOOK_READ,
                ActionContext.create(event.getPlayer(), TriggerType.ITEM_BOOK_READ)
                        .block(event.getClickedBlock())
                        .heldItem(item)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onFishing(PlayerFishEvent event) {
        Player player = event.getPlayer();

        EquipmentSlot hand = event.getHand();

        // The fishing rod may be in either hand
        ItemStack rod = hand == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

        CustomStack cs = CustomStack.byItemStack(rod);
        if (cs == null)
            return;

        TriggerType type = switch (event.getState()) {
            case FISHING -> TriggerType.ITEM_FISHING_START;
            case CAUGHT_FISH, CAUGHT_ENTITY -> TriggerType.ITEM_FISHING_CAUGHT;
            case FAILED_ATTEMPT -> TriggerType.ITEM_FISHING_FAILED;
            case REEL_IN -> TriggerType.ITEM_FISHING_CANCEL;
            case BITE -> TriggerType.ITEM_FISHING_BITE;
            case IN_GROUND -> TriggerType.ITEM_FISHING_IN_GROUND;
            case LURED -> null;
        };

        if (type == null)
            return;

        dispatch(
                cs.getNamespacedID(),
                type,
                ActionContext.create(player, type)
                        .heldItem(rod)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        ItemStack bucket = event.getItemStack();
        CustomStack cs = CustomStack.byItemStack(bucket);
        if (cs == null)
            return;

        dispatch(
                cs.getNamespacedID(),
                TriggerType.ITEM_BUCKET_EMPTY,
                ActionContext.create(event.getPlayer(), TriggerType.ITEM_BUCKET_EMPTY)
                        .block(event.getBlockClicked())
                        .heldItem(bucket)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        ItemStack bucket = event.getItemStack();
        CustomStack cs = CustomStack.byItemStack(bucket);
        if (cs == null)
            return;

        dispatch(
                cs.getNamespacedID(),
                TriggerType.ITEM_BUCKET_FILL,
                ActionContext.create(event.getPlayer(), TriggerType.ITEM_BUCKET_FILL)
                        .block(event.getBlockClicked())
                        .heldItem(bucket)
                        .build()
        );
    }

    private void dispatch(String id, TriggerType type, @Nullable String argument, ActionContext context) {
        List<ActionExecutor> executors = ActionBindings.get(id, type, argument);
        for (ActionExecutor executor : executors)
            executor.run(context);
    }

    private void dispatch(String id, TriggerType type, ActionContext context) {
        dispatch(id, type, null, context);
    }
}
