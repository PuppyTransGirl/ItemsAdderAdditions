package toutouchien.itemsadderadditions.feature.component;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.common.version.VersionUtils;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import static org.junit.jupiter.api.Assertions.*;

class ComponentExecutorTest {
    @BeforeAll
    static void setup() {
        // VersionUtils.version() consults Bukkit, so a server has to be present.
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void keyComesFromComponentAnnotation() {
        assertEquals("probe", new ProbeComponent().key());
    }

    @Test
    void keyThrowsWhenAnnotationMissing() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new UnannotatedComponent().key());
        assertTrue(ex.getMessage().contains("Missing @Component"));
    }

    @Test
    void newInstanceReturnsFreshIsolatedCopy() {
        ProbeComponent prototype = new ProbeComponent();
        prototype.value = 99;

        ComponentExecutor copy = prototype.newInstance();

        assertNotSame(prototype, copy);
        assertSame(ProbeComponent.class, copy.getClass());
        // The fresh copy is built from the no-arg constructor, not cloned from the prototype.
        assertEquals(0, ((ProbeComponent) copy).value);
    }

    @Test
    void newInstancesAreIndependentOfEachOther() {
        ProbeComponent prototype = new ProbeComponent();

        ProbeComponent a = (ProbeComponent) prototype.newInstance();
        ProbeComponent b = (ProbeComponent) prototype.newInstance();
        a.value = 5;

        assertEquals(0, b.value);
    }

    @Test
    void newInstanceThrowsWhenNoArgConstructorMissing() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new NoNoArgComponent("seed").newInstance());
        assertTrue(ex.getMessage().contains("no-arg constructor"));
    }

    @Test
    void defaultConfigureInjectsParameterFields() {
        YamlConfiguration cfg = yamlOf("value: 7");
        ProbeComponent component = new ProbeComponent();

        assertTrue(component.configure(cfg, "test:item"));
        assertEquals(7, component.value);
    }

    @Test
    void defaultConfigureWithNonSectionPreservesDefaults() {
        ProbeComponent component = new ProbeComponent();

        // A raw scalar is not a ConfigurationSection -> injector runs with a null section.
        assertTrue(component.configure("raw-scalar", "test:item"));
        assertEquals(0, component.value);
    }

    @Test
    void minimumVersionDefaultsToNull() {
        assertNull(new ProbeComponent().minimumVersion());
    }

    @Test
    void componentWithoutMinimumVersionIsAlwaysSupported() {
        assertTrue(new ProbeComponent().isSupportedOnCurrentVersion());
    }

    @Test
    void componentRequiringFutureVersionIsNotSupported() {
        // UNKNOWN sits at Integer.MAX_VALUE, so no real server can satisfy it.
        assertFalse(new FutureComponent().isSupportedOnCurrentVersion());
    }

    @Test
    void componentRequiringOldVersionIsSupported() {
        assertTrue(new LegacyComponent().isSupportedOnCurrentVersion());
    }

    private static YamlConfiguration yamlOf(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    @Component(key = "probe")
    static final class ProbeComponent extends ComponentExecutor {
        @Parameter(key = "value", type = Integer.class)
        int value = 0;

        @Override
        public ItemStack apply(ItemStack itemStack, String namespacedID) {
            return itemStack;
        }
    }

    static final class UnannotatedComponent extends ComponentExecutor {
        @Override
        public ItemStack apply(ItemStack itemStack, String namespacedID) {
            return itemStack;
        }
    }

    @Component(key = "future")
    static final class FutureComponent extends ComponentExecutor {
        @Nullable
        @Override
        public VersionUtils minimumVersion() {
            return VersionUtils.UNKNOWN;
        }

        @Override
        public ItemStack apply(ItemStack itemStack, String namespacedID) {
            return itemStack;
        }
    }

    @Component(key = "legacy")
    static final class LegacyComponent extends ComponentExecutor {
        @Nullable
        @Override
        public VersionUtils minimumVersion() {
            return VersionUtils.v1_20_6;
        }

        @Override
        public ItemStack apply(ItemStack itemStack, String namespacedID) {
            return itemStack;
        }
    }

    @Component(key = "no_no_arg")
    static final class NoNoArgComponent extends ComponentExecutor {
        @SuppressWarnings("unused")
        NoNoArgComponent(String seed) {
        }

        @Override
        public ItemStack apply(ItemStack itemStack, String namespacedID) {
            return itemStack;
        }
    }
}
