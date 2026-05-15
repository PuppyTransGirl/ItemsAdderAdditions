package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.common.utils.Task;
import toutouchien.itemsadderadditions.common.utils.TextRenderer;
import toutouchien.itemsadderadditions.nms.api.INmsTextDisplayHandler;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplay;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayHandle;

import java.util.*;
import java.util.concurrent.TimeUnit;

@NullMarked
public final class TextDisplayRuntime {
    private static final String LOG_TAG = "TextDisplay";

    private final JavaPlugin plugin;
    private final String namespacedId;
    private final ItemCategory category;
    private final List<TextDisplaySpec> specs;
    private final INmsTextDisplayHandler nms;
    private final Map<UUID, TextDisplayOwner> owners = new HashMap<>();
    private final Map<UUID, TextDisplayViewerState> viewerStates = new HashMap<>();

    @Nullable
    private ScheduledTask ticker;
    @Nullable
    private ScheduledTask bootstrapScan;
    private boolean running;
    private long tickCounter;

    public TextDisplayRuntime(JavaPlugin plugin, String namespacedId, ItemCategory category, List<TextDisplaySpec> specs) {
        this.plugin = plugin;
        this.namespacedId = namespacedId;
        this.category = category;
        this.specs = List.copyOf(specs);
        this.nms = NmsManager.instance().handler().textDisplays();
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public String namespacedId() {
        return namespacedId;
    }

    public ItemCategory category() {
        return category;
    }

    public boolean matchesId(String actualNamespacedId) {
        return category == ItemCategory.BLOCK
                ? NamespaceUtils.matchesWithRotation(actualNamespacedId, namespacedId)
                : actualNamespacedId.equals(namespacedId);
    }

    public void start() {
        if (ticker != null) return;
        running = true;
        ticker = Task.syncRepeat(task -> tick(), plugin, 50, 50, TimeUnit.MILLISECONDS);
        bootstrapScan = Task.syncLater(task -> {
            bootstrapScan = null;
            if (!running) return;
            restoreLoadedChunks();
        }, plugin, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        running = false;
        if (bootstrapScan != null) {
            bootstrapScan.cancel();
            bootstrapScan = null;
        }
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    public void track(TextDisplayOwner owner) {
        if (!running) return;
        upsertOwner(owner, true);
    }

    public void untrack(UUID ownerId) {
        hideOwner(ownerId);
        owners.remove(ownerId);
    }

    public void forgetViewer(UUID playerId) {
        TextDisplayViewerState state = viewerStates.remove(playerId);
        if (state != null) state.clear();
    }

    public void resync(Player player) {
        if (!running) return;
        syncViewer(player);
    }

    public void destroyAll() {
        for (Map.Entry<UUID, TextDisplayViewerState> entry : viewerStates.entrySet()) {
            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer == null) continue;

            for (TextDisplayVisual visual : entry.getValue().visibleDisplays().values()) {
                destroyPacket(viewer, visual.handle());
            }
        }

        viewerStates.clear();
        owners.clear();
    }

    public void restoreLoadedChunks() {
        if (!running) return;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                scanChunk(chunk, false);
            }
        }
        syncOnlineViewers();
    }

    public void scanChunk(Chunk chunk) {
        scanChunk(chunk, true);
    }

    private void scanChunk(Chunk chunk, boolean syncAfter) {
        if (!running) return;
        if (category == ItemCategory.BLOCK) {
            scanBlockChunk(chunk);
        } else {
            scanFurnitureChunk(chunk);
        }
        if (syncAfter) syncOnlineViewers();
    }

