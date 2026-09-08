package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.contentvariant;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.mockito.MockedStatic;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory.StorageInventoryManager;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantConfig;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantPlacement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentVariantTransformerTest {
    private ServerMock server;
    private WorldMock world;
    private JavaPlugin plugin;
    private Location location;
    private NamespacedKey markerKey;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        plugin = MockBukkit.createMockPlugin("ContentVariantTransformerTest");
        location = new Location(world, 3, 64, 3);
        markerKey = new NamespacedKey(plugin, "content_variant");
    }

    @AfterEach
    void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void fullBlockVariantPreservesRotationAndWritesPersistentOwnershipMarker() {
        ContentVariantTransformer transformer = transformer(new ContentVariantConfig(
                null, null, null, variant(ItemCategory.BLOCK, "pack:full")));
        var block = location.getBlock();
        ItemStack[] contents = {ItemStack.of(Material.STONE, 64)};

        try (MockedStatic<OpenVariantPlacement> placement = mockStatic(OpenVariantPlacement.class);
             MockedStatic<StorageInventoryManager> storage = mockStatic(StorageInventoryManager.class)) {
            placement.when(() -> OpenVariantPlacement.captureBlockState(location))
                    .thenReturn(new OpenVariantPlacement.BlockState("pack:barrel_north", "_north"));
            placement.when(() -> OpenVariantPlacement.rotatedVariantId("pack:full", "_north"))
                    .thenReturn("pack:full_north");
            placement.when(() -> OpenVariantPlacement.placeBlock("pack:full_north", location))
                    .thenReturn(true);

            transformer.applyToBlock(block, contents);

            placement.verify(() -> OpenVariantPlacement.placeBlock("pack:full_north", location));
            storage.verify(() -> StorageInventoryManager.markContentVariant(
                    block, markerKey, "pack:barrel", "pack:full", plugin));
        }
    }

    @Test
    void emptyContentsRestoreOriginalBlockAndClearMarker() {
        ContentVariantTransformer transformer = transformer(new ContentVariantConfig(
                null, variant(ItemCategory.BLOCK, "pack:filled"), null, null));
        var block = location.getBlock();

        try (MockedStatic<OpenVariantPlacement> placement = mockStatic(OpenVariantPlacement.class);
             MockedStatic<StorageInventoryManager> storage = mockStatic(StorageInventoryManager.class)) {
            placement.when(() -> OpenVariantPlacement.captureBlockState(location))
                    .thenReturn(new OpenVariantPlacement.BlockState("pack:filled_east", "_east"));
            placement.when(() -> OpenVariantPlacement.rotatedVariantId("pack:barrel", "_east"))
                    .thenReturn("pack:barrel_east");
            placement.when(() -> OpenVariantPlacement.placeBlock("pack:barrel_east", location))
                    .thenReturn(true);

            transformer.applyToBlock(block, new ItemStack[2]);

            placement.verify(() -> OpenVariantPlacement.placeBlock("pack:barrel_east", location));
            storage.verify(() -> StorageInventoryManager.clearContentVariantMarker(block, markerKey, plugin));
        }
    }

    @Test
    void itemVariantChangesDisplayModelWithoutReplacingFurniture() {
        OpenVariantConfig filled = variant(ItemCategory.ITEM, "pack:filled_item");
        ContentVariantTransformer transformer = transformer(new ContentVariantConfig(null, filled, null, null));
        ItemDisplay display = mock(ItemDisplay.class);
        when(display.isValid()).thenReturn(true);
        CustomStack stack = mock(CustomStack.class);
        ItemStack model = ItemStack.of(Material.BARREL);
        when(stack.getItemStack()).thenReturn(model);

        try (MockedStatic<StorageInventoryManager> storage = mockStatic(StorageInventoryManager.class);
             MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            storage.when(() -> StorageInventoryManager.contentVariantMarker(display, markerKey)).thenReturn(null);
            customStacks.when(() -> CustomStack.getInstance("pack:filled_item")).thenReturn(stack);

            assertSame(display, transformer.applyToEntity(
                    location, display, new ItemStack[]{ItemStack.of(Material.STONE), null}));

            verify(display).setItemStack(model);
            storage.verify(() -> StorageInventoryManager.markContentVariant(
                    display, markerKey, "pack:barrel", "pack:filled_item"));
        }
    }

    @Test
    void unchangedMarkedFurnitureVariantIsNotRespawned() {
        OpenVariantConfig half = variant(ItemCategory.FURNITURE, "pack:half");
        ContentVariantTransformer transformer = transformer(new ContentVariantConfig(null, null, half, null));
        Entity entity = mock(Entity.class);
        when(entity.isValid()).thenReturn(true);

        try (MockedStatic<StorageInventoryManager> storage = mockStatic(StorageInventoryManager.class);
             MockedStatic<OpenVariantPlacement> placement = mockStatic(OpenVariantPlacement.class)) {
            storage.when(() -> StorageInventoryManager.contentVariantMarker(entity, markerKey))
                    .thenReturn("pack:barrel|pack:half");
            storage.when(() -> StorageInventoryManager.contentVariantId(
                    "pack:barrel|pack:half", "pack:barrel"))
                    .thenReturn("pack:half");

            assertSame(entity, transformer.applyToEntity(
                    location, entity, new ItemStack[]{ItemStack.of(Material.STONE), null}));
            placement.verifyNoInteractions();
        }
    }

    @Test
    void furnitureVariantReplacesHolderPreservesYawAndMarksReplacement() {
        OpenVariantConfig half = variant(ItemCategory.FURNITURE, "pack:half");
        ContentVariantTransformer transformer = transformer(new ContentVariantConfig(null, null, half, null));
        Entity original = mock(Entity.class);
        Entity replacement = mock(Entity.class);
        when(original.isValid()).thenReturn(true);
        when(original.getLocation()).thenReturn(new Location(world, 3, 64, 3, 70f, 0f));

        try (MockedStatic<StorageInventoryManager> storage = mockStatic(StorageInventoryManager.class);
             MockedStatic<OpenVariantPlacement> placement = mockStatic(OpenVariantPlacement.class)) {
            storage.when(() -> StorageInventoryManager.contentVariantMarker(original, markerKey)).thenReturn(null);
            placement.when(() -> OpenVariantPlacement.spawnFurniture("pack:half", location, false, 70f))
                    .thenReturn(replacement);

            assertSame(replacement, transformer.applyToEntity(
                    location, original, new ItemStack[]{ItemStack.of(Material.STONE), null}));

            placement.verify(() -> OpenVariantPlacement.removeFurnitureEntity(original));
            storage.verify(() -> StorageInventoryManager.markContentVariant(
                    replacement, markerKey, "pack:barrel", "pack:half"));
        }
    }

    @Test
    void markedBlockCheckUsesPersistentOwnerMarker() {
        ContentVariantTransformer transformer = transformer(new ContentVariantConfig(
                null, variant(ItemCategory.BLOCK, "pack:filled"), null, null));
        var block = location.getBlock();

        try (MockedStatic<StorageInventoryManager> storage = mockStatic(StorageInventoryManager.class)) {
            storage.when(() -> StorageInventoryManager.contentVariantMarker(block, markerKey, plugin))
                    .thenReturn("pack:barrel|pack:filled");
            storage.when(() -> StorageInventoryManager.isContentVariantMarker(
                    "pack:barrel|pack:filled", "pack:barrel"))
                    .thenReturn(true);

            assertTrue(transformer.isMarkedBlock(block));
        }
    }

    private ContentVariantTransformer transformer(ContentVariantConfig config) {
        return new ContentVariantTransformer(config, "pack:barrel", markerKey, plugin);
    }

    private static OpenVariantConfig variant(ItemCategory category, String id) {
        return new OpenVariantConfig(category, id);
    }
}
