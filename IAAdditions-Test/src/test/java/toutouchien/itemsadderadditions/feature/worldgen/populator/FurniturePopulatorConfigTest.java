package toutouchien.itemsadderadditions.feature.worldgen.populator;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FurniturePopulatorConfigTest {
    private ServerMock server;
    private WorldMock world;

    private static Biome differentBiome(Biome biome) {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).stream()
                .filter(candidate -> candidate != biome)
                .findFirst()
                .orElseThrow();
    }

    private static FurniturePopulatorConfig config(
            List<String> worlds,
            int veinBlocks,
            int maxHeight,
            int minHeight,
            int chunkVeins,
            boolean allowLiquidPlacement
    ) {
        return new FurniturePopulatorConfig(
                "ore",
                "items.yml",
                worlds,
                "ns:furniture",
                veinBlocks,
                maxHeight,
                minHeight,
                chunkVeins,
                50.0,
                allowLiquidPlacement
        );
    }

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
    void constructorRejectsMinHeightGreaterThanMaxHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> config(List.of(), 1, 10, 10, 1, false));
    }

    @Test
    void constructorClampsVeinBlocksAndChunkVeinsToAtLeastOne() {
        FurniturePopulatorConfig config = config(List.of(), 0, 80, 20, -5, false);

        assertEquals(1, config.veinBlocks);
        assertEquals(1, config.chunkVeins);
    }

    @Test
    void constructorDefensivelyCopiesWorlds() {
        List<String> worlds = new ArrayList<>(List.of("world"));
        FurniturePopulatorConfig config = config(worlds, 2, 80, 20, 3, false);
        worlds.add("other");

        assertEquals(List.of("world"), config.worlds);
    }

    @Test
    void isWaterOrLavaTrueForWaterAndLava() {
        assertTrue(FurniturePopulatorConfig.isWaterOrLava(Material.WATER));
        assertTrue(FurniturePopulatorConfig.isWaterOrLava(Material.LAVA));
    }

    @Test
    void isWaterOrLavaFalseForAirStoneAndNull() {
        assertFalse(FurniturePopulatorConfig.isWaterOrLava(Material.AIR));
        assertFalse(FurniturePopulatorConfig.isWaterOrLava(Material.STONE));
        assertFalse(FurniturePopulatorConfig.isWaterOrLava(null));
    }

    @Test
    void isActiveInWorld_emptyListAllowsAllWorlds() {
        assertTrue(config(List.of(), 1, 80, 20, 1, false).isActiveInWorld(world));
    }

    @Test
    void isActiveInWorld_starAllowsAllWorlds() {
        assertTrue(config(List.of("*"), 1, 80, 20, 1, false).isActiveInWorld(world));
    }

    @Test
    void isActiveInWorld_exactNameMatches() {
        assertTrue(config(List.of("world"), 1, 80, 20, 1, false).isActiveInWorld(world));
    }

    @Test
    void isActiveInBiome_emptyWhitelistAllowsAllBiomes() {
        Block block = world.getBlockAt(0, 64, 0);

        assertTrue(config(List.of(), 1, 80, 20, 1, false).isActiveInBiome(block));
    }

    @Test
    void isActiveInBiome_matchesAddedBiome() {
        FurniturePopulatorConfig config = config(List.of(), 1, 80, 20, 1, false);
        Block block = world.getBlockAt(0, 64, 0);
        config.addBiome(block.getBiome());

        assertTrue(config.isActiveInBiome(block));
    }

    @Test
    void isActiveInBiome_rejectsOtherBiome() {
        FurniturePopulatorConfig config = config(List.of(), 1, 80, 20, 1, false);
        Block block = world.getBlockAt(0, 64, 0);
        config.addBiome(differentBiome(block.getBiome()));

        assertFalse(config.isActiveInBiome(block));
    }

    @Test
    void isAllowedSurface_emptyWhitelistAllowsNullAndAnyMaterial() {
        FurniturePopulatorConfig config = config(List.of(), 1, 80, 20, 1, false);

        assertTrue(config.isAllowedSurface(null));
        assertTrue(config.isAllowedSurface(Material.STONE));
    }

    @Test
    void isAllowedSurface_matchesAddedMaterialOnly() {
        FurniturePopulatorConfig config = config(List.of(), 1, 80, 20, 1, false);
        config.addReplaceableBlock(Material.GRASS_BLOCK);

        assertTrue(config.isAllowedSurface(Material.GRASS_BLOCK));
        assertFalse(config.isAllowedSurface(Material.STONE));
    }

    @Test
    void isAllowedTarget_allowsAir() {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.AIR);

        assertTrue(config(List.of(), 1, 80, 20, 1, false).isAllowedTarget(block));
    }

    @Test
    void isAllowedTarget_rejectsLiquidWhenDisabled() {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.WATER);

        assertFalse(config(List.of(), 1, 80, 20, 1, false).isAllowedTarget(block));
    }

    @Test
    void isAllowedTarget_allowsLiquidWhenEnabled() {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.LAVA);

        assertTrue(config(List.of(), 1, 80, 20, 1, true).isAllowedTarget(block));
    }

    @Test
    void isAllowedTarget_rejectsSolidBlock() {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.STONE);

        assertFalse(config(List.of(), 1, 80, 20, 1, true).isAllowedTarget(block));
    }
}
