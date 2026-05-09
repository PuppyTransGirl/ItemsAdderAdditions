package toutouchien.itemsadderadditions.utils.loading;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.utils.FileUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Centralized, single-pass loader and category index for all ItemsAdder YAML files.
 *
 * <h3>Problem this solves</h3>
 * Before this class, every subsystem (recipes, furniture populators, surface
 * decorators…) independently walked the entire {@code contents/} directory and
 * re-parsed every YAML file from scratch. With N systems and M files, that is
 * N × M file reads and N × M YAML parses.
 *
 * <h3>Solution</h3>
 * {@link #scan} performs exactly <em>one</em> directory walk and <em>one</em> YAML
 * parse per <em>changed</em> file. The result is partitioned into per-category lists
 * that are stored in an {@link EnumMap} for O(1) retrieval.
 *
 * <h3>Incremental reload</h3>
 * A static persistent cache stores per-file fingerprints ({@code lastModified},
 * {@code length}, MD5) and their parsed {@link CategorizedConfigFile} objects
 * across reload cycles.  On the next reload:
 * <ol>
 *   <li>{@code lastModified} + {@code length} are checked first - if both match,
 *       the file is reused immediately (zero I/O beyond the stat).</li>
 *   <li>If either differs, an MD5 digest of the file body is computed and compared
 *       with the stored digest.  A mismatch triggers a full reparse; a match (rare
 *       "touch without edit" case) updates the fingerprint and reuses the cached
 *       result.</li>
 * </ol>
 * Unchanged files pay no disk read, no YAML parse, and no categorization cost.
 *
 * <h3>Parallel parsing</h3>
 * Changed files are parsed concurrently on a shared daemon thread pool.  Each
 * {@link YamlConfiguration#loadConfiguration} call creates its own internal
 * SnakeYAML parser, so there is no shared-mutable-state concern.
 *
 * <h3>Lifecycle</h3>
 * Create a fresh registry instance per reload cycle; discard it once all loaders
 * have run.  The <em>static</em> persistent cache survives across instances.
 * Call {@link #clearPersistentCache()} only when a clean-slate reload is required
 * (e.g. first startup after upgrading the plugin).
 *
 * <h3>Thread safety</h3>
 * The registry instance is immutable after construction. All lists returned by
 * {@link #getFiles} are unmodifiable views. Concurrent reads from multiple
 * threads are safe.
 *
 * <h3>Extensibility</h3>
 * Adding a new category requires only a new constant in {@link ConfigFileCategory}.
 * No changes to this class are needed.
 */
@NullMarked
public final class ConfigFileRegistry {
    private static final String LOG_TAG = "ConfigFileRegistry";

    private static final ExecutorService PARSE_POOL = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread t = new Thread(r, "cfg-parse-pool");
                t.setDaemon(true);
                return t;
            });

    /**
     * path → [lastModified, length]
     */
    private static final ConcurrentHashMap<String, long[]> PREV_QUICK_FP
            = new ConcurrentHashMap<>();

    /**
     * path → hex-encoded MD5 of file body
     */
    private static final ConcurrentHashMap<String, String> PREV_MD5
            = new ConcurrentHashMap<>();

    /**
     * path → last successfully parsed and categorised result
     */
    private static final ConcurrentHashMap<String, CategorizedConfigFile> PREV_RESULT
            = new ConcurrentHashMap<>();
    /** Category → ordered list of files that belong to it. */
    private final EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> byCategory;

    /**
     * Drops all persistent fingerprint and parse caches.  Call this to force a
     * full re-scan on the next reload (e.g. after a plugin upgrade).
     */
    public static void clearPersistentCache() {
        PREV_QUICK_FP.clear();
        PREV_MD5.clear();
        PREV_RESULT.clear();
        Log.debug(LOG_TAG, "Persistent file cache cleared.");
    }
    private final int totalFilesScanned;
    private final int totalFilesTagged;

    private ConfigFileRegistry(
            EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> byCategory,
            int totalFilesScanned,
            int totalFilesTagged) {
        this.byCategory = byCategory;
        this.totalFilesScanned = totalFilesScanned;
        this.totalFilesTagged = totalFilesTagged;
    }

    /**
     * Scans {@code contentsDir} once, parses every <em>changed</em> {@code .yml}
     * file, and builds the category index.
     *
     * <p>Files whose {@code lastModified} + {@code length} match the previous
     * cycle are reused without any I/O.  Files where only one of those differs are
     * verified with an MD5 digest before deciding whether to reparse.
     *
     * <p>Files that need parsing are dispatched to a shared daemon thread pool,
     * so the wall-clock cost of a large reload scales with the number of available
     * CPU cores rather than the number of changed files.
     *
     * @param contentsDir the ItemsAdder {@code contents/} directory
     * @return an immutable registry; never {@code null}
     */
    public static ConfigFileRegistry scan(File contentsDir) {
        if (!contentsDir.exists()) {
            Log.warn(LOG_TAG, "Contents directory not found: {}", contentsDir.getPath());
            return empty();
        }

        List<File> yamlFiles = FileUtils.collectYamlFiles(contentsDir);
        Log.info(LOG_TAG, "Scanning {} YAML file(s) for all systems...", yamlFiles.size());

        // Pre-populate every category bucket.
        EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> mutableByCategory =
                new EnumMap<>(ConfigFileCategory.class);
        for (ConfigFileCategory cat : ConfigFileCategory.values()) {
            mutableByCategory.put(cat, new ArrayList<>());
        }

        // Partition files into cache-hits (reuse) and cache-misses (parse).
        List<CategorizedConfigFile> reused = new ArrayList<>();
        List<File> toParseFiles = new ArrayList<>();
        // Track which new fingerprints to commit after a successful parse.
        Map<String, long[]> newQuickFp = new HashMap<>();
        Map<String, String> newMd5 = new HashMap<>();

        for (File file : yamlFiles) {
            String path = file.getAbsolutePath();
            long lastMod = file.lastModified();
            long length = file.length();

            long[] prevFp = PREV_QUICK_FP.get(path);
            if (prevFp != null && prevFp[0] == lastMod && prevFp[1] == length) {
                // Quick fingerprint matches - reuse unconditionally.
                CategorizedConfigFile cached = PREV_RESULT.get(path);
                if (cached != null) {
                    reused.add(cached);
                    continue;
                }
                // Fingerprint present but result missing (shouldn't happen normally).
            }

            // Quick fingerprint missed - compute MD5 to confirm whether content changed.
            String currentMd5 = md5Hex(file);
            String prevMd5 = PREV_MD5.get(path);
            if (prevMd5 != null && prevMd5.equals(currentMd5)) {
                // Content unchanged (e.g. file was touched/copied without edit).
                // Update quick fingerprint so the next reload avoids MD5 computation.
                newQuickFp.put(path, new long[]{lastMod, length});
                CategorizedConfigFile cached = PREV_RESULT.get(path);
                if (cached != null) {
                    reused.add(cached);
                    continue;
                }
            }

            // Content changed (or first scan) - schedule for (re)parse.
            toParseFiles.add(file);
            newQuickFp.put(path, new long[]{lastMod, length});
            if (currentMd5 != null) {
                newMd5.put(path, currentMd5);
            }
        }

        int reusedCount = reused.size();
        Log.debug(LOG_TAG, "{} file(s) reused from cache, {} to (re)parse.",
                reusedCount, toParseFiles.size());

        // Incorporate reused entries.
        for (CategorizedConfigFile ccf : reused) {
            for (ConfigFileCategory cat : ccf.categories()) {
                mutableByCategory.get(cat).add(ccf);
            }
        }

        // Parse changed files in parallel.
        AtomicInteger tagged = new AtomicInteger(reused.size());
        if (!toParseFiles.isEmpty()) {
            List<CompletableFuture<Void>> futures = new ArrayList<>(toParseFiles.size());

            // Synchronised collector for results written from worker threads.
            // Using a plain list + synchronized is simpler and avoids ConcurrentHashMap
            // overhead since the final merge is a single-threaded step anyway.
            List<CategorizedConfigFile> parsedResults =
                    Collections.synchronizedList(new ArrayList<>());

            for (File file : toParseFiles) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                        EnumSet<ConfigFileCategory> categories = ConfigFileCategory.detect(yaml);
                        if (categories.isEmpty()) return; // no recognized section

                        parsedResults.add(new CategorizedConfigFile(file, yaml, categories));
                    } catch (Exception e) {
                        Log.error(LOG_TAG, "Failed to parse: " + file.getPath(), e);
                    }
                }, PARSE_POOL));
            }

            // Wait for all workers to finish.
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.error(LOG_TAG, "Parse interrupted - some files may be missing.", e);
            } catch (ExecutionException e) {
                Log.error(LOG_TAG, "Unexpected error during parallel parse.", e.getCause());
            }

            // Merge parsed results into the category maps and persistent cache.
            for (CategorizedConfigFile ccf : parsedResults) {
                String path = ccf.file().getAbsolutePath();
                for (ConfigFileCategory cat : ccf.categories()) {
                    mutableByCategory.get(cat).add(ccf);
                }
                PREV_RESULT.put(path, ccf);
                tagged.incrementAndGet();
            }
        }

        // Commit updated fingerprints.
        PREV_QUICK_FP.putAll(newQuickFp);
        PREV_MD5.putAll(newMd5);

        // Evict entries for files that no longer exist (deleted between reloads).
        Set<String> currentPaths = new HashSet<>(yamlFiles.size() * 2);
        for (File f : yamlFiles) currentPaths.add(f.getAbsolutePath());
        PREV_QUICK_FP.keySet().retainAll(currentPaths);
        PREV_MD5.keySet().retainAll(currentPaths);
        PREV_RESULT.keySet().retainAll(currentPaths);

        // Seal all lists.
        EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> immutableByCategory =
                new EnumMap<>(ConfigFileCategory.class);
        for (Map.Entry<ConfigFileCategory, List<CategorizedConfigFile>> entry
                : mutableByCategory.entrySet()) {
            immutableByCategory.put(entry.getKey(),
                    Collections.unmodifiableList(entry.getValue()));
        }

        Log.info(LOG_TAG,
                "Scan complete: {} file(s) scanned, {} tagged ({} reused, {} parsed) " +
                        "across {} category buckets.",
                yamlFiles.size(), tagged.get(), reusedCount,
                tagged.get() - reusedCount,
                countNonEmptyBuckets(immutableByCategory));

        return new ConfigFileRegistry(immutableByCategory, yamlFiles.size(), tagged.get());
    }

    /**
     * Returns the hex-encoded MD5 digest of {@code file}'s content, or
     * {@code null} if the file cannot be read.  MD5 is chosen for speed and
     * zero-dependency (JDK {@link MessageDigest}); collision resistance against
     * adversaries is irrelevant here.
     */
    private static @org.jspecify.annotations.Nullable String md5Hex(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[8192];
            try (FileInputStream fis = new FileInputStream(file);
                 DigestInputStream dis = new DigestInputStream(fis, md)) {
                while (dis.read(buf) != -1) { /* digest consumed by stream */ }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 is mandated by the JDK spec; this cannot happen.
            throw new IllegalStateException("MD5 unavailable", e);
        } catch (IOException e) {
            Log.warn(LOG_TAG, "Could not compute MD5 for {}: {}", file.getPath(), e.getMessage());
            return null;
        }
    }

    private static ConfigFileRegistry empty() {
        EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> empty =
                new EnumMap<>(ConfigFileCategory.class);
        for (ConfigFileCategory cat : ConfigFileCategory.values()) {
            empty.put(cat, Collections.emptyList());
        }
        return new ConfigFileRegistry(empty, 0, 0);
    }

    private static int countNonEmptyBuckets(
            EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> map) {
        int count = 0;
        for (List<CategorizedConfigFile> list : map.values()) {
            if (!list.isEmpty()) count++;
        }
        return count;
    }

    /**
     * Returns all files tagged with {@code category}, in the order they were
     * discovered during the scan. The list is unmodifiable.
     *
     * <pre>{@code
     * List<CategorizedConfigFile> files = registry.getFiles(ConfigFileCategory.STONECUTTER_RECIPES);
     * for (CategorizedConfigFile ccf : files) {
     *     stonecutterHandler.load(namespace, ccf.yaml().getConfigurationSection("recipes.stonecutter"));
     * }
     * }</pre>
     */
    public List<CategorizedConfigFile> getFiles(ConfigFileCategory category) {
        return byCategory.getOrDefault(category, Collections.emptyList());
    }

    /**
     * Returns the union of files matching <em>any</em> of the supplied categories,
     * preserving scan order and eliminating duplicates.
     *
     * <p>Useful when a single loader handles multiple related categories
     * (e.g. {@code RecipeLoader} handles campfire, stonecutter, and crafting):
     *
     * <pre>{@code
     * List<CategorizedConfigFile> recipeFiles = registry.getFiles(
     *         ConfigFileCategory.CAMPFIRE_RECIPES,
     *         ConfigFileCategory.STONECUTTER_RECIPES,
     *         ConfigFileCategory.CRAFTING_RECIPES);
     * }</pre>
     *
     * @param categories one or more categories to union
     * @return an unmodifiable list; never {@code null}
     */
    public List<CategorizedConfigFile> getFiles(ConfigFileCategory... categories) {
        if (categories.length == 0) return Collections.emptyList();
        if (categories.length == 1) return getFiles(categories[0]);

        // LinkedHashSet preserves insertion order while deduplicating files that
        // belong to more than one of the requested categories.
        LinkedHashSet<CategorizedConfigFile> result = new LinkedHashSet<>();
        for (ConfigFileCategory cat : categories) {
            result.addAll(byCategory.getOrDefault(cat, Collections.emptyList()));
        }
        return Collections.unmodifiableList(new ArrayList<>(result));
    }

    /**
     * Total number of {@code .yml} files visited during the scan.
     */
    public int totalFilesScanned() {
        return totalFilesScanned;
    }

    /**
     * Number of files that matched at least one {@link ConfigFileCategory}.
     * Files skipped because they contain no recognized sections are excluded.
     */
    public int totalFilesTagged() {
        return totalFilesTagged;
    }

    /**
     * Number of files indexed under {@code category}.
     */
    public int fileCount(ConfigFileCategory category) {
        return byCategory.getOrDefault(category, Collections.emptyList()).size();
    }
}
