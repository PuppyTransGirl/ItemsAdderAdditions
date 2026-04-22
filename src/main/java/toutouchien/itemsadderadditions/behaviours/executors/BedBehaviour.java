package toutouchien.itemsadderadditions.behaviours.executors;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomEntity;
import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
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
import toutouchien.itemsadderadditions.behaviours.BehaviourExecutor;
import toutouchien.itemsadderadditions.behaviours.BehaviourHost;
import toutouchien.itemsadderadditions.behaviours.annotations.Behaviour;
import toutouchien.itemsadderadditions.bridge.BedBridge;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.*;

/**
 * Adds multi-slot sleeping behaviour to custom furniture or blocks.
 *
 * <h3>Configuration</h3>
 * <pre>{@code
 * behaviours:
 *   beds:
 *     slots:
 *       - "0,0,0"   # relative block offsets from the furniture's base location
 *       - "1,0,0"   # second slot - useful for double/bunk beds
 * }</pre>
 *
 * <p>Each slot string is parsed as {@code "dx,dy,dz"} (integer block offsets).
 * The sleep position sent to {@link Player#sleep} is the centre of that block
 * (x+0.5, y, z+0.5).  All slots support night-skipping via the ASM patches.</p>
 */
@SuppressWarnings("unused")
@NullMarked
@Behaviour(key = "bed")
public final class BedBehaviour extends BehaviourExecutor implements Listener {
    private static final String TAG = "BedBehaviour";
    /**
     * Maps sleeping player UUID → the exact {@link Location} they are sleeping at.
     */
    private final Map<UUID, Location> sleepers = new HashMap<>();
    /**
     * Parsed slots for this item type (set during {@link #onLoad}).
     */
    private List<SlotOffset> slots = List.of(new SlotOffset(0, 0, 0));
    /**
     * Raw slot strings read from YAML.  Populated by {@link #configure} before
     * {@link #onLoad} is called, then parsed in {@link #onLoad}.
     */
    private List<String> rawSlots = new ArrayList<>();
    private String namespacedID = "";
    private @Nullable JavaPlugin plugin;

    /**
     * Computes the centred sleep location for a slot: the block centre at
     * {@code (base + offset)}, with y at the block floor (matching vanilla bed
     * behaviour).
     */
    private static Location centredSlotLocation(Location base, SlotOffset offset) {
        return new Location(
                base.getWorld(),
                base.getBlockX() + offset.dx() + 0.5,
                base.getBlockY() + offset.dy(),
                base.getBlockZ() + offset.dz() + 0.5
        );
    }

    /**
     * Normalises a location to its block coordinates for map-key / equality
     * comparisons (strips the sub-block floating-point components).
     */
    private static Location toSlotKey(Location loc) {
        return new Location(
                loc.getWorld(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );
    }

    private static boolean shouldIgnoreOffHandDuplicate(PlayerInteractEvent event) {
        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.OFF_HAND) return false;
        return !event.getPlayer().getInventory().getItemInMainHand().isEmpty();
    }

    // Bukkit event handlers

    @Nullable
    private static CustomEntity findNearbyCustomEntity(Location location) {
        if (location.getWorld() == null) return null;
        for (Entity entity : location.getWorld().getNearbyEntities(
                location, 0.1, 0.1, 0.1)) {
            CustomEntity ce = CustomEntity.byAlreadySpawned(entity);
            if (ce != null) return ce;
        }
        return null;
    }

