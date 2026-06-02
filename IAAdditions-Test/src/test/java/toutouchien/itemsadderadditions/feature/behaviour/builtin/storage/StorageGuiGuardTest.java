package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.mockito.MockedStatic;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory.StorageInventoryHolder;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class StorageGuiGuardTest {
    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;
    private Inventory top;
    private InventoryView view;
    private StorageGuiGuard guard;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
        StorageInventoryHolder holder = new StorageInventoryHolder(new Location(world, 0, 64, 0));
        top = mock(Inventory.class);
        holder.inventory(top);
        when(top.getHolder(false)).thenReturn(holder);
        when(top.getSize()).thenReturn(9);
        view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(top);
        when(view.getBottomInventory()).thenReturn(player.getInventory());
        when(view.getPlayer()).thenReturn(player);
        guard = new StorageGuiGuard(Set.of("test:shulker"));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static CustomStack customStack(String id) {
        CustomStack stack = mock(CustomStack.class);
        when(stack.getNamespacedID()).thenReturn(id);
        return stack;
    }

    private InventoryClickEvent click(int rawSlot, ClickType click, InventoryAction action) {
        return new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, rawSlot, click, action);
    }

    @Test
    void placeIntoStorageTopInventoryCancelsForShulkerCursor() {
        ItemStack cursor = ItemStack.of(Material.CHEST);
        when(view.getCursor()).thenReturn(cursor);
        CustomStack shulker = customStack("test:shulker");

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(cursor)).thenReturn(shulker);
            InventoryClickEvent event = click(0, ClickType.LEFT, InventoryAction.PLACE_ALL);

            guard.onInventoryClick(event);

            assertTrue(event.isCancelled());
        }
    }

    @Test
    void placeIntoPlayerInventoryDoesNotCancelForShulkerCursor() {
        ItemStack cursor = ItemStack.of(Material.CHEST);
        when(view.getCursor()).thenReturn(cursor);
        CustomStack shulker = customStack("test:shulker");

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(cursor)).thenReturn(shulker);
            InventoryClickEvent event = click(12, ClickType.LEFT, InventoryAction.PLACE_ALL);

            guard.onInventoryClick(event);

            assertFalse(event.isCancelled());
        }
    }

    @Test
    void shiftMoveFromPlayerInventoryCancelsForShulkerCurrentItem() {
        ItemStack current = ItemStack.of(Material.CHEST);
        when(view.getItem(12)).thenReturn(current);
        CustomStack shulker = customStack("test:shulker");

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(current)).thenReturn(shulker);
            InventoryClickEvent event = click(12, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);

            guard.onInventoryClick(event);

            assertTrue(event.isCancelled());
        }
    }

    @Test
    void shiftMoveFromStorageTopInventoryDoesNotCancel() {
        ItemStack current = ItemStack.of(Material.CHEST);
        when(view.getItem(0)).thenReturn(current);
        CustomStack shulker = customStack("test:shulker");

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(current)).thenReturn(shulker);
            InventoryClickEvent event = click(0, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);

            guard.onInventoryClick(event);

            assertFalse(event.isCancelled());
        }
    }

    @Test
    void hotbarSwapIntoStorageCancelsWhenHotbarItemIsShulker() {
        ItemStack hotbar = ItemStack.of(Material.CHEST);
        player.getInventory().setItem(2, hotbar);
        CustomStack shulker = customStack("test:shulker");

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(hotbar)).thenReturn(shulker);
            InventoryClickEvent event = new InventoryClickEvent(
                    view,
                    InventoryType.SlotType.CONTAINER,
                    0,
                    ClickType.NUMBER_KEY,
                    InventoryAction.HOTBAR_SWAP,
                    2
            );

            guard.onInventoryClick(event);

            assertTrue(event.isCancelled());
        }
    }

    @Test
    void collectToCursorCancelsForShulkerCursorRegardlessOfSlot() {
        ItemStack cursor = ItemStack.of(Material.CHEST);
        when(view.getCursor()).thenReturn(cursor);
        CustomStack shulker = customStack("test:shulker");

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(cursor)).thenReturn(shulker);
            InventoryClickEvent event = click(12, ClickType.DOUBLE_CLICK, InventoryAction.COLLECT_TO_CURSOR);

            guard.onInventoryClick(event);

            assertTrue(event.isCancelled());
        }
    }

    @Test
    void dragIntoStorageTopInventoryCancelsForShulkerOldCursor() {
        ItemStack oldCursor = ItemStack.of(Material.CHEST);
        CustomStack shulker = customStack("test:shulker");

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(oldCursor)).thenReturn(shulker);
            InventoryDragEvent event = new InventoryDragEvent(
                    view,
                    ItemStack.of(Material.AIR),
                    oldCursor,
                    false,
                    Map.of(0, ItemStack.of(Material.CHEST), 12, ItemStack.of(Material.CHEST))
            );

            guard.onInventoryDrag(event);

            assertTrue(event.isCancelled());
        }
    }

    @Test
    void dragOnlyInPlayerInventoryDoesNotCancel() {
        ItemStack oldCursor = ItemStack.of(Material.CHEST);
        CustomStack shulker = customStack("test:shulker");

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(oldCursor)).thenReturn(shulker);
            InventoryDragEvent event = new InventoryDragEvent(
                    view,
                    ItemStack.of(Material.AIR),
                    oldCursor,
                    false,
                    Map.of(12, ItemStack.of(Material.CHEST), 13, ItemStack.of(Material.CHEST))
            );

            guard.onInventoryDrag(event);

            assertFalse(event.isCancelled());
        }
    }

    @Test
    void nonStorageInventoryHolderIsIgnored() {
        Inventory ordinaryTop = mock(Inventory.class);
        when(ordinaryTop.getHolder(false)).thenReturn(null);
        when(ordinaryTop.getSize()).thenReturn(9);
        when(view.getTopInventory()).thenReturn(ordinaryTop);
        ItemStack cursor = ItemStack.of(Material.CHEST);
        when(view.getCursor()).thenReturn(cursor);
        CustomStack shulker = customStack("test:shulker");

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(cursor)).thenReturn(shulker);
            InventoryClickEvent event = click(0, ClickType.LEFT, InventoryAction.PLACE_ALL);

            guard.onInventoryClick(event);

            assertFalse(event.isCancelled());
        }
    }

    @Test
    void vanillaOrUnknownItemsAreNotBlocked() {
        ItemStack cursor = ItemStack.of(Material.DIRT);
        when(view.getCursor()).thenReturn(cursor);

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(cursor)).thenReturn(null);
            InventoryClickEvent event = click(0, ClickType.LEFT, InventoryAction.PLACE_ALL);

            guard.onInventoryClick(event);

            assertFalse(event.isCancelled());
        }
    }
}
