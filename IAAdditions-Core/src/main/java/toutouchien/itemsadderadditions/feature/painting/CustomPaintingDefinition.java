package toutouchien.itemsadderadditions.feature.painting;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.nms.api.painting.NmsPaintingVariant;

@NullMarked
public record CustomPaintingDefinition(
        String variantId,
        int width,
        int height,
        String assetId,
        @Nullable String title,
        @Nullable String author,
        @Nullable String itemId,
        boolean includeInRandom,
        String sourceFile
) {
    public NmsPaintingVariant toNmsVariant() {
        return new NmsPaintingVariant(variantId, width, height, assetId, title, author);
    }
}
