package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

import static org.junit.jupiter.api.Assertions.*;

class SwingHandActionTest {
    private static ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
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
    void key_returnsSwingHand() {
        assertEquals("swing_hand", new SwingHandAction().key());
    }

    @Test
    void configure_withMainHand_returnsTrue() {
        assertTrue(new SwingHandAction().configure(yamlOf("hand: HAND"), "test:item"));
    }

    @Test
    void configure_withOffHand_returnsTrue() {
        assertTrue(new SwingHandAction().configure(yamlOf("hand: OFF_HAND"), "test:item"));
    }

    @Test
    void configure_missingHand_returnsFalse() {
        assertFalse(new SwingHandAction().configure(new YamlConfiguration(), "test:item"));
    }

    @Test
    void configure_caseInsensitiveHand_returnsTrue() {
        assertTrue(new SwingHandAction().configure(yamlOf("hand: hand"), "test:item"));
        assertTrue(new SwingHandAction().configure(yamlOf("hand: off_hand"), "test:item"));
    }

    @Test
    void run_withMainHand_doesNotThrow() {
        PlayerMock player = server.addPlayer();
        SwingHandAction action = new SwingHandAction();
        action.configure(yamlOf("hand: HAND"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        assertDoesNotThrow(() -> action.run(ctx));
    }

    @Test
    void run_withOffHand_doesNotThrow() {
        PlayerMock player = server.addPlayer();
        SwingHandAction action = new SwingHandAction();
        action.configure(yamlOf("hand: OFF_HAND"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        assertDoesNotThrow(() -> action.run(ctx));
    }

    @Test
    void run_withInvalidHandValue_doesNotThrow() {
        PlayerMock player = server.addPlayer();
        SwingHandAction action = new SwingHandAction();
        // Force-configure with an invalid value by overriding via subclass is not possible,
        // but we can trick configure into accepting it and let execute handle the bad value.
        // ParameterInjector only checks presence, not enum validity at configure time.
        action.configure(yamlOf("hand: INVALID_SLOT"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        assertDoesNotThrow(() -> action.run(ctx));
    }

    @Test
    void newInstance_returnsDistinctSwingHandAction() {
        SwingHandAction prototype = new SwingHandAction();
        ActionExecutor copy = prototype.newInstance();

        assertNotSame(prototype, copy);
        assertInstanceOf(SwingHandAction.class, copy);
    }

    @Test
    void isAllowedForAnyTrigger_returnsTrueForAllTriggerTypes() {
        SwingHandAction action = new SwingHandAction();
        for (TriggerType type : TriggerType.values()) {
            assertTrue(action.isAllowedFor(type),
                    "Expected SwingHandAction to be allowed for " + type);
        }
    }
}
