package toutouchien.itemsadderadditions.integration.bridge;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class CooldownBridgeTest {
    private static ServerMock server;
    private PlayerMock player;

    @BeforeAll
    static void boot() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        player = server.addPlayer();
        CooldownBridge.clear(player.getUniqueId());
    }

    @Test
    void unknownPlayerIsNotOnCooldown() {
        assertFalse(CooldownBridge.isOnCooldown(player, 123));
    }

    @Test
    void captureAllowedMeansNoCooldown() {
        CooldownBridge.capture(true, player, 42);
        assertFalse(CooldownBridge.isOnCooldown(player, 42));
    }

    @Test
    void captureBlockedMeansOnCooldown() {
        CooldownBridge.capture(false, player, 42);
        assertTrue(CooldownBridge.isOnCooldown(player, 42));
    }

    @Test
    void capturePassesResultThrough() {
        assertTrue(CooldownBridge.capture(true, player, 1));
        assertFalse(CooldownBridge.capture(false, player, 1));
    }

    @Test
    void captureWithNonLivingEntityIsIgnored() {
        assertTrue(CooldownBridge.capture(true, "not an entity", 7));
        assertFalse(CooldownBridge.isOnCooldown(player, 7));
    }

    @Test
    void knownPlayerUnknownHashIsNotOnCooldown() {
        CooldownBridge.capture(false, player, 100);
        assertFalse(CooldownBridge.isOnCooldown(player, 999));
    }

    @Test
    void clearRemovesState() {
        CooldownBridge.capture(false, player, 5);
        assertTrue(CooldownBridge.isOnCooldown(player, 5));

        CooldownBridge.clear(player.getUniqueId());
        assertFalse(CooldownBridge.isOnCooldown(player, 5));
    }
}