    public void untrackChunk(Chunk chunk) {
        if (owners.isEmpty()) return;

        UUID worldId = chunk.getWorld().getUID();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        List<UUID> toRemove = new ArrayList<>();

        for (TextDisplayOwner owner : owners.values()) {
            Location location = owner.baseLocation();
            World world = location.getWorld();
            if (world == null || !world.getUID().equals(worldId)) continue;
            if ((location.getBlockX() >> 4) != chunkX) continue;
            if ((location.getBlockZ() >> 4) != chunkZ) continue;
            toRemove.add(owner.ownerId());
        }

        for (UUID ownerId : toRemove) {
            untrack(ownerId);
        }
    }

    private void scanBlockChunk(Chunk chunk) {
        try {
            CustomBlock.Advanced.runActionOnBlocks(chunk, (actualNamespacedId, location) -> {
                if (actualNamespacedId == null || location == null) return;
                if (!matchesId(actualNamespacedId)) return;

                Block block = location.getBlock();
                float yaw = TextDisplayLocationMath.blockYaw(actualNamespacedId, 0.0F);
                restoreOwner(TextDisplayOwner.block(namespacedId, block, yaw));
            });
        } catch (RuntimeException ex) {
            Log.error(LOG_TAG, "Failed to restore text_display blocks for '" + namespacedId + "' in chunk " + chunk.getWorld().getName() + ":" + chunk.getX() + "," + chunk.getZ() + ".", ex);
        }
    }

