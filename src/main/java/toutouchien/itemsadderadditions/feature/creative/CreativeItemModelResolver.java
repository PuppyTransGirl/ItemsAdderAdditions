package toutouchien.itemsadderadditions.feature.creative;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Resolves the client-side item model path used for custom creative-menu entries.
 */
@NullMarked
final class CreativeItemModelResolver {
    private CreativeItemModelResolver() {
    }

    static String resolveModel(CustomStack item) {
        FileConfiguration config = item.getConfig();
        String base = "items." + item.getId();
        String namespace = item.getNamespace();
        String id = item.getId();

        if (hasIcon(config, base)) {
            return namespace + ":item/ia_auto/" + id + "_icon";
        }

        String model = getDeclaredModel(config, base);
        if (model != null) {
            return normalizeModelPath(model, namespace);
        }

        return namespace + ":item/ia_auto/" + id;
    }

    static boolean shouldSkip(CustomStack item) {
        FileConfiguration config = item.getConfig();
        String base = "items." + item.getId();
        return config.getBoolean(base + ".template", false)
                || config.getBoolean(base + ".hide_from_inventory", false);
    }

    @Nullable
    private static String getDeclaredModel(FileConfiguration config, String base) {
        String ownModel = getOwnDeclaredModel(config, base);
        if (ownModel != null) return ownModel;

        // A concrete variant with generated graphics should use its own generated ia_auto model,
        // not its template's shared declared model.
        if (usesGeneratedModelOnSelf(config, base)) return null;

        String templateBase = getTemplateBase(config, base);
        return templateBase != null ? getOwnDeclaredModel(config, templateBase) : null;
    }

    @Nullable
    private static String getOwnDeclaredModel(FileConfiguration config, String base) {
        String graphicsModel = config.getString(base + ".graphics.model");
        if (graphicsModel != null && !graphicsModel.isBlank()) return graphicsModel;

        String resourceModel = config.getString(base + ".resource.model_path");
        boolean generate = config.getBoolean(base + ".resource.generate", true);
        if (resourceModel != null && !resourceModel.isBlank() && !generate) {
            return resourceModel;
        }

        return null;
    }

    private static boolean usesGeneratedModelOnSelf(FileConfiguration config, String base) {
        if (hasNonBlankString(config, base + ".graphics.parent")) return true;
        if (hasNonBlankString(config, base + ".graphics.texture")) return true;

        ConfigurationSection textures = config.getConfigurationSection(base + ".graphics.textures");
        if (textures != null && !textures.getKeys(false).isEmpty()) return true;

        String resourceModel = config.getString(base + ".resource.model_path");
        if (resourceModel != null
                && !resourceModel.isBlank()
                && config.getBoolean(base + ".resource.generate", true)) {
            return true;
        }

        return config.contains(base + ".resource.generate")
                && config.getBoolean(base + ".resource.generate", true);
    }

    @Nullable
    private static String getTemplateBase(FileConfiguration config, String base) {
        String templateId = config.getString(base + ".variant_of");
        return templateId == null || templateId.isBlank() ? null : "items." + templateId;
    }

    private static boolean hasIcon(FileConfiguration config, String base) {
        if (hasNonBlankString(config, base + ".graphics.icon")) return true;
        if (hasNonBlankString(config, base + ".resource.icon")) return true;

        String templateBase = getTemplateBase(config, base);
        return templateBase != null
                && (hasNonBlankString(config, templateBase + ".graphics.icon")
                || hasNonBlankString(config, templateBase + ".resource.icon"));
    }

    private static boolean hasNonBlankString(FileConfiguration config, String path) {
        String value = config.getString(path);
        return value != null && !value.isBlank();
    }

    private static String normalizeModelPath(String path, String namespace) {
        path = stripExtension(path);
        return path.contains(":") ? path : namespace + ":" + path;
    }

    private static String stripExtension(String path) {
        if (path.endsWith(".json")) return path.substring(0, path.length() - 5);
        if (path.endsWith(".png")) return path.substring(0, path.length() - 4);
        return path;
    }
}
