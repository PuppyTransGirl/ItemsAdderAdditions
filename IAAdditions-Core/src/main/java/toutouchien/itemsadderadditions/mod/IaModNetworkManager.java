package toutouchien.itemsadderadditions.mod;

import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@NullMarked
public final class IaModNetworkManager implements PluginMessageListener, Listener {

    // Three channels, matching the typed payloads on the mod side.
    // The mod registers each payload with its own Identifier via Fabric's
    // PayloadTypeRegistry / NeoForge's PayloadRegistrar — so bytes sent here
    // must match each StreamCodec's wire format exactly.
    private static final String CHANNEL_HELLO         = "ia:handshake_hello";
    private static final String CHANNEL_CAPABILITIES  = "ia:capabilities";
    private static final String CHANNEL_CREATIVE_TABS = "ia:creative_tabs";

    private static final int PROTOCOL_VERSION  = 1;
    private static final int RATE_LIMIT_MS     = 5_000;
    private static final int MAX_ITEMS_PER_TAB = 1024;
    private static final int MAX_PACKET_SIZE   = 512 * 1024;

    private static final int FLAG_CREATIVE_TABS = 1;
    private static final int FLAG_NOTEBLOCK_FIX = 2;

    private static final String PERMISSION_IA_USER = "ia.user.ia";

    private final JavaPlugin plugin;
    private final Map<UUID, ModPlayerSession> sessions = Collections.synchronizedMap(new HashMap<>());

