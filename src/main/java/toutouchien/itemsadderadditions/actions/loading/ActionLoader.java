package toutouchien.itemsadderadditions.actions.loading;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.TriggerKey;
import toutouchien.itemsadderadditions.actions.TriggerType;
import toutouchien.itemsadderadditions.utils.other.ExecutorRegistry;
import toutouchien.itemsadderadditions.utils.other.ItemCategory;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.Map;
import java.util.Set;

/**
 * Reads every CustomStack's YAML config and populates {@link ActionBindings}.
 *
 * <h3>Standard (non-argumentized) trigger</h3>
 * <pre>
 * events:
 *   block_break:
 *     veinminer:
 *       max_blocks: 32
 * </pre>
 *
 * <h3>Argumentized trigger - wildcard (fire on any interaction)</h3>
 * When no argument sub-key is present the actions are registered as a wildcard
 * and will execute regardless of which specific interaction occurred.
 *
 * <pre>
 * events:
 *   interact:           # argumentized trigger, no argument qualifier
 *     actionbar:
 *       text: "&lt;green&gt;You interacted!"
 * </pre>
 *
 * <h3>Argumentized trigger - qualified (fire only for specific interactions)</h3>
 * Add one or more argument sub-keys to restrict execution to those interaction types.
 * The set of argumentized trigger names per category is declared in
 * {@link #ITEM_ARGUMENTIZED}, {@link #FURNITURE_ARGUMENTIZED}, etc.
 *
 * <pre>
 * # Item / block
 * events:
 *   interact:           # argumentized trigger
 *     right:            # event argument - fires only on right-click
 *       title:
 *         title: "&lt;gold&gt;Right-clicked!"
 *     left_shift:       # event argument - fires only on sneak-left-click
 *       actionbar:
 *         text: "&lt;red&gt;Shift-left!"
 *
 * # Furniture
 * events:
 *   placed_furniture:
 *     interact:         # argumentized trigger
 *       entity:         # event argument - fires only on entity right-click
 *         play_animation:
 *           name: wave
 * </pre>
 *
 * <h3>Known argument values for interact triggers</h3>
 * {@code right}, {@code left}, {@code right_shift}, {@code left_shift}, {@code entity}
 */
@SuppressWarnings("unused")
@NullMarked
public final class ActionLoader {
    private final ExecutorRegistry<ActionExecutor> registry;

    public ActionLoader(ExecutorRegistry<ActionExecutor> registry) {
        this.registry = registry;
    }

    private static final Map<String, TriggerType> ITEM_TRIGGERS = Map.ofEntries(
            // Interact
            Map.entry("interact", TriggerType.ITEM_INTERACT),
            Map.entry("interact_mainhand", TriggerType.ITEM_INTERACT_MAINHAND),
            Map.entry("interact_offhand", TriggerType.ITEM_INTERACT_OFFHAND),
            // Block & Combat
            Map.entry("block_break", TriggerType.ITEM_BREAK_BLOCK),
            Map.entry("attack", TriggerType.ITEM_ATTACK),
            Map.entry("kill", TriggerType.ITEM_KILL),
            // Inventory
            Map.entry("drop", TriggerType.ITEM_DROP),
            Map.entry("pickup", TriggerType.ITEM_PICKUP),
            Map.entry("held", TriggerType.ITEM_HELD),
            Map.entry("held_offhand", TriggerType.ITEM_HELD_OFFHAND),
            Map.entry("unheld", TriggerType.ITEM_UNHELD),
            Map.entry("unheld_offhand", TriggerType.ITEM_UNHELD_OFFHAND),
            Map.entry("item_break", TriggerType.ITEM_BREAK),
            // Consumable
            Map.entry("eat", TriggerType.ITEM_EAT),
            Map.entry("drink", TriggerType.ITEM_DRINK),
            // Ranged
            Map.entry("bow_shot", TriggerType.ITEM_BOW_SHOT),
            Map.entry("gun_shot", TriggerType.ITEM_GUN_SHOT),
            Map.entry("gun_no_ammo", TriggerType.ITEM_GUN_NO_AMMO),
            Map.entry("gun_reload", TriggerType.ITEM_GUN_RELOAD),
            Map.entry("item_throw", TriggerType.ITEM_THROW),
            Map.entry("item_hit_ground", TriggerType.ITEM_HIT_GROUND),
            Map.entry("item_hit_entity", TriggerType.ITEM_HIT_ENTITY),
            // Books
            Map.entry("book_write", TriggerType.ITEM_BOOK_WRITE),
            Map.entry("book_read", TriggerType.ITEM_BOOK_READ),
            // Fishing
            Map.entry("fishing_start", TriggerType.ITEM_FISHING_START),
            Map.entry("fishing_caught", TriggerType.ITEM_FISHING_CAUGHT),
            Map.entry("fishing_failed", TriggerType.ITEM_FISHING_FAILED),
            Map.entry("fishing_cancel", TriggerType.ITEM_FISHING_CANCEL),
            Map.entry("fishing_bite", TriggerType.ITEM_FISHING_BITE),
            Map.entry("fishing_in_ground", TriggerType.ITEM_FISHING_IN_GROUND),
            // Buckets
            Map.entry("bucket_empty", TriggerType.ITEM_BUCKET_EMPTY),
            Map.entry("bucket_fill", TriggerType.ITEM_BUCKET_FILL)
    );

