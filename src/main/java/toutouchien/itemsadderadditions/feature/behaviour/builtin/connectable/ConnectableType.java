package toutouchien.itemsadderadditions.feature.behaviour.builtin.connectable;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public enum ConnectableType {
    STAIR, TABLE;

    public static ConnectableType from(@Nullable String raw) {
        if (raw != null && raw.trim().equalsIgnoreCase("table"))
            return TABLE;
        return STAIR;
    }
}
