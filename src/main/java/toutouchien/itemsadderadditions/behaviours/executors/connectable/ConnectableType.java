package toutouchien.itemsadderadditions.behaviours.executors.connectable;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

@NullMarked
public enum ConnectableType {
    STAIR, TABLE;

    public static ConnectableType from(@Nullable String raw) {
        if (raw != null && raw.trim().equalsIgnoreCase("table"))
            return TABLE;
        return STAIR;
    }
}
