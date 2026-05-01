package toutouchien.itemsadderadditions.behaviours.executors;

import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurniturePlaceSuccessEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.behaviours.BehaviourExecutor;
import toutouchien.itemsadderadditions.behaviours.BehaviourHost;
import toutouchien.itemsadderadditions.behaviours.annotations.Behaviour;
import toutouchien.itemsadderadditions.utils.hook.PlaceholderAPIUtils;

import java.util.List;
import java.util.UUID;

@NullMarked
@Behaviour(key = "hologram")
public final class HologramBehaviour extends BehaviourExecutor implements Listener {
    private static final NamespacedKey FURNITURE_ROTATION_KEY = new NamespacedKey("itemsadderadditions", "furniture_rotation");
    private static final NamespacedKey FURNITURE_PLAYER_KEY = new NamespacedKey("itemsadderadditions", "furniture_player");
    private static final NamespacedKey FURNITURE_UUID_KEY = new NamespacedKey("itemsadderadditions", "furniture_uuid");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Parameter(key = "texts", type = List.class, required = true)
    private List<String> texts;

    @Parameter(key = "offset", type = String.class)
    private String offset = "0,1.0,0";

    @Parameter(key = "scale", type = String.class)
    private String scale = "1,1,1";

    @Parameter(key = "yaw-rotation", type = Float.class)
    private float yawRotation = 0;

    @Parameter(key = "pitch-rotation", type = Float.class)
    private float pitchRotation = 0;

    @Parameter(key = "background", type = String.class)
    private String background = "transparent";

    @Parameter(key = "billboard", type = String.class)
    private String billboard = "fixed";

    @Parameter(key = "shadow", type = Boolean.class)
    private boolean shadow = true;

    @Parameter(key = "block-brightness", type = Integer.class)
    private int blockBrightness = -1;

    @Parameter(key = "sky-brightness", type = Integer.class)
    private int skyBrightness = -1;

    private String namespacedID;

    @Override
    protected void onLoad(BehaviourHost host) {
        this.namespacedID = host.namespacedID();
        Bukkit.getPluginManager().registerEvents(this, host.plugin());
    }

    @Override
    protected void onUnload(BehaviourHost host) {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurniturePlace(FurniturePlaceSuccessEvent event) {
        if (!event.getNamespacedID().equals(namespacedID)) return;

        var furniture = event.getFurniture();
        var player = event.getPlayer();

        float placerYaw = player.getLocation().getYaw();
        createHologram(furniture, placerYaw, player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        if (!event.getNamespacedID().equals(namespacedID)) return;

        var furniture = event.getFurniture();
        UUID furnitureUuid = furniture.getEntity().getUniqueId();
        Location furnitureLoc = furniture.getEntity().getLocation();

        for (Entity entity : furnitureLoc.getWorld().getNearbyEntities(furnitureLoc, 3, 3, 3)) {
            if (!(entity instanceof TextDisplay display)) continue;

            var pdc = display.getPersistentDataContainer();
            if (!pdc.has(FURNITURE_UUID_KEY, PersistentDataType.STRING)) continue;

            String storedFurnitureUuid = pdc.get(FURNITURE_UUID_KEY, PersistentDataType.STRING);
            if (furnitureUuid.toString().equals(storedFurnitureUuid)) {
                display.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        var chunk = event.getChunk();

        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() != EntityType.ARMOR_STAND) continue;

            CustomFurniture furniture = CustomFurniture.byAlreadySpawned(entity);
            if (furniture == null) continue;

            if (!furniture.getNamespacedID().equals(namespacedID)) continue;

            UUID playerUuid = getStoredPlayerUuid(entity);
            Player player = playerUuid != null ? Bukkit.getPlayer(playerUuid) : null;

            createHologram(furniture, null, player);
        }
    }

    private void createHologram(CustomFurniture furniture, Float placerYaw, Player player) {
        Entity furnitureEntity = furniture.getEntity();
        UUID furnitureUuid = furnitureEntity.getUniqueId();
        var furnitureLoc = furnitureEntity.getLocation();

        if (hologramExists(furnitureUuid, furnitureLoc)) return;

        double[] offset = parseOffset(this.offset);
        Vector3f scale = parseScale(this.scale);

        Float storedRotation = getStoredRotation(furnitureEntity);
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

        display.getPersistentDataContainer().set(FURNITURE_UUID_KEY, PersistentDataType.STRING, furnitureUuid.toString());

        if (placerYaw != null && storedRotation == null) {
            storeRotation(furnitureEntity, furnitureYaw);
        }

        if (player != null) {
            storePlayer(furnitureEntity, player.getUniqueId());
        }
    }

    private boolean hologramExists(UUID furnitureUuid, Location furnitureLoc) {
        for (Entity entity : furnitureLoc.getWorld().getNearbyEntities(furnitureLoc, 3, 3, 3)) {
            if (!(entity instanceof TextDisplay display)) continue;

            var pdc = display.getPersistentDataContainer();
            if (!pdc.has(FURNITURE_UUID_KEY, PersistentDataType.STRING)) continue;

            String storedFurnitureUuid = pdc.get(FURNITURE_UUID_KEY, PersistentDataType.STRING);
            if (furnitureUuid.toString().equals(storedFurnitureUuid)) {
                return true;
            }
        }
        return false;
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

    private static UUID getStoredPlayerUuid(Entity entity) {
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
