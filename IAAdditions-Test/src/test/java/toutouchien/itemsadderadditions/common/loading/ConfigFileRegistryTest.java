package toutouchien.itemsadderadditions.common.loading;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigFileRegistryTest {
    @TempDir
    File tempDir;

    private File writeYml(String name, String content) throws IOException {
        File f = new File(tempDir, name);
        Files.writeString(f.toPath(), content);
        return f;
    }

    @Test
    void scan_nonexistentDir_returnsEmpty() {
        File missing = new File(tempDir, "does_not_exist");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(missing);
        assertEquals(0, registry.totalFilesScanned());
        assertEquals(0, registry.totalFilesTagged());
        for (ConfigFileCategory cat : ConfigFileCategory.values()) {
            assertTrue(registry.getFiles(cat).isEmpty());
        }
    }

    @Test
    void scan_emptyDir_noFilesScanned() {
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        assertEquals(0, registry.totalFilesScanned());
        assertEquals(0, registry.totalFilesTagged());
    }

    @Test
    void scan_nonYmlFilesIgnored() throws IOException {
        writeYml("ignored.txt", "recipes:\n  campfire_cooking:\n    r: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        assertEquals(0, registry.totalFilesScanned());
    }

    @Test
    void scan_unrelatedYml_scannedButNotTagged() throws IOException {
        writeYml("item.yml", "items:\n  my_item:\n    display_name: Test");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        assertEquals(1, registry.totalFilesScanned());
        assertEquals(0, registry.totalFilesTagged());
    }

    @Test
    void scan_campfireRecipeFile_categorized() throws IOException {
        writeYml("recipes.yml", "recipes:\n  campfire_cooking:\n    my_recipe: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        assertEquals(1, registry.totalFilesScanned());
        assertEquals(1, registry.totalFilesTagged());
        assertEquals(1, registry.fileCount(ConfigFileCategory.CAMPFIRE_RECIPES));
        assertEquals("recipes.yml", registry.getFiles(ConfigFileCategory.CAMPFIRE_RECIPES).getFirst().file().getName());
    }

    @Test
    void scan_stonecutterRecipeFile_categorized() throws IOException {
        writeYml("stone.yml", "recipes:\n  stonecutter:\n    my_recipe: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        assertEquals(1, registry.fileCount(ConfigFileCategory.STONECUTTER_RECIPES));
        assertEquals(0, registry.fileCount(ConfigFileCategory.CAMPFIRE_RECIPES));
    }

    @Test
    void scan_paintingFile_categorized() throws IOException {
        writeYml("paint.yml", "paintings:\n  my_painting: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        assertEquals(1, registry.fileCount(ConfigFileCategory.PAINTINGS));
    }

    @Test
    void scan_advancementsFile_categorized() throws IOException {
        writeYml("adv.yml", "advancements:\n  my_adv: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        assertEquals(1, registry.fileCount(ConfigFileCategory.ADVANCEMENTS));
    }

    @Test
    void scan_furniturePopulatorFile_categorized() throws IOException {
        writeYml("pop.yml", "blocks_populators:\n  my_pop: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        assertEquals(1, registry.fileCount(ConfigFileCategory.FURNITURE_POPULATORS));
    }

    @Test
    void scan_legacyWorldsPopulatorsAlias_categorized() throws IOException {
        writeYml("worlds.yml", "worlds_populators:\n  pop: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        assertEquals(1, registry.fileCount(ConfigFileCategory.FURNITURE_POPULATORS));
    }

    @Test
    void scan_surfaceDecoratorFile_categorized() throws IOException {
        writeYml("surf.yml", "surface_decorators:\n  my_dec: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        assertEquals(1, registry.fileCount(ConfigFileCategory.SURFACE_DECORATORS));
    }

    @Test
    void scan_multiCategoryFile_appearsInBothBuckets() throws IOException {
        writeYml("multi.yml",
                "recipes:\n  campfire_cooking:\n    r: {}\n  stonecutter:\n    s: {}\npaintings:\n  p: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        assertEquals(1, registry.totalFilesScanned());
        assertEquals(1, registry.totalFilesTagged());
        assertEquals(1, registry.fileCount(ConfigFileCategory.CAMPFIRE_RECIPES));
        assertEquals(1, registry.fileCount(ConfigFileCategory.STONECUTTER_RECIPES));
        assertEquals(1, registry.fileCount(ConfigFileCategory.PAINTINGS));
    }

    @Test
    void scan_multipleFiles_eachCategorizedIndependently() throws IOException {
        writeYml("a.yml", "recipes:\n  campfire_cooking:\n    r: {}");
        writeYml("b.yml", "recipes:\n  campfire_cooking:\n    r2: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        assertEquals(2, registry.fileCount(ConfigFileCategory.CAMPFIRE_RECIPES));
    }

    @Test
    void scan_recursivelyFindsNestedFiles() throws IOException {
        File sub = new File(tempDir, "sub");
        assertTrue(sub.mkdirs());
        Files.writeString(new File(sub, "nested.yml").toPath(), "paintings:\n  p: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        assertEquals(1, registry.fileCount(ConfigFileCategory.PAINTINGS));
    }

    @Test
    void getFiles_noCategory_returnsEmpty() throws IOException {
        writeYml("a.yml", "paintings:\n  p: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        List<CategorizedConfigFile> result = registry.getFiles();
        assertTrue(result.isEmpty());
    }

    @Test
    void getFiles_multipleCategories_deduplicatesSharedFile() throws IOException {
        writeYml("shared.yml",
                "recipes:\n  campfire_cooking:\n    r: {}\n  stonecutter:\n    s: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        List<CategorizedConfigFile> result = registry.getFiles(
                ConfigFileCategory.CAMPFIRE_RECIPES, ConfigFileCategory.STONECUTTER_RECIPES);
        assertEquals(1, result.size());
    }

    @Test
    void getFiles_singleCategory_sameAsDirectGet() throws IOException {
        writeYml("c.yml", "recipes:\n  campfire_cooking:\n    r: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        assertEquals(
                registry.getFiles(ConfigFileCategory.CAMPFIRE_RECIPES),
                registry.getFiles(ConfigFileCategory.CAMPFIRE_RECIPES, ConfigFileCategory.STONECUTTER_RECIPES)
        );
    }

    @Test
    void fileCount_matchesGetFilesSize() throws IOException {
        writeYml("p1.yml", "paintings:\n  a: {}");
        writeYml("p2.yml", "paintings:\n  b: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        assertEquals(registry.getFiles(ConfigFileCategory.PAINTINGS).size(),
                registry.fileCount(ConfigFileCategory.PAINTINGS));
    }

    @Test
    void getFiles_returnedListIsImmutable() throws IOException {
        writeYml("a.yml", "paintings:\n  p: {}");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir);
        List<CategorizedConfigFile> list = registry.getFiles(ConfigFileCategory.PAINTINGS);
        assertThrows(UnsupportedOperationException.class, () -> list.add(null));
    }
}
