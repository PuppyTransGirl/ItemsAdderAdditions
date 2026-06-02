package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.mockito.MockedStatic;
import toutouchien.itemsadderadditions.common.item.ItemCategory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenVariantTransformerTest {
    private ServerMock server;
    private WorldMock world;
    private Location location;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        location = new Location(world, 2, 64, 2);
    }

    @AfterEach
    void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void itemDisplayOpenVariantSwapsAndRestoresItemStackWithReferenceCounting() {
        OpenVariantTransformer transformer = new OpenVariantTransformer(
                new OpenVariantConfig(ItemCategory.ITEM, "pack:open_item"));
        ItemDisplay display = mock(ItemDisplay.class);
        when(display.isValid()).thenReturn(true);
        ItemStack original = ItemStack.of(Material.CHEST, 1);
        ItemStack open = ItemStack.of(Material.BARREL, 1);
        when(display.getItemStack()).thenReturn(original);
        CustomStack openStack = mock(CustomStack.class);
        when(openStack.getItemStack()).thenReturn(open);

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("pack:open_item")).thenReturn(openStack);

            assertSame(display, transformer.onFirstOpen(location, false, display));
            assertTrue(transformer.isTransformed(location));
            assertSame(display, transformer.onFirstOpen(location, false, display));
            assertNull(transformer.onLastClose(location, "pack:closed", false));
            assertTrue(transformer.isTransformed(location));
            assertSame(display, transformer.onLastClose(location, "pack:closed", false));
            assertFalse(transformer.isTransformed(location));
        }

        verify(display).setItemStack(open);
        verify(display).setItemStack(argThat(item -> item != original && item.getType() == Material.CHEST));
    }

    @Test
    void itemDisplayOpenVariantRejectsBlockHolderMissingStackAndInvalidEntity() {
        OpenVariantTransformer blockHolder = new OpenVariantTransformer(
                new OpenVariantConfig(ItemCategory.ITEM, "pack:open_item"));
        try (MockedStatic<OpenVariantPlacement> placement = mockStatic(OpenVariantPlacement.class)) {
            placement.when(() -> OpenVariantPlacement.captureBlockState(location))
                    .thenReturn(new OpenVariantPlacement.BlockState(null, ""));
            assertNull(blockHolder.onFirstOpen(location, true, null));
        }

        ItemDisplay invalid = mock(ItemDisplay.class);
        when(invalid.isValid()).thenReturn(false);
        assertNull(new OpenVariantTransformer(new OpenVariantConfig(ItemCategory.ITEM, "pack:open_item"))
                .onFirstOpen(location, false, invalid));

        ItemDisplay valid = mock(ItemDisplay.class);
        when(valid.isValid()).thenReturn(true);
        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("pack:missing")).thenReturn(null);
            assertNull(new OpenVariantTransformer(new OpenVariantConfig(ItemCategory.ITEM, "pack:missing"))
                    .onFirstOpen(location, false, valid));
        }
    }

    @Test
    void forceRemoveClearsTrackedLiveEntityThroughPlacementApi() {
        OpenVariantTransformer transformer = new OpenVariantTransformer(
                new OpenVariantConfig(ItemCategory.ITEM, "pack:open_item"));
        ItemDisplay display = mock(ItemDisplay.class);
        when(display.isValid()).thenReturn(true);
        when(display.getItemStack()).thenReturn(ItemStack.of(Material.CHEST));
        CustomStack openStack = mock(CustomStack.class);
        when(openStack.getItemStack()).thenReturn(ItemStack.of(Material.BARREL));

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("pack:open_item")).thenReturn(openStack);
            transformer.onFirstOpen(location, false, display);
        }

        try (MockedStatic<OpenVariantPlacement> placement = mockStatic(OpenVariantPlacement.class)) {
            transformer.forceRemove(location);

            placement.verify(() -> OpenVariantPlacement.removeFurnitureEntity(display));
        }
        assertFalse(transformer.isTransformed(location));
    }

    @Test
    void furnitureOpenVariantClearsOriginalBlockAndRestoresFurnitureWithSavedYaw() {
        OpenVariantTransformer transformer = new OpenVariantTransformer(
                new OpenVariantConfig(ItemCategory.FURNITURE, "pack:open_furniture"));
        Entity openEntity = mock(Entity.class);
        when(openEntity.isValid()).thenReturn(true);
        Entity restored = mock(Entity.class);

        try (MockedStatic<OpenVariantPlacement> placement = mockStatic(OpenVariantPlacement.class)) {
            placement.when(() -> OpenVariantPlacement.captureBlockState(location))
                    .thenReturn(new OpenVariantPlacement.BlockState("pack:closed_north", "_north"));
            placement.when(() -> OpenVariantPlacement.spawnFurniture("pack:open_furniture", location, true, null))
                    .thenReturn(openEntity);

            assertSame(openEntity, transformer.onFirstOpen(location, true, null));
            transformer.onLastClose(location, "pack:closed", true);

            placement.verify(() -> OpenVariantPlacement.clearBlock(location));
            placement.verify(() -> OpenVariantPlacement.removeFurnitureEntity(openEntity));
            placement.verify(() -> OpenVariantPlacement.placeBlock("pack:closed_north", location));
        }

        Entity originalEntity = mock(Entity.class);
        when(originalEntity.isValid()).thenReturn(true);
        when(originalEntity.getLocation()).thenReturn(new Location(world, 2, 64, 2, 90f, 0f));
        OpenVariantTransformer furnitureHolder = new OpenVariantTransformer(
                new OpenVariantConfig(ItemCategory.FURNITURE, "pack:open_furniture"));
        try (MockedStatic<OpenVariantPlacement> placement = mockStatic(OpenVariantPlacement.class)) {
            placement.when(() -> OpenVariantPlacement.spawnFurniture("pack:open_furniture", location, false, 90f))
                    .thenReturn(openEntity);
            placement.when(() -> OpenVariantPlacement.spawnFurniture("pack:closed", location, false, 90f))
                    .thenReturn(restored);

            assertSame(openEntity, furnitureHolder.onFirstOpen(location, false, originalEntity));
            assertSame(restored, furnitureHolder.onLastClose(location, "pack:closed", false));

            placement.verify(() -> OpenVariantPlacement.removeFurnitureEntity(originalEntity));
        }
    }

    @Test
    void blockOpenVariantAppliesRotationSuffixAndFallsBackToOriginalIdOnRestore() {
        OpenVariantTransformer transformer = new OpenVariantTransformer(
                new OpenVariantConfig(ItemCategory.BLOCK, "pack:open_block"));

        try (MockedStatic<OpenVariantPlacement> placement = mockStatic(OpenVariantPlacement.class)) {
            placement.when(() -> OpenVariantPlacement.captureBlockState(location))
                    .thenReturn(new OpenVariantPlacement.BlockState(null, "_east"));
            placement.when(() -> OpenVariantPlacement.rotatedVariantId("pack:open_block", "_east"))
                    .thenReturn("pack:open_block_east");

            assertNull(transformer.onFirstOpen(location, true, null));
            assertNull(transformer.onLastClose(location, "pack:closed_block", true));

            placement.verify(() -> OpenVariantPlacement.placeBlock("pack:open_block_east", location));
            placement.verify(() -> OpenVariantPlacement.clearBlock(location));
            placement.verify(() -> OpenVariantPlacement.placeBlock("pack:closed_block", location));
        }
    }

    @Test
    void unsupportedCategoryAndInvalidOriginalFurnitureAreNoOps() {
        OpenVariantTransformer unsupported = new OpenVariantTransformer(
                new OpenVariantConfig(ItemCategory.COMPLEX_FURNITURE, "pack:open_complex"));
        assertNull(unsupported.onFirstOpen(location, false, null));

        Entity invalid = mock(Entity.class);
        when(invalid.isValid()).thenReturn(false);
        OpenVariantTransformer furniture = new OpenVariantTransformer(
                new OpenVariantConfig(ItemCategory.FURNITURE, "pack:open_furniture"));
        try (MockedStatic<OpenVariantPlacement> placement = mockStatic(OpenVariantPlacement.class)) {
            placement.when(() -> OpenVariantPlacement.spawnFurniture("pack:open_furniture", location, false, 0f))
                    .thenReturn(null);

            assertNull(furniture.onFirstOpen(location, false, invalid));
        }
    }
}
