package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    private static Entity entityAt(Location location) {
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getWorld()).thenReturn(location.getWorld());
        return entity;
    }

    private static Location run(TeleportAction action, Player player, Entity target) {
        action.run(ActionContext.create(player, TriggerType.ITEM_INTERACT).target(target).build());
        var location = org.mockito.ArgumentCaptor.forClass(Location.class);
        verify(target).teleportAsync(location.capture());
        return location.getValue();
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
        Entity target = entityAt(new Location(world, 1, 2, 3));

        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yamlOf("x: 100.0\ny: 64.0\nz: -50.0\ntarget: other"), "test:item"));

        Location loc = run(action, player, target);
        assertEquals(100.0, loc.getX(), 0.01);
        assertEquals(64.0, loc.getY(), 0.01);
        assertEquals(-50.0, loc.getZ(), 0.01);
    }

    @Test
    void run_tildePreservesAxis() {
        PlayerMock player = server.addPlayer();
        Entity target = entityAt(new Location(world, 12.5, 70, -4));
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yamlOf("x: '~'\ny: '~'\nz: '~'\ntarget: other"), "test:item"));

        Location location = run(action, player, target);

        assertEquals(12.5, location.getX());
        assertEquals(70, location.getY());
        assertEquals(-4, location.getZ());
    }

    @Test
    void run_appliesPositiveNegativeAndDecimalRelativeOffsets() {
        PlayerMock player = server.addPlayer();
        Entity target = entityAt(new Location(world, 10, 64, -3));
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yamlOf("x: '~10'\ny: '~-2.5'\nz: '~0.25'\ntarget: other"), "test:item"));

        Location location = run(action, player, target);

        assertEquals(20, location.getX());
        assertEquals(61.5, location.getY());
        assertEquals(-2.75, location.getZ());
    }

    @Test
    void run_mixesAbsoluteAndRelativeCoordinatesFromTeleportedEntity() {
        PlayerMock player = server.addPlayer();
        player.setLocation(new Location(world, 500, 500, 500));
        Entity target = entityAt(new Location(world, 10, 20, 30));
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yamlOf("x: 100\ny: '~5'\nz: '~-2'\ntarget: other"), "test:item"));

        Location location = run(action, player, target);

        assertEquals(100, location.getX());
        assertEquals(25, location.getY());
        assertEquals(28, location.getZ());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "nope", "NaN", "Infinity", "~NaN", "~Infinity", "~ 1", "~1foo"})
    void configure_rejectsInvalidCoordinateStrings(String value) {
        YamlConfiguration configuration = yamlOf("x: 1\ny: 2\nz: 3");
        configuration.set("x", value);

        assertFalse(new TeleportAction().configure(configuration, "test:item"));
    }

    @Test
    void configure_rejectsNonFiniteNumericCoordinates() {
        for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            YamlConfiguration configuration = yamlOf("x: 1\ny: 2\nz: 3");
            configuration.set("x", value);
            assertFalse(new TeleportAction().configure(configuration, "test:item"));
        }
    }

    @Test
    void configure_rejectsPartialCoordinates() {
        assertFalse(new TeleportAction().configure(yamlOf("x: 1\ny: 2"), "test:item"));
    }

    @Test
    void run_worldSpawnUsesConfiguredWorld() {
        WorldMock spawnWorld = server.addSimpleWorld("teleport_spawn");
        spawnWorld.setSpawnLocation(7, 80, -9);
        PlayerMock player = server.addPlayer();
        Entity target = entityAt(new Location(world, 1, 2, 3));
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yamlOf("destination: world_spawn\nworld: teleport_spawn\ntarget: other"), "test:item"));

        Location location = run(action, player, target);

        assertEquals(spawnWorld, location.getWorld());
        assertEquals(7, location.getX());
        assertEquals(80, location.getY());
        assertEquals(-9, location.getZ());
    }

    @Test
    void run_worldSpawnWithoutWorldUsesTeleportedEntityWorld() {
        WorldMock targetWorld = server.addSimpleWorld("target_spawn");
        targetWorld.setSpawnLocation(4, 65, 8);
        PlayerMock player = server.addPlayer();
        Entity target = entityAt(new Location(targetWorld, 20, 30, 40));
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yamlOf("destination: world_spawn\ntarget: other"), "test:item"));

        assertEquals(targetWorld.getSpawnLocation(), run(action, player, target));
    }

    @Test
    void run_respawnUsesPlayerRespawnWithoutMovingItToFallbackWorld() {
        WorldMock fallback = server.addSimpleWorld("teleport_fallback");
        Location respawn = new Location(world, 12, 75, -6, 35, 10);
        Player player = mock(Player.class);
        when(player.getRespawnLocation()).thenReturn(respawn);
        when(player.getWorld()).thenReturn(world);
        Entity target = entityAt(new Location(fallback, 1, 2, 3));
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yamlOf("destination: respawn\nworld: teleport_fallback\ntarget: other"), "test:item"));

        Location location = run(action, player, target);

        assertEquals(respawn, location);
    }

    @Test
    void run_missingRespawnFallsBackToConfiguredWorldSpawn() {
        WorldMock fallback = server.addSimpleWorld("respawn_fallback");
        fallback.setSpawnLocation(-8, 90, 14);
        Player player = mock(Player.class);
        when(player.getRespawnLocation()).thenReturn(null);
        when(player.getWorld()).thenReturn(world);
        Entity target = entityAt(new Location(world, 1, 2, 3));
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yamlOf("destination: respawn\nworld: respawn_fallback\ntarget: other"), "test:item"));

        Location location = run(action, player, target);

        assertEquals(fallback.getSpawnLocation(), location);
    }

    @Test
    void run_missingRespawnWithoutWorldUsesPlayerCurrentWorldSpawn() {
        WorldMock playerWorld = server.addSimpleWorld("player_spawn");
        playerWorld.setSpawnLocation(3, 72, -11);
        Player player = mock(Player.class);
        when(player.getRespawnLocation()).thenReturn(null);
        when(player.getWorld()).thenReturn(playerWorld);
        Entity target = entityAt(new Location(world, 1, 2, 3));
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yamlOf("destination: respawn\ntarget: other"), "test:item"));

        assertEquals(playerWorld.getSpawnLocation(), run(action, player, target));
    }

    @Test
    void run_explicitRotationOverridesNamedDestinationOrientation() {
        Location respawn = new Location(world, 12, 75, -6, 35, 10);
        Player player = mock(Player.class);
        when(player.getRespawnLocation()).thenReturn(respawn);
        Entity target = entityAt(new Location(world, 1, 2, 3));
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yamlOf("destination: respawn\nyaw: 90\npitch: -30\ntarget: other"), "test:item"));

        Location location = run(action, player, target);

        assertEquals(90, location.getYaw());
        assertEquals(-30, location.getPitch());
    }

    @Test
    void configure_rejectsNamedDestinationCombinedWithCoordinates() {
        assertFalse(new TeleportAction().configure(
                yamlOf("destination: world_spawn\nx: 1\ny: 2\nz: 3"), "test:item"));
    }

    @Test
    void configure_rejectsUnknownDestination() {
        assertFalse(new TeleportAction().configure(yamlOf("destination: moon"), "test:item"));
    }

    @Test
    void run_namedDestinationWithMissingWorldDoesNotTeleport() {
        PlayerMock player = server.addPlayer();
        Entity target = entityAt(new Location(world, 1, 2, 3));
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(
                yamlOf("destination: world_spawn\nworld: missing_world\ntarget: other"), "test:item"));

        action.run(ActionContext.create(player, TriggerType.ITEM_INTERACT).target(target).build());

        verify(target, never()).teleportAsync(any());
    }

    @Test
    void run_unknownWorld_playerStaysInPlace() {
        PlayerMock player = server.addPlayer();
        Entity target = entityAt(new Location(world, 5, 64, 5));
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yamlOf(
                "x: 0.0\ny: 64.0\nz: 0.0\nworld: nonexistent_world\ntarget: other"), "test:item"));

        action.run(ActionContext.create(player, TriggerType.ITEM_INTERACT).target(target).build());

        verify(target, never()).teleportAsync(any());
    }

    @Test
    void run_withExplicitYawAndPitch_appliesRotation() {
        PlayerMock player = server.addPlayer();
        Entity target = entityAt(new Location(world, 0, 64, 0));
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yamlOf(
                "x: 0.0\ny: 64.0\nz: 0.0\nyaw: 90.0\npitch: -30.0\ntarget: other"), "test:item"));

        Location location = run(action, player, target);

        assertEquals(90.0f, location.getYaw(), 0.01f);
        assertEquals(-30.0f, location.getPitch(), 0.01f);
    }

    @Test
    void run_withoutYaw_preservesPlayerYaw() {
        PlayerMock player = server.addPlayer();
        Entity target = entityAt(new Location(world, 0, 64, 0, 45f, 10f));
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yamlOf("x: 10.0\ny: 64.0\nz: 10.0\ntarget: other"), "test:item"));

        Location location = run(action, player, target);

        assertEquals(45f, location.getYaw(), 0.01f);
        assertEquals(10f, location.getPitch(), 0.01f);
    }

    @Test
    void run_withExplicitWorld_teleportsToThatWorld() {
        WorldMock secondWorld = server.addSimpleWorld("nether");
        PlayerMock player = server.addPlayer();
        Entity target = entityAt(new Location(world, 0, 64, 0));
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yamlOf(
                "x: 0.0\ny: 64.0\nz: 0.0\nworld: nether\ntarget: other"), "test:item"));

        assertEquals(secondWorld, run(action, player, target).getWorld());
    }
}
