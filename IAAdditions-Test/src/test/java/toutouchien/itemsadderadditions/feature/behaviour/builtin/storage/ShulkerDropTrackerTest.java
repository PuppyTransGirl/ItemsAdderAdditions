package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory.StorageInventoryManager;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShulkerDropTrackerTest {
    private static final NamespacedKey CONTENTS_KEY = new NamespacedKey("itemsadderadditions", "contents");
    private static final NamespacedKey UNIQUE_KEY = new NamespacedKey("itemsadderadditions", "unique");

    private static ServerMock server;
    private static WorldMock world;
    private static JavaPlugin plugin;

    private ShulkerDropTracker tracker;

    @BeforeAll
    static void boot() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        plugin = MockBukkit.createMockPlugin("ShulkerDropTrackerTest");
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        for (Item item : world.getEntitiesByClass(Item.class)) {
            item.remove();
        }
        tracker = new ShulkerDropTracker(plugin, "test:crate", CONTENTS_KEY, UNIQUE_KEY);
    }

    private static CustomStack customStack(String id) {
        CustomStack stack = mock(CustomStack.class);
        when(stack.getNamespacedID()).thenReturn(id);
        return stack;
    }

    @Test
    void preCaptureStoresHandContentsForMatchingFurnitureItem() {
        PlayerMock player = server.addPlayer();
        ItemStack hand = ItemStack.of(Material.CHEST);
        ItemStack[] contents = {ItemStack.of(Material.DIAMOND)};
        StorageInventoryManager.injectIntoItem(hand, contents, CONTENTS_KEY);
        player.getInventory().setItemInMainHand(hand);

        CustomStack stack = customStack("test:crate");
        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(hand)).thenReturn(stack);
            tracker.onFurniturePlacePreCapture(new PlayerInteractEvent(
                    player,
                    Action.RIGHT_CLICK_AIR,
                    hand,
                    null,
                    BlockFace.SELF,
                    EquipmentSlot.HAND
            ));
        }

        ItemStack[] captured = tracker.consumePlaceContents(player.getUniqueId());
        assertNotNull(captured);
        assertEquals(Material.DIAMOND, captured[0].getType());
        assertNull(tracker.consumePlaceContents(player.getUniqueId()));
    }

    @Test
    void preCaptureIgnoresOffHandMismatchedAndEmptyItems() {
        PlayerMock player = server.addPlayer();
        ItemStack hand = ItemStack.of(Material.CHEST);
        player.getInventory().setItemInMainHand(hand);

        CustomStack other = customStack("test:other");
        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(hand)).thenReturn(other);
            tracker.onFurniturePlacePreCapture(new PlayerInteractEvent(
                    player,
                    Action.RIGHT_CLICK_AIR,
                    hand,
                    null,
                    BlockFace.SELF,
                    EquipmentSlot.HAND
            ));
            tracker.onFurniturePlacePreCapture(new PlayerInteractEvent(
                    player,
                    Action.RIGHT_CLICK_AIR,
                    hand,
                    null,
                    BlockFace.SELF,
                    EquipmentSlot.OFF_HAND
            ));
        }

        assertNull(tracker.consumePlaceContents(player.getUniqueId()));
    }

    @Test
    void itemSpawnNearStagedDropInjectsContentsAndUniqueId() {
        Location staged = new Location(world, 3, 64, 3);
        ItemStack[] contents = {ItemStack.of(Material.DIAMOND, 2)};
        tracker.stageDrop(staged, contents);
        Item dropped = world.dropItem(staged.clone().add(1, 0, 1), ItemStack.of(Material.CHEST));

        CustomStack customStack = customStack("test:crate_north");
        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(dropped.getItemStack()))
                    .thenReturn(customStack);
            tracker.onItemSpawn(new ItemSpawnEvent(dropped));
        }

        ItemStack stack = dropped.getItemStack();
        ItemStack[] stored = StorageInventoryManager.extractFromItem(stack, CONTENTS_KEY);
        assertNotNull(stored);
        assertEquals(Material.DIAMOND, stored[0].getType());
        assertNotNull(stack.getItemMeta().getPersistentDataContainer().get(UNIQUE_KEY, PersistentDataType.STRING));
    }

    @Test
    void blockDropItemConsumesNearestMatchingStagedDrop() {
        Location staged = new Location(world, 6, 64, 6);
        tracker.stageDrop(staged, new ItemStack[]{ItemStack.of(Material.EMERALD)});
        Item dropped = world.dropItem(staged.clone().add(2, 0, 0), ItemStack.of(Material.CHEST));
        PlayerMock player = server.addPlayer();

        CustomStack stack = customStack("test:crate");
        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(dropped.getItemStack()))
                    .thenReturn(stack);
            tracker.onBlockDropItem(new BlockDropItemEvent(
                    staged.getBlock(),
                    staged.getBlock().getState(),
                    player,
                    List.of(dropped)
            ));
        }

        ItemStack[] stored = StorageInventoryManager.extractFromItem(dropped.getItemStack(), CONTENTS_KEY);
        assertNotNull(stored);
        assertEquals(Material.EMERALD, stored[0].getType());
    }

    @Test
    void emptyContentsStripStaleDataFromDroppedItem() {
        Location staged = new Location(world, 9, 64, 9);
        tracker.stageDrop(staged, new ItemStack[]{null, ItemStack.of(Material.AIR)});
        ItemStack stale = ItemStack.of(Material.CHEST);
        StorageInventoryManager.injectIntoItem(stale, new ItemStack[]{ItemStack.of(Material.DIAMOND)}, CONTENTS_KEY);
        StorageInventoryManager.stampUniqueId(stale, UNIQUE_KEY);
        Item dropped = world.dropItem(staged, stale);

        CustomStack stack = customStack("test:crate");
        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(dropped.getItemStack()))
                    .thenReturn(stack);
            tracker.onItemSpawn(new ItemSpawnEvent(dropped));
        }

        assertNull(StorageInventoryManager.extractFromItem(dropped.getItemStack(), CONTENTS_KEY));
        assertFalse(dropped.getItemStack().getItemMeta().getPersistentDataContainer().has(UNIQUE_KEY, PersistentDataType.STRING));
    }

    @Test
    void mismatchedSpawnLeavesStagedDropAvailableForLaterMatchingSpawn() {
        Location staged = new Location(world, 12, 64, 12);
        tracker.stageDrop(staged, new ItemStack[]{ItemStack.of(Material.DIAMOND)});
        Item wrong = world.dropItem(staged, ItemStack.of(Material.BARREL));
        Item right = world.dropItem(staged, ItemStack.of(Material.CHEST));

        CustomStack other = customStack("test:other");
        CustomStack stack = customStack("test:crate");
        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(wrong.getItemStack()))
                    .thenReturn(other);
            customStacks.when(() -> CustomStack.byItemStack(right.getItemStack()))
                    .thenReturn(stack);
            tracker.onItemSpawn(new ItemSpawnEvent(wrong));
            tracker.onItemSpawn(new ItemSpawnEvent(right));
        }

        assertNull(StorageInventoryManager.extractFromItem(wrong.getItemStack(), CONTENTS_KEY));
        assertNotNull(StorageInventoryManager.extractFromItem(right.getItemStack(), CONTENTS_KEY));
    }

    @Test
    void fallbackScattersContentsWhenNoDropSpawnClaimsStage() {
        tracker.stageDrop(new Location(world, 15, 64, 15), new ItemStack[]{ItemStack.of(Material.DIAMOND)});

        server.getScheduler().performTicks(4);

        Collection<Item> drops = world.getEntitiesByClass(Item.class);
        assertTrue(drops.stream().anyMatch(drop -> drop.getItemStack().getType() == Material.DIAMOND));
    }

    @Test
    void creativeFallbackDropsPortableItemWithContentsWhenNoDropSpawnClaimsStage() {
        Location staged = new Location(world, 16, 64, 16);
        CustomStack original = mock(CustomStack.class);
        when(original.getItemStack()).thenReturn(ItemStack.of(Material.CHEST));

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("test:crate")).thenReturn(original);
            tracker.stageCreativeDrop(staged, new ItemStack[]{ItemStack.of(Material.DIAMOND, 2)});

            server.getScheduler().performTicks(4);
        }

        Collection<Item> drops = world.getEntitiesByClass(Item.class);
        ItemStack portable = drops.stream()
                .map(Item::getItemStack)
                .filter(stack -> stack.getType() == Material.CHEST)
                .findFirst()
                .orElseThrow();
        ItemStack[] stored = StorageInventoryManager.extractFromItem(portable, CONTENTS_KEY);
        assertNotNull(stored);
        assertEquals(Material.DIAMOND, stored[0].getType());
        assertNotNull(portable.getItemMeta().getPersistentDataContainer().get(UNIQUE_KEY, PersistentDataType.STRING));
    }

    @Test
    void creativeFallbackDoesNotOverwriteOlderNearbyPortableDrop() {
        Location staged = new Location(world, 16, 64, 16);
        Item oldDrop = world.dropItem(staged, ItemStack.of(Material.CHEST));
        oldDrop.setTicksLived(40);

        CustomStack original = mock(CustomStack.class);
        when(original.getItemStack()).thenReturn(ItemStack.of(Material.CHEST));
        CustomStack oldStack = customStack("test:crate");

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(oldDrop.getItemStack())).thenReturn(oldStack);
            customStacks.when(() -> CustomStack.getInstance("test:crate")).thenReturn(original);
            tracker.stageCreativeDrop(staged, new ItemStack[]{ItemStack.of(Material.DIAMOND)});

            server.getScheduler().performTicks(2);
        }

        assertNull(StorageInventoryManager.extractFromItem(oldDrop.getItemStack(), CONTENTS_KEY));
        long contentBearingDrops = world.getEntitiesByClass(Item.class).stream()
                .map(Item::getItemStack)
                .filter(stack -> StorageInventoryManager.extractFromItem(stack, CONTENTS_KEY) != null)
                .count();
        assertEquals(1, contentBearingDrops);
    }

    @Test
    void creativeFallbackScattersContentsWhenPortableItemCannotResolve() {
        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("test:crate")).thenReturn(null);
            tracker.stageCreativeDrop(new Location(world, 17, 64, 17), new ItemStack[]{ItemStack.of(Material.EMERALD)});

            server.getScheduler().performTicks(4);
        }

        Collection<Item> drops = world.getEntitiesByClass(Item.class);
        assertTrue(drops.stream().anyMatch(drop -> drop.getItemStack().getType() == Material.EMERALD));
    }

    @Test
    void clearRemovesPendingDropsAndPlaceContents() {
        Location staged = new Location(world, 18, 64, 18);
        tracker.stageDrop(staged, new ItemStack[]{ItemStack.of(Material.DIAMOND)});
        tracker.clear();
        Item dropped = world.dropItem(staged, ItemStack.of(Material.CHEST));

        CustomStack stack = customStack("test:crate");
        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(dropped.getItemStack()))
                    .thenReturn(stack);
            tracker.onItemSpawn(new ItemSpawnEvent(dropped));
        }

        assertNull(StorageInventoryManager.extractFromItem(dropped.getItemStack(), CONTENTS_KEY));
    }
}
