package toutouchien.itemsadderadditions.utils.loading;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.utils.FileUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.io.File;
import java.util.*;

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
 * parse per file. The result is partitioned into per-category lists that are
 * stored in an {@link EnumMap} for O(1) retrieval.
 *
 * <pre>
 *   Startup cost:      1 × walk + 1 × parse per file (was N × walk + N × parse)
 *   Retrieval cost:    O(1) EnumMap lookup
 *   Memory overhead:   one shared {@link YamlConfiguration} reference per file
 * </pre>
 *
 * <h3>Lifecycle</h3>
 * Create a fresh registry at the start of each reload cycle; discard it once all
 * loaders have run. Do not cache the registry across reloads.
 *
 * <h3>Thread safety</h3>
 * The registry is immutable after construction. All lists returned by
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

    /**
     * Category → ordered list of files that belong to it.
     */
    private final EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> byCategory;
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
     * Scans {@code contentsDir} once, parses every {@code .yml} file, and builds
     * the category index.
     *
     * <p>Files that do not match any known {@link ConfigFileCategory} are silently
     * skipped — they are never stored and incur no memory cost beyond the initial
     * parse, which is discarded immediately.
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

        // Pre-populate every category bucket so getOrDefault is never needed later.
        EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> mutableByCategory =
                new EnumMap<>(ConfigFileCategory.class);
        for (ConfigFileCategory cat : ConfigFileCategory.values()) {
            mutableByCategory.put(cat, new ArrayList<>());
        }

        int tagged = 0;
        for (File file : yamlFiles) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                EnumSet<ConfigFileCategory> categories = ConfigFileCategory.detect(yaml);

                if (categories.isEmpty()) {
                    // No recognized section → skip immediately, discard the parsed YAML.
                    continue;
                }

                CategorizedConfigFile ccf = new CategorizedConfigFile(file, yaml, categories);
                for (ConfigFileCategory cat : categories) {
                    mutableByCategory.get(cat).add(ccf);
                }
                tagged++;

            } catch (Exception e) {
                Log.error(LOG_TAG, "Failed to parse: " + file.getPath(), e);
            }
        }

        // Seal all lists.
        EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> immutableByCategory =
                new EnumMap<>(ConfigFileCategory.class);
        for (Map.Entry<ConfigFileCategory, List<CategorizedConfigFile>> entry : mutableByCategory.entrySet()) {
            immutableByCategory.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }

        Log.info(LOG_TAG,
                "Scan complete: {} file(s) scanned, {} tagged across {} category buckets.",
                yamlFiles.size(), tagged, countNonEmptyBuckets(immutableByCategory));

        return new ConfigFileRegistry(immutableByCategory, yamlFiles.size(), tagged);
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
