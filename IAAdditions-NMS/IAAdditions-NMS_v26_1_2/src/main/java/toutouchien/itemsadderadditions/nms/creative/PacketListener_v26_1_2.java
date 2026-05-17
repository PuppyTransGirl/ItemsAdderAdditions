package toutouchien.itemsadderadditions.nms.creative;

import dev.lone.itemsadder.api.CustomStack;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@NullMarked
public final class PacketListener_v26_1_2 {
    /**
     * Atomic reference to an immutable snapshot of the painting-variant-ID →
     * CustomStack mapping. Swapped in a single write on each reload so Netty I/O
     * threads always see either the old or the new complete map, never a
     * partially-empty intermediate state.
     */
    static final AtomicReference<Map<Integer, CustomStack>> PAINTING_ITEMS =
            new AtomicReference<>(Map.of());

    /**
     * Parallel map of painting-variant-ID → precomputed NMS item integer ID.
     * Populated during {@link #updateCache} so that {@link BytePacketListener_v26_1_2}
     * never calls {@code CraftItemStack.asNMSCopy} or {@code Registry.getId}
     * on Netty I/O threads.
     */
    static final AtomicReference<Map<Integer, Integer>> PRECOMPUTED_ITEM_IDS =
            new AtomicReference<>(Map.of());

    private PacketListener_v26_1_2() {
        throw new IllegalStateException("Static class");
    }

    /**
     * Registers the {@link ChannelDupeHandler} on every future player connection.
     * Must be called from {@code onEnable} before any player connects.
     *
     * <p>Must be called <em>before</em> {@link toutouchien.itemsadderadditions.nms.creative.BytePacketListener_v26_1_2#inject(Plugin)} so that
     * {@code "iaadditions_packet_listener"} already exists in the pipeline
     * when {@code BytePacketListener_v26_1_2} looks it up.
     */
    public static void inject() {
        ChannelInitializeListenerHolder.addListener(
                Key.key("iaadditions", "packet_listener"),
                channel -> channel.pipeline().addBefore(
                        "decoder",
                        "iaadditions_packet_listener",
                        new ChannelDupeHandler()
                )
        );
    }

    /**
     * Rebuilds the painting-variant-ID → CustomStack lookup from the live registry.
     *
     * <p>On the very first server start the data pack has not been applied yet
     * ({@code painting_variant} is a frozen registry loaded at startup), so no
     * variants will match and the map stays empty. The feature becomes active
     * after the next restart once the data pack entries are in the registry.
     *
     * @param items all currently loaded ItemsAdder items
     */
    public static void updateCache(Collection<CustomStack> items) {
        if (NamespaceUtils.lastCacheDeltaSize() == 0) return;

        var serverAccess = ((CraftServer) Bukkit.getServer()).getServer().registryAccess();
        Registry<PaintingVariant> registry = serverAccess.lookupOrThrow(Registries.PAINTING_VARIANT);
        var itemRegistry = serverAccess.lookupOrThrow(Registries.ITEM);

        Map<Integer, CustomStack> newItems = new HashMap<>();
        Map<Integer, Integer> newIds = new HashMap<>();

        for (CustomStack item : items) {
            Identifier loc = Identifier.fromNamespaceAndPath(
                    "ia_creative",
                    item.getNamespace() + "_" + item.getId()
            );

            registry.get(ResourceKey.create(Registries.PAINTING_VARIANT, loc)).ifPresent(holder -> {
                if (holder.isBound()) {
                    int paintingId = registry.getId(holder.value());
                    newItems.put(paintingId, item);
                    newIds.put(paintingId, itemRegistry.getId(
                            CraftItemStack.asNMSCopy(item.getItemStack()).getItem()));
                }
            });
        }

        PAINTING_ITEMS.set(Map.copyOf(newItems));
        PRECOMPUTED_ITEM_IDS.set(Map.copyOf(newIds));
    }

    static final class ChannelDupeHandler extends ChannelDuplexHandler {
        /**
         * Returns the {@link ServerPlayer} associated with this channel,
         * or {@code null} if the connection has not yet reached the play phase.
         */
        @Nullable
        ServerPlayer getPlayer(ChannelHandlerContext ctx) {
            Connection connection = (Connection) ctx.channel().pipeline().get("packet_handler");
            if (connection == null)
                return null;

            if (connection.getPacketListener() instanceof ServerGamePacketListenerImpl listener)
                return listener.player;

            return null;
        }
    }
}
