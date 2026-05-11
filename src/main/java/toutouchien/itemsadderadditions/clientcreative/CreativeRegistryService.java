package toutouchien.itemsadderadditions.clientcreative;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CreativeRegistryService {
    private final Plugin plugin;
    private final CreativeRegistryBuilder builder;

    private final Map<UUID, Integer> lastSentHashByPlayer = new HashMap<>();

    private OptimizedCreativeRegistry currentRegistry;
    private byte[] currentPayload;
    private int currentHash;

    public CreativeRegistryService(Plugin plugin, CreativeRegistryBuilder builder) {
        this.plugin = plugin;
        this.builder = builder;
    }

    public void rebuild() {
        OptimizedCreativeRegistry rebuilt = builder.build();

        if (currentRegistry != null && rebuilt.hash() == currentHash) {
            return;
        }

        this.currentRegistry = rebuilt;
        this.currentHash = rebuilt.hash();
        this.currentPayload = CreativeRegistryWireCodec.encode(rebuilt);

        plugin.getLogger().info("[ClientCreativeSync] Registry rebuilt: tabs="
                + rebuilt.tabs().size()
                + ", items=" + rebuilt.items().size()
                + ", bytes=" + currentPayload.length
                + ", hash=" + currentHash);
    }

    public void rebuildAndBroadcast() {
        rebuild();

        for (Player player : Bukkit.getOnlinePlayers()) {
            sendIfChanged(player);
        }
    }

    public void sendIfChanged(Player player) {
        if (player == null || !player.isOnline() || currentPayload == null) {
            return;
        }

        Integer lastSent = lastSentHashByPlayer.get(player.getUniqueId());
        if (lastSent != null && lastSent == currentHash) {
            return;
        }

        sendRegistryPayload(player, currentHash, currentPayload);
        lastSentHashByPlayer.put(player.getUniqueId(), currentHash);
    }

    public void clearPlayer(Player player) {
        if (player != null) {
            lastSentHashByPlayer.remove(player.getUniqueId());
        }
    }

    private void sendRegistryPayload(Player player, int hash, byte[] payload) {
        /*
         * Wire this into your existing chunk sender.
         *
         * Suggested:
         * - Send an accept/status packet first containing the hash.
         * - Split `payload` into 24 KiB chunks.
         * - Include session UUID, hash, chunkIndex, chunkCount, uncompressed size.
         * - Client ignores chunks when it already has the same hash.
         */
        throw new UnsupportedOperationException("Wire this to your current ClientCreativeSync chunk sender");
    }
}
