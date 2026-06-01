package toutouchien.itemsadderadditions.integration.valhalla;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;
import toutouchien.itemsadderadditions.runtime.reload.ReloadableContentSystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Applies ValhallaMMO and ValhallaTrinkets PDC data to custom items.
 *
 * <p>Reads {@code valhalla:} sections from item configs during reload and writes
 * the data to item PDC via the ItemsAdder item modifier hook. Neither ValhallaMMO
 * nor ValhallaTrinkets needs to be loaded for this to work.
 *
 * <p>YAML structure:
 * <pre>
 * items:
 *   my_item:
 *     valhalla:
 *       stats:
 *         - stat: GENERIC_MOVEMENT_SPEED
 *           amount: 0.3
 *           operation: ADD_SCALAR
 *           hidden: false
 *       equipment_class: TRINKET
 *       item_flags:
 *         - DISPLAY_ATTRIBUTES
 *       trinkets:
 *         trinket_id: 7
 *         trinket_unique_id: 529
 *         unique: true
 * </pre>
 */
@NullMarked
public final class ValhallaManager implements ReloadableContentSystem {
    private static final String NAME = "Valhalla";

    private volatile Map<String, ValhallaItemData> bindings = Map.of();
    private volatile boolean active = true;

    public void shutdown() {
        active = false;
        bindings = Map.of();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ReloadPhase phase() {
        return ReloadPhase.ITEM_BINDINGS;
    }

    @Override
    public ReloadStepResult reload(ContentReloadContext context) {
        Map<String, ValhallaItemData> newBindings = new HashMap<>();
        int total = 0;

        for (CustomStack item : context.items()) {
            String namespacedId = item.getNamespacedID();
            FileConfiguration config = item.getConfig();

            ConfigurationSection section =
                    config.getConfigurationSection("items." + item.getId() + ".valhalla");
            if (section == null) continue;

            ValhallaItemData data = ValhallaConfigParser.parse(section, namespacedId);
            if (data == null) continue;

            newBindings.put(namespacedId, data);
            total++;
        }

        this.bindings = Map.copyOf(newBindings);
        Log.loaded(NAME, total, "item(s) with Valhalla data");
        return ReloadStepResult.loaded(NAME, total);
    }

    public ItemStack applyValhalla(String namespacedId, ItemStack itemStack) {
        if (!active) return itemStack;

        ValhallaItemData data = bindings.get(namespacedId);
        if (data == null) return itemStack;

        return ValhallaItemApplier.apply(itemStack, data);
    }
}