    private static final Map<String, TriggerType> BLOCK_TRIGGERS = Map.of(
            "interact", TriggerType.BLOCK_INTERACT
    );

    private static final Map<String, TriggerType> FURNITURE_TRIGGERS = Map.ofEntries(
            Map.entry("interact", TriggerType.FURNITURE_INTERACT),
            Map.entry("interact_mainhand", TriggerType.FURNITURE_INTERACT_MAINHAND),
            Map.entry("interact_offhand", TriggerType.FURNITURE_INTERACT_OFFHAND),
            Map.entry("attack", TriggerType.FURNITURE_ATTACK),
            Map.entry("kill", TriggerType.FURNITURE_KILL),
            Map.entry("drop", TriggerType.FURNITURE_DROP),
            Map.entry("pickup", TriggerType.FURNITURE_PICKUP),
            Map.entry("eat", TriggerType.FURNITURE_EAT),
            Map.entry("drink", TriggerType.FURNITURE_DRINK),
            Map.entry("bow_shot", TriggerType.FURNITURE_BOW_SHOT),
            Map.entry("gun_shot", TriggerType.FURNITURE_GUN_SHOT),
            Map.entry("gun_no_ammo", TriggerType.FURNITURE_GUN_NO_AMMO),
            Map.entry("gun_reload", TriggerType.FURNITURE_GUN_RELOAD),
            Map.entry("book_write", TriggerType.FURNITURE_BOOK_WRITE),
            Map.entry("book_read", TriggerType.FURNITURE_BOOK_READ),
            Map.entry("fishing_start", TriggerType.FURNITURE_FISHING_START),
            Map.entry("fishing_caught", TriggerType.FURNITURE_FISHING_CAUGHT),
            Map.entry("fishing_failed", TriggerType.FURNITURE_FISHING_FAILED),
            Map.entry("fishing_cancel", TriggerType.FURNITURE_FISHING_CANCEL),
            Map.entry("fishing_bite", TriggerType.FURNITURE_FISHING_BITE),
            Map.entry("fishing_in_ground", TriggerType.FURNITURE_FISHING_IN_GROUND),
            Map.entry("wear", TriggerType.FURNITURE_WEAR),
            Map.entry("unwear", TriggerType.FURNITURE_UNWEAR),
            Map.entry("held", TriggerType.FURNITURE_HELD),
            Map.entry("held_offhand", TriggerType.FURNITURE_HELD_OFFHAND),
            Map.entry("unheld", TriggerType.FURNITURE_UNHELD),
            Map.entry("unheld_offhand", TriggerType.FURNITURE_UNHELD_OFFHAND),
            Map.entry("item_throw", TriggerType.FURNITURE_THROW),
            Map.entry("item_hit_ground", TriggerType.FURNITURE_HIT_GROUND),
            Map.entry("item_hit_entity", TriggerType.FURNITURE_HIT_ENTITY),
            Map.entry("item_break", TriggerType.FURNITURE_BREAK),
            Map.entry("bucket_empty", TriggerType.FURNITURE_BUCKET_EMPTY),
            Map.entry("bucket_fill", TriggerType.FURNITURE_BUCKET_FILL)
    );

