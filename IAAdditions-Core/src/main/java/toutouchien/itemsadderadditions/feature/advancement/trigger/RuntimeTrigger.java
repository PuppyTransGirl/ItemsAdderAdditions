package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.utils.EnumUtils;

@NullMarked
public enum RuntimeTrigger {
    OBTAIN_ITEM,
    CONSUME_ITEM,
    PLACE_BLOCK,
    BREAK_BLOCK,
    PLACE_FURNITURE,
    BREAK_FURNITURE,
    INTERACT_FURNITURE,
    CRAFT_RECIPE,
    KILL_ENTITY_WITH_ITEM,
    PERMISSION,
    IN_BIOME,
    USING_ITEM,
    TAME_ANIMAL,
    VILLAGER_TRADE,
    ENCHANTED_ITEM,
    SLEPT_IN_BED,
    BRED_ANIMALS,
    CHANGED_DIMENSION,
    PLAYER_HURT_ENTITY,
    ENTITY_HURT_PLAYER,
    SHOOT_BOW,
    FISHING_ROD_HOOKED,
    FILLED_BUCKET,
    PLAYER_KILLED_ENTITY,
    RECIPE_UNLOCKED,
    USED_TOTEM,
    EFFECTS_CHANGED,
    FALL_FROM_HEIGHT,
    USED_ENDER_EYE,
    BEE_NEST_DESTROYED,
    ENTITY_KILLED_PLAYER,
    ITEM_DURABILITY_CHANGED,
    ITEM_USED_ON_BLOCK,
    KILLED_BY_ARROW,
    PLAYER_INTERACTED_WITH_ENTITY,
    PLAYER_SHEARED_EQUIPMENT,
    RECIPE_CRAFTED,
    SHOT_CROSSBOW,
    STARTED_RIDING,
    HELD_ITEM,
    IMPOSSIBLE;

    @Nullable
    public static RuntimeTrigger fromYaml(String name) {
        return EnumUtils.match(name, RuntimeTrigger.class, null);
    }
}
