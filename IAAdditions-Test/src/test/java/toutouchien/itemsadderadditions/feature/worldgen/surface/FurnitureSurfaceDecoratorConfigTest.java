package toutouchien.itemsadderadditions.feature.worldgen.surface;

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

class FurnitureSurfaceDecoratorConfigTest {
    private ServerMock server;
    private WorldMock world;

    private static Biome differentBiome(Biome biome) {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).stream()
                .filter(candidate -> candidate != biome)
                .findFirst()
                .orElseThrow();
    }

    private static FurnitureSurfaceDecoratorConfig config(
            List<String> worlds,
            int amount,
            int maxHeight,
            int minHeight,
            boolean allowLiquidSurface,
            boolean allowLiquidPlacement
    ) {
        return new FurnitureSurfaceDecoratorConfig(
                "surface",
                "decorators.yml",
                worlds,
                "ns:furniture",
                amount,
                maxHeight,
                minHeight,
                25.0,
                allowLiquidSurface,
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
    void constructorRejectsMinHeightEqualToMaxHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> config(List.of(), 1, 64, 64, false, false));
    }

    @Test
    void constructorClampsAmountToAtLeastOne() {
        FurnitureSurfaceDecoratorConfig config = config(List.of(), -10, 80, 20, false, false);

        assertEquals(1, config.amount);
    }

    @Test
    void constructorStoresLiquidFlags() {
        FurnitureSurfaceDecoratorConfig config = config(List.of(), 2, 80, 20, true, true);

        assertTrue(config.allowLiquidSurface);
        assertTrue(config.allowLiquidPlacement);
    }

    @Test
    void constructorDefensivelyCopiesWorlds() {
        List<String> worlds = new ArrayList<>(List.of("world"));
        FurnitureSurfaceDecoratorConfig config = config(worlds, 2, 80, 20, false, false);
        worlds.add("other");

        assertEquals(List.of("world"), config.worlds);
    }

    @Test
    void isWaterOrLavaTrueForWaterAndLava() {
        assertTrue(FurnitureSurfaceDecoratorConfig.isWaterOrLava(Material.WATER));
        assertTrue(FurnitureSurfaceDecoratorConfig.isWaterOrLava(Material.LAVA));
    }

    @Test
    void isWaterOrLavaFalseForNonLiquidAndNull() {
        assertFalse(FurnitureSurfaceDecoratorConfig.isWaterOrLava(Material.AIR));
        assertFalse(FurnitureSurfaceDecoratorConfig.isWaterOrLava(Material.DIRT));
        assertFalse(FurnitureSurfaceDecoratorConfig.isWaterOrLava(null));
    }

    @Test
    void isActiveInWorld_emptyListAllowsAllWorlds() {
        assertTrue(config(List.of(), 1, 80, 20, false, false).isActiveInWorld(world));
    }

    @Test
    void isActiveInWorld_starAllowsAllWorlds() {
        assertTrue(config(List.of("*"), 1, 80, 20, false, false).isActiveInWorld(world));
    }

    @Test
    void isActiveInWorld_exactNameMatches() {
        assertTrue(config(List.of("world"), 1, 80, 20, false, false).isActiveInWorld(world));
    }

    @Test
    void isActiveInBiome_emptyWhitelistAllowsAllBiomes() {
        Block block = world.getBlockAt(0, 64, 0);

        assertTrue(config(List.of(), 1, 80, 20, false, false).isActiveInBiome(block));
    }

    @Test
    void isActiveInBiome_matchesAddedBiome() {
        FurnitureSurfaceDecoratorConfig config = config(List.of(), 1, 80, 20, false, false);
        Block block = world.getBlockAt(0, 64, 0);
        config.addBiome(block.getBiome());

        assertTrue(config.isActiveInBiome(block));
    }

    @Test
    void isActiveInBiome_rejectsOtherBiome() {
        FurnitureSurfaceDecoratorConfig config = config(List.of(), 1, 80, 20, false, false);
        Block block = world.getBlockAt(0, 64, 0);
        config.addBiome(differentBiome(block.getBiome()));

        assertFalse(config.isActiveInBiome(block));
    }

    @Test
    void isAllowedSurface_emptyWhitelistAllowsNullAndAnyMaterial() {
        FurnitureSurfaceDecoratorConfig config = config(List.of(), 1, 80, 20, false, false);

        assertTrue(config.isAllowedSurface(null));
        assertTrue(config.isAllowedSurface(Material.STONE));
    }

    @Test
    void isAllowedSurface_matchesAddedMaterialOnly() {
        FurnitureSurfaceDecoratorConfig config = config(List.of(), 1, 80, 20, false, false);
        config.addBottomBlock(Material.SAND);

        assertTrue(config.isAllowedSurface(Material.SAND));
        assertFalse(config.isAllowedSurface(Material.GRASS_BLOCK));
    }

    @Test
    void isAllowedTarget_allowsAir() {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.AIR);

        assertTrue(config(List.of(), 1, 80, 20, false, false).isAllowedTarget(block));
    }

    @Test
    void isAllowedTarget_rejectsLiquidWhenDisabled() {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.WATER);

        assertFalse(config(List.of(), 1, 80, 20, false, false).isAllowedTarget(block));
    }

    @Test
    void isAllowedTarget_allowsLiquidWhenEnabled() {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.WATER);

        assertTrue(config(List.of(), 1, 80, 20, false, true).isAllowedTarget(block));
    }

    @Test
    void isAllowedTarget_rejectsSolidBlock() {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.OAK_LOG);

        assertFalse(config(List.of(), 1, 80, 20, false, true).isAllowedTarget(block));
    }
}
