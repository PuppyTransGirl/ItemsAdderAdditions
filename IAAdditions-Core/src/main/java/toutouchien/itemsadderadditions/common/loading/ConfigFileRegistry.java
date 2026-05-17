package toutouchien.itemsadderadditions.common.loading;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.utils.FileUtils;

import java.io.File;
import java.util.*;

/**
 * Single-pass index of ItemsAdder content YAML files.
 *
 * <p>A reload builds one registry, then recipes, paintings and worldgen loaders
 * all read from that same parsed index. The registry deliberately keeps no
 * hidden persistent state: every reload reflects the files currently on disk,
 * and stale deleted/edited sections cannot survive through a cache.</p>
 */
@NullMarked
public final class ConfigFileRegistry {
    private static final String LOG_TAG = "ConfigFiles";

    private final EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> byCategory;
    private final int totalFilesScanned;
    private final int totalFilesTagged;

    private ConfigFileRegistry(
            EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> byCategory,
            int totalFilesScanned,
            int totalFilesTagged
    ) {
        this.byCategory = byCategory;
        this.totalFilesScanned = totalFilesScanned;
        this.totalFilesTagged = totalFilesTagged;
    }

    public static ConfigFileRegistry scan(File contentsDir) {
        if (!contentsDir.exists()) {
            Log.warn(LOG_TAG, "ItemsAdder contents directory not found: {}", contentsDir.getPath());
            return empty();
        }

        List<File> yamlFiles = FileUtils.collectYamlFiles(contentsDir);
        EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> mutableByCategory = emptyBuckets();

        int tagged = 0;
        for (File file : yamlFiles) {
            Optional<CategorizedConfigFile> parsed = parseFile(file);
            if (parsed.isEmpty()) {
                continue;
            }

            CategorizedConfigFile configFile = parsed.get();
            for (ConfigFileCategory category : configFile.categories()) {
                mutableByCategory.get(category).add(configFile);
            }
            tagged++;
        }

        EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> immutableByCategory = freeze(mutableByCategory);
        Log.info(LOG_TAG,
                "Scanned {} YAML file(s), tagged {} file(s) across {} category bucket(s).",
                yamlFiles.size(), tagged, countNonEmptyBuckets(immutableByCategory));

        return new ConfigFileRegistry(immutableByCategory, yamlFiles.size(), tagged);
    }

    private static Optional<CategorizedConfigFile> parseFile(File file) {
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            var categories = ConfigFileCategory.detect(yaml);
            if (categories.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new CategorizedConfigFile(file, yaml, categories));
        } catch (Exception exception) {
            Log.error(LOG_TAG, "Failed to parse " + file.getPath(), exception);
            return Optional.empty();
        }
    }

    private static ConfigFileRegistry empty() {
        return new ConfigFileRegistry(freeze(emptyBuckets()), 0, 0);
    }

    private static EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> emptyBuckets() {
        EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> buckets = new EnumMap<>(ConfigFileCategory.class);
        for (ConfigFileCategory category : ConfigFileCategory.values()) {
            buckets.put(category, new ArrayList<>());
        }
        return buckets;
    }

    private static EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> freeze(
            EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> mutable
    ) {
        EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> frozen = new EnumMap<>(ConfigFileCategory.class);
        for (Map.Entry<ConfigFileCategory, List<CategorizedConfigFile>> entry : mutable.entrySet()) {
            frozen.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return frozen;
    }

    private static int countNonEmptyBuckets(EnumMap<ConfigFileCategory, List<CategorizedConfigFile>> map) {
        int count = 0;
        for (List<CategorizedConfigFile> files : map.values()) {
            if (!files.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public List<CategorizedConfigFile> getFiles(ConfigFileCategory category) {
        return byCategory.getOrDefault(category, Collections.emptyList());
    }

    public List<CategorizedConfigFile> getFiles(ConfigFileCategory... categories) {
        if (categories.length == 0) {
            return Collections.emptyList();
        }
        if (categories.length == 1) {
            return getFiles(categories[0]);
        }

        LinkedHashSet<CategorizedConfigFile> files = new LinkedHashSet<>();
        for (ConfigFileCategory category : categories) {
            files.addAll(getFiles(category));
        }
        return List.copyOf(files);
    }

    public int totalFilesScanned() {
        return totalFilesScanned;
    }

    public int totalFilesTagged() {
        return totalFilesTagged;
    }

    public int fileCount(ConfigFileCategory category) {
        return getFiles(category).size();
    }
}
