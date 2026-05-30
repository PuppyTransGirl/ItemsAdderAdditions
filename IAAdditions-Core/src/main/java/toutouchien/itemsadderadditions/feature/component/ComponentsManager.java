package toutouchien.itemsadderadditions.feature.component;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.registry.ExecutorRegistry;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;
import toutouchien.itemsadderadditions.runtime.reload.ReloadableContentSystem;
import toutouchien.itemsadderadditions.settings.PluginSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages item data component injection for all custom items.
 *
 * <p>The modifier hook is registered once on {@link #registerModifier()} and stays
 * active for the plugin's lifetime. During each reload, {@link #reload} rebuilds the
 * bindings map from the items' {@code components:} config sections. The modifier then
 * does a simple map lookup and applies the pre-configured executors.</p>
 *
 * <h3>Adding a new component</h3>
 * <ol>
 *   <li>Extend {@link ComponentExecutor} and annotate with {@link toutouchien.itemsadderadditions.feature.component.annotation.Component @Component(key = "...")}.</li>
 *   <li>Override {@link ComponentExecutor#minimumVersion()} if the component requires
 *       a specific Minecraft version.</li>
 *   <li>Add an instance to {@link BuiltInComponents#create()}.</li>
 * </ol>
 *
 * <h3>YAML structure</h3>
 * <pre>
 * items:
 *   my_item:
 *     components:
 *       rarity: RARE
 *       use_cooldown:
 *         cooldown_seconds: 1.5
 * </pre>
 */
@NullMarked
public final class ComponentsManager implements ReloadableContentSystem {
    private static final String NAME = "Components";

    private final ExecutorRegistry<ComponentExecutor> registry = new ExecutorRegistry<>(NAME);
    private volatile Map<String, List<ComponentExecutor>> bindings = Map.of();

    public ComponentsManager(PluginSettings settings) {
        applySettings(settings);
    }

    public void applySettings(PluginSettings settings) {
        registry.registerBuiltIns(settings::componentEnabled, BuiltInComponents.create());
    }

    /**
     * Registers the item modifier with ItemsAdder. Must be called once on plugin enable.
     * The modifier itself is lightweight — it delegates to pre-built bindings.
     */
    public void registerModifier() {
        ItemsAdder.Advanced.injectItemModifier(ItemsAdderAdditions.instance(), this::applyComponents);
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
        Map<String, List<ComponentExecutor>> newBindings = new HashMap<>();
        int total = 0;

        for (CustomStack item : context.items()) {
            String namespacedId = item.getNamespacedID();
            FileConfiguration config = item.getConfig();

            ConfigurationSection section =
                    config.getConfigurationSection("items." + item.getId() + ".components");
            if (section == null) continue;

            List<ComponentExecutor> executors = loadItemComponents(section, namespacedId);
            if (executors.isEmpty()) continue;

            newBindings.put(namespacedId, List.copyOf(executors));
            total += executors.size();
        }

        this.bindings = Map.copyOf(newBindings);
        Log.loaded(NAME, total, "component(s)");
        return ReloadStepResult.loaded(NAME, total);
    }

    public ExecutorRegistry<ComponentExecutor> registry() {
        return registry;
    }

    private List<ComponentExecutor> loadItemComponents(ConfigurationSection section, String namespacedId) {
        List<ComponentExecutor> executors = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            ComponentExecutor prototype = registry.getPrototype(key);
            if (prototype == null) continue;

            if (!prototype.isSupportedOnCurrentVersion()) {
                Log.warn(NAME, "Component '{}' on '{}' requires a newer Minecraft version — skipping.", key, namespacedId);
                continue;
            }

            ComponentExecutor instance = prototype.newInstance();
            if (!instance.configure(section.get(key), namespacedId)) continue;

            executors.add(instance);
        }

        return executors;
    }

    private ItemStack applyComponents(String namespacedID, ItemStack itemStack) {
        List<ComponentExecutor> executors = bindings.get(namespacedID);
        if (executors == null) return itemStack;

        for (ComponentExecutor executor : executors) {
            itemStack = executor.apply(itemStack, namespacedID);
        }

        return itemStack;
    }
}
