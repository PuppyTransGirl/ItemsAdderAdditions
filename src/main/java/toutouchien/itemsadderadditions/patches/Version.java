package toutouchien.itemsadderadditions.patches;

public record Version(String minecraft, String itemsAdder) {

    public static final Version ANY = new Version("*", "*");

    public static Version of(String minecraft, String itemsAdder) {
        if (minecraft == null || minecraft.isBlank())
            throw new IllegalArgumentException("minecraft version must not be blank");
        if (itemsAdder == null || itemsAdder.isBlank())
            throw new IllegalArgumentException("itemsAdder version must not be blank");
        return new Version(minecraft, itemsAdder);
    }

    /**
     * Normalizes a version string so "4.0.17-beta-8" becomes "4.0.17".
     * Also keeps "*" as-is.
     */
    public static String normalize(String v) {
        if (v == null) return null;
        if (v.equals("*")) return v;
        int dash = v.indexOf('-');
        return (dash >= 0) ? v.substring(0, dash) : v;
    }

    public static int compareVersionStrings(String a, String b) {
        a = normalize(a);
        b = normalize(b);

        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        int len = Math.max(partsA.length, partsB.length);

        for (int i = 0; i < len; i++) {
            int numA = i < partsA.length ? parseSegment(partsA[i]) : 0;
            int numB = i < partsB.length ? parseSegment(partsB[i]) : 0;
            if (numA != numB) return Integer.compare(numA, numB);
        }
        return 0;
    }

    private static int parseSegment(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
