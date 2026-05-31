package toutouchien.itemsadderadditions.feature.component.parse;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import toutouchien.itemsadderadditions.nms.api.component.ComponentValue;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComponentTreeParserTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static YamlConfiguration yaml(String content) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    @Test
    void nullBecomesNullNode() {
        assertInstanceOf(ComponentValue.NullNode.class, ComponentTreeParser.parse(null));
    }

    @Test
    void booleanTrueBecomesTrue() {
        ComponentValue v = ComponentTreeParser.parse(true);
        assertInstanceOf(ComponentValue.BooleanNode.class, v);
        assertTrue(((ComponentValue.BooleanNode) v).value());
    }

    @Test
    void booleanFalseBecomesTrue() {
        ComponentValue v = ComponentTreeParser.parse(false);
        assertInstanceOf(ComponentValue.BooleanNode.class, v);
        assertFalse(((ComponentValue.BooleanNode) v).value());
    }

    @Test
    void integerBecomesIntNode() {
        ComponentValue v = ComponentTreeParser.parse(42);
        assertInstanceOf(ComponentValue.IntNode.class, v);
        assertEquals(42, ((ComponentValue.IntNode) v).value());
    }

    @Test
    void longBecomesLongNode() {
        ComponentValue v = ComponentTreeParser.parse(9999999999L);
        assertInstanceOf(ComponentValue.LongNode.class, v);
        assertEquals(9999999999L, ((ComponentValue.LongNode) v).value());
    }

    @Test
    void doubleBecomesDoubleNode() {
        ComponentValue v = ComponentTreeParser.parse(3.14);
        assertInstanceOf(ComponentValue.DoubleNode.class, v);
        assertEquals(3.14, ((ComponentValue.DoubleNode) v).value(), 1e-10);
    }

    @Test
    void floatBecomesDoubleNode() {
        ComponentValue v = ComponentTreeParser.parse(1.5f);
        assertInstanceOf(ComponentValue.DoubleNode.class, v);
        assertEquals(1.5, ((ComponentValue.DoubleNode) v).value(), 1e-5);
    }

    @Test
    void stringBecomesStringNode() {
        ComponentValue v = ComponentTreeParser.parse("hello");
        assertInstanceOf(ComponentValue.StringNode.class, v);
        assertEquals("hello", ((ComponentValue.StringNode) v).value());
    }

    @Test
    void listBecomesListNode() {
        ComponentValue v = ComponentTreeParser.parse(List.of("a", "b"));
        assertInstanceOf(ComponentValue.ListNode.class, v);
        assertEquals(2, ((ComponentValue.ListNode) v).values().size());
    }

    @Test
    void listElementsAreParsedRecursively() {
        ComponentValue v = ComponentTreeParser.parse(List.of(1, "two", true));
        ComponentValue.ListNode list = (ComponentValue.ListNode) v;
        assertInstanceOf(ComponentValue.IntNode.class, list.values().get(0));
        assertInstanceOf(ComponentValue.StringNode.class, list.values().get(1));
        assertInstanceOf(ComponentValue.BooleanNode.class, list.values().get(2));
    }

    @Test
    void nestedConfigSectionBecomesObjectNode() {
        YamlConfiguration cfg = yaml("parent:\n  child: 5\n  name: test");
        ComponentValue v = ComponentTreeParser.parse(cfg.getConfigurationSection("parent"));
        assertInstanceOf(ComponentValue.ObjectNode.class, v);
        ComponentValue.ObjectNode obj = (ComponentValue.ObjectNode) v;
        assertEquals(2, obj.entries().size());
        assertInstanceOf(ComponentValue.IntNode.class, obj.entries().get("child"));
        assertInstanceOf(ComponentValue.StringNode.class, obj.entries().get("name"));
    }

    @Test
    void deeplyNestedObjectNode() {
        YamlConfiguration cfg = yaml("root:\n  level1:\n    level2: 99");
        ComponentValue v = ComponentTreeParser.parse(cfg.getConfigurationSection("root"));
        ComponentValue.ObjectNode root = (ComponentValue.ObjectNode) v;
        ComponentValue level1 = root.entries().get("level1");
        assertInstanceOf(ComponentValue.ObjectNode.class, level1);
        ComponentValue level2 = ((ComponentValue.ObjectNode) level1).entries().get("level2");
        assertInstanceOf(ComponentValue.IntNode.class, level2);
        assertEquals(99, ((ComponentValue.IntNode) level2).value());
    }

    @Test
    void objectNodeEntriesAreImmutable() {
        YamlConfiguration cfg = yaml("x:\n  a: 1");
        ComponentValue.ObjectNode node = (ComponentValue.ObjectNode)
                ComponentTreeParser.parse(cfg.getConfigurationSection("x"));
        assertThrows(UnsupportedOperationException.class, () -> node.entries().put("z", new ComponentValue.NullNode()));
    }

    @Test
    void listNodeValuesAreImmutable() {
        ComponentValue.ListNode list = (ComponentValue.ListNode) ComponentTreeParser.parse(List.of(1, 2));
        assertThrows(UnsupportedOperationException.class, () -> list.values().add(new ComponentValue.NullNode()));
    }
}
