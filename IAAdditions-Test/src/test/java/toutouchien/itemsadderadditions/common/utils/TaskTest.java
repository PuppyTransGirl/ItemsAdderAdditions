package toutouchien.itemsadderadditions.common.utils;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {
    private static ServerMock server;
    private static WorldMock world;
    private static Plugin plugin;
    private PlayerMock player;

    @BeforeAll
    static void boot() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        plugin = MockBukkit.createMockPlugin("TaskTestPlugin");
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        player = server.addPlayer();
    }

    @Nested
    class NullArgumentsThrow {
        @Test
        void syncNullConsumer() {
            assertThrows(NullPointerException.class, () -> Task.sync(null, plugin));
        }

        @Test
        void syncNullPlugin() {
            assertThrows(NullPointerException.class, () -> Task.sync(t -> {}, null));
        }

        @Test
        void syncLaterNullTimeUnit() {
            assertThrows(NullPointerException.class, () -> Task.syncLater(t -> {}, plugin, 1, null));
        }

        @Test
        void asyncNullPlugin() {
            assertThrows(NullPointerException.class, () -> Task.async(t -> {}, null));
        }

        @Test
        void runLocationNullLocation() {
            assertThrows(NullPointerException.class, () -> Task.run(t -> {}, plugin, (Location) null));
        }

        @Test
        void runEntityNullEntity() {
            assertThrows(NullPointerException.class,
                    () -> Task.run(t -> {}, plugin, (org.bukkit.entity.Entity) null));
        }

        @Test
        void runDelayedLocationNullTimeUnit() {
            assertThrows(NullPointerException.class,
                    () -> Task.runDelayed(t -> {}, plugin, world.getSpawnLocation(), 1, null));
        }
    }

    @Nested
    class Scheduling {
        @Test
        void syncRunsImmediately() {
            boolean[] ran = {false};
            Task.sync(t -> ran[0] = true, plugin);
            server.getScheduler().performTicks(1);
            assertTrue(ran[0]);
        }

        @Test
        void syncLaterConvertsDelayToTicks() {
            boolean[] ran = {false};
            Task.syncLater(t -> ran[0] = true, plugin, 100, TimeUnit.MILLISECONDS);
            server.getScheduler().performTicks(5);
            assertTrue(ran[0]);
        }

        @Test
        void syncRepeatSchedules() {
            assertDoesNotThrow(() -> Task.syncRepeat(t -> {}, plugin, 50, 50, TimeUnit.MILLISECONDS));
        }

        @Test
        void asyncRunsWithoutThrowing() {
            assertDoesNotThrow(() -> Task.async(t -> {}, plugin));
        }

        @Test
        void asyncLaterSchedules() {
            assertDoesNotThrow(() -> Task.asyncLater(t -> {}, plugin, 1, TimeUnit.SECONDS));
        }

        @Test
        void asyncRepeatSchedules() {
            assertDoesNotThrow(() -> Task.asyncRepeat(t -> {}, plugin, 1, 1, TimeUnit.SECONDS));
        }

        @Test
        void runAtLocationSchedules() {
            assertDoesNotThrow(() -> Task.run(t -> {}, plugin, world.getSpawnLocation()));
        }

        @Test
        void runDelayedAtLocationSchedules() {
            assertDoesNotThrow(() -> Task.runDelayed(t -> {}, plugin, world.getSpawnLocation(), 100, TimeUnit.MILLISECONDS));
        }

        @Test
        void runRepeatAtLocationSchedules() {
            assertDoesNotThrow(() -> Task.runRepeat(t -> {}, plugin, world.getSpawnLocation(), 100, 100, TimeUnit.MILLISECONDS));
        }

        @Test
        void runAtEntitySchedules() {
            assertDoesNotThrow(() -> Task.run(t -> {}, plugin, player));
        }

        @Test
        void runDelayedAtEntitySchedules() {
            assertDoesNotThrow(() -> Task.runDelayed(t -> {}, plugin, player, 100, TimeUnit.MILLISECONDS));
        }

        @Test
        void runRepeatAtEntitySchedules() {
            assertDoesNotThrow(() -> Task.runRepeat(t -> {}, plugin, player, 100, 100, TimeUnit.MILLISECONDS));
        }
    }
}
