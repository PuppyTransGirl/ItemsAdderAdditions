package toutouchien.itemsadderadditions.integration.itemsadder;

import dev.lone.itemsadder.api.CustomEntity;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Safe wrappers around {@link CustomEntity} static methods that throw a
 * {@link RuntimeException} when ItemsAdder's internal entities manager has
 * not been initialized (config flag off, mid-reload, plugin load-order race,
 * etc.). The wrappers convert that into a null return and warn at most once
 * per server session, while still letting the underlying API recover on its
 * own if it becomes available later.
 */
@NullMarked
public final class CustomEntities {
    private static final String TAG = "ItemsAdder";
    private static final AtomicBoolean warned = new AtomicBoolean(false);

    private CustomEntities() {
        throw new IllegalStateException("Utility class");
    }

    @Nullable
    public static CustomEntity byAlreadySpawned(Entity entity) {
        try {
            return CustomEntity.byAlreadySpawned(entity);
        } catch (RuntimeException e) {
            if (warned.compareAndSet(false, true))
                Log.warn(TAG, "ItemsAdder Entities API call failed, complex furniture and custom entity features will return no match until it recovers. Cause: {}", e.getMessage());
            return null;
        }
    }
}
