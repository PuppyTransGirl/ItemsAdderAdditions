package toutouchien.itemsadderadditions.feature.action.listener;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class ActionEventFiltersTest {
    private static ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static PlayerInteractEvent event(PlayerMock player, Action action) {
        return new PlayerInteractEvent(player, action, new ItemStack(Material.STONE), null, BlockFace.SELF, EquipmentSlot.HAND);
    }

    @Test
    void interactArgumentRightClickAir() {
        PlayerMock player = server.addPlayer();
        assertEquals("right", ActionEventFilters.interactArgument(event(player, Action.RIGHT_CLICK_AIR)));
    }

    @Test
    void interactArgumentRightClickShift() {
        PlayerMock player = server.addPlayer();
        player.setSneaking(true);
        assertEquals("right_shift", ActionEventFilters.interactArgument(event(player, Action.RIGHT_CLICK_BLOCK)));
    }

    @Test
    void interactArgumentLeftClickAir() {
        PlayerMock player = server.addPlayer();
        assertEquals("left", ActionEventFilters.interactArgument(event(player, Action.LEFT_CLICK_AIR)));
    }

    @Test
    void interactArgumentLeftClickShift() {
        PlayerMock player = server.addPlayer();
        player.setSneaking(true);
        assertEquals("left_shift", ActionEventFilters.interactArgument(event(player, Action.LEFT_CLICK_BLOCK)));
    }

    @Test
    void interactArgumentPhysicalReturnsNull() {
        PlayerMock player = server.addPlayer();
        assertNull(ActionEventFilters.interactArgument(event(player, Action.PHYSICAL)));
    }

    @Test
    void airInteractionsAreAllowed() {
        PlayerMock player = server.addPlayer();
        assertTrue(ActionEventFilters.isInteractAllowed(event(player, Action.LEFT_CLICK_AIR)));
        assertTrue(ActionEventFilters.isInteractAllowed(event(player, Action.RIGHT_CLICK_AIR)));
    }

    @Test
    void blockInteractionDeniedIsNotAllowed() {
        PlayerMock player = server.addPlayer();
        PlayerInteractEvent event = event(player, Action.RIGHT_CLICK_BLOCK);
        event.setUseInteractedBlock(Event.Result.DENY);

        assertFalse(ActionEventFilters.isInteractAllowed(event));
    }

    @Test
    void blockInteractionAllowedWhenNotDenied() {
        PlayerMock player = server.addPlayer();
        PlayerInteractEvent event = event(player, Action.RIGHT_CLICK_BLOCK);
        event.setUseInteractedBlock(Event.Result.ALLOW);

        assertTrue(ActionEventFilters.isInteractAllowed(event));
    }

    @Test
    void ignoreOffHandDuplicateMainHandNeverIgnored() {
        PlayerMock player = server.addPlayer();
        assertFalse(ActionEventFilters.ignoreOffHandDuplicate(player, EquipmentSlot.HAND));
    }

}
