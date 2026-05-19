package toutouchien.itemsadderadditions.mod;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.ThreadLocalRandom;

@NullMarked
public final class ModPlayerSession {
    private final int protocolVersion;
    private final String modVersion;
    private final int nonce;
    private final long helloTimestamp;
    private boolean capabilitiesSent = false;

    public ModPlayerSession(int protocolVersion, String modVersion) {
        this.protocolVersion = protocolVersion;
        this.modVersion = modVersion;
        this.nonce = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        this.helloTimestamp = System.currentTimeMillis();
    }

    public int protocolVersion() {
        return protocolVersion;
    }

    public String modVersion() {
        return modVersion;
    }

    public int nonce() {
        return nonce;
    }

    public long helloTimestamp() {
        return helloTimestamp;
    }

    public boolean capabilitiesSent() {
        return capabilitiesSent;
    }

    public void markCapabilitiesSent() {
        this.capabilitiesSent = true;
    }
}
