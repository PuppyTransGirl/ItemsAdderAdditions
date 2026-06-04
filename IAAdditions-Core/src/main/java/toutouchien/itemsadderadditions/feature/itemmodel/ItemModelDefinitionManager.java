package toutouchien.itemsadderadditions.feature.itemmodel;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.resourcepack.ResourcePackFiles;
import toutouchien.itemsadderadditions.common.version.VersionUtils;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.nms.api.component.INmsItemComponentHandler;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;
import toutouchien.itemsadderadditions.runtime.reload.ReloadableContentSystem;
import toutouchien.itemsadderadditions.settings.PluginFeature;
import toutouchien.itemsadderadditions.settings.PluginSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@NullMarked
public final class ItemModelDefinitionManager implements ReloadableContentSystem {
    public static final String NAME = "ItemModelDefinition";

    private final ItemModelDefinitionParser parser = new ItemModelDefinitionParser();
    private final ItemModelDefinitionWriter writer = new ItemModelDefinitionWriter();
    private volatile Map<String, ItemModelComponentBinding> bindings = Map.of();
    private volatile boolean enabled;

    public ItemModelDefinitionManager(PluginSettings settings) {
        applySettings(settings);
    }

    public void setup() {
        ResourcePackFiles.ensureItemsAdderMergeFolder(NAME);
    }

    public void applySettings(PluginSettings settings) {
        this.enabled = settings.featureEnabled(PluginFeature.ITEM_MODEL_DEFINITIONS);
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
        if (!enabled) {
            this.bindings = Map.of();
            return ReloadStepResult.unchanged(NAME);
        }

        int configured = countConfigured(context);
        if (configured == 0) {
            this.bindings = Map.of();
            return ReloadStepResult.loaded(NAME, 0);
        }

        if (!supportsCurrentVersion()) {
            this.bindings = Map.of();
            Log.warn(NAME,
                    "{} item_model_definition section(s) ignored: item model definitions require Minecraft 1.21.4 or newer. Current: {}.",
                    configured,
                    VersionUtils.version());
            return ReloadStepResult.loaded(NAME, 0);
        }

        Map<String, ItemModelComponentBinding> newBindings = new HashMap<>();
        Map<String, ItemModelDefinitionData> definitions = new HashMap<>();

        for (CustomStack item : context.items()) {
            Optional<ItemModelDefinitionData> parsed = parser.parse(item);
            if (parsed.isEmpty()) continue;

            ItemModelDefinitionData data = parsed.get();
            definitions.put(data.namespacedItemId(), data);
            if (data.applyItemModelComponent() || data.customModelData() != null) {
                newBindings.put(data.namespacedItemId(), new ItemModelComponentBinding(
                        data.definitionPath().id(),
                        data.customModelData()
                ));
            }
        }

        ItemModelDefinitionWriter.WriteResult result = writer.write(definitions.values());
        this.bindings = Map.copyOf(newBindings);

        Log.success(NAME,
                "Generated {} item model definition(s) ({} file(s) changed, {} existing file(s) skipped) - run /iazip to apply resource pack changes.",
                result.definitions(), result.changedFiles(), result.skippedExistingFiles());
        return ReloadStepResult.loaded(NAME, result.definitions());
    }

    public ItemStack applyItemModelDefinitionComponents(String namespacedId, ItemStack itemStack) {
        ItemModelComponentBinding binding = bindings.get(namespacedId);
        if (binding == null) return itemStack;

        INmsItemComponentHandler handler = NmsManager.instance().handler().itemComponents();
        return binding.apply(itemStack, namespacedId, handler);
    }

    private int countConfigured(ContentReloadContext context) {
        int configured = 0;
        for (CustomStack item : context.items()) {
            if (item.getConfig().contains("items." + item.getId() + ".item_model_definition")) configured++;
        }
        return configured;
    }

    private boolean supportsCurrentVersion() {
        VersionUtils current = VersionUtils.version();
        return current != VersionUtils.UNKNOWN && VersionUtils.isHigherThanOrEquals(VersionUtils.v1_21_4);
    }
}
