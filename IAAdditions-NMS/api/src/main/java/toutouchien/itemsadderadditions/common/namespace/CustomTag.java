package toutouchien.itemsadderadditions.common.namespace;

import org.jspecify.annotations.NullMarked;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@NullMarked
public record CustomTag(
        String id,
        CustomTagType type,
        List<String> values,
        Set<String> valueSet,
        String sourcePath
) {
    public CustomTag(String id, CustomTagType type, List<String> values, String sourcePath) {
        this(id, type, values, new LinkedHashSet<>(values), sourcePath);
    }

    public CustomTag {
        values = List.copyOf(values);
        valueSet = Set.copyOf(valueSet);
    }

    public boolean contains(String value) {
        return valueSet.contains(value);
    }
}
