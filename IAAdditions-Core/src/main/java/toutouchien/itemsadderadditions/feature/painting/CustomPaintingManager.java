package toutouchien.itemsadderadditions.feature.painting;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.loading.ConfigFileCategory;
import toutouchien.itemsadderadditions.common.loading.ConfigFileRegistry;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.nms.api.INmsPaintingHandler;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.nms.api.painting.NmsPaintingVariant;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;
import toutouchien.itemsadderadditions.runtime.reload.ReloadableContentSystem;
import toutouchien.itemsadderadditions.settings.PluginFeature;

import java.util.*;

@NullMarked
public final class CustomPaintingManager implements ReloadableContentSystem {
    private static final String TAG = "CustomPaintings";

    private final ItemsAdderAdditions plugin;
    private final CustomPaintingLoader loader = new CustomPaintingLoader();
    private final Map<String, CustomPaintingDefinition> byVariantId = new LinkedHashMap<>();
    private final Map<String, CustomPaintingDefinition> byItemId = new LinkedHashMap<>();
    private final Map<String, String> registrySignatures = new LinkedHashMap<>();

    private boolean unsupportedWarningShown;

    public CustomPaintingManager(ItemsAdderAdditions plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(new CustomPaintingListener(this), plugin);
    }

    private static String registrySignature(CustomPaintingDefinition definition) {
        return definition.variantId()
                + '|'
                + definition.width()
                + '|'
                + definition.height()
                + '|'
                + definition.assetId()
                + '|'
                + nullToEmpty(definition.title())
                + '|'
                + nullToEmpty(definition.author())
                + '|'
                + definition.includeInRandom();
    }

    private static String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    @Nullable
    private static INmsPaintingHandler handler() {
        return NmsManager.instance().handler().paintings();
    }

    private static boolean isWallFace(BlockFace face) {
        return face == BlockFace.NORTH
                || face == BlockFace.SOUTH
                || face == BlockFace.EAST
                || face == BlockFace.WEST;
    }

    private static void consumeItem(Player player, ItemStack item, EquipmentSlot hand) {
        if (player.getGameMode() == GameMode.CREATIVE) return;

        int remaining = item.getAmount() - 1;
        if (remaining > 0) {
            item.setAmount(remaining);
            return;
        }

        if (hand == EquipmentSlot.HAND)
            player.getInventory().setItemInMainHand(null);
        else if (hand == EquipmentSlot.OFF_HAND)
            player.getInventory().setItemInOffHand(null);
    }

    public boolean reload(ConfigFileRegistry registry) {
        byVariantId.clear();
        byItemId.clear();

        INmsPaintingHandler handler = handler();
        if (handler == null) {
            if (isEnabled() && !unsupportedWarningShown) {
                Log.warn(TAG, "Custom paintings are enabled but not supported on this server version - skipping.");
                unsupportedWarningShown = true;
            }
            return false;
        }

        if (!isEnabled()) {
            boolean registryChanged = !registrySignatures.isEmpty();
            if (registryChanged) {
                handler.updateRandomPlaceableVariants(registrySignatures.keySet(), Set.of());
                registrySignatures.clear();
            }
            return registryChanged;
        }

        List<CustomPaintingDefinition> definitions = loader.loadAll(registry.getFiles(ConfigFileCategory.PAINTINGS));
        for (CustomPaintingDefinition definition : definitions) {
            registerDefinition(definition);
        }

        List<NmsPaintingVariant> nmsVariants = new ArrayList<>(byVariantId.size());
        Set<String> randomVariantIds = new HashSet<>();
        Map<String, String> newRegistrySignatures = new LinkedHashMap<>();
        for (CustomPaintingDefinition definition : byVariantId.values()) {
            nmsVariants.add(definition.toNmsVariant());
            newRegistrySignatures.put(definition.variantId(), registrySignature(definition));

            if (definition.includeInRandom()) {
                randomVariantIds.add(definition.variantId());
            }
        }

        boolean registryChanged = !registrySignatures.equals(newRegistrySignatures);

        handler.injectPaintingVariants(nmsVariants);
        handler.updateRandomPlaceableVariants(byVariantId.keySet(), randomVariantIds);

        registrySignatures.clear();
        registrySignatures.putAll(newRegistrySignatures);

        Log.info(TAG, "Loaded {} custom painting(s), {} linked item(s), {} included in vanilla random placement.",
                byVariantId.size(), byItemId.size(), randomVariantIds.size());

        return registryChanged;
    }

    @Override
    public String name() {
        return TAG;
    }

    @Override
    public ReloadPhase phase() {
        return ReloadPhase.REGISTRY_PREPARE;
    }

    @Override
    public ReloadStepResult reload(ContentReloadContext context) {
        boolean registryChanged = reload(context.registry());
        return ReloadStepResult.registry(name(), registryChanged, byVariantId.size());
    }

    @Nullable
    public CustomPaintingDefinition byItemId(String itemId) {
        return byItemId.get(itemId);
    }

    public boolean tryPlace(
            Player player,
            ItemStack item,
            EquipmentSlot hand,
            Block clickedBlock,
            BlockFace face,
            CustomPaintingDefinition definition
    ) {
        INmsPaintingHandler handler = handler();
        if (handler == null) return false;
        if (!isWallFace(face)) return false;

        Block target = clickedBlock.getRelative(face);
        if (!target.getType().isAir()) return false;

        Location location = target.getLocation();
        Painting painting = location.getWorld().spawn(location, Painting.class, spawned ->
                spawned.setFacingDirection(face, true));

        boolean placed = false;
        try {
            if (!painting.setFacingDirection(face, true)) return false;
            if (!handler.applyVariant(painting, definition.variantId())) return false;
            if (!handler.isStillValid(painting)) return false;

            HangingPlaceEvent placeEvent = new HangingPlaceEvent(painting, player, clickedBlock, face, hand, item);
            Bukkit.getPluginManager().callEvent(placeEvent);
            if (placeEvent.isCancelled()) return false;

            consumeItem(player, item, hand);
            player.swingHand(hand);
            placed = true;
            return true;
        } finally {
            if (!placed) painting.remove();
        }
    }

    private void registerDefinition(CustomPaintingDefinition definition) {
        CustomPaintingDefinition previous = byVariantId.putIfAbsent(definition.variantId(), definition);
        if (previous != null) {
            Log.warn(TAG, "Duplicate custom painting '{}' in {} - already defined in {}. Skipping duplicate.",
                    definition.variantId(), definition.sourceFile(), previous.sourceFile());
            return;
        }

        String itemId = definition.itemId();
        if (itemId == null) return;

        CustomPaintingDefinition previousItem = byItemId.putIfAbsent(itemId, definition);
        if (previousItem != null) {
            Log.warn(TAG, "Duplicate painting item association '{}' for '{}' in {} - already linked to '{}'. Skipping duplicate association.",
                    itemId, definition.variantId(), definition.sourceFile(), previousItem.variantId());
        }
    }

    private boolean isEnabled() {
        return plugin.settings().featureEnabled(PluginFeature.CUSTOM_PAINTINGS);
    }
}
