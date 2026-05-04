package toutouchien.itemsadderadditions.actions.loading;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.TriggerKey;
import toutouchien.itemsadderadditions.actions.TriggerType;
import toutouchien.itemsadderadditions.utils.loading.AbstractItemsAdderItemLoader;
import toutouchien.itemsadderadditions.utils.other.ExecutorRegistry;
import toutouchien.itemsadderadditions.utils.other.ItemCategory;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.Map;
import java.util.Set;

/**
 * Reads each ItemsAdder item's {@code events:} section and turns matching custom
 * action keys into runtime bindings.
 *
 * <h3>Trigger maps</h3>
 * Each category (item, block, furniture, complex-furniture) has its own map from
 * the YAML trigger name to a {@link TriggerEntry} that pairs the {@link TriggerType}
 * with a flag indicating whether the trigger accepts event-argument sub-keys (e.g.
 * {@code "right:"}, {@code "left_shift:"}).
 *
 * <h3>Argumentized triggers</h3>
 * When a trigger is argumentized, the loader checks whether the YAML section below
 * the trigger key contains action keys directly (<em>wildcard</em> layout) or
 * argument sub-keys containing action keys (<em>qualified</em> layout). Both are
 * supported and stored separately.
 */
@NullMarked
@SuppressWarnings("unused")
public final class ActionLoader extends AbstractItemsAdderItemLoader {
    private static final Map<String, TriggerEntry> ITEM_TRIGGERS = Map.ofEntries(
            Map.entry("interact", new TriggerEntry(TriggerType.ITEM_INTERACT, true)),
            Map.entry("interact_mainhand", new TriggerEntry(TriggerType.ITEM_INTERACT_MAINHAND, true)),
            Map.entry("interact_offhand", new TriggerEntry(TriggerType.ITEM_INTERACT_OFFHAND, true)),
            Map.entry("block_break", new TriggerEntry(TriggerType.ITEM_BREAK_BLOCK, false)),
            Map.entry("attack", new TriggerEntry(TriggerType.ITEM_ATTACK, false)),
            Map.entry("kill", new TriggerEntry(TriggerType.ITEM_KILL, false)),
            Map.entry("drop", new TriggerEntry(TriggerType.ITEM_DROP, false)),
            Map.entry("pickup", new TriggerEntry(TriggerType.ITEM_PICKUP, false)),
            Map.entry("held", new TriggerEntry(TriggerType.ITEM_HELD, false)),
            Map.entry("held_offhand", new TriggerEntry(TriggerType.ITEM_HELD_OFFHAND, false)),
            Map.entry("unheld", new TriggerEntry(TriggerType.ITEM_UNHELD, false)),
            Map.entry("unheld_offhand", new TriggerEntry(TriggerType.ITEM_UNHELD_OFFHAND, false)),
            Map.entry("item_break", new TriggerEntry(TriggerType.ITEM_BREAK, false)),
            Map.entry("eat", new TriggerEntry(TriggerType.ITEM_EAT, false)),
            Map.entry("drink", new TriggerEntry(TriggerType.ITEM_DRINK, false)),
            Map.entry("bow_shot", new TriggerEntry(TriggerType.ITEM_BOW_SHOT, false)),
            Map.entry("gun_shot", new TriggerEntry(TriggerType.ITEM_GUN_SHOT, false)),
            Map.entry("gun_no_ammo", new TriggerEntry(TriggerType.ITEM_GUN_NO_AMMO, false)),
            Map.entry("gun_reload", new TriggerEntry(TriggerType.ITEM_GUN_RELOAD, false)),
            Map.entry("item_throw", new TriggerEntry(TriggerType.ITEM_THROW, false)),
            Map.entry("item_hit_ground", new TriggerEntry(TriggerType.ITEM_HIT_GROUND, false)),
            Map.entry("item_hit_entity", new TriggerEntry(TriggerType.ITEM_HIT_ENTITY, false)),
            Map.entry("book_write", new TriggerEntry(TriggerType.ITEM_BOOK_WRITE, false)),
            Map.entry("book_read", new TriggerEntry(TriggerType.ITEM_BOOK_READ, false)),
            Map.entry("fishing_start", new TriggerEntry(TriggerType.ITEM_FISHING_START, false)),
            Map.entry("fishing_caught", new TriggerEntry(TriggerType.ITEM_FISHING_CAUGHT, false)),
            Map.entry("fishing_failed", new TriggerEntry(TriggerType.ITEM_FISHING_FAILED, false)),
            Map.entry("fishing_cancel", new TriggerEntry(TriggerType.ITEM_FISHING_CANCEL, false)),
            Map.entry("fishing_bite", new TriggerEntry(TriggerType.ITEM_FISHING_BITE, false)),
            Map.entry("fishing_in_ground", new TriggerEntry(TriggerType.ITEM_FISHING_IN_GROUND, false)),
            Map.entry("bucket_empty", new TriggerEntry(TriggerType.ITEM_BUCKET_EMPTY, false)),
            Map.entry("bucket_fill", new TriggerEntry(TriggerType.ITEM_BUCKET_FILL, false))
    );

