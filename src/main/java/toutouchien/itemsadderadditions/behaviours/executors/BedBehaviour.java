package toutouchien.itemsadderadditions.behaviours.executors;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomEntity;
import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
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
import toutouchien.itemsadderadditions.behaviours.executors.bed.SlotOffset;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.*;

/**
 * Adds multi-slot sleeping behaviour to custom furniture or blocks.
 *
 * <h3>Configuration</h3>
 * <pre>{@code
 * behaviours:
 *   bed:
 *     slots:
 *       - "0,0,0"   # relative block offsets from the furniture base
 *       - "1,0,0"   # second slot (e.g. double bed)
 * }</pre>
 *
 * <p>Each slot is parsed as {@code "dx,dy,dz"} integer block offsets.
 * A HEAD-part vanilla brown bed is temporarily written into the world at the
 * chosen slot so that {@link Player#sleep} succeeds and the player's rotation
 * matches the bed facing.  The fake block is restored the moment the player
 * wakes up (or disconnects).</p>
 */
@SuppressWarnings("unused")
@NullMarked
@Behaviour(key = "bed")
public final class BedBehaviour extends BehaviourExecutor implements Listener {
    private static final String TAG = "BedBehaviour";

    /**
     * UUID → exact sleep {@link Location} (includes yaw used for bed facing).
     * Present from the moment {@link Player#sleep} returns {@code true} until
     * the player actually wakes up.
     */
    private final Map<UUID, Location> sleepers = new HashMap<>();

    private List<SlotOffset> slots = List.of(new SlotOffset(0, 0, 0));
    private List<String> rawSlots = new ArrayList<>();
    private String namespacedID = "";
    private @Nullable JavaPlugin plugin;

    private static Location centredSlot(Location base, SlotOffset o, float yaw) {
        float n = ((yaw % 360f) + 360f) % 360f;

        // fwd = furniture facing direction vector (local +X maps to this)
        // right = 90° clockwise from fwd in the XZ plane (local +Z maps to this)
        int fwdX, fwdZ, rightX, rightZ;
        if (n < 45f || n >= 315f) {   // South  (+Z)
            fwdX = 0;
            fwdZ = 1;
            rightX = -1;
            rightZ = 0;
        } else if (n < 135f) {        // West   (-X)
            fwdX = -1;
            fwdZ = 0;
            rightX = 0;
            rightZ = 1;
        } else if (n < 225f) {        // North  (-Z)
            fwdX = 0;
            fwdZ = -1;
            rightX = 1;
            rightZ = 0;
        } else {                      // East   (+X)
            fwdX = 1;
            fwdZ = 0;
            rightX = 0;
            rightZ = -1;
        }

        int worldX = base.getBlockX() + o.dx() * fwdX + o.dz() * rightX;
        int worldY = base.getBlockY() + o.dy();
        int worldZ = base.getBlockZ() + o.dx() * fwdZ + o.dz() * rightZ;

        return new Location(
                base.getWorld(),
                worldX + 0.5,
                worldY,
                worldZ + 0.5
        );
    }

