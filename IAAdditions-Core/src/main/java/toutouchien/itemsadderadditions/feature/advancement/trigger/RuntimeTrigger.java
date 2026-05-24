package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
    IMPOSSIBLE;

    @Nullable
    public static RuntimeTrigger fromYaml(String name) {
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "obtain_item" -> OBTAIN_ITEM;
            case "consume_item" -> CONSUME_ITEM;
            case "place_block" -> PLACE_BLOCK;
            case "break_block" -> BREAK_BLOCK;
            case "place_furniture" -> PLACE_FURNITURE;
            case "break_furniture" -> BREAK_FURNITURE;
            case "interact_furniture" -> INTERACT_FURNITURE;
            case "craft_recipe" -> CRAFT_RECIPE;
            case "kill_entity_with_item" -> KILL_ENTITY_WITH_ITEM;
            case "permission" -> PERMISSION;
            case "in_biome" -> IN_BIOME;
            case "using_item" -> USING_ITEM;
            case "tame_animal" -> TAME_ANIMAL;
            case "villager_trade" -> VILLAGER_TRADE;
            case "enchanted_item" -> ENCHANTED_ITEM;
            case "slept_in_bed" -> SLEPT_IN_BED;
            case "bred_animals" -> BRED_ANIMALS;
            case "changed_dimension" -> CHANGED_DIMENSION;
            case "player_hurt_entity" -> PLAYER_HURT_ENTITY;
            case "entity_hurt_player" -> ENTITY_HURT_PLAYER;
            case "shoot_bow" -> SHOOT_BOW;
            case "fishing_rod_hooked" -> FISHING_ROD_HOOKED;
            case "filled_bucket" -> FILLED_BUCKET;
            case "impossible" -> IMPOSSIBLE;
            default -> null;
        };
    }
}
