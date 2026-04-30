package toutouchien.itemsadderadditions.behaviours.executors.bed;

import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.utils.other.Log;

public record SlotOffset(int dx, int dy, int dz) {
    private static final String TAG = "BedBehaviour";

    @Nullable
    public static SlotOffset parse(String raw, String id) {
        String[] parts = raw.trim().split(",", 3);
        if (parts.length != 3) {
            Log.warn(TAG,
                    "{}: invalid slot '{}' - expected 'dx,dy,dz', skipping",
                    id, raw);
            return null;
        }
        try {
            return new SlotOffset(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
            );
        } catch (NumberFormatException e) {
            Log.warn(TAG,
                    "{}: invalid slot '{}' - non-integer component, skipping",
                    id, raw);
            return null;
        }
    }
}
