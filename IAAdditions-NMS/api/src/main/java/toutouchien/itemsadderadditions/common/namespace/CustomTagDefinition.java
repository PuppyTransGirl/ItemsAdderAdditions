package toutouchien.itemsadderadditions.common.namespace;

import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public record CustomTagDefinition(
        String namespace,
        String id,
        CustomTagType type,
        List<String> rawValues,
        String sourcePath
) {
    public CustomTagDefinition {
        rawValues = List.copyOf(rawValues);
    }
}
