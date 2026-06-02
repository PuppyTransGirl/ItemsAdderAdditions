package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenVariantPlacementTest {
    private ServerMock server;
    private WorldMock world;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void captureBlockStateReturnsEmptyWhenNoCustomBlock() {
        Location location = new Location(world, 1, 64, 1);

        try (MockedStatic<CustomBlock> customBlocks = mockStatic(CustomBlock.class)) {
            customBlocks.when(() -> CustomBlock.byAlreadyPlaced(location.getBlock())).thenReturn(null);

            OpenVariantPlacement.BlockState state = OpenVariantPlacement.captureBlockState(location);

            assertNull(state.id());
            assertEquals("", state.rotationSuffix());
        }
    }

    @Test
    void captureBlockStateKeepsOriginalIdAndRotationSuffix() {
        Location location = new Location(world, 1, 64, 1);
        CustomBlock customBlock = mock(CustomBlock.class);
        when(customBlock.getNamespacedID()).thenReturn("pack:crate_north");

        try (MockedStatic<CustomBlock> customBlocks = mockStatic(CustomBlock.class)) {
            customBlocks.when(() -> CustomBlock.byAlreadyPlaced(location.getBlock())).thenReturn(customBlock);

            OpenVariantPlacement.BlockState state = OpenVariantPlacement.captureBlockState(location);

            assertEquals("pack:crate_north", state.id());
            assertEquals("_north", state.rotationSuffix());
        }
    }

    @Test
    void rotatedVariantIdUsesBaseWhenSuffixEmptyOrRotatedVariantMissing() {
        assertEquals("pack:crate_open", OpenVariantPlacement.rotatedVariantId("pack:crate_open", ""));

        try (MockedStatic<CustomBlock> customBlocks = mockStatic(CustomBlock.class)) {
            customBlocks.when(() -> CustomBlock.getInstance("pack:crate_open_north")).thenReturn(null);

            assertEquals("pack:crate_open", OpenVariantPlacement.rotatedVariantId("pack:crate_open", "_north"));
        }
    }

    @Test
    void rotatedVariantIdUsesRotatedVariantWhenRegistered() {
        CustomBlock customBlock = mock(CustomBlock.class);

        try (MockedStatic<CustomBlock> customBlocks = mockStatic(CustomBlock.class)) {
            customBlocks.when(() -> CustomBlock.getInstance("pack:crate_open_north")).thenReturn(customBlock);

            assertEquals("pack:crate_open_north", OpenVariantPlacement.rotatedVariantId("pack:crate_open", "_north"));
        }
    }

    @Test
    void placeBlockReturnsFalseWhenUnresolvedAndPlacesWhenResolved() {
        Location location = new Location(world, 2, 64, 2);
        CustomBlock customBlock = mock(CustomBlock.class);

        try (MockedStatic<CustomBlock> customBlocks = mockStatic(CustomBlock.class)) {
            customBlocks.when(() -> CustomBlock.getInstance("pack:missing")).thenReturn(null);
            assertFalse(OpenVariantPlacement.placeBlock("pack:missing", location));

            customBlocks.when(() -> CustomBlock.getInstance("pack:open")).thenReturn(customBlock);
            assertTrue(OpenVariantPlacement.placeBlock("pack:open", location));
        }

        verify(customBlock).place(location);
    }

    @Test
    void spawnFurnitureUsesCorrectSupportBlockAndYaw() {
        Location location = new Location(world, 5, 70, 5);
        Entity entity = mock(Entity.class);
        CustomFurniture furniture = mock(CustomFurniture.class);
        when(furniture.getEntity()).thenReturn(entity);

        try (MockedStatic<CustomFurniture> customFurniture = mockStatic(CustomFurniture.class)) {
            customFurniture.when(() -> CustomFurniture.spawn("pack:open", world.getBlockAt(5, 69, 5)))
                    .thenReturn(furniture);

            assertSame(entity, OpenVariantPlacement.spawnFurniture("pack:open", location, true, 135f));

            customFurniture.verify(() -> CustomFurniture.spawn("pack:open", world.getBlockAt(5, 69, 5)));
        }

        verify(entity).setRotation(135f, 0f);
    }

    @Test
    void spawnFurnitureReturnsNullWhenItemsAdderCannotSpawnEntity() {
        Location location = new Location(world, 5, 70, 5);
        CustomFurniture furnitureWithoutEntity = mock(CustomFurniture.class);
        when(furnitureWithoutEntity.getEntity()).thenReturn(null);

        try (MockedStatic<CustomFurniture> customFurniture = mockStatic(CustomFurniture.class)) {
            customFurniture.when(() -> CustomFurniture.spawn("pack:missing", location.getBlock())).thenReturn(null);
            assertNull(OpenVariantPlacement.spawnFurniture("pack:missing", location, false, null));

            customFurniture.when(() -> CustomFurniture.spawn("pack:no_entity", location.getBlock()))
                    .thenReturn(furnitureWithoutEntity);
            assertNull(OpenVariantPlacement.spawnFurniture("pack:no_entity", location, false, null));
        }
    }

    @Test
    void removeFurnitureEntityUsesItemsAdderWhenAvailableOtherwiseFallsBack() {
        Entity entity = mock(Entity.class);
        CustomFurniture furniture = mock(CustomFurniture.class);

        try (MockedStatic<CustomFurniture> customFurniture = mockStatic(CustomFurniture.class)) {
            customFurniture.when(() -> CustomFurniture.byAlreadySpawned(entity)).thenReturn(furniture);
            OpenVariantPlacement.removeFurnitureEntity(entity);
            verify(furniture).remove(false);
            verify(entity, never()).remove();

            customFurniture.when(() -> CustomFurniture.byAlreadySpawned(entity)).thenReturn(null);
            OpenVariantPlacement.removeFurnitureEntity(entity);
            verify(entity).remove();
        }
    }

    @Test
    void clearBlockAndScheduledBarrierSweepMutateWorldBlocks() {
        JavaPlugin plugin = MockBukkit.createMockPlugin("OpenVariantPlacementTest");
        Location center = new Location(world, 10, 64, 10);
        world.getBlockAt(10, 64, 10).setType(Material.BARRIER);
        world.getBlockAt(11, 64, 10).setType(Material.STONE);
        world.getBlockAt(12, 64, 10).setType(Material.BARRIER);

        OpenVariantPlacement.clearBlock(center);
        assertEquals(Material.AIR, world.getBlockAt(10, 64, 10).getType());

        OpenVariantPlacement.scheduleBarrierSweep(plugin, center, 1);
        server.getScheduler().performOneTick();

        assertEquals(Material.AIR, world.getBlockAt(10, 64, 10).getType());
        assertEquals(Material.STONE, world.getBlockAt(11, 64, 10).getType());
        assertEquals(Material.BARRIER, world.getBlockAt(12, 64, 10).getType());
    }
}
