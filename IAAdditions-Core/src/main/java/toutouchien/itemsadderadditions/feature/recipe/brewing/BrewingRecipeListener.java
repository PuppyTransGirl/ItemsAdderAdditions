package toutouchien.itemsadderadditions.feature.recipe.brewing;

import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BrewingStartEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.recipe.RecipeActions;

@NullMarked
public final class BrewingRecipeListener implements Listener {
    private static final String LOG_TAG = "BrewingRecipe";

    private final BrewingRecipeHandler handler;

    public BrewingRecipeListener(BrewingRecipeHandler handler) {
        this.handler = handler;
    }

    public void clear() {
        // NMS/Paper owns active brewing state. RecipeManager clears registered mixes via handler.
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrewingStart(BrewingStartEvent event) {
        BlockState state = event.getBlock().getState();
        if (!(state instanceof BrewingStand stand)) return;

        BrewingRecipeMatch match = handler.match(stand.getInventory());
        if (match == null) return;

        BrewingRecipeData recipe = match.recipe();
        if (!hasFuel(stand, recipe.fuelCost())) {
            event.setBrewingTime(0);
            return;
        }

        event.setBrewingTime(recipe.brewTime());
        event.setRecipeBrewTime(recipe.brewTime());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        BrewingRecipeMatch match = handler.match(event.getContents());
        if (match == null) return;

        BrewingRecipeData recipe = match.recipe();
        if (!consumeExtraIngredient(event.getContents(), recipe.ingredientConsume() - 1)) {
            event.setCancelled(true);
            return;
        }

        consumeExtraFuel(event.getBlock().getState(), Math.max(0, recipe.fuelCost() - 1));
        runActions(recipe, playerFor(event.getContents()));
    }

    private boolean hasFuel(BrewingStand stand, int fuelCost) {
        if (fuelCost <= 1) return true;
        return stand.getFuelLevel() >= fuelCost;
    }

    private boolean consumeExtraIngredient(BrewerInventory inventory, int amount) {
        if (amount <= 0) return true;

        ItemStack ingredient = inventory.getIngredient();
        if (ingredient == null || ingredient.getType().isAir() || ingredient.getAmount() < amount + 1) {
            return false;
        }

        ingredient.setAmount(ingredient.getAmount() - amount);
        if (ingredient.getAmount() <= 0) {
            inventory.setIngredient(null);
        } else {
            inventory.setIngredient(ingredient);
        }
        return true;
    }

    private void consumeExtraFuel(BlockState state, int amount) {
        if (amount <= 0 || !(state instanceof BrewingStand stand)) return;

        int level = stand.getFuelLevel();
        if (level <= 0) return;

        stand.setFuelLevel(Math.max(0, level - amount));
        stand.update();
    }

    private void runActions(BrewingRecipeData recipe, @Nullable Player player) {
        RecipeActions actions = recipe.actions();
        if (actions.isEmpty()) return;
        if (player == null) {
            Log.debug(LOG_TAG, "Skipping player-bound on_complete actions for {}: no player viewer.", recipe.key());
            return;
        }
        actions.execute(player);
    }

    @Nullable
    private Player playerFor(BrewerInventory inventory) {
        for (HumanEntity viewer : inventory.getViewers()) {
            if (viewer instanceof Player player) return player;
        }
        return null;
    }
}
