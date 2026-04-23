package toutouchien.itemsadderadditions.nms;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import toutouchien.itemsadderadditions.nms.api.INmsBedHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Places a single HEAD-part white bed directly into the server level so
 * that {@link Player#sleep} has a valid bed block to read the facing from.
 * The original block is restored when {@link #removeFakeBed} is called.</p>
 *
 * <p>After placing the bed server-side, a {@code ClientboundBlockUpdatePacket}
 * with {@code AIR} is sent exclusively to the sleeping player so their client
 * never renders the bed. Beds are block entities drawn by {@code BedRenderer}
 * (a BlockEntityRenderer); blockstates model overrides cannot suppress that
 * renderer, making the fake-packet approach the only reliable way to keep the
 * bed invisible client-side while still allowing {@link Player#sleep} to work.</p>
 */
public final class NmsBedHandler_v26_1_2 implements INmsBedHandler {
    /**
     * Stores the block state that existed at a position before we replaced it
     * with a fake bed, keyed by {@link BlockPos} so we can restore it later.
     */
    private final Map<BlockPos, BlockState> originalStates = new ConcurrentHashMap<>();

    private static ServerLevel serverLevel(Location location) {
        return ((CraftWorld) location.getWorld()).getHandle();
    }

    private static BlockPos blockPos(Location location) {
        return new BlockPos(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    /**
     * Converts a Bukkit yaw angle to the nearest cardinal {@link Direction}.
     *
     * <p>Bukkit yaw convention: 0° = South (+Z), 90° = West (−X),
     * 180° = North (−Z), 270° = East (+X).</p>
     */
    private static Direction yawToFacing(float yaw) {
        float n = ((yaw % 360f) + 360f) % 360f;
        if (n < 45f || n >= 315f) return Direction.SOUTH;
        if (n < 135f) return Direction.WEST;
        if (n < 225f) return Direction.NORTH;
        return Direction.EAST;
    }

    @Override
    public void placeFakeBed(Player player, Location location) {
        ServerLevel level = serverLevel(location);
        BlockPos pos = blockPos(location);

        // Persist whatever was there so we can restore it exactly.
        originalStates.put(pos, level.getBlockState(pos));

        BlockState bed = Blocks.WHITE_BED.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING,
                        yawToFacing(location.getYaw()))
                .setValue(BlockStateProperties.BED_PART, BedPart.HEAD)
                .setValue(BlockStateProperties.OCCUPIED, true);

        // Flag 3 = UPDATE_NEIGHBORS | UPDATE_CLIENTS
        level.setBlock(pos, bed, 3);
    }

    @Override
    public void removeFakeBed(Player player, Location location) {
        BlockPos pos = blockPos(location);
        BlockState original = originalStates.remove(pos);
        if (original == null) return; // already removed or never placed

        ServerLevel level = serverLevel(location);
        level.setBlock(pos, original, 3);
    }
}
