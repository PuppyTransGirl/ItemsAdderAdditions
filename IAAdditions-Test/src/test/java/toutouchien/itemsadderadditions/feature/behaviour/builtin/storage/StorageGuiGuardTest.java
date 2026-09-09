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
import toutouchien.itemsadderadditions.common.namespace.CustomTagDefinition;
import toutouchien.itemsadderadditions.common.namespace.CustomTagRegistry;
import toutouchien.itemsadderadditions.common.namespace.CustomTagType;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory.StorageInventoryHolder;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.session.StorageSessionManager;

import java.util.List;
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
    private StorageSessionManager sessions;
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
        sessions = mock(StorageSessionManager.class);
        when(sessions.ownsInventory(top)).thenReturn(true);
        guard = guard(null, null);
    }

    @AfterEach
    void tearDown() {
        NamespaceUtils.clearCustomTagRegistry();
        MockBukkit.unmock();
    }

    private StorageGuiGuard guard(List<String> allowedItems, List<String> deniedItems) {
        return new StorageGuiGuard(sessions, Set.of("test:shulker"), allowedItems, deniedItems);
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
        when(sessions.ownsInventory(ordinaryTop)).thenReturn(false);
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

    @Test
    void whitelistAllowsExactItemId() {
        guard = guard(List.of("minecraft:book"), null);
        when(view.getCursor()).thenReturn(ItemStack.of(Material.BOOK));
        InventoryClickEvent event = click(0, ClickType.LEFT, InventoryAction.PLACE_ALL);

        guard.onInventoryClick(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void whitelistAllowsCustomExactItemId() {
        guard = guard(List.of("my_pack:book"), null);
        ItemStack cursor = ItemStack.of(Material.BOOK);
        when(view.getCursor()).thenReturn(cursor);
        CustomStack customBook = customStack("my_pack:book");

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(cursor)).thenReturn(customBook);
            InventoryClickEvent event = click(0, ClickType.LEFT, InventoryAction.PLACE_ALL);

            guard.onInventoryClick(event);

            assertFalse(event.isCancelled());
        }
    }

    @Test
    void whitelistAllowsMinecraftAndCustomItemTags() {
        NamespaceUtils.setCustomTagRegistry(CustomTagRegistry.resolve(List.of(new CustomTagDefinition(
                "my_pack", "building_materials", CustomTagType.ITEM,
                List.of("minecraft:stone"), "test.yml"))));

        for (Map.Entry<Material, String> match : Map.of(
                Material.OAK_LOG, "#minecraft:logs",
                Material.STONE, "#my_pack:building_materials").entrySet()) {
            guard = guard(List.of(match.getValue()), null);
            when(view.getCursor()).thenReturn(ItemStack.of(match.getKey()));
            InventoryClickEvent event = click(0, ClickType.LEFT, InventoryAction.PLACE_ALL);

            guard.onInventoryClick(event);

            assertFalse(event.isCancelled());
        }
    }

    @Test
    void whitelistRejectsUnmatchedItem() {
        guard = guard(List.of("minecraft:book"), null);
        when(view.getCursor()).thenReturn(ItemStack.of(Material.TNT));
        InventoryClickEvent event = click(0, ClickType.LEFT, InventoryAction.PLACE_ALL);

        guard.onInventoryClick(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void blacklistRejectsExactItemIdAndTag() {
        guard = guard(null, List.of("minecraft:tnt", "#minecraft:logs"));

        for (Material denied : List.of(Material.TNT, Material.OAK_LOG)) {
            when(view.getCursor()).thenReturn(ItemStack.of(denied));
            InventoryClickEvent event = click(0, ClickType.LEFT, InventoryAction.PLACE_ALL);

            guard.onInventoryClick(event);

            assertTrue(event.isCancelled());
        }
    }

    @Test
    void blacklistAllowsUnrelatedItem() {
        guard = guard(null, List.of("minecraft:tnt", "#minecraft:logs"));
        when(view.getCursor()).thenReturn(ItemStack.of(Material.BOOK));
        InventoryClickEvent event = click(0, ClickType.LEFT, InventoryAction.PLACE_ALL);

        guard.onInventoryClick(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void emptyWhitelistAllowsNothingAndEmptyBlacklistDeniesNothing() {
        when(view.getCursor()).thenReturn(ItemStack.of(Material.BOOK));
        InventoryClickEvent whitelistEvent = click(0, ClickType.LEFT, InventoryAction.PLACE_ALL);
        guard(List.of(), null).onInventoryClick(whitelistEvent);

        InventoryClickEvent blacklistEvent = click(0, ClickType.LEFT, InventoryAction.PLACE_ALL);
        guard(null, List.of()).onInventoryClick(blacklistEvent);

        assertTrue(whitelistEvent.isCancelled());
        assertFalse(blacklistEvent.isCancelled());
    }

    @Test
    void restrictionAppliesToShiftClickFromPlayerInventory() {
        guard = guard(List.of("minecraft:book"), null);
        when(view.getItem(12)).thenReturn(ItemStack.of(Material.TNT));
        InventoryClickEvent event = click(12, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);

        guard.onInventoryClick(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void restrictionAppliesToNumberKeySwap() {
        guard = guard(List.of("minecraft:book"), null);
        player.getInventory().setItem(2, ItemStack.of(Material.TNT));
        InventoryClickEvent event = new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, 0,
                ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP, 2);

        guard.onInventoryClick(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void restrictionAppliesToOffhandSwap() {
        guard = guard(List.of("minecraft:book"), null);
        player.getInventory().setItemInOffHand(ItemStack.of(Material.TNT));
        InventoryClickEvent event = new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, 0,
                ClickType.SWAP_OFFHAND, InventoryAction.HOTBAR_SWAP, -1);

        guard.onInventoryClick(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void restrictionAppliesToInventoryDrag() {
        guard = guard(List.of("minecraft:book"), null);
        InventoryDragEvent event = new InventoryDragEvent(
                view,
                ItemStack.of(Material.AIR),
                ItemStack.of(Material.TNT),
                false,
                Map.of(0, ItemStack.of(Material.TNT), 12, ItemStack.of(Material.TNT))
        );

        guard.onInventoryDrag(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void sessionOwnedMenuInventoryIsGuardedWithoutStorageHolder() {
        when(top.getHolder(false)).thenReturn(null);
        guard = guard(List.of("minecraft:book"), null);
        when(view.getCursor()).thenReturn(ItemStack.of(Material.TNT));
        InventoryClickEvent event = click(0, ClickType.LEFT, InventoryAction.PLACE_ALL);

        guard.onInventoryClick(event);

        assertTrue(event.isCancelled());
    }
}
