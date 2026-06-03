package toutouchien.itemsadderadditions.common.namespace;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

@NullMarked
public enum CustomTagType {
    ITEM,
    BLOCK,
    FURNITURE,
    RECIPE;

    public static @Nullable CustomTagType fromYaml(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return ITEM;

        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "item", "items" -> ITEM;
            case "block", "blocks" -> BLOCK;
            case "furniture", "furnitures" -> FURNITURE;
            case "recipe", "recipes" -> RECIPE;
            default -> null;
        };
    }
}
