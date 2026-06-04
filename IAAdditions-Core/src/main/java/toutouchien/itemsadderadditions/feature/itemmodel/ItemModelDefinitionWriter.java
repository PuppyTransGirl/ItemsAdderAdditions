package toutouchien.itemsadderadditions.feature.itemmodel;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.resourcepack.ResourcePackFiles;

import java.io.File;
import java.util.Collection;

@NullMarked
final class ItemModelDefinitionWriter {
    WriteResult write(Collection<ItemModelDefinitionData> definitions) {
        int changed = 0;
        int skippedExisting = 0;

        for (ItemModelDefinitionData definition : definitions) {
            File out = ResourcePackFiles.resourcePackFile(definition.definitionPath().resourcePackRelativePath());
            if (out.exists() && !definition.overwriteExistingFile()) {
                skippedExisting++;
                Log.itemWarn(ItemModelDefinitionManager.NAME, definition.namespacedItemId(),
                        "Skipped existing item model definition file '{}' because overwrite_existing_file is false.",
                        out.getPath());
                continue;
            }

            if (ResourcePackFiles.writeJson(ItemModelDefinitionManager.NAME, out, definition.json())) {
                changed++;
            }
        }

        return new WriteResult(definitions.size(), changed, skippedExisting);
    }

    record WriteResult(int definitions, int changedFiles, int skippedExistingFiles) {}
}
