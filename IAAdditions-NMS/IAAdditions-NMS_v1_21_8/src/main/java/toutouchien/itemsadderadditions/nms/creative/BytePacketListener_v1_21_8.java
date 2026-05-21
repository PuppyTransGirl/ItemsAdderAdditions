package toutouchien.itemsadderadditions.nms.creative;

import dev.lone.itemsadder.api.CustomStack;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.plugin.Plugin;
import toutouchien.itemsadderadditions.common.logging.Log;

public final class BytePacketListener_v1_21_8 {
    /**
     * Registers the byte-level packet listener on every new player channel.
     *
     * @param plugin the owning plugin
     */
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

        /**
         * Returns the {@link PacketListener_v1_21_8.ChannelDupeHandler} from the pipeline, or {@code null} if absent.
         *
         * @param ctx the channel handler context
         * @return the dupe handler, or {@code null}
         */
        private static PacketListener_v1_21_8.ChannelDupeHandler getDupeHandler(
                ChannelHandlerContext ctx
        ) {
            return (PacketListener_v1_21_8.ChannelDupeHandler) ctx.pipeline()
                    .get("iaadditions_packet_listener");
        }

        /**
         * Returns {@code true} if the channel belongs to a player in the PLAY state.
         *
         * @param ctx the channel handler context
         * @return {@code true} if the connection is in the PLAY phase
         */
        private static boolean isPlay(ChannelHandlerContext ctx) {
            Channel channel = ctx.channel();
            Connection connection = (Connection) channel.pipeline().get("packet_handler");
            return connection != null
                    && connection.getPacketListener() instanceof ServerGamePacketListenerImpl;
        }

        /**
         * Clamps {@code itemCount} to the item's max stack size, promoting it to the full stack when
         * the client sends 64 for an item whose real max exceeds 64.
         *
         * @param nmsItem   the item whose max stack size is used as the upper bound
         * @param itemCount the raw count received from the client
         * @return the adjusted item count
         */
        private static int realItemCount(ItemStack nmsItem, int itemCount) {
            int maxStackSize = nmsItem.getMaxStackSize();

            // if the maxStackSize is lower than 64, set the count to the maxStackSize
            int finalItemCount = Math.min(itemCount, maxStackSize);

            // if the maxStackSize is bigger than 64 and itemCount is equal to 64 set the count to the maxStackSize
            if (maxStackSize > 64 && itemCount == 64)
                finalItemCount = maxStackSize;
            return finalItemCount;
        }

        /**
         * Intercepts incoming bytes and rewrites creative slot packets that carry a custom painting item.
         *
         * @param ctx the channel handler context
         * @param msg the inbound message
         * @throws Exception if the pipeline propagation fails
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (isPlay(ctx) && msg instanceof ByteBuf byteBuf) {
                byteBuf.markReaderIndex();
                boolean handled = false;

                try {
                    handled = tryHandleCreativeSlot(ctx, byteBuf);
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

        /**
         * Parses a raw buffer as a {@code set_creative_mode_slot} (0x37) packet and, if it targets a custom
         * painting item, rewrites it with the base item id and schedules an inventory sync.
         *
         * @param ctx     the channel handler context
         * @param byteBuf the raw inbound buffer (reader index at 0)
         * @return {@code true} if the packet was handled and must not be forwarded as-is
         */
        private boolean tryHandleCreativeSlot(ChannelHandlerContext ctx, ByteBuf byteBuf) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            if (buf.readVarInt() != 0x37) return false;

            short slot = buf.readShort();
            if (slot == -1) return false;

            int itemCount = buf.readVarInt();
            if (itemCount <= 0) return false;

            RegistryAccess.Frozen registryAccess = MinecraftServer.getServer().registryAccess();
            Registry<Item> itemRegistry = registryAccess.lookupOrThrow(Registries.ITEM);
            Registry<DataComponentType<?>> dataComponentRegistry =
                    registryAccess.lookupOrThrow(Registries.DATA_COMPONENT_TYPE);

            int itemId = buf.readVarInt();
            int componentsToAdd = buf.readVarInt();
            int componentsToRemove = buf.readVarInt();

            if (itemId != itemRegistry.getId(Items.PAINTING) || componentsToAdd != 1 || componentsToRemove != 0)
                return false;

            if (buf.readVarInt() != dataComponentRegistry.getId(DataComponents.PAINTING_VARIANT)) return false;

            int dataLength = buf.readVarInt();
            int dataStart = buf.readerIndex();
            int paintingId = VarInt.read(buf.slice(dataStart, dataLength)) - 1;

            PacketListener_v1_21_8.ChannelDupeHandler dupeHandler = getDupeHandler(ctx);
            if (dupeHandler == null) return false;

            CustomStack customItem = PacketListener_v1_21_8.PAINTING_ITEMS.get().get(paintingId);
            if (customItem == null) return false;

            Integer precomputed = PacketListener_v1_21_8.PRECOMPUTED_ITEM_IDS.get().get(paintingId);
            int baseItemId = precomputed != null
                    ? precomputed
                    : itemRegistry.getId(CraftItemStack.asNMSCopy(customItem.getItemStack()).getItem());

            FriendlyByteBuf out = new FriendlyByteBuf(ctx.alloc().buffer());
            out.writeVarInt(0x37);
            out.writeShort(slot);
            out.writeVarInt(1);
            out.writeVarInt(baseItemId);
            out.writeVarInt(0);
            out.writeVarInt(0);

            ctx.fireChannelRead(out);

            ServerPlayer player = dupeHandler.getPlayer(ctx);
            if (player != null) {
                ItemStack nmsItem = CraftItemStack.asNMSCopy(customItem.getItemStack());
                nmsItem.setCount(realItemCount(nmsItem, itemCount));
                player.getBukkitEntity().getScheduler().runDelayed(
                        plugin,
                        t -> {
                            player.inventoryMenu.getSlot(slot).set(nmsItem);
                            player.inventoryMenu.sendAllDataToRemote();
                        },
                        null,
                        1
                );
            }

            return true;
        }
    }
}
