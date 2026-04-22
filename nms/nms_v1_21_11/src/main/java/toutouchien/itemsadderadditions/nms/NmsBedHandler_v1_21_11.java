package toutouchien.itemsadderadditions.nms;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import toutouchien.itemsadderadditions.nms.api.INmsBedHandler;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * 1.21.x NMS implementation of {@link INmsBedHandler}.
 */
public final class NmsBedHandler_v1_21_11 implements INmsBedHandler {

    private static final EntityDataAccessor<Pose> DATA_POSE;

    static {
        try {
            // Using privateLookupIn to access the static field DATA_POSE
            // This bypasses issues where the field might be protected or inaccessible
            // depending on the specific remapping/version of the server.
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                    Entity.class,
                    MethodHandles.lookup()
            );
            DATA_POSE = (EntityDataAccessor<Pose>) lookup
                    .findStaticGetter(Entity.class, "DATA_POSE", EntityDataAccessor.class)
                    .invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize DATA_POSE via MethodHandles", e);
        }
    }

    private static ServerPlayer nms(org.bukkit.entity.Player player) {
        return ((CraftPlayer) player).getHandle();
    }

    private static void broadcastEntityData(ServerPlayer sp) {
        List<SynchedEntityData.DataValue<?>> dirtyValues = sp.getEntityData().getNonDefaultValues();
        if (dirtyValues == null || dirtyValues.isEmpty()) return;

        ClientboundSetEntityDataPacket packet =
                new ClientboundSetEntityDataPacket(sp.getId(), dirtyValues);

        sp.level().getChunkSource().sendToTrackingPlayers(sp, packet);
    }

    private static void sendFakeBedBlock(ServerPlayer sp, BlockPos bedPos, float yaw) {
        BlockState fakeState = Blocks.WHITE_BED.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, yawToFacing(yaw))
                .setValue(BlockStateProperties.BED_PART, BedPart.HEAD)
                .setValue(BlockStateProperties.OCCUPIED, true);

        // sendToTrackingPlayers excludes sp itself — sleeping player is unaffected
        sp.level()
                .getChunkSource()
                .sendToTrackingPlayers(sp, new ClientboundBlockUpdatePacket(bedPos.atY(-64), fakeState));
    }

    private static void revertBedBlock(ServerPlayer sp, BlockPos bedPos) {
        ServerLevel level = sp.level();
        level.getChunkSource()
                .sendToTrackingPlayers(
                        sp,
                        new ClientboundBlockUpdatePacket(bedPos, level.getBlockState(bedPos.atY(-64)))
                );
    }

    @Override
    public void forceWake(org.bukkit.entity.Player player) {
        ServerPlayer sp = nms(player);
        sp.stopSleepInBed(true, false);
    }

    private static net.minecraft.core.Direction yawToFacing(float yaw) {
        float n = ((yaw % 360) + 360) % 360;
        if (n < 45 || n >= 315) return net.minecraft.core.Direction.SOUTH;
        if (n < 135) return net.minecraft.core.Direction.WEST;
        if (n < 225) return net.minecraft.core.Direction.NORTH;
        return net.minecraft.core.Direction.EAST;
    }

    @Override
    public void startDecorativeSleep(org.bukkit.entity.Player player, int x, int y, int z) {
        ServerPlayer sp = nms(player);
        BlockPos bedPos = new BlockPos(x, y, z);

        sp.setSleepingPos(bedPos);
        sendFakeBedBlock(sp, new BlockPos(x, y, z), sp.getBukkitYaw());

/*        // Use our reflected DATA_POSE instead of direct field access
        sp.getEntityData().set(DATA_POSE, Pose.SLEEPING);

        broadcastEntityData(sp);*/
    }

    @Override
    public void stopDecorativeSleep(org.bukkit.entity.Player player) {
        ServerPlayer sp = nms(player);

        sp.clearSleepingPos();

/*        // Use our reflected DATA_POSE instead of direct field access
        sp.getEntityData().set(DATA_POSE, Pose.STANDING);

        broadcastEntityData(sp);*/
    }
}
