package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

import static org.junit.jupiter.api.Assertions.*;

class MessageActionTest {
    private static ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
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

    @Test
    void key_returnsMessage() {
        assertEquals("message", new MessageAction().key());
    }

    @Test
    void configure_missingText_returnsFalse() {
        assertFalse(new MessageAction().configure(new YamlConfiguration(), "test:item"));
    }

    @Test
    void configure_withText_returnsTrue() {
        assertTrue(new MessageAction().configure(yamlOf("text: \"Hello!\""), "test:item"));
    }

    @Test
    void configure_withMiniMessageTags_returnsTrue() {
        assertTrue(new MessageAction().configure(yamlOf("text: \"<red>Hello!</red>\""), "test:item"));
    }

    @Test
    void newInstance_returnsDistinctInstance() {
        MessageAction prototype = new MessageAction();
        ActionExecutor copy = prototype.newInstance();
        assertNotSame(prototype, copy);
        assertInstanceOf(MessageAction.class, copy);
    }

    @Test
    void isAllowedFor_returnsTrueForAllTriggers() {
        MessageAction action = new MessageAction();
        for (TriggerType type : TriggerType.values()) {
            assertTrue(action.isAllowedFor(type), "Expected allowed for " + type);
        }
    }
}
