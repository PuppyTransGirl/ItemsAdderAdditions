package toutouchien.itemsadderadditions.common.namespace;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.*;

@NullMarked
public final class CustomTagRegistry {
    private static final String LOG_TAG = "CustomTags";

    private final Map<CustomTagType, Map<String, CustomTag>> byType;
    private final int definitionCount;
    private final int invalidCount;

    private CustomTagRegistry(
            Map<CustomTagType, Map<String, CustomTag>> byType,
            int definitionCount,
            int invalidCount
    ) {
        EnumMap<CustomTagType, Map<String, CustomTag>> copy = new EnumMap<>(CustomTagType.class);
        for (CustomTagType type : CustomTagType.values()) {
            copy.put(type, Map.copyOf(byType.getOrDefault(type, Map.of())));
        }
        this.byType = Map.copyOf(copy);
        this.definitionCount = definitionCount;
        this.invalidCount = invalidCount;
    }

    public static CustomTagRegistry empty() {
        return new CustomTagRegistry(Map.of(), 0, 0);
    }

    public static CustomTagRegistry resolve(List<CustomTagDefinition> rawDefinitions) {
        LinkedHashMap<TagKey, NormalizedDefinition> definitions = new LinkedHashMap<>();
        int invalid = 0;

        for (CustomTagDefinition definition : rawDefinitions) {
            String id = NamespaceUtils.normalizeCustomTagId(definition.namespace(), definition.id());
            if (!NamespaceUtils.isValidNamespacedId(id)) {
                Log.warn(LOG_TAG,
                        "Invalid tag id '{}' resolved as '{}' in namespace '{}'. File: {}",
                        definition.id(), id, definition.namespace(), definition.sourcePath());
                invalid++;
                continue;
            }

            TagKey key = new TagKey(definition.type(), id);
            if (definitions.containsKey(key)) {
                Log.warn(LOG_TAG,
                        "Duplicate {} tag '#{}' in file '{}'; first declaration from '{}' will be used.",
                        definition.type(), id, definition.sourcePath(), definitions.get(key).sourcePath());
                invalid++;
                continue;
            }

            definitions.put(key, new NormalizedDefinition(
                    definition.namespace().toLowerCase(Locale.ROOT),
                    id,
                    definition.type(),
                    definition.rawValues(),
                    definition.sourcePath()
            ));
        }

        LinkedHashMap<TagKey, CustomTag> resolved = new LinkedHashMap<>();
        LinkedHashSet<TagKey> resolving = new LinkedHashSet<>();
        int[] invalidRefCount = {0};

        for (TagKey key : definitions.keySet()) {
            resolveKey(key, definitions, resolved, resolving, invalidRefCount);
        }

        EnumMap<CustomTagType, Map<String, CustomTag>> byType = new EnumMap<>(CustomTagType.class);
        for (CustomTagType type : CustomTagType.values()) {
            byType.put(type, new LinkedHashMap<>());
        }
        for (Map.Entry<TagKey, CustomTag> entry : resolved.entrySet()) {
            byType.get(entry.getKey().type()).put(entry.getKey().id(), entry.getValue());
        }

        CustomTagRegistry registry = new CustomTagRegistry(byType, rawDefinitions.size(), invalid + invalidRefCount[0]);
        Log.debug(LOG_TAG,
                "Resolved {} custom tag definition(s): loaded={}, invalid={}.",
                rawDefinitions.size(), registry.tagCount(), registry.invalidCount());
        for (CustomTag tag : registry.tags()) {
            Log.debug(LOG_TAG, "#{} ({}) -> {}", tag.id(), tag.type(), tag.values());
        }
        return registry;
    }

