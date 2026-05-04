package toutouchien.itemsadderadditions.nms.api;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

public interface INmsCreativeMenuHandler {
    /**
     * Registers the Netty pipeline handlers for all future connections.
     */
    void injectListeners(Plugin plugin);

    /**
     * Rebuilds the painting-variant-ID → CustomStack lookup from the live
     * registry. Call after every ItemsAdder reload.
     */
    void updatePaintingCache(Collection<CustomStack> items);

    /**
     * Injects custom PaintingVariant entries into the frozen registry.
     */
    void injectPaintingVariants(Collection<CustomStack> items);
}
