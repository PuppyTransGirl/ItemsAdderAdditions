package toutouchien.itemsadderadditions.common.item;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves ItemsAdder {@code variant_of} chains across normal items and templates.
 */
@NullMarked
public final class ItemVariantResolver {
    private static final int MAX_DEPTH = 16;

    private static volatile @Nullable Method templateCopyMethod;
    private static volatile boolean templateCopyMethodChecked;

    private ItemVariantResolver() {
        throw new IllegalStateException("Utility class");
    }

    public static List<ResolvedItemConfig> chainFromSelf(
            FileConfiguration config,
            String itemId,
            String namespacedId
    ) {
        List<ResolvedItemConfig> chain = new ArrayList<>();
        ResolvedItemConfig current = root(config, itemId, namespacedId);
        if (current == null) return chain;

        for (int depth = 0; depth < MAX_DEPTH; depth++) {
            chain.add(current);

            String variantOf = current.section().getString("variant_of");
            if (variantOf == null || variantOf.isBlank()) break;

            ResolvedItemConfig parent = resolveParent(current, variantOf);
            if (parent == null) break;
            current = parent;
        }

        return chain;
    }

    @Nullable
    private static ResolvedItemConfig root(FileConfiguration config, String itemId, String namespacedId) {
        ConfigurationSection section = config.getConfigurationSection("items." + itemId);
        if (section == null) return null;

        return new ResolvedItemConfig(
                section,
                config,
                itemId,
                namespace(namespacedId)
        );
    }

    @Nullable
    private static ResolvedItemConfig resolveParent(ResolvedItemConfig current, String variantOf) {
        VariantId id = VariantId.parse(current.namespace(), variantOf);

        ResolvedItemConfig sameFile = resolveSameFileParent(current, id);
        if (sameFile != null) return sameFile;

        ResolvedItemConfig registered = resolveRegisteredParent(current.namespace(), variantOf);
        if (registered != null) return registered;

        return resolveTemplateParent(id);
    }

    @Nullable
    private static ResolvedItemConfig resolveSameFileParent(ResolvedItemConfig current, VariantId id) {
        FileConfiguration config = current.fileConfig();
        if (config == null) return null;

        if (id.explicitNamespace() && !id.namespace().equals(current.namespace())) {
            return null;
        }

        ConfigurationSection section = config.getConfigurationSection("items." + id.itemId());
        if (section == null) return null;

        return new ResolvedItemConfig(section, config, id.itemId(), id.namespace());
    }

    @Nullable
    private static ResolvedItemConfig resolveRegisteredParent(String currentNamespace, String variantOf) {
        CustomStack stack = NamespaceUtils.customItemByID(currentNamespace, variantOf);
        if (stack == null) {
            stack = getCustomStack(currentNamespace, variantOf);
        }
        if (stack == null) return null;

        FileConfiguration config = stack.getConfig();
        ConfigurationSection section = config.getConfigurationSection("items." + stack.getId());
        if (section == null) return null;

        return new ResolvedItemConfig(section, config, stack.getId(), stack.getNamespace());
    }

    @Nullable
    private static CustomStack getCustomStack(String currentNamespace, String variantOf) {
        try {
            return CustomStack.getInstance(NamespaceUtils.normalizeID(currentNamespace, variantOf));
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    @Nullable
    private static ResolvedItemConfig resolveTemplateParent(VariantId id) {
        Method method = templateCopyMethod();
        if (method == null) return null;

        ConfigurationSection section;
        try {
            Object result = method.invoke(null, id.namespacedId());
            if (!(result instanceof ConfigurationSection configurationSection)) return null;
            section = directTemplateSection(configurationSection, id.itemId());
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException | LinkageError ignored) {
            return null;
        }

        return new ResolvedItemConfig(section, null, id.itemId(), id.namespace());
    }

    private static ConfigurationSection directTemplateSection(ConfigurationSection section, String itemId) {
        ConfigurationSection nested = section.getConfigurationSection("items." + itemId);
        if (nested != null) return nested;

        nested = section.getConfigurationSection(itemId);
        return nested == null ? section : nested;
    }

    @Nullable
    private static Method templateCopyMethod() {
        if (templateCopyMethodChecked) return templateCopyMethod;

        try {
            templateCopyMethod = CustomStack.class.getMethod("getConfigSectionOfTemplateCopy", String.class);
        } catch (NoSuchMethodException | LinkageError ignored) {
            templateCopyMethod = null;
        }
        templateCopyMethodChecked = true;
        return templateCopyMethod;
    }

    public record ResolvedItemConfig(
            ConfigurationSection section,
            @Nullable FileConfiguration fileConfig,
            String itemId,
            String namespace
    ) {
    }

    private record VariantId(String namespace, String itemId, boolean explicitNamespace) {
        private static VariantId parse(String currentNamespace, String raw) {
            String trimmed = raw.trim();
            int colon = trimmed.indexOf(':');
            if (colon >= 0) {
                return new VariantId(
                        trimmed.substring(0, colon).toLowerCase(java.util.Locale.ROOT),
                        trimmed.substring(colon + 1),
                        true
                );
            }
            return new VariantId(currentNamespace, trimmed, false);
        }

        private String namespacedId() {
            return namespace.isBlank() ? itemId : namespace + ":" + itemId;
        }
    }

    private static String namespace(String namespacedId) {
        int colon = namespacedId.indexOf(':');
        return colon < 0 ? "" : namespacedId.substring(0, colon);
    }
}
