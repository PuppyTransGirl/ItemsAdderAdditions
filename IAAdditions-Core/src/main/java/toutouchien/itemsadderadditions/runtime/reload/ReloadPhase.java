package toutouchien.itemsadderadditions.runtime.reload;

import org.jspecify.annotations.NullMarked;

/**
 * Ordered phases of an ItemsAdder content reload.
 */
@NullMarked
public enum ReloadPhase {
    REGISTRY_PREPARE,
    ITEM_BINDINGS,
    CLIENT_REGISTRY,
    CONTENT_FILES,
    POST_CONTENT
}
