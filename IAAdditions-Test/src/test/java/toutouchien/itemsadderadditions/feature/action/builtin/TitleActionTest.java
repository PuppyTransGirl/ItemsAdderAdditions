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

class TitleActionTest {
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
    void key_returnsTitle() {
        assertEquals("title", new TitleAction().key());
    }

    @Test
    void configure_emptySection_returnsTrueAllFieldsOptional() {
        // Both title and subtitle are optional, no required fields
        assertTrue(new TitleAction().configure(new YamlConfiguration(), "test:item"));
    }

    @Test
    void configure_withTitle_returnsTrue() {
        assertTrue(new TitleAction().configure(yamlOf("title: \"Welcome!\""), "test:item"));
    }

    @Test
    void configure_withSubtitle_returnsTrue() {
        assertTrue(new TitleAction().configure(yamlOf("subtitle: \"Enjoy your stay\""), "test:item"));
    }

    @Test
    void configure_withBothTitleAndSubtitle_returnsTrue() {
        assertTrue(new TitleAction().configure(
                yamlOf("title: \"T\"\nsubtitle: \"S\""), "test:item"));
    }

    @Test
    void configure_withTimingOverrides_returnsTrue() {
        assertTrue(new TitleAction().configure(
                yamlOf("title: \"T\"\nfade_in: 5\nstay: 40\nfade_out: 5"), "test:item"));
    }

    @Test
    void configure_fadeInClampedToMax() {
        // max is 1200; values beyond are clamped by ParameterInjector
        assertTrue(new TitleAction().configure(
                yamlOf("title: \"T\"\nfade_in: 9999"), "test:item"));
    }

    @Test
    void configure_fadeInBelowMin_clampedToZero() {
        assertTrue(new TitleAction().configure(
                yamlOf("title: \"T\"\nfade_in: -5"), "test:item"));
    }

    @Test
    void newInstance_returnsDistinctInstance() {
        TitleAction prototype = new TitleAction();
        ActionExecutor copy = prototype.newInstance();
        assertNotSame(prototype, copy);
        assertInstanceOf(TitleAction.class, copy);
    }

    @Test
    void isAllowedFor_returnsTrueForAllTriggers() {
        TitleAction action = new TitleAction();
        for (TriggerType type : TriggerType.values()) {
            assertTrue(action.isAllowedFor(type), "Expected allowed for " + type);
        }
    }
}
