package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.bukkit.Location;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import toutouchien.itemsadderadditions.common.item.ItemCategory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TextDisplayOwnerTest {
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

    @Test
    void constructorClonesBaseLocation() {
        Location base = new Location(server.getWorld("world"), 1, 2, 3);
        TextDisplayOwner owner = new TextDisplayOwner(UUID.randomUUID(), "ns:block", ItemCategory.BLOCK, base, 90f);

        base.add(10, 10, 10);

        assertEquals(1.0, owner.baseLocation().getX(), 0.0001);
        assertEquals(2.0, owner.baseLocation().getY(), 0.0001);
        assertEquals(3.0, owner.baseLocation().getZ(), 0.0001);
    }

    @Test
    void blockOwnerIdIsStableForSameLocation() {
        Location first = new Location(server.getWorld("world"), 4, 5, 6);
        Location second = new Location(server.getWorld("world"), 4.9, 5.1, 6.2);

        assertEquals(TextDisplayOwner.blockOwnerId(first), TextDisplayOwner.blockOwnerId(second));
    }

    @Test
    void blockOwnerIdDiffersForDifferentCoordinates() {
        Location first = new Location(server.getWorld("world"), 4, 5, 6);
        Location second = new Location(server.getWorld("world"), 5, 5, 6);

        assertNotEquals(TextDisplayOwner.blockOwnerId(first), TextDisplayOwner.blockOwnerId(second));
    }

    @Test
    void blockOwnerIdRequiresWorld() {
        assertThrows(IllegalArgumentException.class,
                () -> TextDisplayOwner.blockOwnerId(new Location(null, 1, 2, 3)));
    }

    @Test
    void furnitureUsesEntityUuidLocationAndYaw() {
        PlayerMock entity = server.addPlayer();
        entity.teleport(new Location(server.getWorld("world"), 7, 8, 9, 135f, 0f));

        TextDisplayOwner owner = TextDisplayOwner.furniture("ns:chair", ItemCategory.FURNITURE, entity);

        assertEquals(entity.getUniqueId(), owner.ownerId());
        assertEquals("ns:chair", owner.namespacedId());
        assertEquals(ItemCategory.FURNITURE, owner.category());
        assertEquals(135f, owner.yaw(), 0.001f);
        assertEquals(7.0, owner.baseLocation().getX(), 0.0001);
    }
}
