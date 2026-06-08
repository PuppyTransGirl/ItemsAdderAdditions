package toutouchien.itemsadderadditions.feature.action.listener;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import org.bukkit.Material;
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
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.common.version.VersionUtils;
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
        String itemId = NamespaceUtils.itemID(item);
        if (itemId == null) return;

        TriggerType type = isDrink(item)
                ? TriggerType.ITEM_DRINK
                : TriggerType.ITEM_EAT;

        dispatcher.dispatch(
                itemId,
                type,
                ActionContext.create(event.getPlayer(), type)
                        .heldItem(item)
                        .build()
        );
    }

    private static boolean isDrink(ItemStack item) {
        if (VersionUtils.isHigherThanOrEquals(VersionUtils.v1_21_5)) {
            Consumable consumable = item.getData(DataComponentTypes.CONSUMABLE);
            if (consumable != null) {
                return consumable.animation() == ItemUseAnimation.DRINK;
            }
        }

        Material type = item.getType();
        return type == Material.POTION
                || type == Material.MILK_BUCKET
                || type == Material.HONEY_BOTTLE;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBowShot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack bow = event.getBow();
        if (bow == null) return;

        String bowId = NamespaceUtils.itemID(bow);
        if (bowId == null) return;

        dispatcher.dispatch(
                bowId,
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
        String itemId = NamespaceUtils.itemID(item);
        if (itemId == null) return;

        projectileItems.put(event.getEntity().getUniqueId(), itemId);

        dispatcher.dispatch(
                itemId,
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