    public IaModNetworkManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        // C2S: client → server hello
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_HELLO, this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_HELLO);

        // S2C: server → client
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_CAPABILITIES);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_CREATIVE_TABS);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Log.info("ModNetwork", "Registered IA Mod channels: {} (in), {} (out), {} (out).",
                CHANNEL_HELLO, CHANNEL_CAPABILITIES, CHANNEL_CREATIVE_TABS);
    }

    public void unregister() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL_HELLO);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL_HELLO);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL_CAPABILITIES);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL_CREATIVE_TABS);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] rawMessage) {
        if (!CHANNEL_HELLO.equals(channel)) return;

        if (rawMessage.length > 256) {
            Log.warn("ModNetwork", "Oversized hello from {} ({} bytes) — ignored.", player.getName(), rawMessage.length);
            return;
        }
        if (rawMessage.length == 0) return;

        try {
            IaReader reader     = new IaReader(rawMessage);
            int protocolVersion = reader.readVarInt();
            String modVersion   = reader.readString(64);
            handleHello(player, protocolVersion, modVersion);
        } catch (Exception e) {
            Log.warn("ModNetwork", "Malformed hello from {}: {}", player.getName(), e.getMessage());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    public void resendTabsToAll() {
        List<CustomStack> all = ItemsAdder.getAllItems();
        for (Map.Entry<UUID, ModPlayerSession> entry : new ArrayList<>(sessions.entrySet())) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null && entry.getValue().capabilitiesSent()) {
                if (player.hasPermission(PERMISSION_IA_USER)) {
                    sendCreativeTabs(player, all);
                }
            }
        }
    }

    // ── private ────────────────────────────────────────────────────────────────

    private void handleHello(Player player, int protocolVersion, String modVersion) {
        UUID uuid = player.getUniqueId();
        ModPlayerSession existing = sessions.get(uuid);

        if (existing != null &&
                System.currentTimeMillis() - existing.helloTimestamp() < RATE_LIMIT_MS) {
            Log.debug("ModNetwork", "Rate-limited hello from {}.", player.getName());
            return;
        }

        if (sessions.size() >= plugin.getServer().getMaxPlayers() + 10) {
            Log.warn("ModNetwork", "Sessions map overflow — dropping hello from {}.", player.getName());
            return;
        }

        Log.info("ModNetwork", "Hello from {} — mod v{}, client protocol {}.",
                player.getName(), modVersion, protocolVersion);

        ModPlayerSession session = new ModPlayerSession(protocolVersion, modVersion);
        sessions.put(uuid, session);

        if (protocolVersion != PROTOCOL_VERSION) {
            Log.info("ModNetwork", "Protocol mismatch for {}: client={} server={}. Sending empty capabilities.",
                    player.getName(), protocolVersion, PROTOCOL_VERSION);
            sendCapabilities(player, session, 0);
            return;
        }

        int flags = FLAG_NOTEBLOCK_FIX;
        boolean hasPerm = player.hasPermission(PERMISSION_IA_USER);
        Log.info("ModNetwork", "Permission {}: {} → {}",
                PERMISSION_IA_USER, player.getName(), hasPerm ? "granted" : "denied");
        if (hasPerm) {
            flags |= FLAG_CREATIVE_TABS;
        }

        Log.info("ModNetwork", "Sending capabilities to {} — flags=0x{}.", player.getName(), Integer.toHexString(flags));
        sendCapabilities(player, session, flags);
        session.markCapabilitiesSent();

        if ((flags & FLAG_CREATIVE_TABS) != 0) {
            sendCreativeTabs(player, ItemsAdder.getAllItems());
        }
    }

    private void sendCapabilities(Player player, ModPlayerSession session, int featureBitmask) {
        try {
            // CapabilitiesPayload.CODEC: INT, INT, INT
            IaWriter w = new IaWriter();
            w.writeInt(PROTOCOL_VERSION);
            w.writeInt(featureBitmask);
            w.writeInt(session.nonce());
            player.sendPluginMessage(plugin, CHANNEL_CAPABILITIES, w.toBytes());
        } catch (IOException e) {
            Log.warn("ModNetwork", "Failed to send capabilities to {}: {}", player.getName(), e.getMessage());
        }
    }

    private void sendCreativeTabs(Player player, List<CustomStack> all) {
        Set<String> furnitureIds = CustomFurniture.getNamespacedIdsInRegistry();

        List<CustomStack> items     = new ArrayList<>();
        List<CustomStack> blocks    = new ArrayList<>();
        List<CustomStack> furniture = new ArrayList<>();
        List<CustomStack> complex   = new ArrayList<>();

        for (CustomStack cs : all) {
            if (shouldSkip(cs)) continue;
            if (cs.isBlock()) {
                blocks.add(cs);
            } else if (furnitureIds.contains(cs.getNamespacedID())) {
                (isComplexFurniture(cs) ? complex : furniture).add(cs);
            } else {
                items.add(cs);
            }
        }

        Log.info("ModNetwork", "Creative tabs for {}: items={} blocks={} furniture={} complex={}.",
                player.getName(), items.size(), blocks.size(), furniture.size(), complex.size());

        try {
            // CreativeTabsPayload.CODEC: 4 × list<IaItemData>
            IaWriter w = new IaWriter();
            writeTab(w, items);
            writeTab(w, blocks);
            writeTab(w, furniture);
            writeTab(w, complex);

            byte[] bytes = w.toBytes();
            Log.info("ModNetwork", "Creative tabs packet size for {}: {} bytes.", player.getName(), bytes.length);
            if (bytes.length > MAX_PACKET_SIZE) {
                Log.warn("ModNetwork", "Creative tabs payload for {} exceeds size cap ({} bytes) — skipping.",
                        player.getName(), bytes.length);
                return;
            }
            player.sendPluginMessage(plugin, CHANNEL_CREATIVE_TABS, bytes);
            Log.info("ModNetwork", "Creative tabs sent to {}.", player.getName());
        } catch (IOException e) {
            Log.warn("ModNetwork", "Failed to send creative tabs to {}: {}", player.getName(), e.getMessage());
        }
    }

    private static void writeTab(IaWriter w, List<CustomStack> stacks) throws IOException {
        int count = Math.min(stacks.size(), MAX_ITEMS_PER_TAB);
        w.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            CustomStack cs  = stacks.get(i);
            org.bukkit.inventory.ItemStack bukkit = cs.getItemStack();
            ItemMeta meta   = bukkit.getItemMeta();
            String material  = bukkit.getType().getKey().toString();
            int cmd          = (meta != null && meta.hasCustomModelData()) ? meta.getCustomModelData() : 0;
            String itemModel = (meta != null && meta.hasItemModel() && meta.getItemModel() != null)
                    ? meta.getItemModel().toString() : "";
            String name      = (meta != null && meta.hasDisplayName()) ? stripColors(meta.getDisplayName()) : cs.getId();
            w.writeString(material, 64);
            w.writeVarInt(cmd);
            w.writeString(itemModel, 128);
            w.writeString(name, 256);
        }
    }

    private static final String[] DIRECTIONAL_SUFFIXES = {"_north", "_south", "_east", "_west", "_up", "_down"};

    // Mirrors CreativeRegistryReloader#shouldSkipCreativeItem / CreativeItemModelResolver#shouldSkip.
    private static boolean shouldSkip(CustomStack cs) {
        String path = "items." + cs.getId() + ".";
        if (cs.getConfig().getBoolean(path + "template", false)
                || cs.getConfig().getBoolean(path + "hide_from_inventory", false)) {
            return true;
        }

        // Drop directional variants when the un-suffixed base item exists — they
        // share the base's material/CMD/name and would collide on the client tab.
        String fullId = cs.getNamespacedID();
        for (String direction : DIRECTIONAL_SUFFIXES) {
            if (fullId.endsWith(direction)) {
                String baseFullId = fullId.substring(0, fullId.length() - direction.length());
                return CustomStack.isInRegistry(baseFullId);
            }
        }

        return false;
    }

    private static boolean isComplexFurniture(CustomStack cs) {
        String path = "items." + cs.getId() + ".behaviours.furniture.sub_entities";
        return cs.getConfig().contains(path) && !cs.getConfig().getList(path, Collections.emptyList()).isEmpty();
    }

    private static String stripColors(String s) {
        return s.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    // ── inner IO helpers ───────────────────────────────────────────────────────

    private static final class IaWriter {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        void writeVarInt(int value) {
            while ((value & ~0x7F) != 0) {
                out.write((value & 0x7F) | 0x80);
                value >>>= 7;
            }
            out.write(value);
        }

        void writeInt(int value) throws IOException {
            out.write((value >>> 24) & 0xFF);
            out.write((value >>> 16) & 0xFF);
            out.write((value >>>  8) & 0xFF);
            out.write(value & 0xFF);
        }

        // Matches ByteBufCodecs.stringUtf8(maxChars): writeVarInt(byteLen), bytes.
        // The codec validates string length by *char count* (≤ maxChars) and byte
        // length (≤ maxChars * 3), so truncate by chars to stay legal.
        void writeString(String s, int maxChars) throws IOException {
            if (s.length() > maxChars) {
                s = s.substring(0, maxChars);
            }
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            writeVarInt(bytes.length);
            out.write(bytes);
        }

        byte[] toBytes() {
            return out.toByteArray();
        }
    }

    private static final class IaReader {
        private final byte[] data;
        private int pos = 0;

        IaReader(byte[] data) {
            this.data = data;
        }

        int readVarInt() {
            int value = 0, shift = 0, b;
            do {
                if (shift >= 35) throw new IllegalArgumentException("VarInt too long");
                if (pos >= data.length) throw new IllegalArgumentException("Buffer underflow");
                b = data[pos++] & 0xFF;
                value |= (b & 0x7F) << shift;
                shift += 7;
            } while ((b & 0x80) != 0);
            return value;
        }

        String readString(int maxChars) {
            int len = readVarInt();
            int maxBytes = maxChars * 3;
            if (len < 0 || len > maxBytes) throw new IllegalArgumentException("String len out of range: " + len);
            if (pos + len > data.length) throw new IllegalArgumentException("Buffer underflow reading string");
            String s = new String(data, pos, len, StandardCharsets.UTF_8);
            pos += len;
            if (s.length() > maxChars) throw new IllegalArgumentException("String char count > " + maxChars);
            return s;
        }
    }
}
