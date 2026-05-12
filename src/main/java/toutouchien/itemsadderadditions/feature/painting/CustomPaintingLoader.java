package toutouchien.itemsadderadditions.feature.painting;

import dev.lone.itemsadder.api.CustomStack;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.loading.CategorizedConfigFile;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public final class CustomPaintingLoader {
    private static final String TAG = "CustomPaintings";

    private static boolean isValidKey(String value) {
        try {
            Key.key(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        if (value == null || value.isBlank()) return null;
        return value;
    }

    public List<CustomPaintingDefinition> loadAll(List<CategorizedConfigFile> files) {
        Log.debug(TAG, "Processing {} YAML file(s) for custom painting entries...", files.size());

        List<CustomPaintingDefinition> definitions = new ArrayList<>();
        for (CategorizedConfigFile ccf : files) {
            try {
                loadFile(ccf.yaml(), ccf.file().getPath(), definitions);
            } catch (Exception e) {
                Log.error(TAG, "Failed to parse custom paintings file: " + ccf.file().getPath(), e);
            }
        }

        return definitions;
    }

    private void loadFile(YamlConfiguration yaml, String filePath, List<CustomPaintingDefinition> output) {
        ConfigurationSection info = yaml.getConfigurationSection("info");
        if (info == null) return;

        String namespace = info.getString("namespace");
        if (namespace == null || namespace.isBlank()) return;

        ConfigurationSection section = yaml.getConfigurationSection("paintings");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;

            CustomPaintingDefinition definition = loadEntry(namespace, key, entry, filePath);
            if (definition != null) output.add(definition);
        }
    }

    @Nullable
    private CustomPaintingDefinition loadEntry(String namespace, String key, ConfigurationSection entry, String filePath) {
        if (!entry.getBoolean("enabled", true)) {
            Log.info(TAG, "Skipping disabled custom painting '{}'", key);
            return null;
        }

        String variantId = NamespaceUtils.normalizeID(namespace, key);
        if (!isValidKey(variantId)) {
            Log.warn(TAG, "Custom painting '{}' has an invalid id '{}'. File: {}", key, variantId, filePath);
            return null;
        }

        int width = entry.getInt("width", -1);
        int height = entry.getInt("height", -1);
        if (width <= 0 || height <= 0) {
            Log.warn(TAG, "Custom painting '{}' must define positive width and height. File: {}", variantId, filePath);
            return null;
        }

        String rawAsset = entry.getString("asset");
        if (rawAsset == null || rawAsset.isBlank()) {
            Log.warn(TAG, "Custom painting '{}' is missing required 'asset'. File: {}", variantId, filePath);
            return null;
        }

        String assetId = NamespaceUtils.normalizeID(namespace, rawAsset);
        if (!isValidKey(assetId)) {
            Log.warn(TAG, "Custom painting '{}' has invalid asset '{}'. File: {}", variantId, assetId, filePath);
            return null;
        }

        String itemId = loadLinkedItem(namespace, variantId, entry, filePath);

        return new CustomPaintingDefinition(
                variantId,
                width,
                height,
                assetId,
                emptyToNull(entry.getString("title")),
                emptyToNull(entry.getString("author")),
                itemId,
                entry.getBoolean("include_in_random", false),
                filePath
        );
    }

    @Nullable
    private String loadLinkedItem(String namespace, String variantId, ConfigurationSection entry, String filePath) {
        String rawItem = entry.getString("item");
        if (rawItem == null || rawItem.isBlank()) return null;

        String normalizedItem = NamespaceUtils.normalizeID(namespace, rawItem);
        if (!isValidKey(normalizedItem)) {
            Log.warn(TAG, "Custom painting '{}' has invalid linked item '{}'. File: {}", variantId, rawItem, filePath);
            return null;
        }

        CustomStack customStack = NamespaceUtils.customItemByID(namespace, rawItem);
        if (customStack == null) {
            Log.warn(TAG, "Custom painting '{}' references unknown ItemsAdder item '{}' - variant will still be registered. File: {}",
                    variantId, normalizedItem, filePath);
            return null;
        }

        return customStack.getNamespacedID();
    }
}
