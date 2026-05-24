package toutouchien.itemsadderadditions.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilsTest {
    @TempDir
    File tempDir;

    @Test
    void emptyDirectoryReturnsEmptyList() {
        List<File> files = FileUtils.collectYamlFiles(tempDir);
        assertTrue(files.isEmpty());
    }

    @Test
    void nonYamlFilesIgnored(@TempDir File dir) throws IOException {
        new File(dir, "readme.txt").createNewFile();
        new File(dir, "data.json").createNewFile();
        List<File> files = FileUtils.collectYamlFiles(dir);
        assertTrue(files.isEmpty());
    }

    @Test
    void yamlFilesFound(@TempDir File dir) throws IOException {
        new File(dir, "config.yml").createNewFile();
        new File(dir, "items.yml").createNewFile();
        List<File> files = FileUtils.collectYamlFiles(dir);
        assertEquals(2, files.size());
    }

    @Test
    void yamlFilesFoundRecursively(@TempDir File dir) throws IOException {
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        new File(dir, "root.yml").createNewFile();
        new File(subDir, "nested.yml").createNewFile();
        List<File> files = FileUtils.collectYamlFiles(dir);
        assertEquals(2, files.size());
    }

    @Test
    void mixedFilesOnlyYamlReturned(@TempDir File dir) throws IOException {
        new File(dir, "config.yml").createNewFile();
        new File(dir, "readme.txt").createNewFile();
        new File(dir, "data.yaml").createNewFile(); // .yaml not .yml - should not match
        List<File> files = FileUtils.collectYamlFiles(dir);
        assertEquals(1, files.size());
        assertTrue(files.getFirst().getName().endsWith(".yml"));
    }

    @Test
    void nonExistentDirectoryReturnsEmptyList() {
        File nonExistent = new File(tempDir, "does_not_exist");
        List<File> files = FileUtils.collectYamlFiles(nonExistent);
        assertTrue(files.isEmpty());
    }
}
