package toutouchien.itemsadderadditions.integration.protection;

import net.momirealms.antigrieflib.AntiGriefLib;
import net.momirealms.antigrieflib.Flag;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class StorageProtectionChecksTest {
    @BeforeAll
    static void boot() {
        MockBukkit.mock();
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    @Test
    void deniesStorageWhenOpenContainerPermissionIsDenied() {
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        AntiGriefLib antiGriefLib = mock(AntiGriefLib.class);
        when(antiGriefLib.test(player, Flag.OPEN_CONTAINER, location)).thenReturn(false);

        assertFalse(StorageProtectionChecks.canOpenStorage(player, location, antiGriefLib));
        verify(antiGriefLib).test(player, Flag.OPEN_CONTAINER, location);
    }

    @Test
    void allowsStorageWhenOpenContainerPermissionIsAllowed() {
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        AntiGriefLib antiGriefLib = mock(AntiGriefLib.class);
        when(antiGriefLib.test(player, Flag.OPEN_CONTAINER, location)).thenReturn(true);

        assertTrue(StorageProtectionChecks.canOpenStorage(player, location, antiGriefLib));
        verify(antiGriefLib).test(player, Flag.OPEN_CONTAINER, location);
    }
}