    private static final Map<String, TriggerType> COMPLEX_FURNITURE_TRIGGERS = Map.of(
            "interact", TriggerType.COMPLEX_FURNITURE_INTERACT
    );

    /*
     * Argumentized trigger names
     *
     * Some trigger names accept an extra "event argument" sub-key in YAML.
     * The event argument appears between the trigger name and the action map.
     *
     * Example YAML:
     *
     *   interact:           # trigger name (argumentized)
     *     right:            # event argument
     *       actionbar:      # action
     *         text: "…"
     *
     * To add support for a new event that uses an argument sub-key, simply add
     * the trigger name to the appropriate set below.
     * No other loader code changes are required.
     */

    /**
     * Trigger names that carry an event argument for the ITEM category.
     */
    private static final Set<String> ITEM_ARGUMENTIZED = Set.of(
            "interact", "interact_mainhand", "interact_offhand"
    );

    /**
     * Trigger names that carry an event argument for the FURNITURE category.
     */
    private static final Set<String> FURNITURE_ARGUMENTIZED = Set.of(
            "interact", "interact_mainhand", "interact_offhand"
    );

    /**
     * Trigger names that carry an event argument for the BLOCK category.
     */
    private static final Set<String> BLOCK_ARGUMENTIZED = Set.of();

    /**
     * Trigger names that carry an event argument for the COMPLEX_FURNITURE category.
     */
    private static final Set<String> COMPLEX_FURNITURE_ARGUMENTIZED = Set.of();

    public void load() {
        ActionBindings.clear();
        int totalActions = 0;

        for (CustomStack customStack : ItemsAdder.getAllItems())
            totalActions += loadItem(customStack);

        Log.loaded("Actions", totalActions, "action binding(s)");
    }

    private int loadItem(CustomStack customStack) {
        FileConfiguration config = customStack.getConfig();
        String itemID = customStack.getId();
        String namespacedID = customStack.getNamespacedID();

        ItemCategory category = ItemCategory.determine(customStack, config, itemID);
        ConfigurationSection events = resolveEventsSection(config, itemID, category);
        if (events == null)
            return 0;

        int count = 0;
        for (String triggerName : events.getKeys(false)) {
            ConfigurationSection triggerSection = events.getConfigurationSection(triggerName);
            if (triggerSection == null)
                continue;

            TriggerType type = resolveTriggerType(triggerName, category);
            if (type == null) {
                // Unknown to this system - likely a native ItemsAdder action; skip silently.
                continue;
            }

            String bindingKey = resolveBindingKey(config, itemID, namespacedID, category, type);

            if (isArgumentized(triggerName, category))
                count += parseArgumentizedTrigger(triggerSection, bindingKey, type, namespacedID);
            else
                count += parseActionsFromSection(triggerSection, bindingKey, TriggerKey.of(type), namespacedID);
        }

        return count;
    }

    /**
     * Parses an argumentized trigger section that may be in one of two layouts:
     *
     * <h4>Argument-qualified (fires only for the listed interactions)</h4>
     * <pre>
     * interact:
     *   right:          &lt;- argument sub-key
     *     actionbar:
     *       text: "..."
     *   left_shift:     &lt;- another argument sub-key
     *     title:
     *       title: "..."
     * </pre>
     *
     * <h4>Wildcard (fires for any interaction, argument sub-key omitted)</h4>
     * <pre>
     * interact:
     *   actionbar:      &lt;- action key directly under the trigger (no qualifier)
     *     text: "..."
     * </pre>
     * <p>
     * Detection: if any direct child key is a registered action prototype, the whole
     * section is treated as a wildcard and stored under the null-argument
     * {@link TriggerKey}. Otherwise every child key is treated as an argument qualifier.
     */
    private int parseArgumentizedTrigger(
            ConfigurationSection triggerSection,
            String bindingKey,
            TriggerType type,
            String itemName
    ) {
        Set<String> subKeys = triggerSection.getKeys(false);

        // Detect wildcard layout: if any sub-key is a registered action, there are no
        // argument qualifiers - the actions apply to every interaction variant.
        boolean isWildcard = subKeys.stream().anyMatch(k -> registry.getPrototype(k) != null);
        if (isWildcard) {
            return parseActionsFromSection(triggerSection, bindingKey, TriggerKey.of(type), itemName);
        }

        // Argument-qualified layout: each sub-key is an event argument.
        int count = 0;
        for (String argument : subKeys) {
            ConfigurationSection argumentSection = triggerSection.getConfigurationSection(argument);
            if (argumentSection == null) {
                // A plain scalar under an argumentized trigger - likely a config mistake.
                Log.itemWarn("Actions", itemName,
                        "expected a sub-section under trigger '{}', got a plain value for key '{}' " +
                                "(did you forget an event argument like 'right:' or 'left_shift:'?)",
                        type, argument);
                continue;
            }

            TriggerKey key = TriggerKey.of(type, argument);
            count += parseActionsFromSection(argumentSection, bindingKey, key, itemName);
        }

        return count;
    }

