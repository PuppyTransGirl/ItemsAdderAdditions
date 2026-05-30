package toutouchien.itemsadderadditions.feature.action;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActionExecutorTest {
    private static ServerMock server;
    private static PlayerMock player;

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

    @BeforeEach
    void reset() {
        player = server.addPlayer();
        CountingAction.executions = 0;
        CountingAction.runTargets.clear();
    }

    @Test
    void keyComesFromAnnotation() {
        assertEquals("count", new CountingAction().key());
    }

    @Test
    void missingAnnotationKeyThrows() {
        assertThrows(IllegalStateException.class, () -> new MissingAnnotationAction().key());
    }

    @Test
    void newInstanceCreatesFreshExecutor() {
        CountingAction original = new CountingAction();
        ActionExecutor copy = original.newInstance();

        assertInstanceOf(CountingAction.class, copy);
        assertNotSame(original, copy);
    }

    @Test
    void newInstanceWithInaccessibleConstructorThrows() {
        assertThrows(IllegalStateException.class, () -> new PrivateConstructorAction().newInstance());
    }

    @Test
    void configureInjectsSubclassAndSharedParameters() {
        CountingAction action = new CountingAction();
        YamlConfiguration cfg = yamlOf("value: 7\ntarget: all\ntarget_radius: 3.5\ntarget_in_sight_distance: 9\n");

        assertTrue(action.configure(cfg, "ns:item"));
        assertEquals(7, action.value);
        assertEquals("all", action.target);
        assertEquals(3.5, action.targetRadius, 0.0001);
        assertEquals(9, action.targetInSightDistance);
    }

    @Test
    void isAllowedForHonorsTriggerRestriction() {
        CountingAction action = new CountingAction();

        assertTrue(action.isAllowedFor(TriggerType.ITEM_INTERACT));
        assertFalse(action.isAllowedFor(TriggerType.ITEM_DROP));
    }

    @Test
    void runSelfExecutesOnceOnPlayer() {
        CountingAction action = new CountingAction();
        assertTrue(action.configure(yamlOf("value: 1\ntarget: self\n"), "ns:item"));

        action.run(ActionContext.create(player, TriggerType.ITEM_INTERACT).build());

        assertEquals(1, CountingAction.executions);
        assertEquals(CountingAction.runTargets, List.of(player));
    }

    @Test
    void runAllExecutesForPlayerAndTarget() {
        PlayerMock target = server.addPlayer();
        CountingAction action = new CountingAction();
        assertTrue(action.configure(yamlOf("value: 1\ntarget: all\n"), "ns:item"));

        action.run(ActionContext.create(player, TriggerType.ITEM_INTERACT).target(target).build());

        assertEquals(2, CountingAction.executions);
        assertTrue(CountingAction.runTargets.contains(player));
        assertTrue(CountingAction.runTargets.contains(target));
    }

    @Test
    void runOtherWithoutTargetDoesNothing() {
        CountingAction action = new CountingAction();
        assertTrue(action.configure(yamlOf("value: 1\ntarget: other\n"), "ns:item"));

        action.run(ActionContext.create(player, TriggerType.ITEM_INTERACT).build());

        assertEquals(0, CountingAction.executions);
    }

    @Test
    void runDeniedByPermissionDoesNotExecute() {
        player.setOp(false);
        CountingAction action = new CountingAction();
        assertTrue(action.configure(yamlOf("value: 1\npermission: iaa.denied\n"), "ns:item"));

        action.run(ActionContext.create(player, TriggerType.ITEM_INTERACT).build());

        assertEquals(0, CountingAction.executions);
    }

    @Test
    void runAllowedByPermissionExecutes() {
        player.addAttachment(MockBukkit.createMockPlugin(), "iaa.allowed", true);
        CountingAction action = new CountingAction();
        assertTrue(action.configure(yamlOf("value: 1\npermission: iaa.allowed\n"), "ns:item"));

        action.run(ActionContext.create(player, TriggerType.ITEM_INTERACT).build());

        assertEquals(1, CountingAction.executions);
    }

    @Action(key = "count", triggers = {TriggerType.ITEM_INTERACT})
    public static class CountingAction extends ActionExecutor {
        static final List<Entity> runTargets = new ArrayList<>();
        static int executions;
        @Parameter(key = "value", type = Integer.class, required = true, min = 1, max = 10)
        int value;

        @Override
        protected void execute(ActionContext context) {
            executions++;
            runTargets.add(context.runOn());
        }
    }

    @Action(key = "private_ctor")
    public static class PrivateConstructorAction extends ActionExecutor {
        private PrivateConstructorAction() {
        }

        @Override
        protected void execute(ActionContext context) {
        }
    }

    public static class MissingAnnotationAction extends ActionExecutor {
        @Override
        protected void execute(ActionContext context) {
        }
    }
}
