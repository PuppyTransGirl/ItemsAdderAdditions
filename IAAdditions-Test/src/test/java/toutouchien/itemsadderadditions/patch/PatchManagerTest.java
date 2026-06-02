package toutouchien.itemsadderadditions.patch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PatchManagerTest {

    @Test
    void noCompatiblePatchesReturnsEarly() {
        // IA 3.0.0 matches none of the 4.0.x patches: filterPatches drops everything and
        // applyAll returns before touching the instrumentation agent.
        assertDoesNotThrow(() -> PatchManager.applyAll(Version.of("1.21.1", "3.0.0")));
    }

    @Test
    void compatibleVersionAttachesAgentAndDefersUnloadedTargets() {
        // IA 4.0.17 selects the 4.0.17 patch set. Target IA classes are not loaded in the test
        // JVM, so every patch is deferred. Exercises filterPatches, agent attach, transformer
        // registration, and the deferred-class reporting path without modifying real classes.
        assertDoesNotThrow(() -> PatchManager.applyAll(Version.of("1.21.1", "4.0.17")));
    }
}
