package toutouchien.itemsadderadditions.feature.creative;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CreativeResourcePackFilesTest {
    @TempDir
    Path tempDir;

    @Test
    void writeJsonCreatesParentDirectoriesAndFile() throws Exception {
        Path target = tempDir.resolve("nested/model.json");
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:model");

        assertTrue(CreativeResourcePackFiles.writeJson(target.toFile(), json));

        assertTrue(Files.exists(target));
        assertTrue(Files.readString(target).contains("minecraft:model"));
    }

    @Test
    void writeJsonReturnsFalseWhenContentUnchanged() throws Exception {
        Path target = tempDir.resolve("same.json");
        JsonObject json = new JsonObject();
        json.addProperty("value", 1);

        assertTrue(CreativeResourcePackFiles.writeJson(target.toFile(), json));
        String first = Files.readString(target);

        assertFalse(CreativeResourcePackFiles.writeJson(target.toFile(), json));
        assertEquals(first, Files.readString(target));
    }

    @Test
    void writeJsonRewritesChangedContent() throws Exception {
        Path target = tempDir.resolve("changed.json");
        JsonObject first = new JsonObject();
        first.addProperty("value", 1);
        JsonObject second = new JsonObject();
        second.addProperty("value", 2);

        assertTrue(CreativeResourcePackFiles.writeJson(target.toFile(), first));
        assertTrue(CreativeResourcePackFiles.writeJson(target.toFile(), second));

        assertTrue(Files.readString(target).contains("2"));
    }

    @Test
    void writeJsonReturnsFalseForUnwritableParentPath() throws Exception {
        Path parentAsFile = tempDir.resolve("not_a_directory");
        Files.writeString(parentAsFile, "x");
        JsonObject json = new JsonObject();
        json.addProperty("value", 1);

        assertFalse(CreativeResourcePackFiles.writeJson(parentAsFile.resolve("child.json").toFile(), json));
    }
}
