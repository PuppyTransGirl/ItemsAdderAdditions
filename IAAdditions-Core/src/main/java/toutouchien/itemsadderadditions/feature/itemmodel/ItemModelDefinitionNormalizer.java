package toutouchien.itemsadderadditions.feature.itemmodel;

import com.google.gson.*;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@NullMarked
final class ItemModelDefinitionNormalizer {
    Optional<JsonObject> normalizeBuilderModel(@Nullable Object raw, String itemId) {
        JsonElement element = toJson(raw, true);
        if (!element.isJsonObject()) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                    "item_model_definition.model must be a YAML object.");
            return Optional.empty();
        }

        JsonObject model = element.getAsJsonObject();
        return validateModel(model, itemId, "model") ? Optional.of(model) : Optional.empty();
    }

    Optional<JsonObject> normalizeRawRoot(@Nullable Object raw, String itemId) {
        JsonElement element = toJson(raw, false);
        if (!element.isJsonObject()) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                    "item_model_definition.raw must be a YAML object.");
            return Optional.empty();
        }

        JsonObject root = element.getAsJsonObject();
        JsonElement model = root.get("model");
        if (model == null || !model.isJsonObject()) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                    "item_model_definition.raw must contain a top-level 'model' object.");
            return Optional.empty();
        }

        return Optional.of(root);
    }

    private boolean validateModel(JsonObject model, String itemId, String location) {
        String type = stringValue(model.get("type"));
        if (type == null || type.isBlank()) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                    "item_model_definition.{} is missing required 'type'.", location);
            return false;
        }

        return switch (type) {
            case "minecraft:model" -> requireString(model, "model", itemId, location);
            case "minecraft:composite" -> validateComposite(model, itemId, location);
            case "minecraft:condition" -> validateCondition(model, itemId, location);
            case "minecraft:select" -> validateSelect(model, itemId, location);
            case "minecraft:range_dispatch" -> validateRangeDispatch(model, itemId, location);
            case "minecraft:empty", "minecraft:bundle/selected_item" -> true;
            case "minecraft:special" -> validateSpecial(model, itemId, location);
            default -> true;
        };
    }

    private boolean validateComposite(JsonObject model, String itemId, String location) {
        JsonElement models = model.get("models");
        if (models == null || !models.isJsonArray() || models.getAsJsonArray().size() == 0) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                    "item_model_definition.{} type minecraft:composite requires a non-empty 'models' list.",
                    location);
            return false;
        }

        int index = 0;
        for (JsonElement child : models.getAsJsonArray()) {
            if (!child.isJsonObject()) {
                Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                        "item_model_definition.{}.models[{}] must be an object.", location, index);
                return false;
            }
            if (!validateModel(child.getAsJsonObject(), itemId, location + ".models[" + index + "]")) {
                return false;
            }
            index++;
        }
        return true;
    }

    private boolean validateCondition(JsonObject model, String itemId, String location) {
        if (!requireString(model, "property", itemId, location)) return false;
        if (!requireNestedModel(model, "on_true", itemId, location)) return false;
        return requireNestedModel(model, "on_false", itemId, location);
    }

    private boolean validateSelect(JsonObject model, String itemId, String location) {
        if (!requireString(model, "property", itemId, location)) return false;

        JsonElement cases = model.get("cases");
        if (cases == null || !cases.isJsonArray() || cases.getAsJsonArray().size() == 0) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                    "item_model_definition.{} type minecraft:select requires a non-empty 'cases' list.",
                    location);
            return false;
        }

        int index = 0;
        for (JsonElement entry : cases.getAsJsonArray()) {
            if (!entry.isJsonObject()) {
                Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                        "item_model_definition.{}.cases[{}] must be an object.", location, index);
                return false;
            }
            JsonObject caseObject = entry.getAsJsonObject();
            if (!caseObject.has("when")) {
                Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                        "item_model_definition.{}.cases[{}] is missing 'when'.", location, index);
                return false;
            }
            if (!requireNestedModel(caseObject, "model", itemId, location + ".cases[" + index + "]")) {
                return false;
            }
            index++;
        }

        return validateOptionalNestedModel(model, "fallback", itemId, location);
    }

    private boolean validateRangeDispatch(JsonObject model, String itemId, String location) {
        if (!requireString(model, "property", itemId, location)) return false;

        JsonElement entries = model.get("entries");
        if (entries == null || !entries.isJsonArray() || entries.getAsJsonArray().size() == 0) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                    "item_model_definition.{} type minecraft:range_dispatch requires a non-empty 'entries' list.",
                    location);
            return false;
        }

        int index = 0;
        for (JsonElement entry : entries.getAsJsonArray()) {
            if (!entry.isJsonObject()) {
                Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                        "item_model_definition.{}.entries[{}] must be an object.", location, index);
                return false;
            }
            JsonObject entryObject = entry.getAsJsonObject();
            JsonElement threshold = entryObject.get("threshold");
            if (threshold == null || !threshold.isJsonPrimitive() || !threshold.getAsJsonPrimitive().isNumber()) {
                Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                        "item_model_definition.{}.entries[{}].threshold must be a number.", location, index);
                return false;
            }
            if (!requireNestedModel(entryObject, "model", itemId, location + ".entries[" + index + "]")) {
                return false;
            }
            index++;
        }

        return validateOptionalNestedModel(model, "fallback", itemId, location);
    }

    private boolean validateSpecial(JsonObject model, String itemId, String location) {
        if (!requireString(model, "base", itemId, location)) return false;

        JsonElement specialModel = model.get("model");
        if (specialModel == null || !specialModel.isJsonObject()) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                    "item_model_definition.{} type minecraft:special requires a 'model' object.", location);
            return false;
        }
        JsonObject special = specialModel.getAsJsonObject();
        if (!requireString(special, "type", itemId, location + ".model")) return false;
        return true;
    }

    private boolean requireNestedModel(JsonObject parent, String key, String itemId, String location) {
        JsonElement child = parent.get(key);
        if (child == null || !child.isJsonObject()) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                    "item_model_definition.{} is missing required '{}' model object.", location, key);
            return false;
        }
        return validateModel(child.getAsJsonObject(), itemId, location + "." + key);
    }

    private boolean validateOptionalNestedModel(JsonObject parent, String key, String itemId, String location) {
        JsonElement child = parent.get(key);
        if (child == null) return true;
        if (!child.isJsonObject()) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                    "item_model_definition.{} optional '{}' must be a model object.", location, key);
            return false;
        }
        return validateModel(child.getAsJsonObject(), itemId, location + "." + key);
    }

    private boolean requireString(JsonObject object, String key, String itemId, String location) {
        String value = stringValue(object.get(key));
        if (value == null || value.isBlank()) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, itemId,
                    "item_model_definition.{} is missing required string '{}'.", location, key);
            return false;
        }
        return true;
    }

    @Nullable
    private String stringValue(@Nullable JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) return null;
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        return primitive.isString() ? primitive.getAsString() : null;
    }

    private JsonElement toJson(@Nullable Object value, boolean normalizeKnownKeys) {
        if (value == null) return JsonNull.INSTANCE;
        if (value instanceof ConfigurationSection section) {
            JsonObject object = new JsonObject();
            for (String key : section.getKeys(false)) {
                object.add(key, normalizeEntry(key, section.get(key), normalizeKnownKeys));
            }
            return object;
        }
        if (value instanceof Map<?, ?> map) {
            JsonObject object = new JsonObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                object.add(key, normalizeEntry(key, entry.getValue(), normalizeKnownKeys));
            }
            return object;
        }
        if (value instanceof List<?> list) {
            JsonArray array = new JsonArray();
            for (Object child : list) {
                array.add(toJson(child, normalizeKnownKeys));
            }
            return array;
        }
        if (value instanceof Boolean b) return new JsonPrimitive(b);
        if (value instanceof Number n) return new JsonPrimitive(n);
        return new JsonPrimitive(String.valueOf(value));
    }

    private JsonElement normalizeEntry(String key, @Nullable Object value, boolean normalizeKnownKeys) {
        JsonElement json = toJson(value, normalizeKnownKeys);
        if (!normalizeKnownKeys || !json.isJsonPrimitive()) return json;

        JsonPrimitive primitive = json.getAsJsonPrimitive();
        if (!primitive.isString()) return json;

        if (key.equals("type") || key.equals("property")) {
            return new JsonPrimitive(namespacedMinecraftId(primitive.getAsString()));
        }

        return json;
    }

    private String namespacedMinecraftId(String value) {
        String trimmed = value.trim();
        return trimmed.contains(":") ? trimmed : "minecraft:" + trimmed;
    }
}
