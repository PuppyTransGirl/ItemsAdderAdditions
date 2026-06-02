package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.mockito.MockedStatic;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.common.utils.BlockCoord;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory.StorageInventoryManager;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantConfig;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantTransformer;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.session.StorageSessionManager;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageRuntimeTest {
    private static final NamespacedKey CONTENTS_KEY = new NamespacedKey("itemsadderadditions", "contents");
    private static final NamespacedKey UNIQUE_KEY = new NamespacedKey("itemsadderadditions", "unique");

    private static ServerMock server;
    private static WorldMock world;
    private static JavaPlugin plugin;

    private StorageSessionManager sessions;
    private ShulkerDropTracker dropTracker;

    @BeforeAll
    static void boot() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        plugin = MockBukkit.createMockPlugin("StorageRuntimeTest");
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
        sessions = mock(StorageSessionManager.class);
        dropTracker = mock(ShulkerDropTracker.class);
    }

    private StorageRuntime runtime(StorageType type) {
        return new StorageRuntime(
                plugin,
                "test:crate",
                ItemCategory.BLOCK,
                type,
                CONTENTS_KEY,
                UNIQUE_KEY,
                sessions,
                dropTracker,
                null,
                null
        );
    }

    private StorageRuntime runtimeWithOpenVariant(OpenVariantConfig config, OpenVariantTransformer transformer) {
        return new StorageRuntime(
                plugin,
                "test:crate",
                ItemCategory.BLOCK,
                StorageType.STORAGE,
                CONTENTS_KEY,
                UNIQUE_KEY,
                sessions,
                dropTracker,
                config,
                transformer
        );
    }

    @Test
    void closedAndOpenVariantIdsUseRotationAndConfigRules() {
        OpenVariantTransformer transformer = mock(OpenVariantTransformer.class);
        StorageRuntime runtime = runtimeWithOpenVariant(
                new OpenVariantConfig(ItemCategory.BLOCK, "test:crate_open"),
                transformer
        );

        assertTrue(runtime.matchesClosedId("test:crate"));
        assertTrue(runtime.matchesClosedId("test:crate_north"));
        assertFalse(runtime.matchesClosedId("minecraft:crate_north"));
        assertTrue(runtime.matchesOpenVariantId("test:crate_open"));
        assertFalse(runtime.matchesOpenVariantId("test:crate_open_north"));
        assertTrue(runtime.hasBlockOpenVariant());
        assertFalse(runtime.hasFurnitureOpenVariant());
    }

    @Test
    void furnitureOpenVariantRequiresFurnitureCategoryAndTransformer() {
        StorageRuntime runtime = runtimeWithOpenVariant(
                new OpenVariantConfig(ItemCategory.FURNITURE, "test:crate_open"),
                mock(OpenVariantTransformer.class)
        );

        assertFalse(runtime.hasBlockOpenVariant());
        assertTrue(runtime.hasFurnitureOpenVariant());
    }

    @Test
    void preloadBlockContentsStoresOnlyNonNullContentsByBlockCoordinate() {
        StorageRuntime runtime = runtime(StorageType.STORAGE);
        var block = world.getBlockAt(4, 64, 4);
        ItemStack[] contents = {ItemStack.of(Material.DIAMOND)};

        runtime.preloadBlockContents(block, null);
        assertTrue(runtime.preloadedBlockContents().isEmpty());

        runtime.preloadBlockContents(block, contents);

        assertSame(contents, runtime.consumePreloadedBlockContents(block.getLocation()));
        assertNull(runtime.consumePreloadedBlockContents(block.getLocation()));
    }

    @Test
    void dropContentsIgnoresNullAndAirSlots() {
        StorageRuntime runtime = runtime(StorageType.STORAGE);
        Location loc = new Location(world, 8, 65, 8);

        runtime.dropContents(loc, new ItemStack[]{
                null,
                ItemStack.of(Material.AIR),
                ItemStack.of(Material.EMERALD, 3)
        });

        var drops = new ArrayList<>(world.getEntitiesByClass(Item.class));
        assertEquals(1, drops.size());
        assertEquals(Material.EMERALD, drops.getFirst().getItemStack().getType());
        assertEquals(3, drops.getFirst().getItemStack().getAmount());
    }

    @Test
    void handleContainerBreakDispatchesByStorageType() {
        Location loc = new Location(world, 10, 65, 10);
        ItemStack[] contents = {ItemStack.of(Material.DIAMOND)};

        runtime(StorageType.SHULKER).handleContainerBreak(loc, contents);
        verify(dropTracker).stageDrop(loc, contents);

        clearInvocations(dropTracker);
        runtime(StorageType.DISPOSAL).handleContainerBreak(loc, contents);
        verifyNoInteractions(dropTracker);
    }

    @Test
    void creativeContainerBreakPreservesContentsByStorageType() {
        Location loc = new Location(world, 11, 65, 11);
        ItemStack[] contents = {ItemStack.of(Material.DIAMOND)};

        runtime(StorageType.STORAGE).handleCreativeContainerBreak(loc, contents);
        assertTrue(world.getEntitiesByClass(Item.class).stream()
                .anyMatch(drop -> drop.getItemStack().getType() == Material.DIAMOND));

        runtime(StorageType.SHULKER).handleCreativeContainerBreak(loc, contents);
        verify(dropTracker).stageCreativeDrop(loc, contents);

        clearInvocations(dropTracker);
        runtime(StorageType.DISPOSAL).handleCreativeContainerBreak(loc, contents);
        verifyNoInteractions(dropTracker);
    }

    @Test
    void storageOpenVariantBreakScattersContentsAndDropsOriginalItem() {
        StorageRuntime runtime = runtime(StorageType.STORAGE);
        CustomStack original = mock(CustomStack.class);
        when(original.getItemStack()).thenReturn(ItemStack.of(Material.CHEST));

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("test:crate")).thenReturn(original);
            runtime.handleOpenVariantBreakDrops(new Location(world, 12, 65, 12),
                    new ItemStack[]{ItemStack.of(Material.DIAMOND)});
        }

        assertEquals(2, world.getEntitiesByClass(Item.class).size());
    }

    @Test
    void shulkerOpenVariantBreakInjectsContentsAndUniqueIdIntoOriginalDrop() {
        StorageRuntime runtime = runtime(StorageType.SHULKER);
        CustomStack original = mock(CustomStack.class);
        when(original.getItemStack()).thenReturn(ItemStack.of(Material.CHEST));

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("test:crate")).thenReturn(original);
            runtime.handleOpenVariantBreakDrops(new Location(world, 14, 65, 14),
                    new ItemStack[]{ItemStack.of(Material.DIAMOND)});
        }

        var drops = new ArrayList<>(world.getEntitiesByClass(Item.class));
        ItemStack drop = drops.getLast().getItemStack();
        assertNotNull(StorageInventoryManager.extractFromItem(drop, CONTENTS_KEY));
        assertNotNull(drop.getItemMeta().getPersistentDataContainer().get(UNIQUE_KEY, PersistentDataType.STRING));
    }

    @Test
    void creativeOpenVariantBreakUsesCreativeStorageSemantics() {
        Location loc = new Location(world, 15, 65, 15);
        ItemStack[] contents = {ItemStack.of(Material.DIAMOND)};

        runtime(StorageType.STORAGE).handleCreativeOpenVariantBreakDrops(loc, contents);
        assertTrue(world.getEntitiesByClass(Item.class).stream()
                .anyMatch(drop -> drop.getItemStack().getType() == Material.DIAMOND));

        runtime(StorageType.SHULKER).handleCreativeOpenVariantBreakDrops(loc, contents);
        verify(dropTracker).dropPortableItem(loc, contents);
    }

    @Test
    void openVariantBreakDoesNothingWhenOriginalStackCannotResolve() {
        StorageRuntime runtime = runtime(StorageType.SHULKER);
        int before = world.getEntitiesByClass(Item.class).size();

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("test:crate")).thenReturn(null);
            runtime.handleOpenVariantBreakDrops(new Location(world, 16, 65, 16),
                    new ItemStack[]{ItemStack.of(Material.DIAMOND)});
        }

        assertEquals(before, world.getEntitiesByClass(Item.class).size());
    }

    @Test
    void extractFromHandChecksMainHandBeforeOffHand() {
        StorageRuntime runtime = runtime(StorageType.SHULKER);
        PlayerMock player = server.addPlayer();
        ItemStack main = ItemStack.of(Material.CHEST);
        ItemStack off = ItemStack.of(Material.BARREL);
        ItemStack[] mainContents = {ItemStack.of(Material.DIAMOND)};
        ItemStack[] offContents = {ItemStack.of(Material.EMERALD)};
        StorageInventoryManager.injectIntoItem(main, mainContents, CONTENTS_KEY);
        StorageInventoryManager.injectIntoItem(off, offContents, CONTENTS_KEY);
        player.getInventory().setItemInMainHand(main);
        player.getInventory().setItemInOffHand(off);

        assertSame(mainContents[0].getType(), runtime.extractFromHand(player)[0].getType());
    }

    @Test
    void clearClearsCollaboratorsCachesAndOpenVariantTransformer() {
        OpenVariantTransformer transformer = mock(OpenVariantTransformer.class);
        StorageRuntime runtime = runtimeWithOpenVariant(
                new OpenVariantConfig(ItemCategory.BLOCK, "test:crate_open"),
                transformer
        );
        runtime.preloadedBlockContents().put(new BlockCoord("world", 1, 2, 3), new ItemStack[0]);

        runtime.clear();

        verify(sessions).clear();
        verify(dropTracker).clear();
        verify(transformer).clear();
        assertTrue(runtime.preloadedBlockContents().isEmpty());
    }
}
