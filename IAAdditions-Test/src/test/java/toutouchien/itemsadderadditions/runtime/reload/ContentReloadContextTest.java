package toutouchien.itemsadderadditions.runtime.reload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import toutouchien.itemsadderadditions.common.loading.ConfigFileRegistry;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContentReloadContextTest {
    @TempDir
    Path tempDir;

    @Test
    void recordStoresItemsAndRegistry() {
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir.toFile());
        ContentReloadContext context = new ContentReloadContext(List.of(), registry);

        assertSame(registry, context.registry());
        assertTrue(context.items().isEmpty());
    }

    @Test
    void equalContextsHaveSameHashCode() {
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir.toFile());
        ContentReloadContext first = new ContentReloadContext(List.of(), registry);
        ContentReloadContext second = new ContentReloadContext(List.of(), registry);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertTrue(first.toString().contains("ContentReloadContext"));
    }
}
