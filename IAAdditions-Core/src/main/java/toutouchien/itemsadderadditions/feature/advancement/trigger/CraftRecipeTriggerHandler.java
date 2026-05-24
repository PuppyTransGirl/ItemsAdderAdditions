package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class CraftRecipeTriggerHandler extends AbstractTriggerHandler {
    public CraftRecipeTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @Nullable
    private static NamespacedKey recipeKey(Recipe recipe) {
        if (recipe instanceof ShapedRecipe r) return r.getKey();
        if (recipe instanceof ShapelessRecipe r) return r.getKey();
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        NamespacedKey key = recipeKey(event.getRecipe());
        if (key == null) return;
        String keyStr = key.getNamespace() + ":" + key.getKey();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.CRAFT_RECIPE)) {
            if (!(c.conditions() instanceof AdvancementConditions.CraftRecipe(String recipeId))) continue;
            if (!recipeId.equals(keyStr) && !recipeId.equals(key.getKey())) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
