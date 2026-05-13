package toutouchien.itemsadderadditions.feature.creative;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;
import toutouchien.itemsadderadditions.runtime.reload.ReloadableContentSystem;
import toutouchien.itemsadderadditions.settings.PluginFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reloads the creative-inventory integration and reports whether client-side
 * registry data changed enough to require a client refresh.
 */
@NullMarked
public final class CreativeRegistryReloader implements ReloadableContentSystem {
    private final ItemsAdderAdditions plugin;
    private final Set<String> knownVariantIds = ConcurrentHashMap.newKeySet();

    public CreativeRegistryReloader(ItemsAdderAdditions plugin) {
        this.plugin = plugin;
    }

    private static List<CustomStack> visibleCreativeItems(List<CustomStack> allItems) {
        List<CustomStack> visibleItems = new ArrayList<>();
        for (CustomStack item : allItems) {
            if (!shouldSkipCreativeItem(item)) {
                visibleItems.add(item);
            }
        }
        return visibleItems;
    }

    private static boolean shouldSkipCreativeItem(CustomStack item) {
        FileConfiguration config = item.getConfig();
        String path = "items." + item.getId() + ".";

        if (config.getBoolean(path + "template", false)
                || config.getBoolean(path + "hide_from_inventory", false)) {
            return true;
        }

        // Check if item is a directional variant and base item is valid
        String fullId = item.getNamespacedID();
        String[] directions = {"_north", "_south", "_east", "_west", "_up", "_down"};

        for (String direction : directions) {
            if (fullId.endsWith(direction)) {
                String baseFullId = fullId.substring(0, fullId.length() - direction.length());
                return CustomStack.isInRegistry(baseFullId);
            }
        }

        return false;
    }

    private static String variantId(CustomStack item) {
        return "ia_creative:" + item.getNamespace() + "_" + item.getId();
    }

    public boolean reload(List<CustomStack> allItems) {
        if (!plugin.settings().featureEnabled(PluginFeature.CREATIVE_INVENTORY_INTEGRATION)) {
            return false;
        }

        NmsManager nms = NmsManager.instance();
        if (nms.handler().creativeMenu() == null) {
            return false;
        }

        List<CustomStack> visibleItems = visibleCreativeItems(allItems);
        boolean registryChanged = containsNewVariants(visibleItems);

        nms.handler().creativeMenu().injectPaintingVariants(visibleItems);
        nms.handler().creativeMenu().updatePaintingCache(visibleItems);
        rememberVariants(visibleItems);

        CreativeMenuManager creativeMenuManager = plugin.creativeMenuManager();
        if (creativeMenuManager != null) {
            creativeMenuManager.reload(visibleItems);
        }

        return registryChanged;
    }

    @Override
    public String name() {
        return "CreativeRegistry";
    }

    @Override
    public ReloadPhase phase() {
        return ReloadPhase.CLIENT_REGISTRY;
    }

    @Override
    public ReloadStepResult reload(ContentReloadContext context) {
        boolean registryChanged = reload(context.items());
        return ReloadStepResult.registry(name(), registryChanged, 0);
    }

    private boolean containsNewVariants(List<CustomStack> items) {
        for (CustomStack item : items) {
            if (!knownVariantIds.contains(variantId(item))) {
                return true;
            }
        }
        return false;
    }

    private void rememberVariants(List<CustomStack> items) {
        for (CustomStack item : items) {
            knownVariantIds.add(variantId(item));
        }
    }
}
