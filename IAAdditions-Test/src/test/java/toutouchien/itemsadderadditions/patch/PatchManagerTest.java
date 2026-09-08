package toutouchien.itemsadderadditions.patch;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PatchManagerTest {

    @Test
    void noCompatiblePatchesReturnsEarly() {
        // IA 3.0.0 matches none of the 4.0.x patches: filterPatches drops everything and
        // applyAll returns before touching the instrumentation agent.
        assertDoesNotThrow(() -> PatchManager.applyAll(Version.of("1.21.1", "3.0.0")));
    }

    @Test
    void compatibleVersionAttachesAgentAndDefersUnloadedTargets() {
        // IA 4.0.18 selects the 4.0.18 patch set. Target IA classes are not loaded in the test
        // JVM, so every patch is deferred. Exercises filterPatches, agent attach, transformer
        // registration, and the deferred-class reporting path without modifying real classes.
        assertDoesNotThrow(() -> PatchManager.applyAll(Version.of("1.21.1", "4.0.18")));
    }

    @Test
    void registeredPatchesDoNotBypassItemsAdderCraftingRecipes() throws ReflectiveOperationException {
        Field field = PatchManager.class.getDeclaredField("ALL_PATCHES");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<ClassPatch> patches = (List<ClassPatch>) field.get(null);

        assertFalse(patches.stream()
                .anyMatch(patch -> patch.getClass().getSimpleName().startsWith("CraftingRecipeBypassPatch")));
    }
}
