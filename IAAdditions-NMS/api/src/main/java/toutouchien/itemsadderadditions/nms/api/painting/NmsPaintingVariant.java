package toutouchien.itemsadderadditions.nms.api.painting;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record NmsPaintingVariant(
        String variantId,
        int width,
        int height,
        String assetId,
        @Nullable String title,
        @Nullable String author
) {
}
