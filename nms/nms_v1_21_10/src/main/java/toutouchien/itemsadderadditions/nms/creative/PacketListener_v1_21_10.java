package toutouchien.itemsadderadditions.nms.creative;

import dev.lone.itemsadder.api.CustomStack;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.decoration.PaintingVariant;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
public final class PacketListener_v1_21_10 {
    /**
     * Painting variant registry integer ID -> ItemsAdder CustomStack.
     *
     * <p>A {@link ConcurrentHashMap} so that {@link ChannelDupeHandler} instances
     * (which run on Netty I/O threads) can safely read from it while the main thread
     * rebuilds it on reload. The reference never changes - only the contents do -
     * so every handler instance automatically sees the latest mapping.
     */
    static final Map<Integer, CustomStack> PAINTING_ITEMS = new ConcurrentHashMap<>();

    private PacketListener_v1_21_10() {
        throw new IllegalStateException("Static class");
    }

    /**
     * Registers the {@link ChannelDupeHandler} on every future player connection.
     * Must be called from {@code onEnable} before any player connects.
     *
     * <p>Must be called <em>before</em> {@link BytePacketListener_v1_21_10#inject(Plugin)} so that
     * {@code "iaadditions_packet_listener"} already exists in the pipeline
     * when {@code BytePacketListener_v1_21_10} looks it up.
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
     * Rebuilds the painting-variant-ID -> CustomStack lookup from the live registry.
     *
     * <p>On the very first server start the data pack has not been applied yet
     * ({@code painting_variant} is a frozen registry loaded at startup), so no
     * variants will match and the map stays empty. The feature becomes active
     * after the next restart once the data pack entries are in the registry.
     *
     * @param items all currently loaded ItemsAdder items
     */
    public static void updateCache(Collection<CustomStack> items) {
        Registry<PaintingVariant> registry = ((CraftServer) Bukkit.getServer()).getServer()
                .registryAccess()
                .lookupOrThrow(Registries.PAINTING_VARIANT);

        Map<Integer, CustomStack> newMap = new HashMap<>();

        for (CustomStack item : items) {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                    "ia_creative",
                    item.getNamespace() + "_" + item.getId()
            );

            // Use .get() instead of .getHolder()
            registry.get(ResourceKey.create(Registries.PAINTING_VARIANT, loc)).ifPresent(holder -> {
                if (holder.isBound()) {
                    int id = registry.getId(holder.value());
                    newMap.put(id, item);
                }
            });
        }

        PAINTING_ITEMS.clear();
        PAINTING_ITEMS.putAll(newMap);
    }

    static final class ChannelDupeHandler extends ChannelDuplexHandler {
        /**
         * Direct reference to the shared map. Because the map object itself never
         * changes (only its contents), all handler instances reflect reloads instantly.
         */
        final Map<Integer, CustomStack> paintingItems = PAINTING_ITEMS;

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
