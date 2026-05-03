package toutouchien.itemsadderadditions.worldgen;

import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.generator.BlockPopulator;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * {@link BlockPopulator} that spawns ItemsAdder furniture as surface decorations
 * during chunk generation.
 *
 * <p>Overworld-like dimensions use heightmaps.
 *
 * <p>The Nether cannot reliably use heightmaps because they often resolve to
 * the roof rather than the playable floor. For Nether worlds this implementation
 * scans downward from {@code max_height} until it finds a valid placement spot.
 */
@NullMarked
public final class FurnitureSurfaceDecoratorBehaviour extends BlockPopulator {

    /**
     * All live instances, one per world.
     */
    static final List<FurnitureSurfaceDecoratorBehaviour> INSTANCES =
            new ArrayList<>();
    private static final int CHUNK_SIZE = 16;
    private static final int RANDOM_ANCHOR_BOUND = 15;
    private static final int MAX_ATTEMPTS_PER_SLOT = 8;
    private static final String TAG = "FurnitureSurfaceDecorator";

    private final World world;
    private final boolean isNether;
    private final List<FurnitureSurfaceDecoratorConfig> configs = new ArrayList<>();

    private FurnitureSurfaceDecoratorBehaviour(World world) {
        this.world = world;
        this.isNether = world.getEnvironment() == World.Environment.NETHER;
    }

    public static synchronized void register(
            FurnitureSurfaceDecoratorConfig config,
            World world
    ) {
        if (!config.isActiveInWorld(world)) {
            return;
        }

        FurnitureSurfaceDecoratorBehaviour behaviour = INSTANCES.stream()
                .filter(instance -> instance.world.equals(world))
                .findFirst()
                .orElseGet(() -> createFor(world));

        if (!behaviour.configs.contains(config)) {
            behaviour.configs.add(config);
        }
    }

    public static void onWorldLoad(World world) {
        for (FurnitureSurfaceDecoratorConfig config :
                FurnitureSurfaceDecoratorLoader.REGISTRY.values()) {
            register(config, world);
        }
    }

    public static synchronized void unregisterAll() {
        for (FurnitureSurfaceDecoratorBehaviour behaviour : INSTANCES) {
            behaviour.world.getPopulators().remove(behaviour);
        }
        INSTANCES.clear();
    }

    private static FurnitureSurfaceDecoratorBehaviour createFor(World world) {
        FurnitureSurfaceDecoratorBehaviour behaviour =
                new FurnitureSurfaceDecoratorBehaviour(world);
        INSTANCES.add(behaviour);
        world.getPopulators().add(behaviour);
        Log.info(TAG, "Registered surface decorator for world '"
                + world.getName() + "'");
        return behaviour;
    }

    private static int clamp(int value) {
        return Math.min(CHUNK_SIZE - 1, Math.max(0, value));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void populate(World world, Random random, Chunk chunk) {
        for (FurnitureSurfaceDecoratorConfig config : configs) {
            populateConfig(random, chunk, config);
        }
    }

    private void populateConfig(
            Random random,
            Chunk chunk,
            FurnitureSurfaceDecoratorConfig config
    ) {
        if (config.chunkChance != -1
                && random.nextDouble() * 100.0 > config.chunkChance) {
            return;
        }

        int anchorX = random.nextInt(RANDOM_ANCHOR_BOUND);
        int anchorZ = random.nextInt(RANDOM_ANCHOR_BOUND);
        int spread = Math.max(config.amount / 2, 1);

        for (int i = 0; i < config.amount; i++) {
            for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_SLOT; attempt++) {
                int x = anchorX + random.nextInt(spread) - config.amount / 4;
                int z = anchorZ + random.nextInt(spread) - config.amount / 4;

                x = clamp(x & 0xF);
                z = clamp(z & 0xF);

                int worldX = chunk.getX() * CHUNK_SIZE + x;
                int worldZ = chunk.getZ() * CHUNK_SIZE + z;

                Block target = findCandidateBlock(chunk.getWorld(), config, worldX, worldZ);
                if (target == null) {
                    continue;
                }

                if (config.trySpawn(target)) {
                    Log.debug(TAG, "Spawned " + config.furnitureId + " at "
                            + target.getX() + "," + target.getY() + ","
                            + target.getZ() + " in world "
                            + chunk.getWorld().getName());
                }

                // Mirror IA: stop trying this slot after a valid candidate is
                // found, regardless of whether the spawn itself succeeds.
                break;
            }
        }
    }

    @Nullable
    private Block findCandidateBlock(
            World world,
            FurnitureSurfaceDecoratorConfig config,
            int worldX,
            int worldZ
    ) {
        if (isNether) {
            return findNetherCandidate(world, config, worldX, worldZ);
        }

        int y = resolveNormalPlacementY(world, config, worldX, worldZ);
        if (y > config.maxHeight || y < config.minHeight) {
            return null;
        }

        Block target = world.getBlockAt(worldX, y, worldZ);
        return isValidCandidate(target, config) ? target : null;
    }

    @Nullable
    private Block findNetherCandidate(
            World world,
            FurnitureSurfaceDecoratorConfig config,
            int worldX,
            int worldZ
    ) {
        int startY = Math.min(config.maxHeight, world.getMaxHeight() - 1);
        int minY = Math.max(config.minHeight, world.getMinHeight() + 1);

        for (int y = startY; y >= minY; y--) {
            Block target = world.getBlockAt(worldX, y, worldZ);
            if (isValidCandidate(target, config)) {
                return target;
            }
        }

        return null;
    }

    private int resolveNormalPlacementY(
            World world,
            FurnitureSurfaceDecoratorConfig config,
            int worldX,
            int worldZ
    ) {
        HeightMap map = config.allowLiquidPlacement
                ? HeightMap.OCEAN_FLOOR
                : HeightMap.MOTION_BLOCKING_NO_LEAVES;

        return world.getHighestBlockYAt(worldX, worldZ, map) + 1;
    }

    private boolean isValidCandidate(
            Block target,
            FurnitureSurfaceDecoratorConfig config
    ) {
        if (target.getY() > config.maxHeight || target.getY() < config.minHeight) {
            return false;
        }

        Block below = target.getRelative(BlockFace.DOWN);

        if (!config.allowLiquidSurface
                && FurnitureSurfaceDecoratorConfig.isWaterOrLava(
                below.getType())) {
            return false;
        }

        if (!config.isAllowedTarget(target)) {
            return false;
        }

        if (!config.isAllowedSurface(below.getType())) {
            return false;
        }

        return config.isActiveInBiome(target);
    }
}
