package toutouchien.itemsadderadditions.feature.tag;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.loading.CategorizedConfigFile;
import toutouchien.itemsadderadditions.common.loading.ConfigFileCategory;
import toutouchien.itemsadderadditions.common.loading.ConfigFileRegistry;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.CustomTagDefinition;
import toutouchien.itemsadderadditions.common.namespace.CustomTagRegistry;
import toutouchien.itemsadderadditions.common.namespace.CustomTagType;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public final class CustomTagLoader {
    private static final String LOG_TAG = "CustomTags";

    public CustomTagRegistry loadAll(ConfigFileRegistry registry) {
        List<CustomTagDefinition> definitions = new ArrayList<>();
        List<CategorizedConfigFile> files = registry.getFiles(ConfigFileCategory.TAGS);

        Log.debug(LOG_TAG, "Processing {} YAML file(s) for custom tags...", files.size());
        for (CategorizedConfigFile file : files) {
            loadFile(file, definitions);
        }

        return CustomTagRegistry.resolve(definitions);
    }

    private void loadFile(CategorizedConfigFile file, List<CustomTagDefinition> output) {
        try {
            YamlConfiguration yaml = file.yaml();
            String filePath = file.file().getPath();
            String namespace = yaml.getString("info.namespace");
            if (namespace == null || namespace.isBlank()) {
                Log.warn(LOG_TAG, "Tag file missing 'info.namespace', skipping. File: {}", filePath);
                return;
            }

            ConfigurationSection tags = yaml.getConfigurationSection("tags");
            if (tags == null) {
                Log.warn(LOG_TAG, "'tags' must be a section. File: {}", filePath);
                return;
            }

            for (String tagId : tags.getKeys(false)) {
                loadTag(namespace, tagId, tags, filePath, output);
            }
        } catch (Exception exception) {
            Log.error(LOG_TAG, "Failed to parse custom tags file: " + file.file().getPath(), exception);
        }
    }

    private void loadTag(
            String namespace,
            String tagId,
            ConfigurationSection parent,
            String filePath,
            List<CustomTagDefinition> output
    ) {
        ConfigurationSection entry = parent.getConfigurationSection(tagId);
        if (entry == null) {
            Log.warn(LOG_TAG,
                    "Tag '{}:{}' must be a section. File: {}",
                    namespace, tagId, filePath);
            return;
        }

        @Nullable CustomTagType type = parseType(namespace, tagId, entry, filePath);
        if (type == null) return;

        if (!entry.contains("values")) {
            Log.warn(LOG_TAG,
                    "Tag '{}:{}' is missing required 'values' list. File: {}",
                    namespace, tagId, filePath);
            return;
        }

        List<?> rawValues = entry.getList("values");
        if (rawValues == null) {
            Log.warn(LOG_TAG,
                    "Tag '{}:{}' has non-list 'values'. File: {}",
                    namespace, tagId, filePath);
            return;
        }

        List<String> values = new ArrayList<>();
        for (Object rawValue : rawValues) {
            if (!(rawValue instanceof String value)) {
                Log.warn(LOG_TAG,
                        "Tag '{}:{}' has unsupported value '{}'. Values must be strings. File: {}",
                        namespace, tagId, rawValue, filePath);
                continue;
            }
            values.add(value);
        }

        if (values.isEmpty()) {
            Log.warn(LOG_TAG,
                    "Tag '{}:{}' has no valid values. File: {}",
                    namespace, tagId, filePath);
        }

        output.add(new CustomTagDefinition(namespace, tagId, type, values, filePath));
    }

    private @Nullable CustomTagType parseType(
            String namespace,
            String tagId,
            ConfigurationSection entry,
            String filePath
    ) {
        Object rawType = entry.get("type");
        if (rawType == null) return CustomTagType.ITEM;
        if (!(rawType instanceof String typeText)) {
            Log.warn(LOG_TAG,
                    "Tag '{}:{}' has non-string type '{}'. File: {}",
                    namespace, tagId, rawType, filePath);
            return null;
        }

        CustomTagType type = CustomTagType.fromYaml(typeText);
        if (type == null) {
            Log.warn(LOG_TAG,
                    "Tag '{}:{}' has unknown type '{}'. File: {}",
                    namespace, tagId, typeText, filePath);
            return null;
        }
        return type;
    }
}
