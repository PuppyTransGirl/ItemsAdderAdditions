package toutouchien.itemsadderadditions.feature.action.builtin;

import com.jeff_media.customblockdata.CustomBlockData;
import dev.lone.itemsadder.api.CustomBlock;
import net.momirealms.antigrieflib.AntiGriefLib;
import net.momirealms.antigrieflib.Flag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.CustomTagType;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.behaviour.loading.BehaviourBindings;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** One Paper main-thread budget shared by all bindings and players. */
@NullMarked
final class BlockReplacementQueue {
    static final int MAX_JOBS = 4;
    static final int SCANS_PER_TICK = 512;
    static final int WRITES_PER_TICK = 32;
    static final long TICK_NANOS = 2_000_000;
    private static final ArrayDeque<Job> jobs = new ArrayDeque<>();
    private static long generation;
    @Nullable private static BukkitTask task;
    @Nullable private static Job active;

    private BlockReplacementQueue() {}

    static long generation() { return generation; }

    static boolean submit(Plugin plugin, AntiGriefLib protection, Player player, Location center,
                          ReplaceNearBlocksAction.Options options) {
        if (!Bukkit.isPrimaryThread() || !plugin.isEnabled() || options.generation() != generation) return false;
        if (jobs.size() + (active == null ? 0 : 1) >= MAX_JOBS) {
            Log.debug("ReplaceNearBlocks", "Rejected execution: {} concurrent jobs", MAX_JOBS);
            return false;
        }
        Job job = new Job(plugin, protection, player, center, options);
        jobs.addLast(job);
        if (task == null) {
            try {
                task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(System::nanoTime), 1, 1);
            } catch (RuntimeException exception) {
                jobs.remove(job);
                job.finish("scheduling failed");
                throw exception;
            }
        }
        return true;
    }

    static void tick(LongSupplier clock) {
        long start = clock.getAsLong();
        int scans = 0, writes = 0;
        while (!jobs.isEmpty() && scans < SCANS_PER_TICK && writes < WRITES_PER_TICK
                && clock.getAsLong() - start < TICK_NANOS) {
            Job job = jobs.removeFirst();
            active = job;
            try {
                if (!job.plugin.isEnabled() || !job.player.isOnline() || job.options.generation() != generation) {
                    job.finish("cancelled");
                } else {
                    if (job.step()) writes++;
                    if (job.done) job.finish("complete");
                }
            } catch (RuntimeException exception) {
                job.finish("failed: " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            } finally {
                active = null;
            }
            scans++;
            if (!job.finished) jobs.addLast(job);
        }
        if (jobs.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
    }

    static void cancelAll() {
        generation++;
        if (task != null) task.cancel();
        task = null;
        if (active != null) active.finish("cancelled");
        for (Job job : jobs) job.finish("cancelled");
        jobs.clear();
    }

    private static final class Job {
        final Plugin plugin;
        final AntiGriefLib protection;
        final Player player;
        final World world;
        final ReplaceNearBlocksAction.Options options;
        final int centerX, centerY, centerZ, minX, maxX, minY, maxY, minZ, maxZ;
        final Map<String, Boolean> matches = new HashMap<>();
        final long started = System.nanoTime();
        int chunkX, chunkZ, x, y, z;
        int scanned, changed, denied, skipped, unloadedChunks;
        boolean done, finished;

        Job(Plugin plugin, AntiGriefLib protection, Player player, Location center, ReplaceNearBlocksAction.Options options) {
            this.plugin = plugin;
            this.protection = protection;
            this.player = player;
            this.world = center.getWorld();
            this.options = options;
            centerX = center.getBlockX(); centerY = center.getBlockY(); centerZ = center.getBlockZ();
            minX = Math.subtractExact(centerX, options.radiusX()); maxX = Math.addExact(centerX, options.radiusX());
            minZ = Math.subtractExact(centerZ, options.radiusZ()); maxZ = Math.addExact(centerZ, options.radiusZ());
            minY = Math.max(world.getMinHeight(), Math.subtractExact(centerY, options.radiusY()));
            maxY = Math.min(world.getMaxHeight() - 1, Math.addExact(centerY, options.radiusY()));
            chunkX = minX >> 4; chunkZ = minZ >> 4;
            x = minX; y = minY; z = minZ;
            done = minY > maxY;
        }

        boolean step() {
            if (done || finished) return false;
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                unloadedChunks++;
                nextChunk();
                return false;
            }
            int bx = x, by = y, bz = z;
            advance();
            scanned++;
            if (!options.shape().containsBlock(bx - centerX, by - centerY, bz - centerZ,
                    options.radiusX(), options.radiusY(), options.radiusZ())) return false;
            Block block = world.getBlockAt(bx, by, bz);
            // Do not use blockID(): its exception fallback exposes custom backing material.
            CustomBlock custom = CustomBlock.byAlreadyPlaced(block);
            String id = custom == null ? block.getType().getKey().toString() : custom.getNamespacedID();
            if (id.equals(options.to()) || !matches.computeIfAbsent(id, this::matches)) return false;
            BlockData original = block.getBlockData();
            if (block.getType() == Material.BARRIER || block.getState() instanceof TileState
                    || BehaviourBindings.has(id) || CustomBlockData.hasCustomBlockData(block, plugin)) {
                skipped++;
                return false;
            }
            Location location = block.getLocation();
            if (!protection.test(player, Flag.BREAK, location) || !protection.test(player, Flag.PLACE, location)) {
                denied++;
                return false;
            }
            // Recheck after callbacks. The halo covers ordinary immediate neighbor access,
            // not an arbitrary third-party callback's distant world access.
            if (finished || options.generation() != generation || !loadedNeighborhood(bx >> 4, bz >> 4)) {
                skipped++;
                return false;
            }
            CustomBlock currentCustom = CustomBlock.byAlreadyPlaced(block);
            if (!original.equals(block.getBlockData())
                    || (custom == null ? currentCustom != null
                    : currentCustom == null || !id.equals(currentCustom.getNamespacedID()))
                    || CustomBlockData.hasCustomBlockData(block, plugin)) {
                skipped++;
                return false;
            }
            if (currentCustom != null && !currentCustom.remove()) {
                finish("custom removal failed");
                return true;
            }
            if (finished || options.generation() != generation || !world.isChunkLoaded(bx >> 4, bz >> 4)) return false;
            if (currentCustom != null && CustomBlock.byAlreadyPlaced(block) != null) {
                finish("custom state still present after removal");
                return true;
            }
            // ponytail: keep normal physics; arbitrary blocks are not safe with physics=false.
            // The deadline cannot preempt one engine/ItemsAdder/protection callback.
            block.setBlockData(options.targetData(), true);
            changed++;
            return true;
        }

        boolean matches(String id) {
            for (String rule : options.from())
                if (NamespaceUtils.matchesContentIDOrTag(id, rule, CustomTagType.BLOCK)) return true;
            return false;
        }

        boolean loadedNeighborhood(int cx, int cz) {
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    if (!world.isChunkLoaded(cx + dx, cz + dz)) return false;
            return true;
        }

        void advance() {
            if (y < maxY) { y++; return; }
            y = minY;
            if (z < Math.min(maxZ, (chunkZ << 4) + 15)) { z++; return; }
            z = Math.max(minZ, chunkZ << 4);
            if (x < Math.min(maxX, (chunkX << 4) + 15)) { x++; return; }
            nextChunk();
        }

        void nextChunk() {
            if (++chunkZ > (maxZ >> 4)) {
                chunkZ = minZ >> 4;
                if (++chunkX > (maxX >> 4)) { done = true; return; }
            }
            x = Math.max(minX, chunkX << 4);
            z = Math.max(minZ, chunkZ << 4);
            y = minY;
        }

        void finish(String reason) {
            if (finished) return;
            finished = true;
            Log.debug("ReplaceNearBlocks", "{}: scanned={}, changed={}, denied={}, skipped={}, unloadedChunks={}, elapsedMs={}",
                    reason, scanned, changed, denied, skipped, unloadedChunks, (System.nanoTime() - started) / 1_000_000);
        }
    }
}
