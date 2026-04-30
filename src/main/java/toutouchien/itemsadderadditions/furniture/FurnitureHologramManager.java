package toutouchien.itemsadderadditions.furniture;

import dev.lone.itemsadder.api.CustomFurniture;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import toutouchien.itemsadderadditions.utils.hook.PlaceholderAPIUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FurnitureHologramManager {
    private static final NamespacedKey FURNITURE_KEY = new NamespacedKey("itemsadderadditions", "furniture_hologram");
    private static final NamespacedKey FURNITURE_ROTATION_KEY = new NamespacedKey("itemsadderadditions", "furniture_rotation");
    private static final NamespacedKey FURNITURE_PLAYER_KEY = new NamespacedKey("itemsadderadditions", "furniture_player");
    private static final ConcurrentHashMap<String, List<UUID>> FURNITURE_TO_HOLOGRAMS = new ConcurrentHashMap<>();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String CONFIG_SECTION = "furniture-holograms.";

    private FurnitureHologramManager() {
    }

    public static void createHologram(CustomFurniture furniture, String furnitureId) {
        createHologram(furniture, furnitureId, null, null);
    }

    public static void createHologram(CustomFurniture furniture, String furnitureId, Float placerYaw) {
        createHologram(furniture, furnitureId, placerYaw, null);
    }

    public static void createHologram(CustomFurniture furniture, String furnitureId, Float placerYaw, Player player) {
        var plugin = Bukkit.getPluginManager().getPlugin("ItemsAdderAdditions");
        if (plugin == null) return;

        var config = plugin.getConfig();
        if (!config.getBoolean(CONFIG_SECTION + "enabled", true)) return;

        var holograms = config.getConfigurationSection(CONFIG_SECTION + "holograms");
        if (holograms == null) return;

        var section = holograms.getConfigurationSection(furnitureId);
        if (section == null) return;

        List<String> texts = section.getStringList("texts");
        if (texts.isEmpty()) return;

        Location furnitureLoc = furniture.getEntity().getLocation();
        String furnitureKey = locationKey(furnitureLoc);

        if (FURNITURE_TO_HOLOGRAMS.containsKey(furnitureKey)) {
            return;
        }

        double[] offset = parseOffset(section.getString("offset", "0,1.0,0"));
        Vector3f scale = parseScale(section.getString("scale", "1,1,1"));

        String background = section.getString("background", "transparent");
        String billboard = section.getString("billboard", "fixed");
        boolean shadow = section.getBoolean("shadow", true);
        float yawRotation = (float) section.getDouble("yaw-rotation", 0);
        float pitchRotation = (float) section.getDouble("pitch-rotation", 0);
        int blockBrightness = section.getInt("block-brightness", -1);
        int skyBrightness = section.getInt("sky-brightness", -1);

        Float storedRotation = getStoredRotation(furniture.getEntity());
        float furnitureYaw = storedRotation != null ? storedRotation : yawToFurnitureRotation(placerYaw != null ? placerYaw : furnitureLoc.getYaw());

        boolean needsFlip = furnitureYaw == 0 || furnitureYaw == 180 || furnitureYaw == -180;
        float totalYaw = normalizeYaw(yawRotation + furnitureYaw + (needsFlip ? 180 : 0));
        double[] rotatedOffset = rotateOffset(offset[0], offset[2], furnitureYaw);

        Location hologramLoc = furnitureLoc.clone().add(rotatedOffset[0], offset[1], rotatedOffset[1]);

        TextDisplay display = (TextDisplay) hologramLoc.getWorld().spawnEntity(hologramLoc, EntityType.TEXT_DISPLAY);

        String combinedText = String.join("\n", texts);
        String parsedText = PlaceholderAPIUtils.parsePlaceholders(player, combinedText);
        display.text(MINI_MESSAGE.deserialize(parsedText));
        display.setBillboard(parseBillboard(billboard));
        display.setShadowed(shadow);
        display.setPersistent(true);
        display.setInvulnerable(true);
        display.setRotation(totalYaw, pitchRotation);

        Transformation transformation = display.getTransformation();
        display.setTransformation(new Transformation(
            transformation.getTranslation(),
            transformation.getLeftRotation(),
            scale,
            transformation.getRightRotation()
        ));

        display.setBackgroundColor(parseBackgroundColor(background));

        if (blockBrightness >= 0 || skyBrightness >= 0) {
            display.setBrightness(new Display.Brightness(
                blockBrightness >= 0 ? blockBrightness : 0,
                skyBrightness >= 0 ? skyBrightness : 0
            ));
        }

        display.getPersistentDataContainer().set(FURNITURE_KEY, PersistentDataType.STRING, furnitureLoc.toString());

        FURNITURE_TO_HOLOGRAMS.put(furnitureKey, List.of(display.getUniqueId()));

        if (placerYaw != null && storedRotation == null) {
            storeRotation(furniture.getEntity(), furnitureYaw);
        }

        if (player != null) {
            storePlayer(furniture.getEntity(), player.getUniqueId());
        }
    }

    public static void removeHologram(Location furnitureLoc) {
        String furnitureKey = locationKey(furnitureLoc);
        List<UUID> hologramUuids = FURNITURE_TO_HOLOGRAMS.remove(furnitureKey);

        if (hologramUuids != null) {
            for (UUID uuid : hologramUuids) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity instanceof TextDisplay display) {
                    display.remove();
                }
            }
        }
    }

    public static void clearAll() {
        FURNITURE_TO_HOLOGRAMS.values().forEach(uuids -> {
            for (UUID uuid : uuids) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity instanceof TextDisplay display) {
                    display.remove();
                }
            }
        });
        FURNITURE_TO_HOLOGRAMS.clear();
    }

    public static UUID getStoredPlayerUuid(Entity entity) {
        var pdc = entity.getPersistentDataContainer();
        if (pdc.has(FURNITURE_PLAYER_KEY, PersistentDataType.STRING)) {
            String uuidStr = pdc.get(FURNITURE_PLAYER_KEY, PersistentDataType.STRING);
            if (uuidStr != null) {
                try {
                    return UUID.fromString(uuidStr);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return null;
    }

    public static boolean hasHologram(String furnitureId) {
        var plugin = Bukkit.getPluginManager().getPlugin("ItemsAdderAdditions");
        if (plugin == null) return false;

        var holograms = plugin.getConfig().getConfigurationSection(CONFIG_SECTION + "holograms");
        if (holograms == null) return false;

        var section = holograms.getConfigurationSection(furnitureId);
        if (section == null) return false;

        return !section.getStringList("texts").isEmpty();
    }

    private static String locationKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private static Float getStoredRotation(Entity entity) {
        var pdc = entity.getPersistentDataContainer();
        if (pdc.has(FURNITURE_ROTATION_KEY, PersistentDataType.FLOAT)) {
            return pdc.get(FURNITURE_ROTATION_KEY, PersistentDataType.FLOAT);
        }
        return null;
    }

    private static void storeRotation(Entity entity, float yaw) {
        entity.getPersistentDataContainer().set(FURNITURE_ROTATION_KEY, PersistentDataType.FLOAT, yaw);
    }

    private static void storePlayer(Entity entity, UUID playerUuid) {
        entity.getPersistentDataContainer().set(FURNITURE_PLAYER_KEY, PersistentDataType.STRING, playerUuid.toString());
    }

    private static double[] parseOffset(String value) {
        String[] parts = value.split(",");
        double x = parts.length > 0 ? parseDouble(parts[0], 0) : 0;
        double y = parts.length > 1 ? parseDouble(parts[1], 1.0) : 1.0;
        double z = parts.length > 2 ? parseDouble(parts[2], 0) : 0;
        return new double[]{x, y, z};
    }

    private static Vector3f parseScale(String value) {
        String[] parts = value.split(",");
        float x = parts.length > 0 ? parseFloat(parts[0], 1) : 1;
        float y = parts.length > 1 ? parseFloat(parts[1], 1) : 1;
        float z = parts.length > 2 ? parseFloat(parts[2], 1) : 1;
        return new Vector3f(x, y, z);
    }

    private static double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static float parseFloat(String value, float defaultValue) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Display.Billboard parseBillboard(String value) {
        return switch (value.toLowerCase()) {
            case "fixed" -> Display.Billboard.FIXED;
            case "horizontal" -> Display.Billboard.HORIZONTAL;
            case "vertical" -> Display.Billboard.VERTICAL;
            default -> Display.Billboard.CENTER;
        };
    }

    private static org.bukkit.Color parseBackgroundColor(String value) {
        if (value.equalsIgnoreCase("transparent")) {
            return org.bukkit.Color.fromARGB(0, 0, 0, 0);
        }
        if (value.startsWith("#")) {
            try {
                int rgb = Integer.parseInt(value.substring(1), 16);
                return org.bukkit.Color.fromRGB(rgb);
            } catch (NumberFormatException ignored) {
            }
        }
        return org.bukkit.Color.fromARGB(0, 0, 0, 0);
    }

    private static double[] rotateOffset(double x, double z, double yawDegrees) {
        double yawRad = Math.toRadians(-yawDegrees);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double newX = x * cos - z * sin;
        double newZ = x * sin + z * cos;
        return new double[]{newX, newZ};
    }

    private static float yawToFurnitureRotation(float yaw) {
        float normalized = ((yaw % 360) + 360) % 360;
        if (normalized >= 315 || normalized < 45) return 0;
        if (normalized >= 45 && normalized < 135) return -90;
        if (normalized >= 135 && normalized < 225) return 180;
        return 90;
    }

    private static float normalizeYaw(float yaw) {
        while (yaw > 180) yaw -= 360;
        while (yaw < -180) yaw += 360;
        return yaw;
    }
}
