package toutouchien.itemsadderadditions.utils.other;

import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ordered registry of executor prototypes keyed by their YAML config key.
 *
 * <p>The registry stores one prototype per key. Loaders call
 * {@link #getPrototype(String)} and then invoke {@code newInstance()} on the
 * prototype so each loaded item gets its own isolated copy.
 */
@NullMarked
public final class ExecutorRegistry<E extends Keyed> {
    private final Map<String, E> prototypes = new LinkedHashMap<>();
    private final String logPrefix;

    public ExecutorRegistry(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    @SafeVarargs
    public final void register(E... executors) {
        for (E executor : executors) {
            registerPrototype(executor);
        }
    }

    @SafeVarargs
    public final void registerIfEnabled(FileConfiguration config, String configPrefix, E... executors) {
        for (E executor : executors) {
            String key = registerKey(executor);
            if (key == null) {
                continue;
            }

            if (!config.getBoolean(configPrefix + key, true)) {
                Log.disabled(logPrefix, key);
                continue;
            }

            prototypes.put(key, executor);
            Log.registered(logPrefix, key);
        }
    }

    @Nullable
    public E getPrototype(String key) {
        return prototypes.get(key);
    }

    public Collection<E> getAll() {
        return Collections.unmodifiableCollection(prototypes.values());
    }

    @Nullable
    private String registerKey(E executor) {
        try {
            return executor.key();
        } catch (IllegalStateException ex) {
            Log.warn(logPrefix, "Skipping registration - missing key annotation on {}",
                    executor.getClass().getName());
            return null;
        }
    }

    private void registerPrototype(E executor) {
        String key = registerKey(executor);
        if (key == null) {
            return;
        }

        prototypes.put(key, executor);
        Log.registered(logPrefix, key);
    }
}
