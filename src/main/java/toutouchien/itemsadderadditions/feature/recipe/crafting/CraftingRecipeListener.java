package toutouchien.itemsadderadditions.feature.recipe.crafting;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.Arrays;

/**
 * Enforces ingredient predicates (amount, damage, replacement, ignoreDurability)
 * for our custom crafting recipes.
 *
 * <h3>Optimizations over the original version</h3>
 * <ol>
 *   <li><b>O(1) recipe lookup</b> - {@link #matchRecipe} now delegates to
 *       {@link CraftingRecipeHandler#predicateRecipeByKey}, which is backed by a
 *       {@link java.util.HashMap}.  The old O(n) linear scan fired on every
 *       {@link PrepareItemCraftEvent} (extremely hot).</li>
 *
 *   <li><b>Cached {@code hasPredicates}</b> - the field is precomputed once in
 *       {@link CraftingRecipeData}.  The old code called
 *       {@code stream().anyMatch()} on every event.</li>
 *
 *   <li><b>Material-indexed ingredient lookup</b> - {@link CraftingPredicateEngine#ingredientsSatisfied}
 *       uses {@link CraftingRecipeData#materialIndex} to skip ingredients that
 *       can never match a given slot's material, reducing the inner loop from
 *       O(matrix × all_ingredients) to O(matrix × candidates_for_material)
 *       (usually 1).</li>
 *
 *   <li><b>Cached ingredient list</b> - {@link CraftingPredicateEngine#applyPredicatesOnce} uses
 *       {@link CraftingRecipeData#ingredientList} instead of allocating a new
 *       {@code List.copyOf(values())} on every craft.</li>
 *
 *   <li><b>Clone avoidance in {@code testIngredient}</b> - the {@code ignoreDurability}
 *       path now skips unnecessary clones when the item has no actual
 *       damage to strip (damage == 0 or type has no durability), which is the
 *       common case for freshly-placed items.</li>
 * </ol>
 */
@NullMarked
public final class CraftingRecipeListener implements Listener {
    private final CraftingRecipeHandler handler;
    private final Plugin plugin;

    public CraftingRecipeListener(CraftingRecipeHandler handler, Plugin plugin) {
        this.handler = handler;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepare(PrepareItemCraftEvent event) {
        Log.debug("Crafting", "PrepareItemCraftEvent");

        CraftingRecipeData data = matchRecipe(event.getRecipe());
        // Use cached boolean field - no stream() allocation
        if (data == null || !data.hasPredicates) return;

        CraftingInventory inv = event.getInventory();

        if (!CraftingPredicateEngine.ingredientsSatisfied(data, inv.getMatrix())) {
            Log.debug("Crafting", "Result blocked (ingredients)");
            inv.setResult(null);
            return;
        }

        if (data.permission() != null) {
            Log.debug("Crafting", "Checking permission {}", data.permission());
            boolean anyViewer = event.getViewers().stream()
                    .anyMatch(v -> v.hasPermission(data.permission()));
            if (!anyViewer) {
                Log.debug("Crafting", "Result blocked (permission)");
                inv.setResult(null);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory() instanceof CraftingInventory inv)) return;
        if (event.getRawSlot() == 0) return;

        Log.debug("Crafting", "InventoryClick slot {}", event.getRawSlot());

        Bukkit.getScheduler().runTask(plugin, () -> {
            Log.debug("Crafting", "Revalidating grid");

            if (player.getOpenInventory().getTopInventory() != inv) return;

            ItemStack[] padded = CraftingPredicateEngine.toNineSlot(inv.getMatrix());
            if (padded == null) return;

            Recipe matched = Bukkit.getCraftingRecipe(padded, player.getWorld());
            CraftingRecipeData data = matchRecipe(matched);
            if (data == null || !data.hasPredicates) return;

            if (CraftingPredicateEngine.ingredientsSatisfied(data, inv.getMatrix())) {
                if (CraftingPredicateEngine.isAir(inv.getResult())) {
                    Log.debug("Crafting", "Restoring result");
                    inv.setResult(data.result().clone());
                }
            } else {
                Log.debug("Crafting", "Clearing result");
                inv.setResult(null);
            }

            player.updateInventory();
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        Log.debug("Crafting", "CraftItemEvent action={} shift={}",
                event.getAction(), event.isShiftClick());

        CraftingRecipeData data = matchRecipe(event.getRecipe());
        if (data == null || !data.hasPredicates) return;

        event.setCancelled(true);

        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();
        Player player = (Player) event.getWhoClicked();

        Log.debug("Crafting", "Matrix before craft: {}", Arrays.toString(matrix));

        if (data.permission() != null && !player.hasPermission(data.permission())) {
            Log.debug("Crafting", "Blocked (permission)");
            return;
        }

        if (!CraftingPredicateEngine.ingredientsSatisfied(data, matrix)) {
            Log.debug("Crafting", "Blocked (ingredients)");
            return;
        }

        int craftCount = event.isShiftClick()
                ? CraftingPredicateEngine.calculateMaxCrafts(data, matrix, player)
                : 1;

        Log.debug("Crafting", "Craft count = {}", craftCount);

        if (craftCount <= 0) return;

        ItemStack result = data.result().clone();
        result.setAmount(data.result().getAmount() * craftCount);

        if (event.isShiftClick()) {
            CraftingPredicateEngine.giveOrDrop(player, result);
        } else {
            event.getView().setCursor(result);
        }

        for (int c = 0; c < craftCount; c++) {
            CraftingPredicateEngine.applyPredicatesOnce(data, matrix);
            if (c < craftCount - 1 && !CraftingPredicateEngine.canCraftAgain(data, matrix)) break;
        }

        inv.setMatrix(matrix);
        player.updateInventory();
    }

    /**
     * O(1) lookup replacing the original O(n) linear scan over all predicate
     * recipes.
     *
     * <p>The previous implementation iterated {@code handler.predicateRecipes()}
     * on every event.  This version delegates to
     * {@link CraftingRecipeHandler#predicateRecipeByKey}, which is a
     * {@link java.util.HashMap} keyed on {@link NamespacedKey}.
     */
    @Nullable
    private CraftingRecipeData matchRecipe(@Nullable Recipe recipe) {
        if (recipe == null) {
            Log.debug("Crafting", "matchRecipe: null recipe");
            return null;
        }

        NamespacedKey key = CraftingPredicateEngine.recipeKey(recipe);
        if (key == null) {
            Log.debug("Crafting", "matchRecipe: no key");
            return null;
        }

        CraftingRecipeData data = handler.predicateRecipeByKey(key);
        Log.debug("Crafting",
                data != null ? "Matched recipe {}" : "No match for {}", key);
        return data;
    }
}