    private void scanFurnitureChunk(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            CustomFurniture furniture;
            try {
                furniture = CustomFurniture.byAlreadySpawned(entity);
            } catch (RuntimeException ex) {
                continue;
            }
            if (furniture == null) continue;
            if (!matchesId(furniture.getNamespacedID())) continue;

            Entity furnitureEntity = furniture.getEntity();
            if (furnitureEntity == null || !furnitureEntity.isValid()) continue;
            restoreOwner(TextDisplayOwner.furniture(namespacedId, category, furnitureEntity));
        }
    }

    private void upsertOwner(TextDisplayOwner owner, boolean syncAfter) {
        TextDisplayOwner existing = owners.get(owner.ownerId());
        if (Objects.equals(existing, owner)) {
            if (syncAfter) syncOnlineViewers();
            return;
        }

        if (existing != null) {
            hideOwner(owner.ownerId());
        }

        owners.put(owner.ownerId(), owner);
        if (syncAfter) syncOnlineViewers();
    }

    private void restoreOwner(TextDisplayOwner owner) {
        owners.putIfAbsent(owner.ownerId(), owner);
    }

    private void syncOnlineViewers() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            syncViewer(viewer);
        }
    }

    private void tick() {
        if (!running) return;
        tickCounter++;
        syncOnlineViewers();
    }

    private void syncViewer(Player viewer) {
        if (!running) return;
        TextDisplayViewerState state = viewerStates.computeIfAbsent(viewer.getUniqueId(), ignored -> new TextDisplayViewerState());
        Set<TextDisplayDisplayKey> desiredKeys = new HashSet<>();

        for (TextDisplayOwner owner : owners.values()) {
            for (TextDisplaySpec spec : specs) {
                TextDisplayDisplayKey key = new TextDisplayDisplayKey(owner.ownerId(), spec.id());
                if (!shouldSee(viewer, owner, spec)) continue;

                desiredKeys.add(key);
                syncDisplay(viewer, state, owner, spec, key);
            }
        }

        Iterator<Map.Entry<TextDisplayDisplayKey, TextDisplayVisual>> iterator = state.visibleDisplays().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<TextDisplayDisplayKey, TextDisplayVisual> entry = iterator.next();
            if (desiredKeys.contains(entry.getKey())) continue;

            destroyPacket(viewer, entry.getValue().handle());
            iterator.remove();
        }

        state.retainSpawnAttempts(desiredKeys);

        if (state.isEmpty()) {
            viewerStates.remove(viewer.getUniqueId());
        }
    }

    private void syncDisplay(
            Player viewer,
            TextDisplayViewerState state,
            TextDisplayOwner owner,
            TextDisplaySpec spec,
            TextDisplayDisplayKey key
    ) {
        TextDisplayVisual visual = state.get(key);

        if (visual == null) {
            if (state.canAttemptSpawn(key, tickCounter)) {
                showDisplay(viewer, state, owner, spec, key);
            }
            return;
        }

        if (shouldRefresh(spec)) {
            updateDisplay(viewer, state, key, visual, spec);
        }
    }

    private boolean shouldSee(Player viewer, TextDisplayOwner owner, TextDisplaySpec spec) {
        Location viewerLocation = viewer.getLocation();
        Location base = owner.baseLocation();
        if (viewerLocation.getWorld() == null || base.getWorld() == null) return false;
        if (!viewerLocation.getWorld().getUID().equals(base.getWorld().getUID())) return false;

        double range = spec.viewRange();
        return viewerLocation.distanceSquared(base) <= range * range;
    }

    private boolean shouldRefresh(TextDisplaySpec spec) {
        int interval = spec.refreshInterval();
        return interval > 0 && tickCounter % interval == 0;
    }

    private void showDisplay(
            Player viewer,
            TextDisplayViewerState state,
            TextDisplayOwner owner,
            TextDisplaySpec spec,
            TextDisplayDisplayKey key
    ) {
        Location location = TextDisplayLocationMath.applyLocalOffset(owner.baseLocation(), owner.yaw(), spec.offset());
        Component text = TextRenderer.render(viewer, spec.rawText());

        try {
            PacketTextDisplayHandle handle = nms.spawn(viewer, new PacketTextDisplay(location, text, spec.visual()));
            state.put(key, new TextDisplayVisual(handle));
        } catch (RuntimeException ex) {
            state.markSpawnFailed(key, tickCounter);
            Log.error(LOG_TAG, "Failed to spawn text_display '" + namespacedId + "' display '" + spec.id() + "' for viewer '" + viewer.getName() + "'. Retrying after the spawn cooldown while the display remains in range.", ex);
        }
    }

    private void updateDisplay(
            Player viewer,
            TextDisplayViewerState state,
            TextDisplayDisplayKey key,
            TextDisplayVisual visual,
            TextDisplaySpec spec
    ) {
        Component text = TextRenderer.render(viewer, spec.rawText());
        try {
            nms.updateMetadata(viewer, visual.handle(), text, spec.visual());
            state.clearUpdateFailure(key);
        } catch (RuntimeException ex) {
            if (state.shouldLogUpdateFailure(key, tickCounter)) {
                Log.error(LOG_TAG, "Failed to update text_display '" + namespacedId + "' display '" + spec.id() + "' for viewer '" + viewer.getName() + "'.", ex);
            }
        }
    }

    private void hideOwner(UUID ownerId) {
        Iterator<Map.Entry<UUID, TextDisplayViewerState>> states = viewerStates.entrySet().iterator();
        while (states.hasNext()) {
            Map.Entry<UUID, TextDisplayViewerState> entry = states.next();
            Player viewer = Bukkit.getPlayer(entry.getKey());
            TextDisplayViewerState state = entry.getValue();
            Iterator<Map.Entry<TextDisplayDisplayKey, TextDisplayVisual>> iterator = state.visibleDisplays().entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<TextDisplayDisplayKey, TextDisplayVisual> visible = iterator.next();
                if (!visible.getKey().ownerId().equals(ownerId)) continue;

                if (viewer != null) destroyPacket(viewer, visible.getValue().handle());
                iterator.remove();
            }

            state.removeOwnerState(ownerId);
            if (state.isEmpty()) states.remove();
        }
    }

    private void destroyPacket(Player viewer, PacketTextDisplayHandle handle) {
        try {
            nms.destroy(viewer, handle);
        } catch (RuntimeException ex) {
            Log.error(LOG_TAG, "Failed to destroy text_display packet entity " + handle.entityId() + " for viewer '" + viewer.getName() + "'.", ex);
        }
    }
}
