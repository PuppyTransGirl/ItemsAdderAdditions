package toutouchien.itemsadderadditions.feature.worldgen;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import toutouchien.itemsadderadditions.common.loading.ConfigFileRegistry;
import toutouchien.itemsadderadditions.feature.worldgen.populator.FurniturePopulatorLoader;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorldgenReloadSystemTest {
    @TempDir
    Path contents;

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        FurniturePopulatorLoader.clear();
    }

    @AfterEach
    void tearDown() {
        FurniturePopulatorLoader.clear();
        MockBukkit.unmock();
    }

    private ContentReloadContext context() {
        ConfigFileRegistry registry = ConfigFileRegistry.scan(contents.toFile());
        return new ContentReloadContext(List.of(), registry);
    }

    @Test
    void nameAndPhaseAreStable() {
        WorldgenReloadSystem system = new WorldgenReloadSystem();
        assertEquals("Worldgen", system.name());
        assertEquals(ReloadPhase.POST_CONTENT, system.phase());
    }

    @Test
    void reloadWithNoFilesReturnsUnchanged() {
        ReloadStepResult result = new WorldgenReloadSystem().reload(context());
        assertFalse(result.registryChanged());
        assertTrue(FurniturePopulatorLoader.REGISTRY.isEmpty());
    }

    @Test
    void reloadParsesPopulatorFileButSkipsUnknownFurniture() throws IOException {
        Files.writeString(contents.resolve("pop.yml"), """
                blocks_populators:
                  my_pop:
                    enabled: true
                    furniture: "unknown_pack:nope"
                    worlds: [world]
                    replaceable_blocks: [STONE]
                    chunk_chance: 10.0
                """);

        ReloadStepResult result = new WorldgenReloadSystem().reload(context());

        // Unknown furniture is rejected (CustomStack registry has no such entry under MockBukkit).
        assertTrue(FurniturePopulatorLoader.REGISTRY.isEmpty());
        assertFalse(result.registryChanged());
    }

    @Test
    void reloadSkipsDisabledPopulatorEntry() throws IOException {
        Files.writeString(contents.resolve("disabled.yml"), """
                blocks_populators:
                  off_pop:
                    enabled: false
                    furniture: "pack:thing"
                """);

        assertDoesNotThrow(() -> new WorldgenReloadSystem().reload(context()));
        assertTrue(FurniturePopulatorLoader.REGISTRY.isEmpty());
    }

    @Test
    void reloadIgnoresEntryWithoutFurnitureId() throws IOException {
        Files.writeString(contents.resolve("nofur.yml"), """
                surface_decorators:
                  deco:
                    enabled: true
                    worlds: [world]
                """);

        assertDoesNotThrow(() -> new WorldgenReloadSystem().reload(context()));
    }
}
