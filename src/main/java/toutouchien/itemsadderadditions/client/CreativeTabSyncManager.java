package toutouchien.itemsadderadditions.client;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CreativeTabSyncManager implements PluginMessageListener, Listener {
    private static final String TAG = "ClientCreativeSync";
    private static final long REGISTRY_SEND_DELAY_TICKS = 20L;

    private final ItemsAdderAdditions plugin;
    private final Map<UUID, ClientSession> sessions = new ConcurrentHashMap<>();
    private volatile CreativeRegistrySnapshot snapshot;

    public CreativeTabSyncManager(ItemsAdderAdditions plugin) {
        this.plugin = plugin;
    }

    private static byte[] encodeRegistryChunk(
            UUID sessionId,
            UUID registryId,
            int chunkIndex,
            int chunkCount,
            int totalSize,
            byte[] chunk
    ) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(chunk.length + 64);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(CreativeSyncProtocol.S2C_REGISTRY_CHUNK);
            WireIO.writeUuid(out, sessionId);
            WireIO.writeUuid(out, registryId);
            out.writeInt(chunkIndex);
            out.writeInt(chunkCount);
            out.writeInt(totalSize);
            out.writeInt(chunk.length);
            out.write(chunk);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to encode registry chunk", ex);
        }
    }

    private static byte[] encodeRegistryComplete(UUID sessionId, UUID registryId, int chunkCount, int totalSize) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(64);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(CreativeSyncProtocol.S2C_REGISTRY_COMPLETE);
            WireIO.writeUuid(out, sessionId);
            WireIO.writeUuid(out, registryId);
            out.writeInt(chunkCount);
            out.writeInt(totalSize);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to encode registry complete", ex);
        }
    }

    public void register() {
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CreativeSyncProtocol.HANDSHAKE_CHANNEL, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CreativeSyncProtocol.HANDSHAKE_CHANNEL);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CreativeSyncProtocol.REGISTRY_CHANNEL);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void shutdown() {
        sessions.clear();
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CreativeSyncProtocol.HANDSHAKE_CHANNEL, this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CreativeSyncProtocol.HANDSHAKE_CHANNEL);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CreativeSyncProtocol.REGISTRY_CHANNEL);
    }

    public void rebuildRegistrySnapshot(Collection<CustomStack> allItems) {
        if (!plugin.getConfig().getBoolean("features.client_creative_tab_sync", true)) {
            snapshot = null;
            Log.info(TAG, "Client creative tab sync is disabled in config.yml.");
            return;
        }
        snapshot = CreativeRegistryBuilder.build(allItems);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CreativeSyncProtocol.HANDSHAKE_CHANNEL.equals(channel)) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            byte packetId = in.readByte();
            if (packetId != CreativeSyncProtocol.C2S_HANDSHAKE) {
                Log.warn(TAG, "Ignoring invalid handshake packet {} from {}", packetId, player.getName());
                return;
            }

            int protocol = in.readInt();
            String modVersion = WireIO.readString(in);
            handleHandshake(player, protocol, modVersion);
        } catch (IOException ex) {
            Log.warn(TAG, "Failed to decode handshake from {}", player.getName(), ex);
            sendHandshakeResponse(player, false, "Malformed handshake", null);
        }
    }

    private void handleHandshake(Player player, int protocol, String modVersion) {
        if (!plugin.getConfig().getBoolean("features.client_creative_tab_sync", true)) {
            Log.info(TAG, "Handshake rejected for {} (feature disabled)", player.getName());
            sendHandshakeResponse(player, false, "Client creative tab sync is disabled on this server", null);
            return;
        }

        if (protocol != CreativeSyncProtocol.PROTOCOL_VERSION) {
            Log.info(TAG, "Handshake rejected for {} (client protocol {}, server protocol {}, mod {})",
                    player.getName(), protocol, CreativeSyncProtocol.PROTOCOL_VERSION, modVersion);
            sendHandshakeResponse(player, false, "Unsupported protocol " + protocol, null);
            return;
        }

        UUID playerId = player.getUniqueId();
        ClientSession session = sessions.computeIfAbsent(playerId, ignored -> new ClientSession(UUID.randomUUID()));
        session.accepted = true;
        session.modVersion = modVersion;

        Log.info(TAG, "Handshake accepted for {} (mod={}, protocol={}, session={})",
                player.getName(), modVersion, protocol, session.sessionId);
        sendHandshakeResponse(player, true, "Accepted", session.sessionId);

        if (session.registrySent || session.sendScheduled) {
            Log.debug(TAG, "Skipping duplicate registry send for {} (sent={}, scheduled={})",
                    player.getName(), session.registrySent, session.sendScheduled);
            return;
        }

        session.sendScheduled = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> sendRegistryIfStillValid(player.getUniqueId(), session.sessionId), REGISTRY_SEND_DELAY_TICKS);
    }

    private void sendRegistryIfStillValid(UUID playerId, UUID sessionId) {
        Player player = Bukkit.getPlayer(playerId);
        ClientSession session = sessions.get(playerId);
        if (player == null || !player.isOnline() || session == null || !session.accepted || !session.sessionId.equals(sessionId)) {
            return;
        }

        session.sendScheduled = false;
        if (session.registrySent) {
            Log.debug(TAG, "Registry already sent to {}; ignoring delayed duplicate task", player.getName());
            return;
        }

        CreativeRegistrySnapshot currentSnapshot = snapshot;
        if (currentSnapshot == null) {
            List<CustomStack> allItems = ItemsAdder.getAllItems();
            currentSnapshot = CreativeRegistryBuilder.build(allItems);
            snapshot = currentSnapshot;
        }

        UUID registryId = UUID.randomUUID();
        byte[] registryBytes = currentSnapshot.encoded();
        int chunkCount = Math.max(1, (int) Math.ceil(registryBytes.length / (double) CreativeSyncProtocol.MAX_CHUNK_DATA_BYTES));

        Log.info(TAG, "Sending creative registry to {}: categories={}, items={}, totalSerializedSize={} bytes, chunks={}",
                player.getName(), currentSnapshot.categories().size(), currentSnapshot.itemCount(), registryBytes.length, chunkCount);

        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            int start = chunkIndex * CreativeSyncProtocol.MAX_CHUNK_DATA_BYTES;
            int length = Math.min(CreativeSyncProtocol.MAX_CHUNK_DATA_BYTES, registryBytes.length - start);
            byte[] chunk = new byte[length];
            System.arraycopy(registryBytes, start, chunk, 0, length);
            player.sendPluginMessage(plugin, CreativeSyncProtocol.REGISTRY_CHANNEL,
                    encodeRegistryChunk(sessionId, registryId, chunkIndex, chunkCount, registryBytes.length, chunk));
        }
        player.sendPluginMessage(plugin, CreativeSyncProtocol.REGISTRY_CHANNEL,
                encodeRegistryComplete(sessionId, registryId, chunkCount, registryBytes.length));

        session.registrySent = true;
        Log.info(TAG, "Creative registry sent to {} ({} chunks)", player.getName(), chunkCount);
    }

    private void sendHandshakeResponse(Player player, boolean accepted, String reason, UUID sessionId) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(CreativeSyncProtocol.S2C_HANDSHAKE_RESPONSE);
            out.writeByte(accepted ? CreativeSyncProtocol.HANDSHAKE_ACCEPTED : CreativeSyncProtocol.HANDSHAKE_REJECTED);
            out.writeInt(CreativeSyncProtocol.PROTOCOL_VERSION);
            WireIO.writeString(out, reason);
            WireIO.writeUuid(out, sessionId == null ? new UUID(0L, 0L) : sessionId);
            out.flush();
            player.sendPluginMessage(plugin, CreativeSyncProtocol.HANDSHAKE_CHANNEL, bytes.toByteArray());
        } catch (IOException ex) {
            Log.warn(TAG, "Failed to encode handshake response for {}", player.getName(), ex);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
        Log.debug(TAG, "Cleared client sync state for {}", event.getPlayer().getName());
    }

    private static final class ClientSession {
        final UUID sessionId;
        boolean accepted;
        boolean sendScheduled;
        boolean registrySent;
        String modVersion = "unknown";

        ClientSession(UUID sessionId) {
            this.sessionId = sessionId;
        }
    }
}
