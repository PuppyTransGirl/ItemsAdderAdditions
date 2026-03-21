package toutouchien.itemsadderadditions.behaviours.loading;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.utils.ExecutorRegistry;
import toutouchien.itemsadderadditions.utils.ItemCategory;
import toutouchien.itemsadderadditions.behaviours.BehaviourExecutor;
import toutouchien.itemsadderadditions.behaviours.BehaviourHost;

/**
 * Reads every CustomStack's YAML config, finds the {@code behaviours:} section,
 * and populates {@link BehaviourBindings}.
 *
 * <h3>YAML structure</h3>
 * <pre>
 * items:
 *   my_spiky_block:
 *     # ... ItemsAdder built-in fields ...
 *     behaviours:
 *       furniture: ...            # ItemsAdder built-in - skipped silently
 *       contact_damage:             # registered custom behaviour → loaded
 *         damage: 1.0
 *         delay: 2
 *       your_behaviour:           # another custom behaviour
 *         some_param: value
 * </pre>
 *
 * <p>Keys present in {@code behaviours:} that are not registered in
 * {@link toutouchien.itemsadderadditions.utils.ExecutorRegistry} are skipped silently - this covers all
 * ItemsAdder built-in behaviour keys ({@code furniture}, {@code complex_furniture},
 * {@code gun}, etc.) as well as keys from other plugins.
 */
@NullMarked
public final class BehaviourLoader {
    private final ExecutorRegistry<BehaviourExecutor> registry;

    public BehaviourLoader(ExecutorRegistry<BehaviourExecutor> registry) {
        this.registry = registry;
    }
    public void load() {
        BehaviourBindings.clear();
        int total = 0;

        for (CustomStack customStack : ItemsAdder.getAllItems())
            total += loadItem(customStack);

        ItemsAdderAdditions.instance().getSLF4JLogger().info("[Behaviours] Loaded {} behaviour binding(s).", total);
    }

    private int loadItem(CustomStack customStack) {
        FileConfiguration config = customStack.getConfig();
        String itemID = customStack.getId();
        String namespacedID = customStack.getNamespacedID();

        ConfigurationSection behavioursSection = config.getConfigurationSection("items." + itemID + ".behaviours");
        if (behavioursSection == null)
            return 0;

        ItemCategory category = ItemCategory.determine(customStack, config, itemID);
        int count = 0;

        for (String behaviourKey : behavioursSection.getKeys(false)) {
            BehaviourExecutor prototype = registry.getPrototype(behaviourKey);
            if (prototype == null)
                continue;

            BehaviourExecutor instance = prototype.newInstance();

            // Get the raw value (could be a Section, a List, or a String)
            Object configValue = behavioursSection.get(behaviourKey);

            if (!instance.configure(configValue, namespacedID))
                continue;

            BehaviourHost host = new BehaviourHost(namespacedID, category, ItemsAdderAdditions.instance());
            instance.load(host);
            BehaviourBindings.add(namespacedID, instance);
            count++;
        }

        return count;
    }
}
