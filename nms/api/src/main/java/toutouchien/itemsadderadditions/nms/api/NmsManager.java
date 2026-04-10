package toutouchien.itemsadderadditions.nms.api;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;

public class NmsManager {

    private static NmsManager instance;

    private final INmsHandler handler;

    private NmsManager(INmsHandler handler) {
        this.handler = handler;
    }

    public static void initialize(ComponentLogger logger) {
        if (instance != null)
            throw new IllegalStateException("NmsManager is already initialized");

        String rawVersion = Bukkit.getServer().getMinecraftVersion().replace(".", "_");
        String version = "v" + rawVersion;
        String className = "toutouchien.itemsadderadditions.nms.NmsHandler_" + version;

        logger.info("Loading NMS handler for version: {}", version);

        try {
            Class<?> clazz = Class.forName(className);
            INmsHandler handler = (INmsHandler) clazz.getDeclaredConstructor().newInstance();
            instance = new NmsManager(handler);
            logger.info("NMS handler loaded successfully: {}", className);
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException(
                    "Unsupported Minecraft version: " + rawVersion, e
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate NMS handler: " + className, e);
        }
    }

    public static void shutdown() {
        instance = null;
    }

    public static NmsManager instance() {
        if (instance == null)
            throw new IllegalStateException("NmsManager has not been initialized yet");

        return instance;
    }

    public INmsHandler handler() {
        return handler;
    }
}
