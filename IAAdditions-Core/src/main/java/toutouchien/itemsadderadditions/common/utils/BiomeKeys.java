package toutouchien.itemsadderadditions.common.utils;

import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

@NullMarked
public final class BiomeKeys {
    private BiomeKeys() {
    }

    public static NamespacedKey key(Object biome) {
        try {
            Method getKey = biome.getClass().getMethod("getKey");
            Object result = getKey.invoke(biome);
            if (result instanceof NamespacedKey key) return key;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Log.debug("BiomeKeys", "Could not resolve biome key reflectively for {}: {}", biome, e.getMessage());
        }

        String key = biome instanceof Enum<?> enumBiome ? enumBiome.name() : biome.toString();
        return new NamespacedKey(NamespacedKey.MINECRAFT, key.toLowerCase(Locale.ROOT));
    }

    public static String asString(Object biome) {
        return key(biome).toString();
    }
}
