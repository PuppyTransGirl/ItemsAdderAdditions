package toutouchien.itemsadderadditions.feature.advancement;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
public sealed interface AdvancementConditions permits
        AdvancementConditions.ObtainItem,
        AdvancementConditions.ConsumeItem,
        AdvancementConditions.PlaceBlock,
        AdvancementConditions.BreakBlock,
        AdvancementConditions.PlaceFurniture,
        AdvancementConditions.BreakFurniture,
        AdvancementConditions.InteractFurniture,
        AdvancementConditions.CraftRecipe,
        AdvancementConditions.KillEntityWithItem,
        AdvancementConditions.Permission,
        AdvancementConditions.InBiome,
        AdvancementConditions.UsingItem,
        AdvancementConditions.TameAnimal,
        AdvancementConditions.EnchantedItem,
        AdvancementConditions.BredAnimals,
        AdvancementConditions.ChangedDimension,
        AdvancementConditions.PlayerHurtEntity,
        AdvancementConditions.EntityHurtPlayer,
        AdvancementConditions.ShootBow,
        AdvancementConditions.None {

    record ObtainItem(List<String> itemIds, int amount) implements AdvancementConditions {}

    record ConsumeItem(String itemId) implements AdvancementConditions {}

    record PlaceBlock(String blockId) implements AdvancementConditions {}

    record BreakBlock(String blockId) implements AdvancementConditions {}

    record PlaceFurniture(String furnitureId) implements AdvancementConditions {}

    record BreakFurniture(String furnitureId) implements AdvancementConditions {}

    record InteractFurniture(String furnitureId) implements AdvancementConditions {}

    record CraftRecipe(String recipeId) implements AdvancementConditions {}

    record KillEntityWithItem(String itemId, @Nullable String entityType) implements AdvancementConditions {}

    record Permission(String node) implements AdvancementConditions {}

    record InBiome(String biomeId, @Nullable String world) implements AdvancementConditions {}

    record UsingItem(String itemId) implements AdvancementConditions {}

    record TameAnimal(@Nullable String entityType) implements AdvancementConditions {}

    record EnchantedItem(@Nullable String itemId) implements AdvancementConditions {}

    record BredAnimals(@Nullable String entityType) implements AdvancementConditions {}

    record ChangedDimension(@Nullable String dimension) implements AdvancementConditions {}

    record PlayerHurtEntity(@Nullable String itemId, @Nullable String entityType) implements AdvancementConditions {}

    record EntityHurtPlayer(@Nullable String entityType) implements AdvancementConditions {}

    record ShootBow(@Nullable String itemId) implements AdvancementConditions {}

    record None() implements AdvancementConditions {
        public static final None INSTANCE = new None();
    }
}
