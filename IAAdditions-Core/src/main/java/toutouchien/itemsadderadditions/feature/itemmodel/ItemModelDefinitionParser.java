package toutouchien.itemsadderadditions.feature.itemmodel;

import com.google.gson.JsonObject;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.parse.ComponentTreeParser;
import toutouchien.itemsadderadditions.nms.api.component.ComponentValue;

import java.util.Optional;

@NullMarked
final class ItemModelDefinitionParser {
    private final ItemModelDefinitionNormalizer normalizer = new ItemModelDefinitionNormalizer();

    Optional<ItemModelDefinitionData> parse(CustomStack item) {
        FileConfiguration config = item.getConfig();
        String base = "items." + item.getId() + ".item_model_definition";
        ConfigurationSection section = config.getConfigurationSection(base);
        if (section == null) return Optional.empty();

        String namespacedId = item.getNamespacedID();
        if (!section.getBoolean("enabled", true)) return Optional.empty();

        Optional<ItemModelDefinitionPath> definitionPath = ItemModelDefinitionPath.parse(
                section.getString("path"),
                item.getNamespace(),
                item.getId(),
                namespacedId
        );
        if (definitionPath.isEmpty()) return Optional.empty();

        boolean hasRaw = section.contains("raw");
        boolean hasModel = section.contains("model");
        if (!hasRaw && !hasModel) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, namespacedId,
                    "item_model_definition requires either 'model' or 'raw'.");
            return Optional.empty();
        }
        if (hasRaw && hasModel) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, namespacedId,
                    "item_model_definition has both 'raw' and 'model'; using 'raw'.");
        }

        Optional<JsonObject> json = hasRaw
                ? normalizer.normalizeRawRoot(section.get("raw"), namespacedId)
                : buildRootFromModel(section.get("model"), namespacedId);
        if (json.isEmpty()) return Optional.empty();

        boolean applyComponent = section.getBoolean("apply_component", true);
        boolean overwriteExistingFile = section.getBoolean("overwrite_existing_file", true);

        ExistingComponent existingItemModel = existingComponent(config, item.getId(), "item_model");
        boolean applyItemModelComponent = applyComponent;
        if (applyComponent && existingItemModel.present()) {
            String generatedId = definitionPath.get().id();
            if (!generatedId.equals(existingItemModel.value())) {
                Log.itemWarn(ItemModelDefinitionManager.NAME, namespacedId,
                        "components.{} is '{}', but generated item model id is '{}'; leaving the existing component untouched.",
                        existingItemModel.key(), existingItemModel.value(), generatedId);
            }
            applyItemModelComponent = false;
        }

        ComponentValue customModelData = null;
        if (applyComponent && section.contains("custom_model_data")) {
            ExistingComponent existingCustomModelData = existingComponent(config, item.getId(), "custom_model_data");
            if (existingCustomModelData.present()) {
                Log.itemWarn(ItemModelDefinitionManager.NAME, namespacedId,
                        "item_model_definition.custom_model_data skipped because components.{} already exists.",
                        existingCustomModelData.key());
            } else {
                customModelData = parseCustomModelData(section.get("custom_model_data"), namespacedId);
            }
        }

        return Optional.of(new ItemModelDefinitionData(
                namespacedId,
                definitionPath.get(),
                json.get(),
                applyItemModelComponent,
                overwriteExistingFile,
                customModelData
        ));
    }

    private Optional<JsonObject> buildRootFromModel(@Nullable Object rawModel, String namespacedId) {
        Optional<JsonObject> model = normalizer.normalizeBuilderModel(rawModel, namespacedId);
        if (model.isEmpty()) return Optional.empty();

        JsonObject root = new JsonObject();
        root.add("model", model.get());
        return Optional.of(root);
    }

    @Nullable
    private ComponentValue parseCustomModelData(@Nullable Object raw, String namespacedId) {
        if (!(raw instanceof ConfigurationSection) && !(raw instanceof java.util.Map<?, ?>)) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, namespacedId,
                    "item_model_definition.custom_model_data must be an object with floats, flags, strings and/or colors lists.");
            return null;
        }
        return ComponentTreeParser.parse(raw);
    }

    private ExistingComponent existingComponent(FileConfiguration config, String itemId, String shortKey) {
        ConfigurationSection components = config.getConfigurationSection("items." + itemId + ".components");
        if (components == null) return ExistingComponent.missing();

        if (components.contains(shortKey)) {
            return new ExistingComponent(true, shortKey, scalarAsString(components.get(shortKey)));
        }

        String namespacedKey = "minecraft:" + shortKey;
        if (components.contains(namespacedKey)) {
            return new ExistingComponent(true, namespacedKey, scalarAsString(components.get(namespacedKey)));
        }

        return ExistingComponent.missing();
    }

    private String scalarAsString(@Nullable Object value) {
        return value instanceof String s ? s : String.valueOf(value);
    }

    private record ExistingComponent(boolean present, String key, String value) {
        static ExistingComponent missing() {
            return new ExistingComponent(false, "", "");
        }
    }
}
