package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.contentvariant;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantConfig;

import java.util.stream.Stream;

/** Visual variants selected from the contents of a persistent storage. */
@NullMarked
public record ContentVariantConfig(
        @Nullable OpenVariantConfig empty,
        @Nullable OpenVariantConfig filled,
        @Nullable OpenVariantConfig half,
        @Nullable OpenVariantConfig full
) {
    public boolean isConfigured() {
        return empty != null || filled != null || half != null || full != null;
    }

    /**
     * Selects the visual for the current contents. A two-state configuration uses
     * {@code filled_variant} for every non-empty inventory. When the optional
     * {@code half_variant} and {@code full_variant} are supplied they take priority
     * for partially-filled inventories and inventories at stack capacity respectively.
     */
    @Nullable
    public OpenVariantConfig variantFor(@Nullable ItemStack @Nullable [] contents) {
        return switch (StorageFillState.fromContents(contents)) {
            case EMPTY -> empty;
            case HALF -> half != null ? half : filled;
            case FULL -> full != null ? full : (filled != null ? filled : half);
        };
    }

    public boolean containsId(String id) {
        return variants().anyMatch(variant -> variant.id().equals(id));
    }

    public Stream<OpenVariantConfig> variants() {
        return Stream.of(empty, filled, half, full).filter(java.util.Objects::nonNull);
    }
}
