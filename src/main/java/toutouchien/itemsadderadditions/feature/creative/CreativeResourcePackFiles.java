package toutouchien.itemsadderadditions.feature.creative;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import javax.imageio.ImageIO;

@NullMarked
final class CreativeResourcePackFiles {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private CreativeResourcePackFiles() {
    }

    static File resourcePackFile(String relativePath) {
        return new File(
                ItemsAdderAdditions.instance().getDataFolder(),
                "resourcepack/" + relativePath
        );
    }

    static void writeBlankPaintingTexture() {
        File out = resourcePackFile("assets/iaadditions/textures/painting/placeholder.png");
        if (out.exists()) return;

        try {
            out.getParentFile().mkdirs();
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, 0x00000000);

            if (!ImageIO.write(image, "PNG", out)) {
                throw new IOException("No PNG writer available");
            }
        } catch (IOException e) {
            Log.error("CreativeMenu", "Failed to write placeholder painting texture", e);
        }
    }

    static boolean writeJson(File file, JsonObject json) {
        String content = GSON.toJson(json);
        if (file.exists()) {
            try {
                if (Files.readString(file.toPath()).equals(content)) return false;
            } catch (IOException ignored) {
                // Rewrite below if the existing file cannot be read.
            }
        }

        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
            }
            return true;
        } catch (IOException e) {
            Log.error("CreativeMenu", "Failed to write " + file.getPath(), e);
            return false;
        }
    }
}
