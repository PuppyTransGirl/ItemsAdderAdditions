package toutouchien.itemsadderadditions.common.loading;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;

import java.io.File;
import java.util.EnumSet;

/**
 * Immutable snapshot of a YAML file that has already been parsed and categorized.
 *
 * <p>Created exclusively by {@link ConfigFileRegistry#scan} - callers never
 * instantiate this class directly.
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>The {@link YamlConfiguration} is shared; no loader should mutate it.</li>
 *   <li>{@code categories} is a defensive copy so external code cannot alter the
 *       registry's internal state.</li>
 *   <li>The {@link File} reference is retained for error-message context only;
 *       loaders never re-read it from disk.</li>
 * </ul>
 */
@NullMarked
public final class CategorizedConfigFile {

    private final File file;
    private final YamlConfiguration yaml;
    private final EnumSet<ConfigFileCategory> categories;

    CategorizedConfigFile(File file, YamlConfiguration yaml, EnumSet<ConfigFileCategory> categories) {
        this.file = file;
        this.yaml = yaml;
        // Defensive copy: callers hold a view, not the registry's live set.
        this.categories = EnumSet.copyOf(categories);
    }

    /**
     * The physical file on disk. Used only for log / error messages; the YAML
     * content is always accessed via {@link #yaml()}.
     */
    public File file() {
        return file;
    }

    /**
     * The pre-parsed, fully-loaded YAML configuration. Shared across all systems
     * that receive this file - treat as read-only.
     */
    public YamlConfiguration yaml() {
        return yaml;
    }

    /**
     * The full set of {@link ConfigFileCategory}s detected in this file.
     * Returns an independent copy; mutating it has no effect on the registry.
     */
    public EnumSet<ConfigFileCategory> categories() {
        return EnumSet.copyOf(categories);
    }

    /**
     * Convenience check: returns {@code true} if this file was tagged with
     * {@code category}, allowing loaders to decide which sub-sections to process.
     *
     * <pre>{@code
     * if (ccf.hasCategory(ConfigFileCategory.CAMPFIRE_RECIPES)) {
     *     campfireHandler.load(namespace, yaml.getConfigurationSection("recipes.campfire_cooking"));
     * }
     * }</pre>
     */
    public boolean hasCategory(ConfigFileCategory category) {
        return categories.contains(category);
    }

    @Override
    public String toString() {
        return "CategorizedConfigFile{file=" + file.getPath() + ", categories=" + categories + "}";
    }
}