    @Nullable
    private static CustomFurniture findNearbyCustomFurniture(Location location) {
        if (location.getWorld() == null) return null;
        Collection<Entity> nearby = location.getWorld()
                .getNearbyEntities(location, 0.5, 0.5, 0.5);
        for (Entity entity : nearby) {
            if (entity instanceof Player) continue;
            CustomFurniture furniture = CustomFurniture.byAlreadySpawned(entity);
            if (furniture != null) return furniture;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overridden to extract the {@code slots} list from the YAML section
     * before the standard parameter injector runs (which cannot handle
     * {@code List<String>} generics at runtime).</p>
     */
    @Override
    public boolean configure(Object configData, String namespacedID) {
        if (!(configData instanceof org.bukkit.configuration.ConfigurationSection section)) {
            return false;
        }

        List<?> raw = section.getList("slots");
        if (raw == null || raw.isEmpty()) {
            Log.warn(
                    "BedBehaviour",
                    "{}: no 'slots' list defined - defaulting to single slot at 0,0,0",
                    namespacedID
            );
            rawSlots = List.of("0,0,0");
        } else {
            rawSlots = new ArrayList<>(raw.size());
            for (Object o : raw) {
                if (o != null) rawSlots.add(o.toString());
            }
        }

        return true;
    }

    @Override
    protected void onLoad(BehaviourHost host) {
        this.namespacedID = host.namespacedID();
        this.plugin = host.plugin();

        // Parse raw slot strings now that namespacedID is available for logging.
        List<SlotOffset> parsed = new ArrayList<>(rawSlots.size());
        for (String raw : rawSlots) {
            SlotOffset offset = SlotOffset.parse(raw, namespacedID);
            if (offset != null) parsed.add(offset);
        }
        this.slots = parsed.isEmpty() ? List.of(new SlotOffset(0, 0, 0)) : List.copyOf(parsed);

        Bukkit.getPluginManager().registerEvents(this, host.plugin());
    }

    @Override
    protected void onUnload(BehaviourHost host) {
        HandlerList.unregisterAll(this);

        for (UUID uuid : new HashSet<>(sleepers.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isSleeping()) p.wakeup(false);
            BedBridge.unregisterCustomSleeper(uuid);
        }
        sleepers.clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnitureInteract(FurnitureInteractEvent event) {
        if (!event.getNamespacedID().equals(namespacedID)) return;
        Log.debug(TAG, "onFurnitureInteract fired for {} by {}",
                namespacedID, event.getPlayer().getName());

        event.setCancelled(true);
        handleBedInteract(event.getPlayer(), event.getBukkitEntity().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onComplexFurnitureInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != org.bukkit.Material.BARRIER)
            return;

        if (shouldIgnoreOffHandDuplicate(event)) return;

        CustomEntity customEntity = findNearbyCustomEntity(clickedBlock.getLocation());
        Log.debug(TAG, "onComplexFurnitureInteract: found custom entity near {}: {}",
                clickedBlock.getLocation(), customEntity != null ? customEntity.getNamespacedID() : "null");
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
        Log.debug(TAG, "onCustomBlockInteract: found custom block at {}: {}",
                clickedBlock.getLocation(), customBlock != null ? customBlock.getNamespacedID() : "null");
        if (customBlock == null || !customBlock.getNamespacedID().equals(namespacedID))
            return;

        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        handleBedInteract(event.getPlayer(), clickedBlock.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerWakeUp(PlayerBedLeaveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (sleepers.remove(uuid) == null) return;
        BedBridge.unregisterCustomSleeper(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        if (!event.getNamespacedID().equals(namespacedID)) return;

        Location base = event.getBukkitEntity().getLocation();
        Set<Location> slotLocations = slotLocations(base);

        Set<UUID> toRemove = new HashSet<>();
        for (Map.Entry<UUID, Location> entry : sleepers.entrySet()) {
            if (!slotLocations.contains(toSlotKey(entry.getValue()))) continue;

            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && p.isSleeping()) p.wakeup(false);

            BedBridge.unregisterCustomSleeper(entry.getKey());
            toRemove.add(entry.getKey());
        }
        sleepers.keySet().removeAll(toRemove);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (sleepers.remove(uuid) != null)
            BedBridge.unregisterCustomSleeper(uuid);
    }

    private void handleBedInteract(Player player, Location furnitureBase) {
        if (player.isSneaking()) return;
        if (player.isSleeping()) return;

        Log.debug(TAG, "handleBedInteract: player={} furnitureBase=[world={} x={} y={} z={}]",
                player.getName(),
                furnitureBase.getWorld() != null ? furnitureBase.getWorld().getName() : "null",
                furnitureBase.getX(), furnitureBase.getY(), furnitureBase.getZ());

        // --- Yaw resolution ---
        float furnitureYaw = 0F;
        float resolvedYaw = 0F;
        if (furnitureBase.getWorld() != null) {
            CustomFurniture cf = findNearbyCustomFurniture(furnitureBase);
            if (cf != null) {
                Location entityLoc = cf.getEntity().getLocation();
                furnitureYaw = entityLoc.getYaw();
                resolvedYaw = furnitureYaw;

                Log.debug(TAG,
                        "handleBedInteract: found CustomFurniture '{}' | " +
                                "entity loc=[x={} y={} z={}] yaw={} pitch={}",
                        cf.getNamespacedID(),
                        entityLoc.getX(), entityLoc.getY(), entityLoc.getZ(),
                        furnitureYaw, entityLoc.getPitch());
            } else {
                Log.debug(TAG,
                        "handleBedInteract: NO CustomFurniture found within 0.5 blocks of " +
                                "furnitureBase - yaw defaulting to 0");
            }
        } else {
            Log.debug(TAG, "handleBedInteract: furnitureBase world is null - skipping yaw lookup");
        }

        Log.debug(TAG, "handleBedInteract: furnitureYaw={} resolvedYaw (final)={}",
                furnitureYaw, resolvedYaw);

        // Monster-proximity check (mirrors vanilla ServerPlayer.getBedResult).
        if (player.getGameMode() != GameMode.CREATIVE
                && furnitureBase.getWorld() != null) {
            boolean unsafe = !furnitureBase
                    .getWorld()
                    .getNearbyEntities(furnitureBase, 8.0, 5.0, 8.0, e -> e instanceof Monster)
                    .isEmpty();
            Log.debug(TAG, "handleBedInteract: monster nearby check → unsafe={}", unsafe);
            if (unsafe) {
                player.sendActionBar(
                        Component.translatable("block.minecraft.bed.not_safe")
                );
                return;
            }
        }

        // Find the first unoccupied slot.
        @Nullable Location freeSlot = findFreeSlot(furnitureBase);
        Log.debug(TAG, "handleBedInteract: freeSlot={}",
                freeSlot != null
                        ? "[x=" + freeSlot.getX() + " y=" + freeSlot.getY()
                          + " z=" + freeSlot.getZ() + "]"
                        : "null (all occupied)");

        if (freeSlot == null) {
            player.sendActionBar(
                    Component.translatable("block.minecraft.bed.occupied")
            );
            return;
        }

        trySleep(player, furnitureBase, freeSlot, resolvedYaw);
    }

    /**
     * Attempts {@link Player#sleep} at the chosen slot location.
     * Registers with {@link BedBridge} first so the ASM {@code checkBedExists}
     * patch keeps the player asleep.
     */
    private void trySleep(Player player, Location furnitureBase,
                          Location slotLoc, float yaw) {
        UUID uuid = player.getUniqueId();

        Log.debug(TAG,
                "trySleep: player={} slotLoc=[x={} y={} z={}] rawYaw={}",
                player.getName(),
                slotLoc.getX(), slotLoc.getY(), slotLoc.getZ(),
                yaw);

        sleepers.put(uuid, slotLoc);
        BedBridge.registerCustomSleeper(uuid, yaw);

        Location sleepLoc = slotLoc.clone();
        // NOTE: Player#sleep ignores the Location's yaw entirely.
        // Rotation is corrected in the delayed task below.

        Log.debug(TAG,
                "trySleep: calling Player#sleep at [x={} y={} z={} yaw={} pitch={}]",
                sleepLoc.getX(), sleepLoc.getY(), sleepLoc.getZ(),
                sleepLoc.getYaw(), sleepLoc.getPitch());

        Location playerBefore = player.getLocation();
        Log.debug(TAG,
                "trySleep: player location BEFORE sleep=[x={} y={} z={} yaw={} pitch={}]",
                playerBefore.getX(), playerBefore.getY(), playerBefore.getZ(),
                playerBefore.getYaw(), playerBefore.getPitch());

        boolean sleeping = player.sleep(sleepLoc, false);

        Location playerAfter = player.getLocation();
        Log.debug(TAG,
                "trySleep: Player#sleep returned={} | player location AFTER sleep=[x={} y={} z={} yaw={} pitch={}]",
                sleeping,
                playerAfter.getX(), playerAfter.getY(), playerAfter.getZ(),
                playerAfter.getYaw(), playerAfter.getPitch());

        if (!sleeping) {
            Log.debug(TAG, "trySleep: sleep failed - cleaning up sleeper registry for {}", player.getName());
            sleepers.remove(uuid);
            BedBridge.unregisterCustomSleeper(uuid);
        } else {
            NmsManager.instance().handler().bed().startDecorativeSleep(player, sleepLoc.blockX(), sleepLoc.blockY(), sleepLoc.blockZ());
        }

        JavaPlugin pl = plugin;
        Bukkit.getScheduler().runTaskLater(pl, () -> {
            if (!player.isSleeping()) {
                Log.debug(TAG, "trySleep (delayed): {} no longer sleeping, skipping", player.getName());
                return;
            }

            // Player#sleep ignores yaw, so we force it now that the client
            // has received and processed the sleep packet.
            float targetYaw = BedBridge.getSleeperYaw(uuid);
            Log.debug(TAG,
                    "trySleep (delayed): forcing rotation yaw={} for {}",
                    targetYaw, player.getName());
            player.setRotation(targetYaw, 0F);
            Log.debug(TAG,
                    "trySleep (delayed): yaw after setRotation={}",
                    player.getLocation().getYaw());
        }, 2L);
    }

    /**
     * Returns the first slot location (centred in its block) that is not
     * currently occupied by a sleeping player, or {@code null} if all slots
     * are taken.
     */
    @Nullable
    private Location findFreeSlot(Location furnitureBase) {
        for (SlotOffset offset : slots) {
            Location candidate = centredSlotLocation(furnitureBase, offset);
            if (!isSlotOccupied(candidate)) return candidate;
        }
        return null;
    }

    /**
     * Returns {@code true} if any current sleeper is already using the given
     * slot (compared by block coordinates + world).
     */
    private boolean isSlotOccupied(Location slotLoc) {
        Location key = toSlotKey(slotLoc);
        for (Location occupied : sleepers.values()) {
            if (toSlotKey(occupied).equals(key)) return true;
        }
        return false;
    }

    /**
     * Returns the set of block-snapped (slot-key) locations for every slot of
     * a furniture placed at {@code base}.  Used for occupancy checks on furniture
     * break.
     */
    private Set<Location> slotLocations(Location base) {
        Set<Location> result = HashSet.newHashSet(slots.size() * 2);
        for (SlotOffset offset : slots) {
            result.add(toSlotKey(centredSlotLocation(base, offset)));
        }
        return result;
    }

    /**
     * A relative block-offset from a furniture's base location.
     */
    private record SlotOffset(int dx, int dy, int dz) {
        /**
         * Parses {@code "dx,dy,dz"}.  Returns {@code null} and logs a warning if
         * the format is invalid.
         */
        @Nullable
        static SlotOffset parse(String raw, String namespacedID) {
            String[] parts = raw.trim().split(",", 3);
            if (parts.length != 3) {
                Log.warn(
                        "BedBehaviour",
                        "{}: invalid slot '{}' - expected 'dx,dy,dz', skipping",
                        namespacedID,
                        raw
                );
                return null;
            }
            try {
                return new SlotOffset(
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim())
                );
            } catch (NumberFormatException e) {
                Log.warn(
                        "BedBehaviour",
                        "{}: invalid slot '{}' - non-integer component, skipping",
                        namespacedID,
                        raw
                );
                return null;
            }
        }
    }
}