    /**
     * Trigger maps per item category
     * Convention: if an entry has argumentized = true, the loader will also look
     * for per-argument sub-sections under the trigger key in YAML.
     */
    private static final Map<String, TriggerEntry> BLOCK_TRIGGERS = Map.of(
            "interact", new TriggerEntry(TriggerType.BLOCK_INTERACT, false),
            "break", new TriggerEntry(TriggerType.PLACED_BLOCK_BREAK, false)
    );
    private static final Map<String, TriggerEntry> FURNITURE_TRIGGERS = Map.ofEntries(
            Map.entry("interact", new TriggerEntry(TriggerType.FURNITURE_INTERACT, true)),
            Map.entry("interact_mainhand", new TriggerEntry(TriggerType.FURNITURE_INTERACT_MAINHAND, true)),
            Map.entry("interact_offhand", new TriggerEntry(TriggerType.FURNITURE_INTERACT_OFFHAND, true)),
            Map.entry("attack", new TriggerEntry(TriggerType.FURNITURE_ATTACK, false)),
            Map.entry("kill", new TriggerEntry(TriggerType.FURNITURE_KILL, false)),
            Map.entry("drop", new TriggerEntry(TriggerType.FURNITURE_DROP, false)),
            Map.entry("pickup", new TriggerEntry(TriggerType.FURNITURE_PICKUP, false)),
            Map.entry("eat", new TriggerEntry(TriggerType.FURNITURE_EAT, false)),
            Map.entry("drink", new TriggerEntry(TriggerType.FURNITURE_DRINK, false)),
            Map.entry("bow_shot", new TriggerEntry(TriggerType.FURNITURE_BOW_SHOT, false)),
            Map.entry("gun_shot", new TriggerEntry(TriggerType.FURNITURE_GUN_SHOT, false)),
            Map.entry("gun_no_ammo", new TriggerEntry(TriggerType.FURNITURE_GUN_NO_AMMO, false)),
            Map.entry("gun_reload", new TriggerEntry(TriggerType.FURNITURE_GUN_RELOAD, false)),
            Map.entry("book_write", new TriggerEntry(TriggerType.FURNITURE_BOOK_WRITE, false)),
            Map.entry("book_read", new TriggerEntry(TriggerType.FURNITURE_BOOK_READ, false)),
            Map.entry("fishing_start", new TriggerEntry(TriggerType.FURNITURE_FISHING_START, false)),
            Map.entry("fishing_caught", new TriggerEntry(TriggerType.FURNITURE_FISHING_CAUGHT, false)),
            Map.entry("fishing_failed", new TriggerEntry(TriggerType.FURNITURE_FISHING_FAILED, false)),
            Map.entry("fishing_cancel", new TriggerEntry(TriggerType.FURNITURE_FISHING_CANCEL, false)),
            Map.entry("fishing_bite", new TriggerEntry(TriggerType.FURNITURE_FISHING_BITE, false)),
            Map.entry("fishing_in_ground", new TriggerEntry(TriggerType.FURNITURE_FISHING_IN_GROUND, false)),
            Map.entry("wear", new TriggerEntry(TriggerType.FURNITURE_WEAR, false)),
            Map.entry("unwear", new TriggerEntry(TriggerType.FURNITURE_UNWEAR, false)),
            Map.entry("held", new TriggerEntry(TriggerType.FURNITURE_HELD, false)),
            Map.entry("held_offhand", new TriggerEntry(TriggerType.FURNITURE_HELD_OFFHAND, false)),
            Map.entry("unheld", new TriggerEntry(TriggerType.FURNITURE_UNHELD, false)),
            Map.entry("unheld_offhand", new TriggerEntry(TriggerType.FURNITURE_UNHELD_OFFHAND, false)),
            Map.entry("item_throw", new TriggerEntry(TriggerType.FURNITURE_THROW, false)),
            Map.entry("item_hit_ground", new TriggerEntry(TriggerType.FURNITURE_HIT_GROUND, false)),
            Map.entry("item_hit_entity", new TriggerEntry(TriggerType.FURNITURE_HIT_ENTITY, false)),
            Map.entry("item_break", new TriggerEntry(TriggerType.FURNITURE_BREAK, false)),
            Map.entry("bucket_empty", new TriggerEntry(TriggerType.FURNITURE_BUCKET_EMPTY, false)),
            Map.entry("bucket_fill", new TriggerEntry(TriggerType.FURNITURE_BUCKET_FILL, false))
    );
    private static final Map<String, TriggerEntry> COMPLEX_FURNITURE_TRIGGERS = Map.of(
            "interact", new TriggerEntry(TriggerType.COMPLEX_FURNITURE_INTERACT, false)
    );
    private final ExecutorRegistry<ActionExecutor> registry;

