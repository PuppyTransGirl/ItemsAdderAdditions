package toutouchien.itemsadderadditions.client;

/**
 * Shared wire constants for the ItemsAdderAdditions client companion protocol.
 */
public final class CreativeSyncProtocol {
    public static final String NAMESPACE = "itemsadderadditions";
    public static final String HANDSHAKE_CHANNEL = NAMESPACE + ":handshake";
    public static final String REGISTRY_CHANNEL = NAMESPACE + ":creative_registry";

    public static final int PROTOCOL_VERSION = 1;

    public static final byte C2S_HANDSHAKE = 1;
    public static final byte S2C_HANDSHAKE_RESPONSE = 2;
    public static final byte S2C_REGISTRY_CHUNK = 3;
    public static final byte S2C_REGISTRY_COMPLETE = 4;

    public static final byte HANDSHAKE_ACCEPTED = 0;
    public static final byte HANDSHAKE_REJECTED = 1;

    public static final int REGISTRY_MAGIC = 0x49414354; // IACT
    public static final int REGISTRY_FORMAT_VERSION = 1;

    /**
     * Keeps plugin messages well below the 32 KiB vanilla custom payload limit.
     */
    public static final int MAX_CHUNK_DATA_BYTES = 24 * 1024;

    private CreativeSyncProtocol() {
    }
}
