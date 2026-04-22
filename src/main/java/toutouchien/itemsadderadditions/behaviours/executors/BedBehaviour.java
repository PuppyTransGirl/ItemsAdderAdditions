package toutouchien.itemsadderadditions.behaviours.executors;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomEntity;
import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.behaviours.BehaviourExecutor;
import toutouchien.itemsadderadditions.behaviours.BehaviourHost;
import toutouchien.itemsadderadditions.behaviours.annotations.Behaviour;
import toutouchien.itemsadderadditions.bridge.BedBridge;
import toutouchien.itemsadderadditions.nms.api.INmsBedHandler;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.utils.Task;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Adds sleeping behaviour to custom furniture or blocks.
 *
 * <h3>Modes</h3>
 * Two exclusive modes are selected automatically based on the {@code skip_night} setting:
 *
 * <h4>Functional mode ({@code skip_night: true})</h4>
 * <p>Attempts to call {@link Player#sleep(Location, boolean)} directly at the furniture's
 * location. This does not place a fake bed block anymore, so vanilla bed validation will
 * usually reject it unless some other server-side logic makes the location valid.
 * All cleanup and phantom handling still works.</p>
 *
 * <h4>Decorative mode ({@code skip_night: false})</h4>
 * <p>The player is put into the {@code SLEEPING} entity pose via direct NMS entity-data
 * manipulation without touching the vanilla sleep state machine.
 * {@link Player#setSleepingIgnored(boolean)} is set to {@code true} so the player is
 * never counted toward the "all players asleep → skip night" check.
 * The player can leave by pressing the vanilla "Leave Bed" button, which flows through
 * {@link PlayerBedLeaveEvent} and is handled here for cleanup.
 * When {@code set_spawn} is {@code true}, the respawn location is updated via
 * {@link Player#setRespawnLocation(Location)}.</p>
 */
@SuppressWarnings("unused")
@NullMarked
@Behaviour(key = "bed")
public final class BedBehaviour extends BehaviourExecutor implements Listener {
    /**
     * Functional mode: maps a player UUID to the location they are sleeping at.
     */
    private final Map<UUID, Location> functionalSleepers = new HashMap<>();
    /**
     * Functional mode, {@code reset_phantoms: false}: phantom timer value captured
     * just before sleep so it can be restored after wakeup.
     */
    private final Map<UUID, Integer> savedPhantomTimers = new HashMap<>();
    /**
     * Decorative mode: maps a sleeping player's UUID to the furniture/block
     * {@link Location} they are sleeping at.
     */
    private final Map<UUID, Location> decorativeSleepers = new HashMap<>();
    @Parameter(key = "skip_night", type = Boolean.class)
    private boolean skipNight = true;
    @Parameter(key = "reset_phantoms", type = Boolean.class)
    private boolean resetPhantoms = true;
    @Parameter(key = "set_spawn", type = Boolean.class)
    private boolean setSpawn = true;
    private String namespacedID = "";
    private @Nullable JavaPlugin plugin;

    private static boolean shouldIgnoreOffHandDuplicate(PlayerInteractEvent event) {
        @Nullable EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.OFF_HAND) return false;
        return !event.getPlayer().getInventory().getItemInMainHand().isEmpty();
    }

    @Nullable
    private static CustomEntity findNearbyCustomEntity(Location location) {
        if (location.getWorld() == null) return null;

        Collection<Entity> nearby = location.getWorld().getNearbyEntities(
                location,
                0.1,
                0.1,
                0.1
        );

        for (Entity entity : nearby) {
            CustomEntity ce = CustomEntity.byAlreadySpawned(entity);
            if (ce != null) return ce;
        }
        return null;
    }

    @Nullable
    private static CustomFurniture findNearbyCustomFurniture(Location location) {
        if (location.getWorld() == null) return null;

        Collection<Entity> nearby = location.getWorld().getNearbyEntities(
                location,
                0.5,
                0.5,
                0.5
        );

        for (Entity entity : nearby) {
            if (entity instanceof Player)
                continue;

            CustomFurniture furniture = CustomFurniture.byAlreadySpawned(entity);
            if (furniture != null)
                return furniture;
        }
        return null;
    }

    private static Location createFunctionalSleepLocation(
            Player player,
            Location furnitureLocation
    ) {
        Location sleepLoc = furnitureLocation.clone();
        sleepLoc.setX(sleepLoc.getBlockX() + 0.5);
        sleepLoc.setY(sleepLoc.getBlockY());
        sleepLoc.setZ(sleepLoc.getBlockZ() + 0.5);
        sleepLoc.setYaw(
                sleepYawFromFurniture(
                        findNearbyCustomFurniture(furnitureLocation),
                        player.getLocation().getYaw()
                )
        );
        sleepLoc.setPitch(0f);
        return sleepLoc;
    }

    private static float sleepYawFromFurniture(
            @Nullable CustomFurniture furniture,
            float fallbackYaw
    ) {
        if (furniture == null) return fallbackYaw;

        Location loc = furniture.getEntity().getLocation();
        float yaw = loc.getYaw();

        if (Float.isNaN(yaw)) return fallbackYaw;

        // Beds generally need the sleeping pose to face the opposite direction
        // of the furniture's forward direction.
        return normalizeYaw(yaw + 180f);
    }

    private static float normalizeYaw(float yaw) {
        while (yaw <= -180f) yaw += 360f;
        while (yaw > 180f) yaw -= 360f;
        return yaw;
    }

    private static boolean isSameBlock(Location a, Location b) {
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ()
                && Objects.equals(a.getWorld(), b.getWorld());
    }

    @Override
    protected void onLoad(BehaviourHost host) {
        this.namespacedID = host.namespacedID();
        this.plugin = host.plugin();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    protected void onUnload(BehaviourHost host) {
        HandlerList.unregisterAll(this);

        INmsBedHandler bedHandler = NmsManager.instance().handler().bed();

        for (UUID uuid : new HashSet<>(decorativeSleepers.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setSleepingIgnored(false);
                bedHandler.stopDecorativeSleep(p);
            }
        }
        decorativeSleepers.clear();

        for (UUID uuid : new HashSet<>(functionalSleepers.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isSleeping()) p.wakeup(false);
            BedBridge.unregisterCustomSleeper(uuid);
        }
        functionalSleepers.clear();
        savedPhantomTimers.clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnitureInteract(FurnitureInteractEvent event) {
        if (!event.getNamespacedID().equals(namespacedID)) return;

        event.setCancelled(true);
        handleBedInteract(event.getPlayer(), event.getBukkitEntity().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onComplexFurnitureInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.BARRIER) return;

        if (shouldIgnoreOffHandDuplicate(event)) return;

        CustomEntity customEntity = findNearbyCustomEntity(clickedBlock.getLocation());
        if (customEntity == null || !customEntity.getNamespacedID().equals(namespacedID))
            return;

        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        handleBedInteract(event.getPlayer(), customEntity.getEntity().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCustomBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        if (shouldIgnoreOffHandDuplicate(event)) return;

        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(clickedBlock);
        if (customBlock == null || !customBlock.getNamespacedID().equals(namespacedID))
            return;

        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        handleBedInteract(event.getPlayer(), clickedBlock.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerWakeUp(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (decorativeSleepers.remove(uuid) != null) {
            player.setSleepingIgnored(false);
            return;
        }

        Location sleepLoc = functionalSleepers.remove(uuid);
        if (sleepLoc == null) return;

        BedBridge.unregisterCustomSleeper(uuid);

        if (!resetPhantoms) {
            Integer savedTimer = savedPhantomTimers.remove(uuid);
            if (savedTimer != null && plugin != null) {
                final int timer = savedTimer;
                Task.syncLater(
                        t -> {
                            if (player.isOnline()) {
                                player.setStatistic(Statistic.TIME_SINCE_REST, timer);
                            }
                        },
                        plugin,
                        50,
                        TimeUnit.MILLISECONDS
                );
            }
        } else {
            savedPhantomTimers.remove(uuid);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        if (!event.getNamespacedID().equals(namespacedID)) return;

        Location furnitureLoc = event.getBukkitEntity().getLocation();
        INmsBedHandler bedHandler = NmsManager.instance().handler().bed();

        decorativeSleepers.entrySet().removeIf(entry -> {
            if (!isSameBlock(entry.getValue(), furnitureLoc)) return false;

            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) {
                p.setSleepingIgnored(false);
                bedHandler.stopDecorativeSleep(p);
            }
            return true;
        });

        Set<UUID> toRemove = new HashSet<>();
        for (Map.Entry<UUID, Location> entry : functionalSleepers.entrySet()) {
            if (!isSameBlock(entry.getValue(), furnitureLoc)) continue;

            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && p.isSleeping()) p.wakeup(false);

            savedPhantomTimers.remove(entry.getKey());
            BedBridge.unregisterCustomSleeper(entry.getKey());
            toRemove.add(entry.getKey());
        }
        functionalSleepers.keySet().removeAll(toRemove);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        if (decorativeSleepers.remove(uuid) != null) {
            return;
        }

        functionalSleepers.remove(uuid);
        savedPhantomTimers.remove(uuid);
        BedBridge.unregisterCustomSleeper(uuid);
    }

    private void handleBedInteract(Player player, Location furnitureLocation) {
        if (player.isSneaking()) return;

        UUID uuid = player.getUniqueId();

        if (decorativeSleepers.containsKey(uuid)) {
            wakeDecorativeSleeper(player);
            return;
        }

        if (player.isSleeping()) return;

        if (skipNight) {
            tryFunctionalSleep(player, furnitureLocation);
        } else {
            tryDecorativeSleep(player, furnitureLocation);
        }
    }

    /**
     * Attempts vanilla sleep directly at the furniture location.
     * No fake bed block is placed anymore.
     */
    private void tryFunctionalSleep(Player player, Location furnitureLocation) {
        UUID uuid = player.getUniqueId();

        if (!resetPhantoms) {
            savedPhantomTimers.put(
                    uuid,
                    player.getStatistic(Statistic.TIME_SINCE_REST)
            );
        }

        Location sleepLoc = createFunctionalSleepLocation(player, furnitureLocation);
        functionalSleepers.put(uuid, sleepLoc);
        BedBridge.registerCustomSleeper(uuid); // tell checkBedExists to allow this player

        boolean sleeping = player.sleep(sleepLoc, false);

        if (!sleeping) {
            functionalSleepers.remove(uuid);
            savedPhantomTimers.remove(uuid);
            BedBridge.unregisterCustomSleeper(uuid);
        }
    }

    private void tryDecorativeSleep(Player player, Location furnitureLocation) {
        Location sleepLoc = new Location(
                furnitureLocation.getWorld(),
                furnitureLocation.getBlockX() + 0.5,
                furnitureLocation.getBlockY() + 0.5625,
                furnitureLocation.getBlockZ() + 0.5,
                player.getLocation().getYaw(),
                0f
        );
        player.teleport(sleepLoc);

        player.setSleepingIgnored(true);

        decorativeSleepers.put(player.getUniqueId(), furnitureLocation);

        if (setSpawn) {
            player.setRespawnLocation(furnitureLocation, true);
        }

        if (resetPhantoms) {
            player.setStatistic(Statistic.TIME_SINCE_REST, 0);
        }

        NmsManager.instance().handler().bed().startDecorativeSleep(
                player,
                furnitureLocation.getBlockX(),
                furnitureLocation.getBlockY(),
                furnitureLocation.getBlockZ()
        );
    }

    private void wakeDecorativeSleeper(Player player) {
        decorativeSleepers.remove(player.getUniqueId());
        player.setSleepingIgnored(false);
        NmsManager.instance().handler().bed().stopDecorativeSleep(player);
    }
}