    public ActionLoader(ExecutorRegistry<ActionExecutor> registry) {
        super("Actions", "action binding(s)");
        this.registry = registry;
    }

    private static Map<String, TriggerEntry> triggerMapFor(ItemCategory category) {
        return switch (category) {
            case ITEM -> ITEM_TRIGGERS;
            case BLOCK -> BLOCK_TRIGGERS;
            case FURNITURE -> FURNITURE_TRIGGERS;
            case COMPLEX_FURNITURE -> COMPLEX_FURNITURE_TRIGGERS;
        };
    }

    @Override
    protected void beforeLoad() {
        ActionBindings.clear();
    }

    @Override
    protected int loadItem(ItemLoadContext context) {
        ConfigurationSection events = resolveEventsSection(context);
        if (events == null) return 0;

        int count = parseTriggersInSection(
                events, context.config(), context.itemId(), context.namespacedId(), context.category());

        // Blocks also expose a nested "placed_block" sub-section for block-specific events.
        if (context.category() == ItemCategory.BLOCK) {
            ConfigurationSection placedBlock = events.getConfigurationSection("placed_block");
            if (placedBlock != null) {
                count += parseTriggersInSection(
                        placedBlock, context.config(), context.itemId(),
                        context.namespacedId(), context.category());
            }
        }

        return count;
    }

    private int parseTriggersInSection(
            ConfigurationSection section,
            FileConfiguration config,
            String itemID,
            String namespacedID,
            ItemCategory category
    ) {
        int count = 0;

        for (String triggerName : section.getKeys(false)) {
            ConfigurationSection triggerSection = section.getConfigurationSection(triggerName);
            if (triggerSection == null) continue;

            TriggerEntry entry = triggerMapFor(category).get(triggerName);
            if (entry == null) continue;

            String bindingKey = resolveBindingKey(config, itemID, namespacedID, category, entry.type());

            if (entry.argumentized()) {
                count += parseArgumentizedTrigger(triggerSection, bindingKey, entry.type(), namespacedID);
            } else {
                count += parseActionsFromSection(
                        triggerSection, bindingKey, TriggerKey.of(entry.type()), namespacedID);
            }
        }

        return count;
    }

