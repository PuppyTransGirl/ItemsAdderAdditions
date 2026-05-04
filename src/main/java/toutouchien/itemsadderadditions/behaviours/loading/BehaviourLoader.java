package toutouchien.itemsadderadditions.behaviours.loading;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.behaviours.BehaviourExecutor;
import toutouchien.itemsadderadditions.behaviours.BehaviourHost;
import toutouchien.itemsadderadditions.utils.loading.AbstractItemsAdderItemLoader;
import toutouchien.itemsadderadditions.utils.other.ExecutorRegistry;

/**
 * Reads every ItemsAdder item's {@code behaviours:} section and instantiates the
 * matching custom behaviour executors.
 *
 * <p>Built-in ItemsAdder behaviour keys are ignored automatically because they are
 * simply absent from the registry.
 */
@NullMarked
public final class BehaviourLoader extends AbstractItemsAdderItemLoader {
    private final ExecutorRegistry<BehaviourExecutor> registry;

    public BehaviourLoader(ExecutorRegistry<BehaviourExecutor> registry) {
        super("Behaviours", "behaviour binding(s)");
        this.registry = registry;
    }

    @Override
    protected void beforeLoad() {
        BehaviourBindings.clear();
    }

    @Override
    protected int loadItem(ItemLoadContext context) {
        FileConfiguration config = context.config();
        ConfigurationSection behavioursSection = config.getConfigurationSection("items." + context.itemId() + ".behaviours");
        if (behavioursSection == null) {
            return 0;
        }

        int count = 0;
        BehaviourHost host = new BehaviourHost(context.namespacedId(), context.category(), ItemsAdderAdditions.instance());

        for (String behaviourKey : behavioursSection.getKeys(false)) {
            BehaviourExecutor prototype = registry.getPrototype(behaviourKey);
            if (prototype == null) {
                continue;
            }

            BehaviourExecutor instance = prototype.newInstance();
            Object configValue = behavioursSection.get(behaviourKey);

            if (!instance.configure(configValue, context.namespacedId())) {
                continue;
            }

            instance.load(host);
            BehaviourBindings.add(context.namespacedId(), instance);
            count++;
        }

        return count;
    }
}
