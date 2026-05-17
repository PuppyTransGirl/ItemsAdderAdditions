package toutouchien.itemsadderadditions.common.loading;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * Every distinct kind of data that can appear in an ItemsAdder YAML file.
 *
 * <p>Each constant embeds the detection logic (a {@link Predicate}) that decides
 * whether a parsed {@link YamlConfiguration} belongs to that category. This means
 * adding support for a new system only requires a single new enum constant - the
 * rest of the pipeline ({@link ConfigFileRegistry}, loaders, {@code ItemsAdderLoadListener})
 * picks it up automatically.
 *
 * <h3>Detection contract</h3>
 * <ul>
 *   <li>A predicate must be <em>cheap</em>: it only checks key existence with
 *       {@link org.bukkit.configuration.Configuration#contains}, never parses values.</li>
 *   <li>A predicate may return {@code true} even if the section is empty or malformed -
 *       individual loaders perform full validation.</li>
 *   <li>A file may match <em>multiple</em> categories (e.g. one file can contain both
 *       stonecutter recipes and furniture populators).</li>
 * </ul>
 *
 * <h3>Adding a new category</h3>
 * <pre>{@code
 * MY_NEW_SECTION(yaml -> yaml.contains("my_new_section")),
 * }</pre>
 * Then create a loader that calls
 * {@code registry.getFiles(ConfigFileCategory.MY_NEW_SECTION)} and process the
 * pre-filtered list. No other change is required.
 */
@NullMarked
public enum ConfigFileCategory {
    CAMPFIRE_RECIPES(yaml -> {
        ConfigurationSection r = yaml.getConfigurationSection("recipes");
        return r != null && r.contains("campfire_cooking");
    }),

    STONECUTTER_RECIPES(yaml -> {
        ConfigurationSection r = yaml.getConfigurationSection("recipes");
        return r != null && r.contains("stonecutter");
    }),

    /**
     * Matches files that have either {@code iaa_crafting_table}
     * {@code iaa_crafting} sections under {@code recipes:}.
     */
    CRAFTING_RECIPES(yaml -> {
        ConfigurationSection r = yaml.getConfigurationSection("recipes");
        return r != null && (r.contains("iaa_crafting_table") || r.contains("iaa_crafting"));
    }),

    /**
     * Matches files with a {@code blocks_populators} section (or the legacy
     * {@code worlds_populators} alias).
     */
    FURNITURE_POPULATORS(yaml ->
            yaml.contains("blocks_populators") || yaml.contains("worlds_populators")),

    /**
     * Matches files with a {@code surface_decorators} section.
     */
    SURFACE_DECORATORS(yaml -> yaml.contains("surface_decorators")),

    /**
     * Matches files with a {@code paintings} section.
     */
    PAINTINGS(yaml -> yaml.contains("paintings")),

    // Add future categories here
    ;

    private final Predicate<YamlConfiguration> detector;

    ConfigFileCategory(Predicate<YamlConfiguration> detector) {
        this.detector = detector;
    }

    /**
     * Detects all categories that apply to {@code yaml} in a single pass over
     * the enum values.
     *
     * @param yaml the parsed YAML to inspect
     * @return an {@link EnumSet} (possibly empty) of matching categories
     */
    public static EnumSet<ConfigFileCategory> detect(YamlConfiguration yaml) {
        EnumSet<ConfigFileCategory> result = EnumSet.noneOf(ConfigFileCategory.class);
        for (ConfigFileCategory cat : values()) {
            if (cat.matches(yaml)) {
                result.add(cat);
            }
        }
        return result;
    }

    /**
     * Returns {@code true} if this category's section is present in {@code yaml}.
     *
     * @param yaml a fully-parsed {@link YamlConfiguration}; never {@code null}
     */
    public boolean matches(YamlConfiguration yaml) {
        return detector.test(yaml);
    }
}
