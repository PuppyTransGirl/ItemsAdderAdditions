package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class RecipeUnlockedTriggerHandler extends AbstractTriggerHandler {
    public RecipeUnlockedTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDiscover(PlayerRecipeDiscoverEvent event) {
        String recipeKey = event.getRecipe().toString();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.RECIPE_UNLOCKED)) {
            if (!(c.conditions() instanceof AdvancementConditions.RecipeUnlocked(String recipe))) continue;
            if (!recipe.isEmpty() && !recipe.equals(recipeKey)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
