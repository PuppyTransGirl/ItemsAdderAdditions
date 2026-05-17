package toutouchien.itemsadderadditions.nms.api;

import org.bukkit.entity.Painting;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.painting.NmsPaintingVariant;

import java.util.Collection;

@NullMarked
public interface INmsPaintingHandler {
    /**
     * Injects custom PaintingVariant entries into Minecraft's runtime registry.
     */
    void injectPaintingVariants(Collection<NmsPaintingVariant> variants);

    /**
     * Updates which managed custom variants are part of Minecraft's vanilla random painting pool.
     */
    void updateRandomPlaceableVariants(Collection<String> managedVariantIds, Collection<String> randomVariantIds);

    /**
     * Assigns an injected PaintingVariant to an already spawned Bukkit painting.
     */
    boolean applyVariant(Painting painting, String variantId);

    /**
     * Returns whether the painting can still exist at its current position.
     */
    boolean isStillValid(Painting painting);
}
