package toutouchien.itemsadderadditions.feature.behaviour.builtin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.feature.behaviour.BehaviourHost;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextDisplayBehaviourTest {
    private static YamlConfiguration yamlOf(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> specs(TextDisplayBehaviour behaviour) throws Exception {
        Field f = TextDisplayBehaviour.class.getDeclaredField("specs");
        f.setAccessible(true);
        return (List<Object>) f.get(behaviour);
    }

    private static Object runtime(TextDisplayBehaviour behaviour) throws Exception {
        Field f = TextDisplayBehaviour.class.getDeclaredField("runtime");
        f.setAccessible(true);
        return f.get(behaviour);
    }

    @Test
    void configureRejectsNonSection() {
        assertFalse(new TextDisplayBehaviour().configure("wrong", "ns:item"));
    }

    @Test
    void configureRejectsSectionWithoutValidDisplays() {
        assertFalse(new TextDisplayBehaviour().configure(yamlOf("other: value\n"), "ns:item"));
    }

    @Test
    void configureSingleDisplayStoresSpec() throws Exception {
        TextDisplayBehaviour behaviour = new TextDisplayBehaviour();

        assertTrue(behaviour.configure(yamlOf("text: '<yellow>Hello'\n"), "ns:block"));

        assertEquals(1, specs(behaviour).size());
    }

    @Test
    void configureMultiDisplayStoresAllValidSpecs() throws Exception {
        TextDisplayBehaviour behaviour = new TextDisplayBehaviour();
        YamlConfiguration cfg = yamlOf("""
                displays:
                  a:
                    text: A
                  b:
                    text: B
                  invalid:
                    text: "   "
                """);

        assertTrue(behaviour.configure(cfg, "ns:block"));

        assertEquals(2, specs(behaviour).size());
    }

    @Test
    void loadUnsupportedItemCategoryDoesNotCreateRuntime() throws Exception {
        TextDisplayBehaviour behaviour = new TextDisplayBehaviour();
        assertTrue(behaviour.configure(yamlOf("text: Hello\n"), "ns:item"));

        behaviour.load(new BehaviourHost("ns:item", ItemCategory.ITEM, null));

        assertNull(runtime(behaviour));
        assertSame(behaviour.host().category(), ItemCategory.ITEM);
    }

    @Test
    void unloadAfterUnsupportedLoadClearsHost() {
        TextDisplayBehaviour behaviour = new TextDisplayBehaviour();
        assertTrue(behaviour.configure(yamlOf("text: Hello\n"), "ns:item"));
        behaviour.load(new BehaviourHost("ns:item", ItemCategory.ITEM, null));

        behaviour.unload();

        assertNull(behaviour.host());
    }
}
