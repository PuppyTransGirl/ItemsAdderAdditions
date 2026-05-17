package toutouchien.itemsadderadditions.feature.creative;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.lone.itemsadder.api.CustomStack;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;

@NullMarked
final class CreativePaintingModelWriter {
    int write(Collection<CustomStack> items) {
        JsonObject root = new JsonObject();
        root.add("model", selectModel(items));

        CreativeResourcePackFiles.writeJson(
                CreativeResourcePackFiles.resourcePackFile("assets/minecraft/items/painting.json"),
                root
        );
        return root.getAsJsonObject("model").getAsJsonArray("cases").size();
    }

    private JsonObject selectModel(Collection<CustomStack> items) {
        JsonObject selectModel = new JsonObject();
        selectModel.addProperty("type", "minecraft:select");
        selectModel.addProperty("property", "minecraft:component");
        selectModel.addProperty("component", "minecraft:painting/variant");
        selectModel.add("cases", cases(items));
        selectModel.add("fallback", fallbackModel());
        return selectModel;
    }

    private JsonArray cases(Collection<CustomStack> items) {
        JsonArray cases = new JsonArray();

        for (CustomStack item : items) {
            if (CreativeItemModelResolver.shouldSkip(item)) continue;
            cases.add(caseFor(item));
        }

        return cases;
    }

    private JsonObject caseFor(CustomStack item) {
        JsonObject caseObject = new JsonObject();
        caseObject.addProperty("when", variantKey(item));
        caseObject.add("model", itemModel(CreativeItemModelResolver.resolveModel(item)));
        return caseObject;
    }

    private JsonObject itemModel(String modelKey) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", modelKey);
        return model;
    }

    private JsonObject fallbackModel() {
        return itemModel("minecraft:item/painting");
    }

    /**
     * Must match the ResourceLocation used by every version-specific RegistryInjector.
     */
    private String variantKey(CustomStack item) {
        return "ia_creative:" + item.getNamespace() + "_" + item.getId();
    }
}
