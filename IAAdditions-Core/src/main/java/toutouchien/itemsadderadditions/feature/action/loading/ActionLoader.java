package toutouchien.itemsadderadditions.feature.action.loading;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.common.loading.AbstractItemsAdderItemLoader;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.registry.ExecutorRegistry;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.TriggerKey;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

import java.util.Set;

/**
 * Reads ItemsAdder {@code events:} sections and creates runtime action bindings.
 */
@NullMarked
public final class ActionLoader extends AbstractItemsAdderItemLoader {
    private static final String LOG_TAG = "Actions";

    private final ExecutorRegistry<ActionExecutor> registry;

    public ActionLoader(ExecutorRegistry<ActionExecutor> registry) {
        super(LOG_TAG, "action binding(s)");
        this.registry = registry;
    }

    @Nullable
    private static ConfigurationSection resolveEventsSection(ItemLoadContext context) {
        String eventsPath = "items." + context.itemId() + ".events";
        return switch (context.category()) {
            case COMPLEX_FURNITURE, FURNITURE ->
                    context.config().getConfigurationSection(eventsPath + ".placed_furniture");
            case BLOCK, ITEM -> context.config().getConfigurationSection(eventsPath);
        };
    }

    /**
     * Complex-furniture interact events dispatch on the entity ID declared in
     * {@code behaviours.complex_furniture.entity}; every other trigger dispatches
     * on the item's own namespaced ID.
     */
    private static String bindingKey(
            FileConfiguration config,
            String itemId,
            String namespacedId,
            ItemCategory category,
            TriggerType type
    ) {
        if (category != ItemCategory.COMPLEX_FURNITURE
                || type != TriggerType.COMPLEX_FURNITURE_INTERACT) {
            return namespacedId;
        }

        String entityId = config.getString("items." + itemId + ".behaviours.complex_furniture.entity");
        if (entityId == null) {
            return namespacedId;
        }

        String namespace = namespacedId.substring(0, namespacedId.indexOf(':'));
        return namespace + ":" + entityId;
    }

    @Override
    protected String requiredItemSection() {
        return "events";
    }

    @Override
    protected void beforeLoad() {
        ActionBindings.clear();
    }

    @Override
    protected int loadItem(ItemLoadContext context) {
        ConfigurationSection events = resolveEventsSection(context);
        if (events == null) {
            return 0;
        }

        int count = parseTriggerSection(events, context);

        if (context.category() == ItemCategory.BLOCK) {
            ConfigurationSection placedBlock = events.getConfigurationSection("placed_block");
            if (placedBlock != null) {
                count += parseTriggerSection(placedBlock, context);
            }
        }

        return count;
    }

    private int parseTriggerSection(ConfigurationSection section, ItemLoadContext context) {
        int count = 0;

        for (String triggerName : section.getKeys(false)) {
            ConfigurationSection triggerSection = section.getConfigurationSection(triggerName);
            if (triggerSection == null) {
                continue;
            }

            TriggerDefinition trigger = TriggerCatalog.forCategory(context.category()).get(triggerName);
            if (trigger == null) {
                continue;
            }

            String bindingKey = bindingKey(context.config(), context.itemId(), context.namespacedId(), context.category(), trigger.type());
            count += trigger.argumentized()
                    ? parseArgumentizedTrigger(triggerSection, bindingKey, trigger.type(), context.namespacedId())
                    : parseActions(triggerSection, bindingKey, TriggerKey.of(trigger.type()), context.namespacedId());
        }

        return count;
    }

    /**
     * Argumentized triggers support both direct wildcard actions and qualified
     * argument sections such as {@code right:} or {@code left_shift:}.
     */
    private int parseArgumentizedTrigger(
            ConfigurationSection triggerSection,
            String bindingKey,
            TriggerType type,
            String itemName
    ) {
        Set<String> subKeys = triggerSection.getKeys(false);
        boolean wildcardLayout = subKeys.stream().anyMatch(key -> registry.getPrototype(key) != null);
        if (wildcardLayout) {
            return parseActions(triggerSection, bindingKey, TriggerKey.of(type), itemName);
        }

        int count = 0;
        for (String argument : subKeys) {
            ConfigurationSection argumentSection = triggerSection.getConfigurationSection(argument);
            if (argumentSection == null) {
                Log.itemWarn(LOG_TAG, itemName,
                        "expected a sub-section under trigger '{}' for argument '{}'",
                        type, argument);
                continue;
            }

            count += parseActions(argumentSection, bindingKey, TriggerKey.of(type, argument), itemName);
        }
        return count;
    }

    private int parseActions(
            ConfigurationSection section,
            String bindingKey,
            TriggerKey triggerKey,
            String itemName
    ) {
        int count = 0;

        for (String actionKey : section.getKeys(false)) {
            ActionExecutor prototype = registry.getPrototype(actionKey);
            if (prototype == null) {
                continue;
            }

            if (!prototype.isAllowedFor(triggerKey.type())) {
                Log.itemWarn(LOG_TAG, itemName,
                        "action '{}' is not allowed for trigger '{}' - skipping",
                        actionKey, triggerKey.type());
                continue;
            }

            ActionExecutor instance = prototype.newInstance();
            if (instance.configure(section.getConfigurationSection(actionKey), itemName)) {
                ActionBindings.add(bindingKey, triggerKey, instance);
                count++;
            }
        }

        return count;
    }
}
