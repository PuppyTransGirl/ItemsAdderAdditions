package toutouchien.itemsadderadditions.feature.component.model;

import org.jspecify.annotations.NullMarked;

import java.util.Locale;

/**
 * Normalized component key. Short keys ("custom_data") are expanded to "minecraft:custom_data".
 * Already-namespaced keys ("mymod:foo") are preserved as-is.
 */
@NullMarked
public record ComponentKey(String normalized) {
    public static ComponentKey from(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (!s.contains(":")) {
            s = "minecraft:" + s;
        }

        return new ComponentKey(s);
    }

    public boolean isValid() {
        int sep = normalized.indexOf(':');
        return sep > 0 && sep < normalized.length() - 1;
    }

    @Override
    public String toString() {
        return normalized;
    }
}
