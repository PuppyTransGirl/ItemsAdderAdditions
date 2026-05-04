package toutouchien.itemsadderadditions.utils;

import org.bukkit.Bukkit;

@SuppressWarnings("java:S115")
public enum VersionUtils {
    UNKNOWN(Integer.MAX_VALUE),
    v26_1_2(775),
    v1_21_11(774),
    v1_21_10(773),
    v1_21_8(772),
    v1_21_7(772),
    v1_21_6(771),
    v1_21_5(770),
    v1_21_4(769),
    v1_21_3(768),
    v1_21_1(767),
    v1_20_6(766);

    private static volatile VersionUtils serverVersion;
    public final int value;

    VersionUtils(int value) {
        this.value = value;
    }

    public static synchronized VersionUtils version() {
        if (serverVersion != null)
            return serverVersion;

        // Try getting the version from the bukkit method
        VersionUtils version = minecraftVersionMethod();
        if (version == null)
            version = protocolVersionMethod();

        if (version == null)
            version = UNKNOWN;

        return serverVersion = version;
    }

    private static VersionUtils minecraftVersionMethod() {
        String minecraftVersion = "v" + Bukkit.getMinecraftVersion()
                .replace(".", "_"); // e.g. "1.21.4" → "v1_21_4"

        return EnumUtils.match(minecraftVersion, VersionUtils.class, null);
    }

    @SuppressWarnings("deprecation") // We need to use Bukkit.getUnsafe() for this method
    private static VersionUtils protocolVersionMethod() {
        int protocol = Bukkit.getUnsafe().getProtocolVersion(); // e.g. 769
        for (VersionUtils version : VersionUtils.values()) {
            if (version.value == protocol)
                return version;
        }

        return null;
    }

    public static boolean isHigherThan(VersionUtils target) {
        return version().value > target.value;
    }

    public static boolean isHigherThanOrEquals(VersionUtils target) {
        return version().value >= target.value;
    }

    public static boolean isLowerThan(VersionUtils target) {
        return version().value < target.value;
    }

    public static boolean isLowerThanOrEquals(VersionUtils target) {
        return version().value <= target.value;
    }

    @SuppressWarnings("java:S1201")
    public static boolean equals(VersionUtils target) {
        return version().value == target.value;
    }

    @Override
    public String toString() {
        if (this == UNKNOWN)
            return "UNKNOWN";

        return this.name()
                .substring(1)
                .replace("_", ".");
    }
}
