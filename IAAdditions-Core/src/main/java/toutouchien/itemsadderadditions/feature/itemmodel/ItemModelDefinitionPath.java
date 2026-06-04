package toutouchien.itemsadderadditions.feature.itemmodel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.Optional;
import java.util.regex.Pattern;

@NullMarked
record ItemModelDefinitionPath(String namespace, String path) {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");

    static Optional<ItemModelDefinitionPath> parse(
            @Nullable String rawPath,
            String itemNamespace,
            String itemId,
            String namespacedId
    ) {
        String input = rawPath == null || rawPath.isBlank() ? itemId : rawPath.trim();
        input = stripJsonExtension(input);

        if (input.isBlank()) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, namespacedId, "item_model_definition.path is empty.");
            return Optional.empty();
        }

        if (input.startsWith("/") || input.startsWith("\\") || input.contains("\\") || input.contains("..")) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, namespacedId,
                    "item_model_definition.path '{}' is unsafe; absolute paths, backslashes and '..' are not allowed.",
                    rawPath);
            return Optional.empty();
        }

        String namespace = itemNamespace;
        String path = input;
        int colon = input.indexOf(':');
        if (colon >= 0) {
            if (colon == 0 || colon == input.length() - 1 || input.indexOf(':', colon + 1) >= 0) {
                Log.itemWarn(ItemModelDefinitionManager.NAME, namespacedId,
                        "item_model_definition.path '{}' is not a valid namespaced path.", rawPath);
                return Optional.empty();
            }
            namespace = input.substring(0, colon);
            path = input.substring(colon + 1);
        }

        if (!NAMESPACE.matcher(namespace).matches()) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, namespacedId,
                    "item_model_definition namespace '{}' is invalid. Use lowercase letters, digits, '_', '-' or '.'.",
                    namespace);
            return Optional.empty();
        }

        if (path.startsWith("/") || path.endsWith("/") || path.contains("//") || !PATH.matcher(path).matches()) {
            Log.itemWarn(ItemModelDefinitionManager.NAME, namespacedId,
                    "item_model_definition path '{}' is invalid. Use lowercase resource-pack path characters only.",
                    path);
            return Optional.empty();
        }

        return Optional.of(new ItemModelDefinitionPath(namespace, path));
    }

    String id() {
        return namespace + ":" + path;
    }

    String resourcePackRelativePath() {
        return "assets/" + namespace + "/items/" + path + ".json";
    }

    private static String stripJsonExtension(String input) {
        return input.endsWith(".json") ? input.substring(0, input.length() - 5) : input;
    }
}
