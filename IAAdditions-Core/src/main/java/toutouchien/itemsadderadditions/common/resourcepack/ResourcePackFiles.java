package toutouchien.itemsadderadditions.common.resourcepack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import javax.imageio.ImageIO;

@NullMarked
public final class ResourcePackFiles {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String IA_MERGE_PATH = "ItemsAdderAdditions/resourcepack";
    private static final String MERGE_SETTING_ITEMSADDER =
            "resource-pack.zip.merge_other_plugins_resourcepacks_folders";

    private ResourcePackFiles() {
    }

    public static File resourcePackFile(String relativePath) {
        String cleanPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return new File(
                ItemsAdderAdditions.instance().getDataFolder(),
                "resourcepack/" + cleanPath
        );
    }

    public static void ensureItemsAdderMergeFolder(String subsystem) {
        File iaConfig = new File(
                ItemsAdderAdditions.instance().getDataFolder().getParentFile(),
                "ItemsAdder/config.yml"
        );

        if (!iaConfig.exists()) {
            Log.warn(
                    subsystem,
                    "Could not locate ItemsAdder/config.yml - add '{}' to merge_other_plugins_resourcepacks_folders manually",
                    IA_MERGE_PATH
            );
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(iaConfig);
        List<String> mergeFolders = config.getStringList(MERGE_SETTING_ITEMSADDER);

        if (mergeFolders.contains(IA_MERGE_PATH)) return;

        mergeFolders.add(IA_MERGE_PATH);
        config.set(MERGE_SETTING_ITEMSADDER, mergeFolders);

        try {
            config.save(iaConfig);
            Log.success(subsystem, "Registered '{}' in ItemsAdder's merge list.", IA_MERGE_PATH);
        } catch (IOException e) {
            Log.error(subsystem, "Failed to save ItemsAdder/config.yml", e);
        }
    }

    public static void writeTransparentPixelPng(String relativePath, String subsystem) {
        File out = resourcePackFile(relativePath);
        if (out.exists()) return;

        try {
            out.getParentFile().mkdirs();
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, 0x00000000);

            if (!ImageIO.write(image, "PNG", out)) {
                throw new IOException("No PNG writer available");
            }
        } catch (IOException e) {
            Log.error(subsystem, "Failed to write " + relativePath, e);
        }
    }

    public static boolean writeJson(String subsystem, File file, JsonObject json) {
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
            Log.error(subsystem, "Failed to write " + file.getPath(), e);
            return false;
        }
    }
}
