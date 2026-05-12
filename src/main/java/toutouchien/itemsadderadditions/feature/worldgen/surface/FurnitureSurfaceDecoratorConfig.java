package toutouchien.itemsadderadditions.feature.worldgen.surface;

import dev.lone.itemsadder.api.CustomFurniture;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable configuration snapshot for a single furniture surface-decorator
 * entry.
 *
 * <p>Mirrors the fields ItemsAdder uses in {@code SurfaceDecoratorConfig}, but
 * targets furniture (spawned via {@link CustomFurniture#spawn}) instead of
 * custom blocks.
 */
@NullMarked
public final class FurnitureSurfaceDecoratorConfig {
    /**
     * ItemsAdder namespaced ID of the furniture to spawn.
     */
    public final String furnitureId;

    public final String name;
    public final String filePath;

    /**
     * Worlds this decorator is active in. Empty = all worlds.
     */
    public final List<String> worlds;

    /**
     * Furniture pieces to attempt per chunk.
     */
    public final int amount;

    /**
     * 0-100 chance per chunk, -1 = always.
     */
    public final double chunkChance;

    public final int maxHeight;
    public final int minHeight;

    /**
     * Whether a liquid block directly below the spawn point is allowed as a
     * surface.
     */
    public final boolean allowLiquidSurface;

    /**
     * Whether the target block itself may be water or lava.
     * Only WATER and LAVA are considered liquids here.
     */
    public final boolean allowLiquidPlacement;

    /**
     * Biome whitelist. Empty = all biomes allowed.
     */
    private final List<Biome> biomes = new ArrayList<>();

    /**
     * Surface-block whitelist (block directly below spawn point). Empty = any.
     */
    private final List<Material> bottomBlocks = new ArrayList<>();

    public FurnitureSurfaceDecoratorConfig(
            String name,
            String filePath,
            List<String> worlds,
            String furnitureId,
            int amount,
            int maxHeight,
            int minHeight,
            double chunkChance,
            boolean allowLiquidSurface,
            boolean allowLiquidPlacement
    ) {
        if (minHeight >= maxHeight) {
            throw new IllegalArgumentException(
                    "min_height must be less than max_height");
        }

        this.name = name;
        this.filePath = filePath;
        this.worlds = new ArrayList<>(worlds);
        this.furnitureId = furnitureId;
        this.amount = Math.max(amount, 1);
        this.maxHeight = maxHeight;
        this.minHeight = minHeight;
        this.chunkChance = chunkChance;
        this.allowLiquidSurface = allowLiquidSurface;
        this.allowLiquidPlacement = allowLiquidPlacement;
    }

    /**
     * Returns true if {@code material} is WATER or LAVA.
     */
    public static boolean isWaterOrLava(@Nullable Material material) {
        return material == Material.WATER || material == Material.LAVA;
    }

    public boolean isActiveInWorld(World world) {
        if (worlds.isEmpty() || worlds.contains("*")) return true;


        String worldName = world.getName();
        if (worlds.contains(worldName)) return true;


        for (String pattern : worlds) {
            if (FilenameUtils.wildcardMatch(worldName, pattern))
                return true;

        }

        return false;
    }

    public boolean isActiveInBiome(Block block) {
        return biomes.isEmpty() || biomes.contains(block.getBiome());
    }

    /**
     * Returns true if {@code material} is an allowed surface, or no whitelist is
     * configured.
     */
    public boolean isAllowedSurface(@Nullable Material material) {
        return bottomBlocks.isEmpty() || bottomBlocks.contains(material);
    }

    /**
     * Returns true if the target block may be used for placement.
     */
    public boolean isAllowedTarget(Block block) {
        Material type = block.getType();
        return type.isAir() || allowLiquidPlacement && isWaterOrLava(type);
    }

    /**
     * Attempts to spawn the furniture at {@code block}.
     *
     * @return true if the spawn succeeded
     */
    public boolean trySpawn(Block block) {
        return CustomFurniture.spawn(furnitureId, block) != null;
    }

    void addBiome(Biome biome) {
        biomes.add(biome);
    }

    void addBottomBlock(Material material) {
        bottomBlocks.add(material);
    }
}
