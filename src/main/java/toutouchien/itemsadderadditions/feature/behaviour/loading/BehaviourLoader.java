package toutouchien.itemsadderadditions.feature.behaviour.loading;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.loading.AbstractItemsAdderItemLoader;
import toutouchien.itemsadderadditions.common.registry.ExecutorRegistry;
import toutouchien.itemsadderadditions.feature.behaviour.BehaviourExecutor;
import toutouchien.itemsadderadditions.feature.behaviour.BehaviourHost;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads every ItemsAdder item's {@code behaviours:} section and instantiates the
 * matching custom behaviour executors.
 *
 * <p>Built-in ItemsAdder behaviour keys are ignored automatically because they are
 * simply absent from the registry.
 *
 * <p>Behaviours defined on a template item (via {@code variant_of}) are inherited by
 * all concrete items that extend it. Child definitions override parent definitions for
 * the same behaviour key.
 */
@NullMarked
public final class BehaviourLoader extends AbstractItemsAdderItemLoader {
    private final ExecutorRegistry<BehaviourExecutor> registry;

    public BehaviourLoader(ExecutorRegistry<BehaviourExecutor> registry) {
        super("Behaviours", "behaviour binding(s)");
        this.registry = registry;
    }

    @Override
    protected String requiredItemSection() {
        // Cannot pre-filter by own behaviours section alone: an item may inherit all its
        // behaviours from a variant_of ancestor. We do a cheap early-exit in loadItem instead.
        return null;
    }

    @Override
    protected void beforeLoad() {
        BehaviourBindings.clear();
    }

    @Override
    protected int loadItem(ItemLoadContext context) {
        // Templates are never placed in the world; their behaviours exist only to be inherited.
        if (context.config().getBoolean("items." + context.itemId() + ".template", false)) return 0;

        Map<String, Object> behaviours = collectMergedBehaviours(context);
        if (behaviours.isEmpty()) return 0;

        int count = 0;
        BehaviourHost host = new BehaviourHost(context.namespacedId(), context.category(), ItemsAdderAdditions.instance());

        for (Map.Entry<String, Object> entry : behaviours.entrySet()) {
            BehaviourExecutor prototype = registry.getPrototype(entry.getKey());
            if (prototype == null) continue;

            BehaviourExecutor instance = prototype.newInstance();
            if (!instance.configure(entry.getValue(), context.namespacedId())) continue;

            instance.load(host);
            BehaviourBindings.add(context.namespacedId(), instance);
            count++;
        }

        return count;
    }

    /**
     * Returns a merged map of behaviour key → config value for the given item,
     * combining its own {@code behaviours:} section with those of all {@code variant_of}
     * ancestors. Ancestors are applied from root to child so child definitions win.
     */
    private Map<String, Object> collectMergedBehaviours(ItemLoadContext context) {
        String namespacedId = context.namespacedId();
        String namespace = namespacedId.contains(":") ? namespacedId.substring(0, namespacedId.indexOf(':')) : "";

        // Walk up the variant_of chain, collecting ancestor behaviour sections root-first.
        List<ConfigurationSection> chain = new ArrayList<>();
        String currentId = context.itemId();
        FileConfiguration currentConfig = context.config();

        for (int depth = 0; depth < 16; depth++) {
            String variantOf = currentConfig.getString("items." + currentId + ".variant_of");
            if (variantOf == null || variantOf.isBlank()) break;

            String parentId = variantOf.contains(":") ? variantOf.substring(variantOf.indexOf(':') + 1) : variantOf;

            if (currentConfig.contains("items." + parentId)) {
                currentId = parentId;
            } else {
                String parentNsId = variantOf.contains(":") ? variantOf : namespace + ":" + variantOf;
                CustomStack parent = CustomStack.getInstance(parentNsId);
                if (parent == null) break;
                currentConfig = parent.getConfig();
                currentId = parent.getId();
            }

            ConfigurationSection section = currentConfig.getConfigurationSection("items." + currentId + ".behaviours");
            if (section != null) chain.add(0, section); // prepend so root ends up first
        }

        // Append the item's own behaviours last so they override any ancestor.
        ConfigurationSection own = context.config().getConfigurationSection("items." + context.itemId() + ".behaviours");
        if (own != null) chain.add(own);

        if (chain.isEmpty()) return Map.of();

        // Merge root → child; later entries overwrite earlier ones for the same key.
        Map<String, Object> merged = new LinkedHashMap<>();
        for (ConfigurationSection section : chain) {
            for (String key : section.getKeys(false)) {
                merged.put(key, section.get(key));
            }
        }
        return merged;
    }
}