    /**
     * Parses an argumentized trigger section that may use either:
     * <ul>
     *   <li><em>Wildcard layout</em> - action keys directly under the trigger
     *       (fires for any argument).</li>
     *   <li><em>Qualified layout</em> - argument keys (e.g. {@code "right:"}) that
     *       each contain action keys (fires only for that specific argument).</li>
     * </ul>
     */
    private int parseArgumentizedTrigger(
            ConfigurationSection triggerSection,
            String bindingKey,
            TriggerType type,
            String itemName
    ) {
        Set<String> subKeys = triggerSection.getKeys(false);

        // If any direct sub-key matches a registered action, treat the whole section
        // as a wildcard (no argument filter).
        boolean isWildcard = subKeys.stream().anyMatch(k -> registry.getPrototype(k) != null);
        if (isWildcard) {
            return parseActionsFromSection(triggerSection, bindingKey, TriggerKey.of(type), itemName);
        }

        int count = 0;
        for (String argument : subKeys) {
            ConfigurationSection argumentSection = triggerSection.getConfigurationSection(argument);
            if (argumentSection == null) {
                Log.itemWarn("Actions", itemName,
                        "expected a sub-section under trigger '{}' for argument '{}' "
                                + "(did you forget an event argument like 'right:' or 'left_shift:'?)",
                        type, argument);
                continue;
            }

            count += parseActionsFromSection(
                    argumentSection, bindingKey, TriggerKey.of(type, argument), itemName);
        }

        return count;
    }

    /**
     * Reads action keys from {@code section} and registers each matching one.
     */
    private int parseActionsFromSection(
            ConfigurationSection section,
            String bindingKey,
            TriggerKey triggerKey,
            String itemName
    ) {
        int count = 0;

        for (String actionKey : section.getKeys(false)) {
            ActionExecutor prototype = registry.getPrototype(actionKey);
            if (prototype == null) continue;

            if (!prototype.isAllowedFor(triggerKey.type())) {
                Log.itemWarn("Actions", itemName,
                        "action '{}' is not allowed for trigger '{}' - skipping",
                        actionKey, triggerKey.type());
                continue;
            }

            ActionExecutor instance = prototype.newInstance();
            ConfigurationSection actionSection = section.getConfigurationSection(actionKey);

            if (instance.configure(actionSection, itemName)) {
                ActionBindings.add(bindingKey, triggerKey, instance);
                count++;
            }
        }

        return count;
    }

    @Nullable
    private ConfigurationSection resolveEventsSection(ItemLoadContext context) {
        String eventsPath = "items." + context.itemId() + ".events";

        return switch (context.category()) {
            case COMPLEX_FURNITURE, FURNITURE ->
                    context.config().getConfigurationSection(eventsPath + ".placed_furniture");
            case BLOCK, ITEM -> context.config().getConfigurationSection(eventsPath);
        };
    }

    /**
     * Returns the binding key for this item/trigger combination.
     *
     * <p>For complex-furniture interact triggers the binding key is the entity's namespaced ID
     * (from {@code behaviours.complex_furniture.entity}), not the item's own ID - because the
     * listener dispatches on the entity ID, not the held-item ID.
     */
    private String resolveBindingKey(
            FileConfiguration config,
            String itemID,
            String namespacedID,
            ItemCategory category,
            TriggerType type
    ) {
        if (category != ItemCategory.COMPLEX_FURNITURE
                || type != TriggerType.COMPLEX_FURNITURE_INTERACT) {
            return namespacedID;
        }

        String entityID = config.getString("items." + itemID + ".behaviours.complex_furniture.entity");
        if (entityID == null) return namespacedID;

        String namespace = namespacedID.substring(0, namespacedID.indexOf(':'));
        return namespace + ":" + entityID;
    }

    /**
     * Pairs a {@link TriggerType} with its argumentized flag.
     *
     * @param type         the resolved trigger type
     * @param argumentized {@code true} when the trigger can carry an event-argument sub-key
     *                     (e.g. {@code "right:"}, {@code "left_shift:"}); {@code false} otherwise
     */
    private record TriggerEntry(TriggerType type, boolean argumentized) {}
}
