package toutouchien.itemsadderadditions.common.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorRegistryTest {
    private ExecutorRegistry<StubExecutor> registry;

    @BeforeEach
    void fresh() {
        registry = new ExecutorRegistry<>("Test");
    }

    @Test
    void getPrototypeReturnsNullWhenEmpty() {
        assertNull(registry.getPrototype("missing"));
    }

    @Test
    void externalRegistrationRetrieves() {
        StubExecutor e = new StubExecutor("foo");
        registry.register(e);
        assertSame(e, registry.getPrototype("foo"));
    }

    @Test
    void registerMultipleExternal() {
        StubExecutor a = new StubExecutor("a");
        StubExecutor b = new StubExecutor("b");
        registry.register(a, b);
        assertSame(a, registry.getPrototype("a"));
        assertSame(b, registry.getPrototype("b"));
    }

    @Test
    void getAllReturnsAllRegistered() {
        StubExecutor a = new StubExecutor("a");
        StubExecutor b = new StubExecutor("b");
        registry.register(a, b);
        Collection<StubExecutor> all = registry.getAll();
        assertEquals(2, all.size());
        assertTrue(all.contains(a));
        assertTrue(all.contains(b));
    }

    @Test
    void registerBuiltInsAllEnabled() {
        StubExecutor a = new StubExecutor("a");
        StubExecutor b = new StubExecutor("b");
        registry.registerBuiltIns(key -> true, a, b);
        assertSame(a, registry.getPrototype("a"));
        assertSame(b, registry.getPrototype("b"));
    }

    @Test
    void registerBuiltInsDisabledSkipped() {
        StubExecutor a = new StubExecutor("a");
        registry.registerBuiltIns(key -> false, a);
        assertNull(registry.getPrototype("a"));
    }

    @Test
    void registerBuiltInsSelectivelyEnabled() {
        StubExecutor a = new StubExecutor("a");
        StubExecutor b = new StubExecutor("b");
        registry.registerBuiltIns(key -> key.equals("a"), a, b);
        assertNotNull(registry.getPrototype("a"));
        assertNull(registry.getPrototype("b"));
    }

    @Test
    void externalPrototypeBlocksBuiltInSameKey() {
        StubExecutor external = new StubExecutor("shared");
        registry.register(external);

        StubExecutor builtIn = new StubExecutor("shared");
        registry.registerBuiltIns(key -> true, builtIn);

        // External takes priority: built-in must not override it
        assertSame(external, registry.getPrototype("shared"));
    }

    @Test
    void registerBuiltInsReplacesOldBuiltIns() {
        StubExecutor first = new StubExecutor("x");
        registry.registerBuiltIns(key -> true, first);
        assertSame(first, registry.getPrototype("x"));

        StubExecutor second = new StubExecutor("x");
        registry.registerBuiltIns(key -> true, second);
        assertSame(second, registry.getPrototype("x"));
    }

    @Test
    void getAllIsUnmodifiable() {
        registry.register(new StubExecutor("a"));
        assertThrows(UnsupportedOperationException.class,
                () -> registry.getAll().clear());
    }

    // Minimal Keyed implementation for testing
    private static class StubExecutor implements Keyed {
        private final String key;

        StubExecutor(String key) {
            this.key = key;
        }

        @Override
        public String key() {
            return key;
        }
    }
}
