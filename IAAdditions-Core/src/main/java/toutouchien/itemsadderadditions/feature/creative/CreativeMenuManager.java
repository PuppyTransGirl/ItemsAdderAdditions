package toutouchien.itemsadderadditions.feature.creative;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.resourcepack.ResourcePackFiles;

import java.util.Collection;

/**
 * Populates the vanilla creative Decorations tab with ItemsAdder items.
 *
 * <p>The public manager coordinates setup/reload only. JSON construction, model
 * resolution and file writing live in focused helper classes so future changes to
 * IA model resolution do not touch ItemsAdder config mutation or lifecycle code.</p>
 */
@NullMarked
public final class CreativeMenuManager {
    private final CreativePaintingModelWriter paintingModelWriter = new CreativePaintingModelWriter();

    public void setup() {
        ResourcePackFiles.ensureItemsAdderMergeFolder("CreativeMenu");
        ResourcePackFiles.writeTransparentPixelPng("assets/iaadditions/textures/painting/placeholder.png", "CreativeMenu");
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

}
