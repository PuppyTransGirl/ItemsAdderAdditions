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
        AdvancementConditions.SitFurniture,
        AdvancementConditions.UnsitFurniture,
        AdvancementConditions.OpenTradeMachine,
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
        AdvancementConditions.FilledBucket,
        AdvancementConditions.FishingRodHooked,
        AdvancementConditions.VillagerTrade,
        AdvancementConditions.PlayerKilledEntity,
        AdvancementConditions.RecipeUnlocked,
        AdvancementConditions.EffectsChanged,
        AdvancementConditions.BeeNestDestroyed,
        AdvancementConditions.EntityKilledPlayer,
        AdvancementConditions.ItemDurabilityChanged,
        AdvancementConditions.ItemUsedOnBlock,
        AdvancementConditions.KilledByArrow,
        AdvancementConditions.PlayerInteractedWithEntity,
        AdvancementConditions.PlayerShearedEquipment,
        AdvancementConditions.RecipeCrafted,
        AdvancementConditions.ShotCrossbow,
        AdvancementConditions.StartedRiding,
        AdvancementConditions.HeldItem,
        AdvancementConditions.FallFromHeight,
        AdvancementConditions.None {

    record ObtainItem(List<String> itemIds, int amount) implements AdvancementConditions {}

    record ConsumeItem(String itemId) implements AdvancementConditions {}

    record PlaceBlock(String blockId) implements AdvancementConditions {}

    record BreakBlock(String blockId) implements AdvancementConditions {}

    record PlaceFurniture(String furnitureId) implements AdvancementConditions {}

    record BreakFurniture(String furnitureId) implements AdvancementConditions {}

    record InteractFurniture(String furnitureId) implements AdvancementConditions {}

    record SitFurniture(String furnitureId) implements AdvancementConditions {}

    record UnsitFurniture(String furnitureId) implements AdvancementConditions {}

    record OpenTradeMachine(String tradeMachineId) implements AdvancementConditions {}

    record CraftRecipe(String recipeId) implements AdvancementConditions {}

    record KillEntityWithItem(String itemId, @Nullable String entityType) implements AdvancementConditions {}

    record Permission(String node) implements AdvancementConditions {}

    record InBiome(String biomeId, @Nullable String world) implements AdvancementConditions {}

    record UsingItem(String itemId) implements AdvancementConditions {}

    record TameAnimal(@Nullable String entityType) implements AdvancementConditions {}

    /** @param minLevels 0 = no minimum; @param maxLevels {@link Integer#MAX_VALUE} = no maximum */
    record EnchantedItem(@Nullable String itemId, int minLevels, int maxLevels) implements AdvancementConditions {}

    record BredAnimals(@Nullable String entityType, @Nullable String parentType, @Nullable String partnerType) implements AdvancementConditions {}

    record ChangedDimension(@Nullable String to, @Nullable String from) implements AdvancementConditions {}

    record PlayerHurtEntity(@Nullable String itemId, @Nullable String entityType) implements AdvancementConditions {}

    record EntityHurtPlayer(@Nullable String entityType) implements AdvancementConditions {}

    record ShootBow(@Nullable String itemId) implements AdvancementConditions {}

    record FilledBucket(@Nullable String itemId) implements AdvancementConditions {}

    record FishingRodHooked(@Nullable String rod, @Nullable String caughtEntityType) implements AdvancementConditions {}

    record VillagerTrade(@Nullable String itemId) implements AdvancementConditions {}

    record PlayerKilledEntity(@Nullable String entityType, @Nullable String itemId) implements AdvancementConditions {}

    record RecipeUnlocked(String recipe) implements AdvancementConditions {}

    record EffectsChanged(@Nullable String effect) implements AdvancementConditions {}

    record BeeNestDestroyed(@Nullable String blockId) implements AdvancementConditions {}

    record EntityKilledPlayer(@Nullable String entityType) implements AdvancementConditions {}

    record ItemDurabilityChanged(@Nullable String itemId) implements AdvancementConditions {}

    record ItemUsedOnBlock(@Nullable String itemId, @Nullable String blockId) implements AdvancementConditions {}

    record KilledByArrow(@Nullable String entityType) implements AdvancementConditions {}

    record PlayerInteractedWithEntity(@Nullable String entityType, @Nullable String itemId) implements AdvancementConditions {}

    record PlayerShearedEquipment(@Nullable String entityType) implements AdvancementConditions {}

    record RecipeCrafted(String recipeId) implements AdvancementConditions {}

    record ShotCrossbow(@Nullable String itemId) implements AdvancementConditions {}

    record StartedRiding(@Nullable String entityType) implements AdvancementConditions {}

    record HeldItem(@Nullable String itemId) implements AdvancementConditions {}

    /** @param minDistance 0 = no minimum; @param maxDistance {@link Double#MAX_VALUE} = no maximum */
    record FallFromHeight(double minDistance, double maxDistance) implements AdvancementConditions {}

    record None() implements AdvancementConditions {
        public static final None INSTANCE = new None();
    }
}
