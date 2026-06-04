package toutouchien.itemsadderadditions.feature.component;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.registry.ExecutorRegistry;
import toutouchien.itemsadderadditions.feature.component.binding.GenericComponentBinding;
import toutouchien.itemsadderadditions.feature.component.model.ComponentKey;
import toutouchien.itemsadderadditions.feature.component.parse.ComponentTreeParser;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.nms.api.component.ComponentValue;
import toutouchien.itemsadderadditions.nms.api.component.INmsItemComponentHandler;
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
 * <p>Lookup order per component key:</p>
 * <ol>
 *   <li>Raw key matches a registered specialized handler: use it (existing Paper API path).</li>
 *   <li>No specialized match: normalize key, attempt generic NMS codec path.</li>
 *   <li>Generic backend unsupported on this version: warn and skip.</li>
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
 *       custom_data:
 *         my_plugin:
 *           id: my_item
 *       minecraft:repairable:
 *         items: "#minecraft:planks"
 * </pre>
 */
@NullMarked
public final class ComponentsManager implements ReloadableContentSystem {
    private static final String NAME = "Components";

    private final ExecutorRegistry<ComponentExecutor> registry = new ExecutorRegistry<>(NAME);
    private volatile Map<String, List<ComponentExecutor>> specializedBindings = Map.of();
    private volatile Map<String, List<GenericComponentBinding>> genericBindings = Map.of();

    public ComponentsManager(PluginSettings settings) {
        applySettings(settings);
    }

    public void applySettings(PluginSettings settings) {
        registry.registerBuiltIns(settings::componentEnabled, BuiltInComponents.create());
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
        Map<String, List<ComponentExecutor>> newSpecialized = new HashMap<>();
        Map<String, List<GenericComponentBinding>> newGeneric = new HashMap<>();
        int total = 0;

        for (CustomStack item : context.items()) {
            String namespacedId = item.getNamespacedID();
            FileConfiguration config = item.getConfig();

            ConfigurationSection section =
                    config.getConfigurationSection("items." + item.getId() + ".components");
            if (section == null) continue;

            List<ComponentExecutor> specialized = new ArrayList<>();
            List<GenericComponentBinding> generic = new ArrayList<>();
            loadItemComponents(section, namespacedId, specialized, generic);

            if (!specialized.isEmpty()) {
                newSpecialized.put(namespacedId, List.copyOf(specialized));
                total += specialized.size();
            }

            if (!generic.isEmpty()) {
                newGeneric.put(namespacedId, List.copyOf(generic));
                total += generic.size();
            }
        }

        this.specializedBindings = Map.copyOf(newSpecialized);
        this.genericBindings = Map.copyOf(newGeneric);
        Log.loaded(NAME, total, "component(s)");
        return ReloadStepResult.loaded(NAME, total);
    }

    public ExecutorRegistry<ComponentExecutor> registry() {
        return registry;
    }

    private void loadItemComponents(
            ConfigurationSection section,
            String namespacedId,
            List<ComponentExecutor> specialized,
            List<GenericComponentBinding> generic
    ) {
        for (String key : section.getKeys(false)) {
            // 1. Try specialized handler. Prefer the raw IAA key, but also let
            // minecraft:<key> resolve to a native IAA component before falling
            // back to the generic NMS path.
            ComponentExecutor prototype = specializedPrototype(key);
            if (prototype != null) {
                if (!prototype.isSupportedOnCurrentVersion()) {
                    Log.warn(NAME, "Component '{}' on '{}' requires a newer Minecraft version - skipping.", key, namespacedId);
                    continue;
                }

                try {
                    ComponentExecutor instance = prototype.newInstance();
                    if (instance.configure(section.get(key), namespacedId)) {
                        specialized.add(instance);
                    }
                } catch (RuntimeException e) {
                    Log.itemWarn(NAME, namespacedId,
                            "Component '{}' failed to configure: {}", key, e.getMessage());
                }
                continue;
            }

            // 2. Generic fallback: normalize key and add as generic binding.
            ComponentKey normalized = ComponentKey.from(key);
            if (!normalized.isValid()) {
                Log.itemWarn(NAME, namespacedId, "Component key '{}' is invalid - skipping.", key);
                continue;
            }

            try {
                ComponentValue value = ComponentTreeParser.parse(section.get(key));
                generic.add(new GenericComponentBinding(normalized, value));
            } catch (RuntimeException e) {
                Log.itemWarn(NAME, namespacedId,
                        "Generic component '{}' failed to parse: {}", key, e.getMessage());
            }
        }
    }

    private @Nullable ComponentExecutor specializedPrototype(String key) {
        ComponentExecutor prototype = registry.getPrototype(key);
        if (prototype != null) return prototype;

        ComponentKey normalized = ComponentKey.from(key);
        String prefix = "minecraft:";
        if (!normalized.normalized().startsWith(prefix)) return null;

        return registry.getPrototype(normalized.normalized().substring(prefix.length()));
    }

    public ItemStack applyComponents(String namespacedID, ItemStack itemStack) {
        return applyBindings(
                itemStack,
                namespacedID,
                specializedBindings.get(namespacedID),
                genericBindings.get(namespacedID)
        );
    }

    /**
     * Applies a raw {@code components:} config section to an arbitrary stack using
     * the same specialized + generic component pipeline as normal item definitions.
     *
     * @param itemStack          stack to clone and modify
     * @param componentsSection  raw YAML {@code components:} section
     * @param context            label used in warnings (item id, recipe id, etc.)
     * @return cloned stack with all successfully-applied components
     */
    public ItemStack applyComponentsToStack(
            ItemStack itemStack,
            ConfigurationSection componentsSection,
            String context
    ) {
        List<ComponentExecutor> specialized = new ArrayList<>();
        List<GenericComponentBinding> generic = new ArrayList<>();
        loadItemComponents(componentsSection, context, specialized, generic);
        return applyBindings(itemStack.clone(), context, specialized, generic);
    }

    private ItemStack applyBindings(
            ItemStack itemStack,
            String context,
            @Nullable List<ComponentExecutor> executors,
            @Nullable List<GenericComponentBinding> generics
    ) {
        if (executors != null) {
            for (ComponentExecutor executor : executors) {
                try {
                    itemStack = executor.apply(itemStack, context);
                } catch (RuntimeException e) {
                    Log.itemWarn(NAME, context,
                            "Component '{}' failed to apply: {}", executor.key(), e.getMessage());
                }
            }
        }

        if (generics != null) {
            INmsItemComponentHandler handler = NmsManager.instance().handler().itemComponents();
            for (GenericComponentBinding binding : generics) {
                itemStack = binding.apply(itemStack, context, handler);
            }
        }

        return itemStack;
    }
}
