package toutouchien.itemsadderadditions.feature.creative;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Populates the vanilla creative Decorations tab with ItemsAdder items.
 *
 * <p>The public manager coordinates setup/reload only. JSON construction, model
 * resolution and file writing live in focused helper classes so future changes to
 * IA model resolution do not touch ItemsAdder config mutation or lifecycle code.</p>
 */
@NullMarked
public final class CreativeMenuManager {
    private static final String IA_MERGE_PATH = "ItemsAdderAdditions/resourcepack";
    private static final String MERGE_SETTING_ITEMSADDER =
            "resource-pack.zip.merge_other_plugins_resourcepacks_folders";

    private final CreativePaintingModelWriter paintingModelWriter = new CreativePaintingModelWriter();

    public void setup() {
        configureItemsAdder();
        CreativeResourcePackFiles.writeBlankPaintingTexture();
    }

    public void reload() {
        reload(ItemsAdder.getAllItems());
    }

    public void reload(Collection<CustomStack> items) {
        int count = paintingModelWriter.write(items);

        Log.success(
                "CreativeMenu",
                "Generated {} entries - run /iazip to apply resource pack changes.",
                count
        );
    }

    private void configureItemsAdder() {
        File iaConfig = new File(
                ItemsAdderAdditions.instance().getDataFolder().getParentFile(),
                "ItemsAdder/config.yml"
        );

        if (!iaConfig.exists()) {
            Log.warn(
                    "CreativeMenu",
                    "Could not locate ItemsAdder/config.yml - add '{}' to "
                            + "merge_other_plugins_resourcepacks_folders manually",
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
            Log.success(
                    "CreativeMenu",
                    "Registered '{}' in ItemsAdder's merge list.",
                    IA_MERGE_PATH
            );
        } catch (IOException e) {
            Log.error("CreativeMenu", "Failed to save ItemsAdder/config.yml", e);
        }
    }
}
