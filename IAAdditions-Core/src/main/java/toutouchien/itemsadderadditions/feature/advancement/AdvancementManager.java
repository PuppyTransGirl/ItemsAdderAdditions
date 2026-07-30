package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.loading.CategorizedConfigFile;
import toutouchien.itemsadderadditions.common.loading.ConfigFileCategory;
import toutouchien.itemsadderadditions.common.loading.ConfigFileRegistry;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.nms.api.AdvancementSpec;
import toutouchien.itemsadderadditions.nms.api.INmsAdvancementHandler;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;
import toutouchien.itemsadderadditions.runtime.reload.ReloadableContentSystem;

import java.util.*;

@NullMarked
public final class AdvancementManager implements ReloadableContentSystem {
    private static final String LOG_TAG = "AdvancementManager";

    private final Plugin plugin;
    private final AdvancementRegistry registry = new AdvancementRegistry();
    private final AdvancementRuntimeService runtimeService;
    private final INmsAdvancementHandler advancementHandler;
    private Map<NamespacedKey, AdvancementSpec> registeredSpecs = Map.of();

    public AdvancementManager(Plugin plugin) {
        this(plugin, NmsManager.instance().handler().advancements());
    }

    AdvancementManager(Plugin plugin, INmsAdvancementHandler advancementHandler) {
        this.plugin = plugin;
        this.runtimeService = new AdvancementRuntimeService(registry, plugin);
        this.advancementHandler = advancementHandler;
    }

    private static List<AdvancementDefinition> loadAll(ConfigFileRegistry fileRegistry) {
        List<AdvancementDefinition> result = new ArrayList<>();
        for (CategorizedConfigFile ccf : fileRegistry.getFiles(ConfigFileCategory.ADVANCEMENTS)) {
            YamlConfiguration yaml = ccf.yaml();
            String namespace = yaml.getString("info.namespace");
            if (namespace == null || namespace.isBlank()) {
                Log.warn(LOG_TAG, "Advancement file missing 'info.namespace', skipping.");
                continue;
            }
            ConfigurationSection sec = yaml.getConfigurationSection("advancements");
            result.addAll(AdvancementLoader.loadAll(namespace, sec));
        }
        return result;
    }

    @Override
    public String name() {
        return "Advancements";
    }

    @Override
    public ReloadPhase phase() {
        return ReloadPhase.CONTENT_FILES;
    }

    @Override
    public ReloadStepResult reload(ContentReloadContext context) {
        long start = System.currentTimeMillis();
        Log.debug(LOG_TAG, "Loading custom advancements...");

        runtimeService.unregister();

        List<AdvancementDefinition> defs = loadAll(context.registry());
        List<AdvancementSpec> specs = AdvancementSpecBuilder.buildAll(defs);

        registry.setAll(defs);
        synchronizeRegistrations(specs);
        runtimeService.register(plugin);

        Log.info(LOG_TAG, "Loaded {} advancement(s) in {}ms.",
                defs.size(), System.currentTimeMillis() - start);

        return ReloadStepResult.loaded(name(), defs.size());
    }

    public void shutdown() {
        runtimeService.unregister();
        advancementHandler.unregisterAll(registeredSpecs.keySet());
        registeredSpecs = Map.of();
        registry.clear();
    }

    public AdvancementRegistry registry() {
        return registry;
    }

    void synchronizeRegistrations(List<AdvancementSpec> specs) {
        Map<NamespacedKey, AdvancementSpec> nextSpecs = indexByKey(specs);
        if (registeredSpecs.equals(nextSpecs)) {
            Log.debug(LOG_TAG, "Advancement definitions are unchanged; keeping existing registrations and player progress.");
            return;
        }

        advancementHandler.replaceAll(Set.copyOf(registeredSpecs.keySet()), specs);
        registeredSpecs = nextSpecs;
    }

    private static Map<NamespacedKey, AdvancementSpec> indexByKey(List<AdvancementSpec> specs) {
        Map<NamespacedKey, AdvancementSpec> byKey = new LinkedHashMap<>();
        for (AdvancementSpec spec : specs) {
            byKey.put(spec.key(), spec);
        }
        return Map.copyOf(byKey);
    }
}