    private static List<String> resolveKey(
            TagKey key,
            Map<TagKey, NormalizedDefinition> definitions,
            Map<TagKey, CustomTag> resolved,
            LinkedHashSet<TagKey> resolving,
            int[] invalidRefCount
    ) {
        CustomTag existing = resolved.get(key);
        if (existing != null) return existing.values();

        NormalizedDefinition definition = definitions.get(key);
        if (definition == null) return List.of();

        if (resolving.contains(key)) {
            invalidRefCount[0]++;
            Log.warn(LOG_TAG,
                    "Circular custom tag reference detected while resolving '#{}': {} -> #{}. File: {}",
                    key.id(), formatPath(resolving), key.id(), definition.sourcePath());
            return List.of();
        }

        resolving.add(key);
        LinkedHashSet<String> values = new LinkedHashSet<>();

        for (String rawValue : definition.rawValues()) {
            String raw = rawValue.trim();
            if (raw.isBlank()) {
                invalidRefCount[0]++;
                Log.warn(LOG_TAG,
                        "Tag '#{}' contains a blank value. File: {}",
                        definition.id(), definition.sourcePath());
                continue;
            }

            if (NamespaceUtils.isTagReference(raw)) {
                String nestedId = NamespaceUtils.stripTagPrefix(
                        NamespaceUtils.normalizeCustomTagReference(definition.namespace(), raw, definition.type()));
                TagKey nestedKey = new TagKey(definition.type(), nestedId);
                NormalizedDefinition nested = definitions.get(nestedKey);
                if (nested == null) {
                    @Nullable CustomTagType otherType = findDefinedType(definitions, nestedId);
                    if (otherType != null) {
                        Log.warn(LOG_TAG,
                                "Tag '#{}' ({}) references '#{}' as {}, but it is declared as {}. File: {}",
                                definition.id(), definition.type(), nestedId, definition.type(), otherType, definition.sourcePath());
                    } else {
                        Log.warn(LOG_TAG,
                                "Tag '#{}' references unknown custom tag '#{}'. File: {}",
                                definition.id(), nestedId, definition.sourcePath());
                    }
                    invalidRefCount[0]++;
                    continue;
                }

                values.addAll(resolveKey(nestedKey, definitions, resolved, resolving, invalidRefCount));
                continue;
            }

            @Nullable String normalized = NamespaceUtils.normalizeContentIdForCustomTagValue(
                    definition.namespace(), raw, definition.type());
            if (normalized == null || normalized.isBlank()) {
                invalidRefCount[0]++;
                Log.warn(LOG_TAG,
                        "Tag '#{}' contains invalid {} value '{}'. File: {}",
                        definition.id(), definition.type(), raw, definition.sourcePath());
                continue;
            }

            values.add(normalized);
        }

        resolving.remove(key);
        CustomTag tag = new CustomTag(definition.id(), definition.type(), new ArrayList<>(values), definition.sourcePath());
        resolved.put(key, tag);
        return tag.values();
    }

    private static @Nullable CustomTagType findDefinedType(Map<TagKey, NormalizedDefinition> definitions, String id) {
        for (TagKey key : definitions.keySet()) {
            if (key.id().equals(id)) return key.type();
        }
        return null;
    }

    private static String formatPath(LinkedHashSet<TagKey> resolving) {
        List<String> ids = new ArrayList<>(resolving.size());
        for (TagKey key : resolving) {
            ids.add("#" + key.id());
        }
        return String.join(" -> ", ids);
    }

    public boolean hasTag(String id, CustomTagType type) {
        return tag(id, type) != null;
    }

    public @Nullable CustomTag tag(String id, CustomTagType type) {
        return byType.getOrDefault(type, Map.of()).get(id);
    }

    public List<String> values(String id, CustomTagType type) {
        CustomTag tag = tag(id, type);
        return tag == null ? List.of() : tag.values();
    }

    public boolean contains(String id, CustomTagType type, String value) {
        CustomTag tag = tag(id, type);
        return tag != null && tag.contains(value);
    }

    public List<CustomTag> tags() {
        List<CustomTag> result = new ArrayList<>();
        for (CustomTagType type : CustomTagType.values()) {
            result.addAll(byType.getOrDefault(type, Map.of()).values());
        }
        return List.copyOf(result);
    }

    public int tagCount() {
        int count = 0;
        for (Map<String, CustomTag> tags : byType.values()) {
            count += tags.size();
        }
        return count;
    }

    public int definitionCount() {
        return definitionCount;
    }

    public int invalidCount() {
        return invalidCount;
    }

    private record TagKey(CustomTagType type, String id) {}

    private record NormalizedDefinition(
            String namespace,
            String id,
            CustomTagType type,
            List<String> rawValues,
            String sourcePath
    ) {}
}
