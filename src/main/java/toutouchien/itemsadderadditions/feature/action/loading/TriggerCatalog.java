package toutouchien.itemsadderadditions.feature.action.loading;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

import java.util.Map;

/**
 * All ItemsAdder event keys understood by the actions system.
 */
@NullMarked
final class TriggerCatalog {
    private static final Map<String, TriggerDefinition> ITEM = Map.ofEntries(
            trigger("interact", TriggerType.ITEM_INTERACT, true),
            trigger("interact_mainhand", TriggerType.ITEM_INTERACT_MAINHAND, true),
            trigger("interact_offhand", TriggerType.ITEM_INTERACT_OFFHAND, true),
            trigger("block_break", TriggerType.ITEM_BREAK_BLOCK),
            trigger("attack", TriggerType.ITEM_ATTACK),
            trigger("kill", TriggerType.ITEM_KILL),
            trigger("drop", TriggerType.ITEM_DROP),
            trigger("pickup", TriggerType.ITEM_PICKUP),
            trigger("held", TriggerType.ITEM_HELD),
            trigger("held_offhand", TriggerType.ITEM_HELD_OFFHAND),
            trigger("unheld", TriggerType.ITEM_UNHELD),
            trigger("unheld_offhand", TriggerType.ITEM_UNHELD_OFFHAND),
            trigger("item_break", TriggerType.ITEM_BREAK),
            trigger("eat", TriggerType.ITEM_EAT),
            trigger("drink", TriggerType.ITEM_DRINK),
            trigger("bow_shot", TriggerType.ITEM_BOW_SHOT),
            trigger("gun_shot", TriggerType.ITEM_GUN_SHOT),
            trigger("gun_no_ammo", TriggerType.ITEM_GUN_NO_AMMO),
            trigger("gun_reload", TriggerType.ITEM_GUN_RELOAD),
            trigger("item_throw", TriggerType.ITEM_THROW),
            trigger("item_hit_ground", TriggerType.ITEM_HIT_GROUND),
            trigger("item_hit_entity", TriggerType.ITEM_HIT_ENTITY),
            trigger("book_write", TriggerType.ITEM_BOOK_WRITE),
            trigger("book_read", TriggerType.ITEM_BOOK_READ),
            trigger("fishing_start", TriggerType.ITEM_FISHING_START),
            trigger("fishing_caught", TriggerType.ITEM_FISHING_CAUGHT),
            trigger("fishing_failed", TriggerType.ITEM_FISHING_FAILED),
            trigger("fishing_cancel", TriggerType.ITEM_FISHING_CANCEL),
            trigger("fishing_bite", TriggerType.ITEM_FISHING_BITE),
            trigger("fishing_in_ground", TriggerType.ITEM_FISHING_IN_GROUND),
            trigger("bucket_empty", TriggerType.ITEM_BUCKET_EMPTY),
            trigger("bucket_fill", TriggerType.ITEM_BUCKET_FILL)
    );

    private static final Map<String, TriggerDefinition> BLOCK = Map.of(
            "interact", new TriggerDefinition(TriggerType.BLOCK_INTERACT, false),
            "break", new TriggerDefinition(TriggerType.PLACED_BLOCK_BREAK, false)
    );

    private static final Map<String, TriggerDefinition> FURNITURE = Map.ofEntries(
            trigger("interact", TriggerType.FURNITURE_INTERACT, true),
            trigger("interact_mainhand", TriggerType.FURNITURE_INTERACT_MAINHAND, true),
            trigger("interact_offhand", TriggerType.FURNITURE_INTERACT_OFFHAND, true),
            trigger("attack", TriggerType.FURNITURE_ATTACK),
            trigger("kill", TriggerType.FURNITURE_KILL),
            trigger("drop", TriggerType.FURNITURE_DROP),
            trigger("pickup", TriggerType.FURNITURE_PICKUP),
            trigger("eat", TriggerType.FURNITURE_EAT),
            trigger("drink", TriggerType.FURNITURE_DRINK),
            trigger("bow_shot", TriggerType.FURNITURE_BOW_SHOT),
            trigger("gun_shot", TriggerType.FURNITURE_GUN_SHOT),
            trigger("gun_no_ammo", TriggerType.FURNITURE_GUN_NO_AMMO),
            trigger("gun_reload", TriggerType.FURNITURE_GUN_RELOAD),
            trigger("book_write", TriggerType.FURNITURE_BOOK_WRITE),
            trigger("book_read", TriggerType.FURNITURE_BOOK_READ),
            trigger("fishing_start", TriggerType.FURNITURE_FISHING_START),
            trigger("fishing_caught", TriggerType.FURNITURE_FISHING_CAUGHT),
            trigger("fishing_failed", TriggerType.FURNITURE_FISHING_FAILED),
            trigger("fishing_cancel", TriggerType.FURNITURE_FISHING_CANCEL),
            trigger("fishing_bite", TriggerType.FURNITURE_FISHING_BITE),
            trigger("fishing_in_ground", TriggerType.FURNITURE_FISHING_IN_GROUND),
            trigger("wear", TriggerType.FURNITURE_WEAR),
            trigger("unwear", TriggerType.FURNITURE_UNWEAR),
            trigger("held", TriggerType.FURNITURE_HELD),
            trigger("held_offhand", TriggerType.FURNITURE_HELD_OFFHAND),
            trigger("unheld", TriggerType.FURNITURE_UNHELD),
            trigger("unheld_offhand", TriggerType.FURNITURE_UNHELD_OFFHAND),
            trigger("item_throw", TriggerType.FURNITURE_THROW),
            trigger("item_hit_ground", TriggerType.FURNITURE_HIT_GROUND),
            trigger("item_hit_entity", TriggerType.FURNITURE_HIT_ENTITY),
            trigger("item_break", TriggerType.FURNITURE_BREAK),
            trigger("bucket_empty", TriggerType.FURNITURE_BUCKET_EMPTY),
            trigger("bucket_fill", TriggerType.FURNITURE_BUCKET_FILL)
    );

    private static final Map<String, TriggerDefinition> COMPLEX_FURNITURE = Map.of(
            "interact", new TriggerDefinition(TriggerType.COMPLEX_FURNITURE_INTERACT, false)
    );

    private TriggerCatalog() {
    }

    static Map<String, TriggerDefinition> forCategory(ItemCategory category) {
        return switch (category) {
            case ITEM -> ITEM;
            case BLOCK -> BLOCK;
            case FURNITURE -> FURNITURE;
            case COMPLEX_FURNITURE -> COMPLEX_FURNITURE;
        };
    }

    private static Map.Entry<String, TriggerDefinition> trigger(String key, TriggerType type) {
        return trigger(key, type, false);
    }

    private static Map.Entry<String, TriggerDefinition> trigger(String key, TriggerType type, boolean argumentized) {
        return Map.entry(key, new TriggerDefinition(type, argumentized));
    }
}
