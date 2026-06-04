package toutouchien.itemsadderadditions.feature.behaviour.builtin.contact;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomEntity;
import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.integration.itemsadder.CustomEntities;

import java.util.Collection;
import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * Determines whether a player is in contact with a specific custom block or furniture.
 */
@NullMarked
public final class ContactDetector {
    public static final double HALF_PLAYER_WIDTH = 0.3;
    public static final double HALF_BLOCK_SIZE = 0.5;
    public static final double ENTITY_SEARCH_RADIUS = HALF_BLOCK_SIZE + HALF_PLAYER_WIDTH + 1.0;
    public static final double THRESHOLD = HALF_BLOCK_SIZE + HALF_PLAYER_WIDTH + 0.01;
    private static final int PLAYER_HEIGHT = 2;

    private final String namespacedID;
    private final EnumSet<BlockFace> activeFaces;
    private final boolean topFaceActive;

    public ContactDetector(
            String namespacedID,
            EnumSet<BlockFace> activeFaces,
            boolean topFaceActive
    ) {
        this.namespacedID = namespacedID;
        this.activeFaces = activeFaces;
        this.topFaceActive = topFaceActive;
    }

    public boolean isTouchingBlock(Player player) {
        return touchingBlockLocation(player) != null;
    }

    public boolean isTouchingComplexFurniture(Player player) {
        return touchingComplexFurnitureLocation(player) != null;
    }

    public @Nullable Location touchingBlockLocation(Player player) {
        return overlapLocation(player, block -> {
            CustomBlock cb = CustomBlock.byAlreadyPlaced(block);
            if (cb != null && namespacedID.equals(cb.getNamespacedID())) return true;
            CustomFurniture cf = CustomFurniture.byAlreadySpawned(block);
            return cf != null && namespacedID.equals(cf.getNamespacedID());
        });
    }

    public @Nullable Location touchingComplexFurnitureLocation(Player player) {
        Collection<Entity> nearby = player.getWorld().getNearbyEntities(
                player.getLocation(),
                ENTITY_SEARCH_RADIUS, ENTITY_SEARCH_RADIUS, ENTITY_SEARCH_RADIUS
        );

        for (Entity entity : nearby) {
            CustomEntity ce = CustomEntities.byAlreadySpawned(entity);
            if (ce == null || !namespacedID.equals(ce.getNamespacedID())) continue;
            Block barrierBlock = entity.getLocation().getBlock();
            if (overlapsAny(player, barrierBlock::equals)) return entity.getLocation();
        }

        return null;
    }

    private boolean overlapsAny(Player player, Predicate<Block> isTarget) {
        return overlapLocation(player, isTarget) != null;
    }

    private @Nullable Location overlapLocation(Player player, Predicate<Block> isTarget) {
        Location loc = player.getLocation();
        double px = loc.getX();
        double pz = loc.getZ();
        int footY = loc.getBlockY();
        int bx0 = loc.getBlockX();
        int bz0 = loc.getBlockZ();

        for (int dy = 0; dy < PLAYER_HEIGHT; dy++) {
            Block origin = player.getWorld().getBlockAt(bx0, footY + dy, bz0);
            if (isTarget.test(origin)) return origin.getLocation();

            if (dy == 0 && topFaceActive) {
                Block below = origin.getRelative(BlockFace.DOWN);
                if (isTarget.test(below)) return below.getLocation();
            }

            for (BlockFace face : activeFaces) {
                if (face == BlockFace.UP) continue;

                Block neighbour = origin.getRelative(face);
                if (!isTarget.test(neighbour)) continue;

                double cx = neighbour.getX() + 0.5;
                double cz = neighbour.getZ() + 0.5;
                if (Math.abs(px - cx) <= THRESHOLD && Math.abs(pz - cz) <= THRESHOLD)
                    return neighbour.getLocation();
            }
        }

        return null;
    }
}