    /**
     * Reads action keys from {@code triggerSection} and registers each one against
     * {@code bindingKey} + {@code triggerKey}.
     */
    private int parseActionsFromSection(
            ConfigurationSection triggerSection,
            String bindingKey,
            TriggerKey triggerKey,
            String itemName
    ) {
        int count = 0;

        for (String actionKey : triggerSection.getKeys(false)) {
            ActionExecutor prototype = registry.getPrototype(actionKey);
            if (prototype == null) {
                // Native ItemsAdder action - skip silently.
                continue;
            }

            if (!prototype.isAllowedFor(triggerKey.type())) {
                Log.itemWarn("Actions", itemName,
                        "action '{}' is not allowed for trigger '{}' - skipping",
                        actionKey, triggerKey.type());
                continue;
            }

            ActionExecutor instance = prototype.newInstance();
            ConfigurationSection actionSection = triggerSection.getConfigurationSection(actionKey);

            if (instance.configure(actionSection, itemName)) {
                ActionBindings.add(bindingKey, triggerKey, instance);
                count++;
            }
        }

        return count;
    }

    @Nullable
    private ConfigurationSection resolveEventsSection(FileConfiguration config, String itemID, ItemCategory category) {
        String eventsPath = "items." + itemID + ".events";

        return switch (category) {
            case COMPLEX_FURNITURE, FURNITURE -> config.getConfigurationSection(eventsPath + ".placed_furniture");
            case BLOCK, ITEM -> config.getConfigurationSection(eventsPath);
        };
    }

    /**
     * Returns the {@link ActionBindings} key for this (item, trigger) pair.
     *
     * <p>Complex-furniture interact events are keyed by the entity's namespaced ID
     * (because the listener detects them via the spawned armor-stand entity, not the
     * held item). All other triggers use the item's own namespaced ID.
     */
    private String resolveBindingKey(
            FileConfiguration config,
            String itemID,
            String namespacedID,
            ItemCategory category,
            TriggerType type
    ) {
        if (category != ItemCategory.COMPLEX_FURNITURE || type != TriggerType.COMPLEX_FURNITURE_INTERACT)
            return namespacedID;

        String entityID = config.getString("items." + itemID + ".behaviours.complex_furniture.entity");
        if (entityID == null)
            return namespacedID;

        String namespace = namespacedID.substring(0, namespacedID.indexOf(':'));
        return namespace + ":" + entityID;
    }

    @Nullable
    private TriggerType resolveTriggerType(String triggerName, ItemCategory category) {
        return switch (category) {
            case COMPLEX_FURNITURE -> COMPLEX_FURNITURE_TRIGGERS.get(triggerName);
            case FURNITURE -> FURNITURE_TRIGGERS.get(triggerName);
            case BLOCK -> BLOCK_TRIGGERS.get(triggerName);
            case ITEM -> ITEM_TRIGGERS.get(triggerName);
        };
    }

    private boolean isArgumentized(String triggerName, ItemCategory category) {
        return switch (category) {
            case ITEM -> ITEM_ARGUMENTIZED.contains(triggerName);
            case FURNITURE -> FURNITURE_ARGUMENTIZED.contains(triggerName);
            case BLOCK -> BLOCK_ARGUMENTIZED.contains(triggerName);
            case COMPLEX_FURNITURE -> COMPLEX_FURNITURE_ARGUMENTIZED.contains(triggerName);
        };
    }
}
