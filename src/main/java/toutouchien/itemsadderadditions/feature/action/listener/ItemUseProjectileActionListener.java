package toutouchien.itemsadderadditions.feature.action.listener;

import dev.lone.itemsadder.api.CustomStack;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionDispatcher;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
public final class ItemUseProjectileActionListener implements Listener {
    private final Map<UUID, String> projectileItems = new ConcurrentHashMap<>();
    private final ActionDispatcher dispatcher;

    public ItemUseProjectileActionListener(ActionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null) return;

        TriggerType type = item.getData(DataComponentTypes.CONSUMABLE).animation() == ItemUseAnimation.DRINK
                ? TriggerType.ITEM_DRINK
                : TriggerType.ITEM_EAT;

        dispatcher.dispatch(
                customStack.getNamespacedID(),
                type,
                ActionContext.create(event.getPlayer(), type)
                        .heldItem(item)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onBowShot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack bow = event.getBow();
        if (bow == null) return;

        CustomStack customStack = CustomStack.byItemStack(bow);
        if (customStack == null) return;

        dispatcher.dispatch(
                customStack.getNamespacedID(),
                TriggerType.ITEM_BOW_SHOT,
                ActionContext.create(player, TriggerType.ITEM_BOW_SHOT)
                        .heldItem(bow)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null) return;

        projectileItems.put(event.getEntity().getUniqueId(), customStack.getNamespacedID());

        dispatcher.dispatch(
                customStack.getNamespacedID(),
                TriggerType.ITEM_THROW,
                ActionContext.create(player, TriggerType.ITEM_THROW)
                        .heldItem(item)
                        .build()
        );
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        String itemId = projectileItems.remove(event.getEntity().getUniqueId());
        if (itemId == null) return;
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        Entity hitEntity = event.getHitEntity();
        TriggerType type = hitEntity != null
                ? TriggerType.ITEM_HIT_ENTITY
                : TriggerType.ITEM_HIT_GROUND;

        dispatcher.dispatch(
                itemId,
                type,
                ActionContext.create(player, type)
                        .target(hitEntity)
                        .block(event.getHitBlock())
                        .build()
        );
    }
}
