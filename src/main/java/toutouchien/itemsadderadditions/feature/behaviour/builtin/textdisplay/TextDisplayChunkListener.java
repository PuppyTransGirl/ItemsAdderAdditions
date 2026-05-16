package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.utils.Task;

import java.util.concurrent.TimeUnit;

/**
 * Triggers chunk scans on load and removes owners when a chunk unloads.
 */
@NullMarked
public final class TextDisplayChunkListener implements Listener {
    private final TextDisplayRuntime runtime;

    public TextDisplayChunkListener(TextDisplayRuntime runtime) {
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        Task.syncLater(ignored -> runtime.scanChunk(chunk), runtime.plugin(), 50, TimeUnit.MILLISECONDS);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        runtime.untrackChunk(event.getChunk());
    }
}
