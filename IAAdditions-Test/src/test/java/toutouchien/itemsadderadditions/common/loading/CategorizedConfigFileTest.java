package toutouchien.itemsadderadditions.common.loading;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class CategorizedConfigFileTest {
    private static CategorizedConfigFile make(EnumSet<ConfigFileCategory> cats) {
        return new CategorizedConfigFile(new File("test.yml"), new YamlConfiguration(), cats);
    }

    @Test
    void hasCategoryReturnsTrueWhenPresent() {
        CategorizedConfigFile ccf = make(EnumSet.of(ConfigFileCategory.PAINTINGS));
        assertTrue(ccf.hasCategory(ConfigFileCategory.PAINTINGS));
    }

    @Test
    void hasCategoryReturnsFalseWhenAbsent() {
        CategorizedConfigFile ccf = make(EnumSet.of(ConfigFileCategory.PAINTINGS));
        assertFalse(ccf.hasCategory(ConfigFileCategory.CAMPFIRE_RECIPES));
    }

    @Test
    void multipleCategories() {
        CategorizedConfigFile ccf = make(EnumSet.of(ConfigFileCategory.PAINTINGS, ConfigFileCategory.CAMPFIRE_RECIPES));
        assertTrue(ccf.hasCategory(ConfigFileCategory.PAINTINGS));
        assertTrue(ccf.hasCategory(ConfigFileCategory.CAMPFIRE_RECIPES));
        assertFalse(ccf.hasCategory(ConfigFileCategory.STONECUTTER_RECIPES));
    }

    @Test
    void categoriesReturnsCopyMutationDoesNotAffectInternal() {
        CategorizedConfigFile ccf = make(EnumSet.of(ConfigFileCategory.PAINTINGS));
        EnumSet<ConfigFileCategory> returned = ccf.categories();
        returned.add(ConfigFileCategory.CAMPFIRE_RECIPES);
        assertFalse(ccf.hasCategory(ConfigFileCategory.CAMPFIRE_RECIPES));
    }

    @Test
    void constructorDefensiveCopyOfInput() {
        EnumSet<ConfigFileCategory> original = EnumSet.of(ConfigFileCategory.PAINTINGS);
        CategorizedConfigFile ccf = make(original);
        original.add(ConfigFileCategory.CAMPFIRE_RECIPES);
        assertFalse(ccf.hasCategory(ConfigFileCategory.CAMPFIRE_RECIPES));
    }

    @Test
    void fileIsRetained() {
        File file = new File("my_config.yml");
        CategorizedConfigFile ccf = new CategorizedConfigFile(file, new YamlConfiguration(), EnumSet.of(ConfigFileCategory.PAINTINGS));
        assertSame(file, ccf.file());
    }

    @Test
    void yamlIsRetained() {
        YamlConfiguration yaml = new YamlConfiguration();
        CategorizedConfigFile ccf = new CategorizedConfigFile(new File("x.yml"), yaml, EnumSet.of(ConfigFileCategory.PAINTINGS));
        assertSame(yaml, ccf.yaml());
    }

    @Test
    void toStringContainsCategories() {
        CategorizedConfigFile ccf = make(EnumSet.of(ConfigFileCategory.PAINTINGS));
        String str = ccf.toString();
        assertTrue(str.contains("PAINTINGS"), "toString should mention the category");
    }

    @Test
    void toStringContainsFilePath() {
        CategorizedConfigFile ccf = new CategorizedConfigFile(new File("data/test.yml"), new YamlConfiguration(), EnumSet.of(ConfigFileCategory.PAINTINGS));
        assertTrue(ccf.toString().contains("test.yml"));
    }
}
