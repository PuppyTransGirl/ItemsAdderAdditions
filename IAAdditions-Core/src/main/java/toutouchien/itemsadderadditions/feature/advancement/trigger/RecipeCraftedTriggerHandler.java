package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.Keyed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class RecipeCraftedTriggerHandler extends AbstractTriggerHandler {
    public RecipeCraftedTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getRecipe() instanceof Keyed keyed)) return;
        String recipeKey = keyed.getKey().toString();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.RECIPE_CRAFTED)) {
            if (!(c.conditions() instanceof AdvancementConditions.RecipeCrafted(String condRecipeId))) continue;
            if (!condRecipeId.isBlank() && !condRecipeId.equals(recipeKey)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
