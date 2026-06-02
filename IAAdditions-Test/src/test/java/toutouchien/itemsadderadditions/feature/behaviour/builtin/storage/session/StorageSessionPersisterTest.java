package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.session;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.mockito.MockedStatic;
import toutouchien.itemsadderadditions.common.utils.BlockCoord;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageType;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory.StorageInventoryManager;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantTransformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageSessionPersisterTest {
    private static final NamespacedKey CONTENTS_KEY = new NamespacedKey("itemsadderadditions", "contents");

    private ServerMock server;
    private WorldMock world;
    private JavaPlugin plugin;
    private Player player;
    private Inventory inventory;
    private ItemStack[] contents;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        plugin = MockBukkit.createMockPlugin("StorageSessionPersisterTest");
        player = mock(Player.class);
        when(player.getName()).thenReturn("Tester");
        inventory = mock(Inventory.class);
        contents = new ItemStack[]{ItemStack.of(Material.DIAMOND, 2)};
        when(inventory.getContents()).thenReturn(contents);
    }

    @AfterEach
    void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void disposalSessionsAreNotPersisted() {
        StorageSessionPersister persister = persister(StorageType.DISPOSAL, null);
        StorageSession session = blockSession(world.getBlockAt(1, 64, 1), inventory);

        try (MockedStatic<StorageInventoryManager> storage = mockStatic(StorageInventoryManager.class)) {
            persister.saveAfterClose(session, true);
            Set<Inventory> saved = persister.saveBeforeHolderBreak(List.of(session), new HashMap<>());

            assertTrue(saved.isEmpty());
            storage.verifyNoInteractions();
        }
    }

    @Test
    void saveAfterClosePersistsBlockAndRestoresOpenVariantOnLastClose() {
        OpenVariantTransformer transformer = mock(OpenVariantTransformer.class);
        StorageSessionPersister persister = persister(StorageType.STORAGE, transformer);
        Block block = world.getBlockAt(2, 64, 2);
        StorageSession session = blockSession(block, inventory);

        try (MockedStatic<StorageInventoryManager> storage = mockStatic(StorageInventoryManager.class)) {
            persister.saveAfterClose(session, true);

            storage.verify(() -> StorageInventoryManager.saveToBlock(block, contents, CONTENTS_KEY, plugin));
        }
        verify(transformer).onLastClose(block.getLocation(), "pack:closed", true);
    }

    @Test
    void saveAfterClosePersistsFurnitureOnlyWhenLastViewerAndUsesRestoredTarget() {
        Entity original = mock(Entity.class);
        Entity restored = mock(Entity.class);
        Location location = new Location(world, 4, 64, 4);
        when(original.getLocation()).thenReturn(location);
        OpenVariantTransformer transformer = mock(OpenVariantTransformer.class);
        when(transformer.onLastClose(location, "pack:closed", false)).thenReturn(restored);
        StorageSessionPersister persister = persister(StorageType.STORAGE, transformer);
        StorageSession session = furnitureSession(original, inventory);

        try (MockedStatic<StorageInventoryManager> storage = mockStatic(StorageInventoryManager.class)) {
            persister.saveAfterClose(session, false);
            storage.verifyNoInteractions();

            persister.saveAfterClose(session, true);
            storage.verify(() -> StorageInventoryManager.saveToEntity(restored, contents, CONTENTS_KEY));
        }
    }

    @Test
    void saveAfterCloseLogsAndSkipsSessionWithoutHolder() {
        StorageSessionPersister persister = persister(StorageType.STORAGE, null);
        StorageSession session = new StorageSession(player, inventory, null, null, StorageType.STORAGE);

        try (MockedStatic<StorageInventoryManager> storage = mockStatic(StorageInventoryManager.class)) {
            persister.saveAfterClose(session, true);
            storage.verifyNoInteractions();
        }
    }

    @Test
    void saveBeforeHolderBreakPersistsUniqueInventoriesAndPreloadsBlockContents() {
        StorageSessionPersister persister = persister(StorageType.STORAGE, null);
        Block first = world.getBlockAt(1, 64, 1);
        Block duplicateInventory = world.getBlockAt(2, 64, 2);
        Inventory otherInventory = mock(Inventory.class);
        ItemStack[] otherContents = new ItemStack[]{ItemStack.of(Material.GOLD_INGOT, 3)};
        when(otherInventory.getContents()).thenReturn(otherContents);
        Map<BlockCoord, ItemStack[]> preload = new HashMap<>();

        try (MockedStatic<StorageInventoryManager> storage = mockStatic(StorageInventoryManager.class)) {
            Set<Inventory> saved = persister.saveBeforeHolderBreak(List.of(
                    blockSession(first, inventory),
                    blockSession(duplicateInventory, inventory),
                    blockSession(world.getBlockAt(3, 64, 3), otherInventory)
            ), preload);

            assertEquals(Set.of(inventory, otherInventory), saved);
            assertSame(contents, preload.get(BlockCoord.of(first.getLocation())));
            assertFalse(preload.containsKey(BlockCoord.of(duplicateInventory.getLocation())));
            storage.verify(() -> StorageInventoryManager.saveToBlock(first, contents, CONTENTS_KEY, plugin));
            storage.verify(() -> StorageInventoryManager.saveToBlock(world.getBlockAt(3, 64, 3), otherContents, CONTENTS_KEY, plugin));
        }
    }

    @Test
    void saveBeforeHolderBreakPersistsFurnitureOnlyWithoutOpenVariantTransformer() {
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(new Location(world, 7, 64, 7));
        StorageSession session = furnitureSession(entity, inventory);

        try (MockedStatic<StorageInventoryManager> storage = mockStatic(StorageInventoryManager.class)) {
            persister(StorageType.STORAGE, null).saveBeforeHolderBreak(List.of(session), null);
            storage.verify(() -> StorageInventoryManager.saveToEntity(entity, contents, CONTENTS_KEY));
        }

        try (MockedStatic<StorageInventoryManager> storage = mockStatic(StorageInventoryManager.class)) {
            persister(StorageType.STORAGE, mock(OpenVariantTransformer.class))
                    .saveBeforeHolderBreak(List.of(session), null);
            storage.verifyNoInteractions();
        }
    }

    @Test
    void forceRemoveOpenVariantDelegatesOnlyWhenConfigured() {
        OpenVariantTransformer transformer = mock(OpenVariantTransformer.class);
        Location location = new Location(world, 8, 64, 8);

        persister(StorageType.STORAGE, transformer).forceRemoveOpenVariant(location);
        verify(transformer).forceRemove(location);

        assertDoesNotThrow(() -> persister(StorageType.STORAGE, null).forceRemoveOpenVariant(location));
    }

    private StorageSessionPersister persister(StorageType type, OpenVariantTransformer transformer) {
        return new StorageSessionPersister(plugin, type, CONTENTS_KEY, "pack:closed", transformer);
    }

    private StorageSession blockSession(Block block, Inventory inv) {
        return new StorageSession(player, inv, block, null, StorageType.STORAGE);
    }

    private StorageSession furnitureSession(Entity entity, Inventory inv) {
        return new StorageSession(player, inv, null, entity, StorageType.STORAGE);
    }
}
