package toutouchien.itemsadderadditions.common.registry;

import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.*;
import java.util.function.Predicate;

/**
 * Ordered registry of executor prototypes keyed by their YAML config key.
 *
 * <p>Built-in prototypes can be rebuilt when {@code config.yml} is reloaded,
 * while externally registered prototypes remain untouched. Loaders retrieve the
 * prototype and create a fresh instance per item binding.</p>
 */
@NullMarked
public final class ExecutorRegistry<E extends Keyed> {
    private final Map<String, Entry<E>> prototypes = new LinkedHashMap<>();
    private final String logPrefix;

    public ExecutorRegistry(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    @SafeVarargs
    public final void register(E... executors) {
        for (E executor : executors) {
            registerPrototype(executor, Source.EXTERNAL);
        }
    }

    @SafeVarargs
    public final void registerIfEnabled(FileConfiguration config, String configPrefix, E... executors) {
        registerBuiltIns(key -> config.getBoolean(configPrefix + key, true), executors);
    }

    @SafeVarargs
    public final void registerBuiltIns(Predicate<String> enabled, E... executors) {
        registerBuiltIns(enabled, List.of(executors));
    }

    public void registerBuiltIns(Predicate<String> enabled, Collection<? extends E> executors) {
        prototypes.entrySet().removeIf(entry -> entry.getValue().source() == Source.BUILT_IN);

        for (E executor : executors) {
            String key = registerKey(executor);
            if (key == null) {
                continue;
            }

            if (!enabled.test(key)) {
                Log.disabled(logPrefix, key);
                continue;
            }

            Entry<E> existing = prototypes.get(key);
            if (existing != null && existing.source() == Source.EXTERNAL) {
                Log.warn(logPrefix,
                        "Built-in '{}' was not registered because an external executor already uses that key.",
                        key);
                continue;
            }

            prototypes.put(key, new Entry<>(executor, Source.BUILT_IN));
            Log.registered(logPrefix, key);
        }
    }

    @Nullable
    public E getPrototype(String key) {
        Entry<E> entry = prototypes.get(key);
        return entry == null ? null : entry.prototype();
    }

    public Collection<E> getAll() {
        return Collections.unmodifiableList(
                prototypes.values().stream()
                        .map(Entry::prototype)
                        .toList()
        );
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

    private void registerPrototype(E executor, Source source) {
        String key = registerKey(executor);
        if (key == null) {
            return;
        }

        prototypes.put(key, new Entry<>(executor, source));
        Log.registered(logPrefix, key);
    }

    private enum Source {
        BUILT_IN,
        EXTERNAL
    }

    private record Entry<E>(E prototype, Source source) {}
}
