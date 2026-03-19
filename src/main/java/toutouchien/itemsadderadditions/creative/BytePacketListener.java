package toutouchien.itemsadderadditions.creative;

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
import toutouchien.itemsadderadditions.ItemsAdderAdditions;

/**
 * Intercepts the {@code set_creative_mode_slot} (0x37) packet before it is decoded.
 *
 * <p>When a player picks a custom item from the creative Decorations tab, the client
 * sends a painting with a custom {@code painting_variant} component. This handler:
 * <ol>
 *   <li>Identifies that the item is one of our registered painting variants.</li>
 *   <li>Rewrites the packet to send the base material item (no painting component)
 *       so the server doesn't reject an unknown variant.</li>
 *   <li>Schedules a 1-tick task that replaces the base item in the slot with the
 *       full ItemsAdder custom item (including all NBT components), then syncs
 *       the inventory back to the client.</li>
 * </ol>
 *
 * <p>Must be injected <em>after</em> {@link PacketListener#inject()} so that
 * {@code "itemsadder_additions_packet_listener"} is already in the pipeline.
 */
public final class BytePacketListener {
    public static void inject() {
        ChannelInitializeListenerHolder.addListener(
                Key.key("itemsadder_additions", "byte_packet_listener"),
                channel -> channel.pipeline().addBefore(
                        "decoder",
                        "itemsadder_additions_byte_packet_listener",
                        new ByteChannelDupeHandler()
                )
        );
    }

    private static final class ByteChannelDupeHandler extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (isPlay(ctx) && msg instanceof ByteBuf byteBuf) {
                byteBuf.markReaderIndex();
                boolean handled = false;

                try {
                    FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
                    int packetId = buf.readVarInt();

                    if (packetId == 0x37) { // set_creative_mode_slot
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

                                        // Holder<PaintingVariant> is encoded as VarInt(registry_id + 1)
                                        // where 0 means inline/direct. Subtract 1 to get the real registry ID.
                                        int dataLength = buf.readVarInt();
                                        int dataStart = buf.readerIndex();
                                        int paintingId = VarInt.read(buf.slice(dataStart, dataLength)) - 1;

                                        PacketListener.ChannelDupeHandler dupeHandler = getDupeHandler(ctx);
                                        if (dupeHandler != null) {
                                            CustomStack customItem = dupeHandler.paintingItems.get(paintingId);
                                            if (customItem != null) {
                                                // Replace the painting with just the base material so
                                                // the server processes a valid, known item type.
                                                int baseItemId = itemRegistry.getId(
                                                        CraftItemStack.asNMSCopy(customItem.getItemStack()).getItem()
                                                );

                                                ByteBuf newPacket = ctx.alloc().buffer();
                                                FriendlyByteBuf out = new FriendlyByteBuf(newPacket);
                                                out.writeVarInt(0x37);
                                                out.writeShort(slot);
                                                out.writeVarInt(1); // count
                                                out.writeVarInt(baseItemId);
                                                out.writeVarInt(0); // no components to add
                                                out.writeVarInt(0); // no components to remove

                                                ctx.fireChannelRead(newPacket);
                                                handled = true;

                                                // The rewritten packet sets a plain base item in the
                                                // slot. Once the server has processed it, we replace
                                                // it with the full ItemsAdder ItemStack (with all NBT)
                                                // and sync the inventory back to the client.
                                                ServerPlayer player = dupeHandler.getPlayer(ctx);
                                                if (player != null) {
                                                    final int finalSlot = slot;
                                                    final ItemStack nmsItem = CraftItemStack.asNMSCopy(customItem.getItemStack());
                                                    nmsItem.setCount(itemCount);

                                                    player.getBukkitEntity().getScheduler().runDelayed(
                                                            ItemsAdderAdditions.instance(),
                                                            t -> {
                                                                // Set directly via the slot to avoid
                                                                // clobbering the stateId counter.
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
                    ItemsAdderAdditions.instance().getSLF4JLogger().error(
                            "[CreativeMenu] Failed to parse set_creative_mode_slot (0x37)", e
                    );
                }

                // Release the original buffer when we handled it; otherwise reset so
                // the subsequent super.channelRead call can read from the beginning.
                if (handled) {
                    byteBuf.release();
                    return;
                }

                byteBuf.resetReaderIndex();
            }

            super.channelRead(ctx, msg);
        }

        private static PacketListener.ChannelDupeHandler getDupeHandler(ChannelHandlerContext ctx) {
            return (PacketListener.ChannelDupeHandler) ctx.pipeline()
                    .get("itemsadder_additions_packet_listener");
        }

        private static boolean isPlay(ChannelHandlerContext ctx) {
            Channel channel = ctx.channel();
            Connection connection = (Connection) channel.pipeline().get("packet_handler");
            return connection != null
                    && connection.getPacketListener() instanceof ServerGamePacketListenerImpl;
        }
    }
}
