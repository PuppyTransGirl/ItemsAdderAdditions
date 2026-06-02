package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.sound;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class StorageSoundPlayerTest {
    private static ServerMock server;
    private static WorldMock world;

    @BeforeAll
    static void boot() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    private static Sound sound() {
        return Sound.sound(Key.key("minecraft:block.chest.open"), Sound.Source.BLOCK, 1f, 1f);
    }

    @Test
    void nullSoundsAreNoOps() {
        StorageSoundPlayer player = new StorageSoundPlayer(null, null);
        Location loc = world.getBlockAt(0, 64, 0).getLocation();

        assertDoesNotThrow(() -> player.playOpen(loc));
        assertDoesNotThrow(() -> player.playClose(loc, true));
    }

    @Test
    void playCloseDisabledIsNoOpEvenWithSound() {
        StorageSoundPlayer player = new StorageSoundPlayer(sound(), sound());
        Location loc = world.getBlockAt(1, 64, 0).getLocation();

        assertDoesNotThrow(() -> player.playClose(loc, false));
    }

    @Test
    void playOpenWithSoundPlaysInWorld() {
        StorageSoundPlayer player = new StorageSoundPlayer(sound(), sound());
        Location loc = world.getBlockAt(2, 64, 0).getLocation();

        assertDoesNotThrow(() -> player.playOpen(loc));
    }

    @Test
    void playCloseEnabledWithSoundPlays() {
        StorageSoundPlayer player = new StorageSoundPlayer(sound(), sound());
        Location loc = world.getBlockAt(3, 64, 0).getLocation();

        assertDoesNotThrow(() -> player.playClose(loc, true));
    }

    @Test
    void worldlessLocationIsNoOp() {
        StorageSoundPlayer player = new StorageSoundPlayer(sound(), sound());
        Location loc = new Location(null, 0, 0, 0);

        assertDoesNotThrow(() -> player.playOpen(loc));
    }
}