    /**
     * Strips sub-block components so locations can be compared by block
     * position and world only.
     */
    private static Location toSlotKey(Location loc) {
        return new Location(
                loc.getWorld(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );
    }

    private static boolean shouldIgnoreOffHandDuplicate(PlayerInteractEvent e) {
        return e.getHand() == EquipmentSlot.OFF_HAND
                && !e.getPlayer().getInventory().getItemInMainHand().isEmpty();
    }

    @Nullable
    private static CustomEntity findNearbyCustomEntity(Location loc) {
        if (loc.getWorld() == null) return null;
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 0.1, 0.1, 0.1)) {
            CustomEntity ce = CustomEntity.byAlreadySpawned(entity);
            if (ce != null) return ce;
        }
        return null;
    }

    @Nullable
    private static CustomFurniture findNearbyCustomFurniture(Location loc) {
        if (loc.getWorld() == null) return null;
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 0.5, 0.5, 0.5)) {
            if (entity instanceof Player) continue;
            CustomFurniture cf = CustomFurniture.byAlreadySpawned(entity);
            if (cf != null) return cf;
        }
        return null;
    }

    @Override
    public boolean configure(Object configData, String namespacedID) {
        if (!(configData instanceof org.bukkit.configuration.ConfigurationSection section)) {
            return false;
        }

        List<?> raw = section.getList("slots");
        if (raw == null || raw.isEmpty()) {
            Log.warn(TAG,
                    "{}: no 'slots' list defined - defaulting to single slot at 0,0,0",
                    namespacedID);
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

        List<SlotOffset> parsed = new ArrayList<>(rawSlots.size());
        for (String raw : rawSlots) {
            SlotOffset offset = SlotOffset.parse(raw, namespacedID);
            if (offset != null) parsed.add(offset);
        }
        this.slots = parsed.isEmpty()
                ? List.of(new SlotOffset(0, 0, 0))
                : List.copyOf(parsed);

        Bukkit.getPluginManager().registerEvents(this, host.plugin());
    }

    @Override
    protected void onUnload(BehaviourHost host) {
        HandlerList.unregisterAll(this);

        // Wake and clean up any remaining sleepers.
        for (UUID uuid : new HashSet<>(sleepers.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isSleeping()) {
                p.wakeup(false); // triggers PlayerBedLeaveEvent → cleans up
            } else {
                // Player is offline or already awake - clean up manually.
                Location loc = sleepers.remove(uuid);
                if (loc != null && p != null) {
                    NmsManager.instance().handler().bed().removeFakeBed(p, loc);
                }
            }
        }
        sleepers.clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnitureInteract(FurnitureInteractEvent event) {
        if (!event.getNamespacedID().equals(namespacedID)) return;
        handleBedInteract(
                event.getPlayer(),
                event.getBukkitEntity().getLocation()
        );
    }

    /**
     * Handles furniture built from BARRIER blocks (complex multi-block models).
     * Finds the nearest custom entity to the clicked barrier and delegates.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onComplexFurnitureInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (shouldIgnoreOffHandDuplicate(event)) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != org.bukkit.Material.BARRIER)
            return;

        CustomEntity ce = findNearbyCustomEntity(clicked.getLocation());
        if (ce == null || !ce.getNamespacedID().equals(namespacedID)) return;

        handleBedInteract(event.getPlayer(), ce.getEntity().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCustomBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (shouldIgnoreOffHandDuplicate(event)) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        CustomBlock cb = CustomBlock.byAlreadyPlaced(clicked);
        if (cb == null || !cb.getNamespacedID().equals(namespacedID)) return;

        handleBedInteract(event.getPlayer(), clicked.getLocation());
    }

    /**
     * Primary cleanup path: called whenever a sleeping player wakes up,
     * regardless of the reason (morning, explosion, forced wakeup, etc.).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerWakeUp(PlayerBedLeaveEvent event) {
        cleanUp(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanUp(event.getPlayer());
    }

    /**
     * Wakes any players sleeping in slots that belong to this furniture piece
     * when it is broken.  Their {@link PlayerBedLeaveEvent} will handle the
     * block restoration automatically.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        if (!event.getNamespacedID().equals(namespacedID)) return;

        Location base = event.getBukkitEntity().getLocation();
        float yaw = event.getBukkitEntity().getLocation().getYaw();
        Set<Location> broken = slotKeys(base, yaw);

        for (Map.Entry<UUID, Location> entry : new HashSet<>(sleepers.entrySet())) {
            if (!broken.contains(toSlotKey(entry.getValue()))) continue;

            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && p.isSleeping()) {
                p.wakeup(false); // → PlayerBedLeaveEvent → cleanUp
            } else {
                sleepers.remove(entry.getKey());
            }
        }
    }

    private void handleBedInteract(Player player, Location furnitureBase) {
        if (player.isSneaking() || player.isSleeping()) return;

        // Determine the facing yaw from the furniture entity (if present).
        float yaw = 0f;
        CustomFurniture cf = findNearbyCustomFurniture(furnitureBase);
        if (cf != null) yaw = cf.getEntity().getLocation().getYaw();

        Location freeSlot = findFreeSlot(furnitureBase, yaw);
        if (freeSlot == null) {
            player.sendActionBar(
                    Component.translatable("block.minecraft.bed.occupied")
            );
            return;
        }

        trySleep(player, freeSlot, yaw);
    }

    /**
     * Places a fake bed, calls {@link Player#sleep}, and cleans up immediately
     * if sleep was refused.  On success the fake bed stays until wakeup.
     */
    private void trySleep(Player player, Location slotLoc, float yaw) {
        // Bake the yaw into the location - the NMS handler reads it to set
        // HORIZONTAL_FACING on the fake bed block.
        slotLoc = slotLoc.clone();
        slotLoc.setYaw(yaw);

        UUID uuid = player.getUniqueId();

        // Register before placing so that any concurrent check sees the slot
        // as occupied.
        sleepers.put(uuid, slotLoc);

        // Write the real vanilla bed block into the world temporarily.
        NmsManager.instance().handler().bed().placeFakeBed(player, slotLoc);

        boolean sleeping = player.sleep(slotLoc, false);

        if (!sleeping) {
            // Sleep was rejected (wrong time, obstacle, etc.) - undo everything.
            sleepers.remove(uuid);
            NmsManager.instance().handler().bed().removeFakeBed(player, slotLoc);

            Log.debug(TAG, "trySleep: Player#sleep rejected for {}", player.getName());
        } else {
            // Player is now sleeping - the fake bed must remain so that vanilla
            // bed-existence checks (if any) still pass. It will be removed by
            // cleanUp() when the player wakes up or disconnects.
            Log.debug(TAG, "trySleep: {} is now sleeping at [{} {} {}]",
                    player.getName(), slotLoc.getBlockX(), slotLoc.getBlockY(),
                    slotLoc.getBlockZ());
        }
    }

    /**
     * Removes the fake bed from the world and unregisters the player.
     * Safe to call even if the player was never registered.
     */
    private void cleanUp(Player player) {
        Location loc = sleepers.remove(player.getUniqueId());
        if (loc == null) return;
        NmsManager.instance().handler().bed().removeFakeBed(player, loc);
    }

    @Nullable
    private Location findFreeSlot(Location base, float yaw) {
        for (SlotOffset offset : slots) {
            Location candidate = centredSlot(base, offset, yaw);
            if (!isOccupied(candidate)) return candidate;
        }
        return null;
    }

    private boolean isOccupied(Location slotLoc) {
        Location key = toSlotKey(slotLoc);
        for (Location occupied : sleepers.values()) {
            if (toSlotKey(occupied).equals(key)) return true;
        }
        return false;
    }

    /**
     * All block-snapped slot locations for {@code base} (used on break).
     */
    private Set<Location> slotKeys(Location base, float yaw) {
        Set<Location> keys = HashSet.newHashSet(slots.size());
        for (SlotOffset o : slots) keys.add(toSlotKey(centredSlot(base, o, yaw)));
        return keys;
    }
}
