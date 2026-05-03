package toutouchien.itemsadderadditions.worldgen;

import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.generator.BlockPopulator;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * {@link BlockPopulator} that spawns ItemsAdder furniture during chunk
 * generation.
 */
@NullMarked
public final class FurniturePopulatorBehaviour extends BlockPopulator {

    /**
     * All live instances, one per world.
     */
    static final List<FurniturePopulatorBehaviour> INSTANCES = new ArrayList<>();
    private static final int CHUNK_SIZE = 16;
    private static final int MAX_ATTEMPTS_MULTIPLIER = 8;
    private static final String TAG = "FurniturePopulator";

    private final World world;
    private final List<FurniturePopulatorConfig> configs = new ArrayList<>();

    private FurniturePopulatorBehaviour(World world) {
        this.world = world;
    }

    public static synchronized void register(
            FurniturePopulatorConfig config,
            World world
    ) {
        if (!config.isActiveInWorld(world)) {
            return;
        }

        FurniturePopulatorBehaviour behaviour = INSTANCES.stream()
                .filter(instance -> instance.world.equals(world))
                .findFirst()
                .orElseGet(() -> createFor(world));

        if (!behaviour.configs.contains(config)) {
            behaviour.configs.add(config);
        }
    }

    public static void onWorldLoad(World world) {
        for (FurniturePopulatorConfig config :
                FurniturePopulatorLoader.REGISTRY.values()) {
            register(config, world);
        }
    }

    public static synchronized void unregisterAll() {
        for (FurniturePopulatorBehaviour behaviour : INSTANCES) {
            behaviour.world.getPopulators().remove(behaviour);
        }
        INSTANCES.clear();
    }

    private static FurniturePopulatorBehaviour createFor(World world) {
        FurniturePopulatorBehaviour behaviour =
                new FurniturePopulatorBehaviour(world);
        INSTANCES.add(behaviour);
        world.getPopulators().add(behaviour);
        Log.info(TAG, "Registered populator for world '" + world.getName() + "'");
        return behaviour;
    }

    private static int clamp(int value) {
        return Math.min(CHUNK_SIZE - 1, Math.max(0, value));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void populate(World world, Random random, Chunk chunk) {
        for (FurniturePopulatorConfig config : configs) {
            populateConfig(random, chunk, config);
        }
    }

    private void populateConfig(
            Random random,
            Chunk chunk,
            FurniturePopulatorConfig config
    ) {
        if (config.chunkChance != -1
                && random.nextDouble() * 100.0 > config.chunkChance) {
            return;
        }

        int anchorX = random.nextInt(CHUNK_SIZE);
        int anchorZ = random.nextInt(CHUNK_SIZE);

        for (int vein = 0; vein < config.chunkVeins; vein++) {
            spawnVein(random, chunk, config, anchorX, anchorZ);
        }
    }

    private void spawnVein(
            Random random,
            Chunk chunk,
            FurniturePopulatorConfig config,
            int anchorX,
            int anchorZ
    ) {
        int placed = 0;
        int maxAttempts = config.veinBlocks * MAX_ATTEMPTS_MULTIPLIER;

        for (int attempt = 0; attempt < maxAttempts && placed < config.veinBlocks;
             attempt++) {
            int spread = Math.max(config.veinBlocks, 2);
            int x = clamp(anchorX + random.nextInt(spread) - spread / 2);
            int z = clamp(anchorZ + random.nextInt(spread) - spread / 2);

            int worldX = chunk.getX() * CHUNK_SIZE + x;
            int worldZ = chunk.getZ() * CHUNK_SIZE + z;
            int y = chunk.getWorld().getHighestBlockYAt(
                    worldX,
                    worldZ,
                    HeightMap.MOTION_BLOCKING_NO_LEAVES
            ) + 1;

            if (y > config.maxHeight || y < config.minHeight) {
                continue;
            }

            Block target = chunk.getBlock(x, y, z);
            Block below = target.getRelative(BlockFace.DOWN);

            if (!config.isAllowedTarget(target)) {
                continue;
            }
            if (!config.isAllowedSurface(below.getType())) {
                continue;
            }
            if (!config.isActiveInBiome(target)) {
                continue;
            }

            if (!config.trySpawn(target)) {
                continue;
            }

            placed++;
            Log.debug(TAG, "Spawned " + config.furnitureId + " at "
                    + target.getX() + "," + target.getY() + ","
                    + target.getZ() + " in world "
                    + chunk.getWorld().getName());
        }
    }
}
