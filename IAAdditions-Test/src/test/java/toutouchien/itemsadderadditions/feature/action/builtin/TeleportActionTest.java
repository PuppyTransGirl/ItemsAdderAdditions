package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

import static org.junit.jupiter.api.Assertions.*;

class TeleportActionTest {
    private static ServerMock server;
    private static WorldMock world;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
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
    void key_returnsTeleport() {
        assertEquals("teleport", new TeleportAction().key());
    }

    @Test
    void configureMissingCoordinates_returnsFalse() {
        assertFalse(new TeleportAction().configure(new YamlConfiguration(), "test:item"));
    }

    @Test
    void configureWithCoordinates_returnsTrue() {
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yamlOf("x: 100.0\ny: 64.0\nz: -50.0"), "test:item"));
    }

    @Test
    void run_teleportsPlayerToCoordinates() {
        PlayerMock player = server.addPlayer();
        player.setLocation(new Location(world, 0, 64, 0));

        TeleportAction action = new TeleportAction();
        action.configure(yamlOf("x: 100.0\ny: 64.0\nz: -50.0"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        action.run(ctx);

        Location loc = player.getLocation();
        assertEquals(100.0, loc.getX(), 0.01);
        assertEquals(64.0, loc.getY(), 0.01);
        assertEquals(-50.0, loc.getZ(), 0.01);
    }

    @Test
    void run_unknownWorld_playerStaysInPlace() {
        PlayerMock player = server.addPlayer();
        player.setLocation(new Location(world, 5, 64, 5));

        TeleportAction action = new TeleportAction();
        action.configure(yamlOf("x: 0.0\ny: 64.0\nz: 0.0\nworld: nonexistent_world"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        action.run(ctx);

        assertEquals(5.0, player.getLocation().getX(), 0.01);
        assertEquals(5.0, player.getLocation().getZ(), 0.01);
    }

    @Test
    void run_withExplicitYawAndPitch_appliesRotation() {
        PlayerMock player = server.addPlayer();
        player.setLocation(new Location(world, 0, 64, 0));

        TeleportAction action = new TeleportAction();
        action.configure(yamlOf("x: 0.0\ny: 64.0\nz: 0.0\nyaw: 90.0\npitch: -30.0"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        action.run(ctx);

        assertEquals(90.0f, player.getLocation().getYaw(), 0.01f);
        assertEquals(-30.0f, player.getLocation().getPitch(), 0.01f);
    }

    @Test
    void run_withoutYaw_preservesPlayerYaw() {
        PlayerMock player = server.addPlayer();
        player.setLocation(new Location(world, 0, 64, 0, 45f, 10f));

        TeleportAction action = new TeleportAction();
        action.configure(yamlOf("x: 10.0\ny: 64.0\nz: 10.0"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        action.run(ctx);

        // yaw/pitch not specified -> inherits from player's current location
        assertEquals(45f, player.getLocation().getYaw(), 0.01f);
        assertEquals(10f, player.getLocation().getPitch(), 0.01f);
    }

    @Test
    void run_withExplicitWorld_teleportsToThatWorld() {
        WorldMock secondWorld = server.addSimpleWorld("nether");
        PlayerMock player = server.addPlayer();
        player.setLocation(new Location(world, 0, 64, 0));

        TeleportAction action = new TeleportAction();
        action.configure(yamlOf("x: 0.0\ny: 64.0\nz: 0.0\nworld: nether"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        action.run(ctx);

        assertEquals(secondWorld, player.getWorld());
    }
}
