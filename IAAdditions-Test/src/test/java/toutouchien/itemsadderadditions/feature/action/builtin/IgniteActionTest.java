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

class IgniteActionTest {
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
    void key_returnsIgnite() {
        assertEquals("ignite", new IgniteAction().key());
    }

    @Test
    void configureMissingDuration_throwsBecausePrimitiveFieldCannotBeNulled() {
        // ParameterInjector tries to set null on the primitive int field before
        // returning false for required fields - this surfaces as IllegalArgumentException.
        assertThrows(IllegalArgumentException.class,
                () -> new IgniteAction().configure(new YamlConfiguration(), "test:item"));
    }

    @Test
    void configureWithValidDuration_returnsTrue() {
        assertTrue(new IgniteAction().configure(yamlOf("duration: 200"), "test:item"));
    }

    @Test
    void run_setsFireTicksOnPlayer() {
        PlayerMock player = server.addPlayer();
        IgniteAction action = new IgniteAction();
        action.configure(yamlOf("duration: 200"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        action.run(ctx);

        assertEquals(200, player.getFireTicks());
    }

    @Test
    void run_durationClampedToMin() {
        PlayerMock player = server.addPlayer();
        IgniteAction action = new IgniteAction();
        action.configure(yamlOf("duration: 0"), "test:item"); // min = 1

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        action.run(ctx);

        assertEquals(1, player.getFireTicks());
    }

    @Test
    void run_durationClampedToMax() {
        PlayerMock player = server.addPlayer();
        IgniteAction action = new IgniteAction();
        action.configure(yamlOf("duration: 999999"), "test:item"); // max = 72 000

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        action.run(ctx);

        assertEquals(72_000, player.getFireTicks());
    }

    @Test
    void run_targetSelf_fireOnlyAffectsPlayer() {
        PlayerMock player = server.addPlayer();
        PlayerMock bystander = server.addPlayer();
        IgniteAction action = new IgniteAction();
        action.configure(yamlOf("duration: 100\ntarget: self"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        action.run(ctx);

        assertEquals(100, player.getFireTicks());
        // MockBukkit initialises players with -20 fire ticks (not on fire); the action
        // must leave the bystander untouched, so their ticks stay ≤ 0.
        assertTrue(bystander.getFireTicks() <= 0);
    }

    @Test
    void newInstance_returnsDistinctIgniteAction() {
        IgniteAction prototype = new IgniteAction();
        ActionExecutor copy = prototype.newInstance();

        assertNotSame(prototype, copy);
        assertInstanceOf(IgniteAction.class, copy);
    }

    @Test
    void isAllowedForAnyTrigger_returnsTrueForAllTriggerTypes() {
        IgniteAction action = new IgniteAction();
        for (TriggerType type : TriggerType.values()) {
            assertTrue(action.isAllowedFor(type),
                    "Expected IgniteAction to be allowed for " + type);
        }
    }
}
