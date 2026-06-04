package toutouchien.itemsadderadditions.feature.itemmodel;

import com.google.gson.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.nms.api.component.ComponentValue;

@NullMarked
record ItemModelDefinitionData(
        String namespacedItemId,
        ItemModelDefinitionPath definitionPath,
        JsonObject json,
        boolean applyItemModelComponent,
        boolean overwriteExistingFile,
        @Nullable ComponentValue customModelData
) {}
