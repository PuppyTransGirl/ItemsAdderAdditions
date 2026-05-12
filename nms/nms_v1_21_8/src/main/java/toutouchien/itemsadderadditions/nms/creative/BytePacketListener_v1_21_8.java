package toutouchien.itemsadderadditions.nms.creative;

import dev.lone.itemsadder.api.CustomStack;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import net.kyori.adventure.key.Key;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.plugin.Plugin;
import toutouchien.itemsadderadditions.common.logging.Log;

public final class BytePacketListener_v1_21_8 {
    public static void inject(Plugin plugin) {
        ChannelInitializeListenerHolder.addListener(
                Key.key("iaadditions", "byte_packet_listener"),
                channel -> channel.pipeline().addBefore(
                        "decoder",
                        "iaadditions_byte_packet_listener",
                        new ByteChannelDupeHandler(plugin)
                )
        );
    }

    private static final class ByteChannelDupeHandler extends ChannelDuplexHandler {
        private final Plugin plugin;

        ByteChannelDupeHandler(Plugin plugin) {
            this.plugin = plugin;
        }

        private static PacketListener_v1_21_8.ChannelDupeHandler getDupeHandler(
                ChannelHandlerContext ctx
        ) {
            return (PacketListener_v1_21_8.ChannelDupeHandler) ctx.pipeline()
                    .get("iaadditions_packet_listener");
        }

        private static boolean isPlay(ChannelHandlerContext ctx) {
            Channel channel = ctx.channel();
            Connection connection = (Connection) channel.pipeline().get("packet_handler");
            return connection != null
                    && connection.getPacketListener() instanceof ServerGamePacketListenerImpl;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (isPlay(ctx) && msg instanceof ByteBuf byteBuf) {
                byteBuf.markReaderIndex();
                boolean handled = false;

                try {
                    FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
                    int packetId = buf.readVarInt();

                    if (packetId == 0x37) {
                        short slot = buf.readShort();

                        if (slot != -1) {
                            int itemCount = buf.readVarInt();
                            if (itemCount > 0) {
                                var registryAccess = MinecraftServer.getServer().registryAccess();
                                var itemRegistry = registryAccess.lookupOrThrow(Registries.ITEM);
                                var dataComponentRegistry = registryAccess.lookupOrThrow(Registries.DATA_COMPONENT_TYPE);

                                int itemId = buf.readVarInt();
                                int componentsToAdd = buf.readVarInt();
                                int componentsToRemove = buf.readVarInt();

                                if (itemId == itemRegistry.getId(Items.PAINTING)
                                        && componentsToAdd == 1
                                        && componentsToRemove == 0) {
                                    int componentTypeId = buf.readVarInt();
                                    if (componentTypeId == dataComponentRegistry.getId(DataComponents.PAINTING_VARIANT)) {
                                        int dataLength = buf.readVarInt();
                                        int dataStart = buf.readerIndex();
                                        int paintingId = VarInt.read(buf.slice(dataStart, dataLength)) - 1;

                                        PacketListener_v1_21_8.ChannelDupeHandler dupeHandler = getDupeHandler(ctx);
                                        if (dupeHandler != null) {
                                            CustomStack customItem = PacketListener_v1_21_8.PAINTING_ITEMS.get().get(paintingId);
                                            if (customItem != null) {
                                                // Use precomputed NMS item ID - avoids asNMSCopy + Registry.getId on the Netty I/O thread.
                                                Integer precomputed = PacketListener_v1_21_8.PRECOMPUTED_ITEM_IDS.get().get(paintingId);
                                                int baseItemId = (precomputed != null) ? precomputed
                                                        : itemRegistry.getId(CraftItemStack.asNMSCopy(customItem.getItemStack()).getItem()); // rare stale-state fallback

                                                ByteBuf newPacket = ctx.alloc().buffer();
                                                FriendlyByteBuf out = new FriendlyByteBuf(newPacket);
                                                out.writeVarInt(0x37);
                                                out.writeShort(slot);
                                                out.writeVarInt(1);
                                                out.writeVarInt(baseItemId);
                                                out.writeVarInt(0);
                                                out.writeVarInt(0);

                                                ctx.fireChannelRead(newPacket);
                                                handled = true;

                                                ServerPlayer player = dupeHandler.getPlayer(ctx);
                                                if (player != null) {
                                                    final int finalSlot = slot;
                                                    final ItemStack nmsItem = CraftItemStack.asNMSCopy(customItem.getItemStack());
                                                    nmsItem.setCount(itemCount);

                                                    player.getBukkitEntity().getScheduler().runDelayed(
                                                            plugin,
                                                            t -> {
                                                                player.inventoryMenu.getSlot(finalSlot).set(nmsItem);
                                                                player.inventoryMenu.sendAllDataToRemote();
                                                            },
                                                            null,
                                                            1
                                                    );
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.error("CreativeMenu", "Failed to parse set_creative_mode_slot (0x37)", e);
                }

                if (handled) {
                    byteBuf.release();
                    return;
                }

                byteBuf.resetReaderIndex();
            }

            super.channelRead(ctx, msg);
        }
    }
}
