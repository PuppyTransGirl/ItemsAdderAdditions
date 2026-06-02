package toutouchien.itemsadderadditions.integration.bridge;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class StatRequirementsBridgeTest {
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
        StatRequirementsBridge.clear(player.getUniqueId());
    }

    @Test
    void unknownPlayerIsNotBlocked() {
        assertFalse(StatRequirementsBridge.isBlocked(player, 123));
    }

    @Test
    void captureWithNonPlayerIsIgnored() {
        StatRequirementsBridge.capture(new FakeLoader(new FakeStack(true, "ns:x")), "not a player");
        assertFalse(StatRequirementsBridge.isBlocked(player, "ns:x".hashCode()));
    }

    @Test
    void captureWithNullLoaderIsIgnored() {
        StatRequirementsBridge.capture(null, player);
        assertFalse(StatRequirementsBridge.isBlocked(player, 0));
    }

    @Test
    void captureWithUnresolvableLoaderFailsOpen() {
        // a loader with no matching field name -> reflection fails -> fail-open
        StatRequirementsBridge.capture(new Object(), player);
        assertFalse(StatRequirementsBridge.isBlocked(player, 1));
    }

    @Test
    void captureStoresMetRequirements() {
        StatRequirementsBridge.capture(new FakeLoader(new FakeStack(true, "ns:met")), player);
        assertFalse(StatRequirementsBridge.isBlocked(player, "ns:met".hashCode()));
    }

    @Test
    void captureStoresBlockedRequirements() {
        StatRequirementsBridge.capture(new FakeLoader(new FakeStack(false, "ns:blocked")), player);
        assertTrue(StatRequirementsBridge.isBlocked(player, "ns:blocked".hashCode()));
    }

    @Test
    void clearRemovesState() {
        StatRequirementsBridge.capture(new FakeLoader(new FakeStack(false, "ns:c")), player);
        assertTrue(StatRequirementsBridge.isBlocked(player, "ns:c".hashCode()));

        StatRequirementsBridge.clear(player.getUniqueId());
        assertFalse(StatRequirementsBridge.isBlocked(player, "ns:c".hashCode()));
    }

    // Mirrors the obfuscated ItemsAdder ActionsLoader shape the bridge reflects on:
    // a field (one of yg/xW/xI/ya) holding an internal custom stack.
    @SuppressWarnings("unused")
    public static final class FakeLoader {
        private final Object yg;

        FakeLoader(Object internalCustomStack) {
            this.yg = internalCustomStack;
        }
    }

    @SuppressWarnings("unused")
    public static final class FakeStack {
        private final boolean met;
        private final String namespacedId;

        FakeStack(boolean met, String namespacedId) {
            this.met = met;
            this.namespacedId = namespacedId;
        }

        public boolean checkStatRequirements(Player player) {
            return met;
        }

        public String getNamespacedId() {
            return namespacedId;
        }
    }
}
