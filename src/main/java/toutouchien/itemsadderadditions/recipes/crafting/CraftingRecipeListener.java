package toutouchien.itemsadderadditions.recipes.crafting;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.recipes.crafting.ingredient.ParsedIngredient;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Enforces ingredient predicates (amount, damage, replacement, ignoreDurability)
 * for our custom crafting recipes.
 *
 * <h3>Strategy</h3>
 * <ul>
 *   <li>{@link PrepareItemCraftEvent} - gates the result slot on:
 *     <ol>
 *       <li>Full ingredient validation (custom-item check via
 *           {@link ParsedIngredient#validationChoice()} + amount predicate).</li>
 *       <li>Permission node (if present).</li>
 *     </ol>
 *     This ensures the result slot is hidden whenever the grid is invalid,
 *     including the case where a vanilla item sits in a slot that requires a
 *     custom item registered with a broad {@link org.bukkit.inventory.RecipeChoice.MaterialChoice}.</li>
 *
 *   <li>{@link InventoryClickEvent} - catches shift-clicks from the player's
 *       inventory into the crafting grid and schedules a 1-tick re-validation.
 *       Bukkit does not re-fire {@link PrepareItemCraftEvent} when the recipe
 *       pattern is unchanged and only stack sizes grow, so without this the
 *       result slot stays blank even after the required amount is met.</li>
 *
 *   <li>{@link CraftItemEvent} - always <em>cancelled</em> when predicates are
 *       present, giving us full control over ingredient consumption and result
 *       delivery:
 *     <ul>
 *       <li>Normal click: result goes to the player's <em>cursor</em>.</li>
 *       <li>Shift-click: result is added to the player's inventory (overflow
 *           drops at their feet), crafting as many times as possible.</li>
 *       <li>Number key: result goes into the targeted hotbar slot; aborts
 *           silently if that slot is already occupied.</li>
 *     </ul>
 *   </li>
 * </ul>
 */
@NullMarked
public final class CraftingRecipeListener implements Listener {
    private final CraftingRecipeHandler handler;
    private final Plugin plugin;

    public CraftingRecipeListener(CraftingRecipeHandler handler, Plugin plugin) {
        this.handler = handler;
        this.plugin = plugin;
    }

    private static boolean ingredientsSatisfied(
            CraftingRecipeData data, ItemStack[] matrix
    ) {
        Log.debug("Crafting", "Checking ingredients for '{}'", data.key());

        for (ParsedIngredient ingredient : data.ingredients().values()) {
            int totalFound = 0;

            for (ItemStack slot : matrix) {
                if (isAir(slot)) continue;

                if (testIngredient(ingredient, slot)) {
                    totalFound += slot.getAmount();
                }
            }

            Log.debug("Crafting",
                    "Ingredient {} requires {} → found {}",
                    ingredient, ingredient.requiredAmount(), totalFound
            );

            if (totalFound < ingredient.requiredAmount()) {
                Log.debug("Crafting", "Ingredient NOT satisfied: {}", ingredient);
                return false;
            }
        }

        Log.debug("Crafting", "All ingredients satisfied");
        return true;
    }

    private static boolean canCraftAgain(
            CraftingRecipeData data, ItemStack[] matrix
    ) {
        for (ParsedIngredient ingredient : data.ingredients().values()) {
            if (ingredient.replacement() != null || ingredient.damageAmount() > 0) continue;

            int totalFound = 0;
            for (ItemStack slot : matrix) {
                if (isAir(slot)) continue;
                if (testIngredient(ingredient, slot)) totalFound += slot.getAmount();
            }

            if (totalFound < ingredient.requiredAmount()) {
                Log.debug("Crafting", "Cannot craft again, missing {}", ingredient);
                return false;
            }
        }

        return true;
    }

    private static void applyPredicatesOnce(
            CraftingRecipeData data, ItemStack[] matrix
    ) {
        Log.debug("Crafting", "Applying predicates once");
        Log.debug("Crafting", "Matrix before: {}", Arrays.toString(matrix));

        Map<ParsedIngredient, Integer> remainingToConsume = new HashMap<>();
        for (ParsedIngredient ingredient : data.ingredients().values()) {
            remainingToConsume.put(ingredient, ingredient.requiredAmount());
        }

        for (int i = 0; i < matrix.length; i++) {
            ItemStack slot = matrix[i];
            if (isAir(slot)) continue;

            ParsedIngredient ingredient = findIngredient(data, slot);
            if (ingredient == null) continue;

            if (ingredient.replacement() != null) {
                Log.debug("Crafting", "Replacing {} with {}", itemInfo(slot), ingredient.replacement());
                matrix[i] = ingredient.replacement().clone();

            } else if (ingredient.damageAmount() > 0) {
                Log.debug("Crafting", "Damaging {} by {}", itemInfo(slot), ingredient.damageAmount());

                ItemStack damaged = slot.clone();
                if (applyDamage(damaged, ingredient.damageAmount())) {
                    Log.debug("Crafting", "Item broke");
                    matrix[i] = null;
                } else {
                    matrix[i] = damaged;
                }

            } else {
                int needed = remainingToConsume.getOrDefault(ingredient, 0);
                if (needed <= 0) continue;

                int toConsume = Math.min(slot.getAmount(), needed);
                int remaining = slot.getAmount() - toConsume;

                Log.debug("Crafting",
                        "Consuming {} from {} (needed left: {})",
                        toConsume, itemInfo(slot), needed
                );

                matrix[i] = (remaining <= 0) ? null : slot.clone();
                if (matrix[i] != null) matrix[i].setAmount(remaining);

                remainingToConsume.put(ingredient, needed - toConsume);
            }
        }

        Log.debug("Crafting", "Matrix after: {}", Arrays.toString(matrix));
    }

    @Nullable
    private static ParsedIngredient findIngredient(
            CraftingRecipeData data, ItemStack slot
    ) {
        for (ParsedIngredient ingredient : data.ingredients().values()) {
            if (testIngredient(ingredient, slot)) return ingredient;
        }
        return null;
    }

    private static boolean testIngredient(ParsedIngredient ingredient, ItemStack slot) {
        Log.debug("Crafting", "Testing {} against {}", itemInfo(slot), ingredient);

        ItemStack toTest = slot;

        if (ingredient.ignoreDurability()) {
            Log.debug("Crafting", "Ignoring durability");
            toTest = slot.clone();
            ItemMeta meta = toTest.getItemMeta();
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(0);
                toTest.setItemMeta(meta);
            }
        }

        if (!ingredient.validationChoice().test(toTest)) {
            Log.debug("Crafting", "ValidationChoice failed");
            return false;
        }

        if (ingredient.potionType() != null) {
            Log.debug("Crafting", "Checking potion type {}", ingredient.potionType());

            ItemMeta meta = slot.getItemMeta();
            if (!(meta instanceof PotionMeta potionMeta)) return false;

            PotionType type = potionMeta.getBasePotionType();
            if (type == null) return false;

            boolean match = type.getKey().toString().equalsIgnoreCase(ingredient.potionType());
            Log.debug("Crafting", "Potion match: {}", match);
            return match;
        }

        return true;
    }

    private static int calculateMaxCrafts(
            CraftingRecipeData data,
            ItemStack[] matrix,
            HumanEntity player
    ) {
        Log.debug("Crafting", "Calculating max crafts...");

        int max = Integer.MAX_VALUE;

        for (ParsedIngredient ingredient : data.ingredients().values()) {
            Log.debug("Crafting", "Processing ingredient {}", ingredient);

            if (ingredient.replacement() != null) {
                max = Math.min(max, 1);
                continue;
            }

            int totalAmount = 0;
            int minDurabilityCrafts = Integer.MAX_VALUE;

            for (ItemStack slot : matrix) {
                if (isAir(slot) || !testIngredient(ingredient, slot)) continue;

                if (ingredient.damageAmount() > 0) {
                    int durability = getRemainingDurability(slot);
                    int craftsFromSlot = (durability > 0)
                            ? Math.max(1, durability / ingredient.damageAmount())
                            : 1;

                    Log.debug("Crafting",
                            "Durability slot {} → {} crafts",
                            itemInfo(slot), craftsFromSlot
                    );

                    minDurabilityCrafts = Math.min(minDurabilityCrafts, craftsFromSlot);
                } else {
                    totalAmount += slot.getAmount();
                }
            }

            if (ingredient.damageAmount() > 0) {
                max = Math.min(max, minDurabilityCrafts);
            } else {
                int possibleCrafts = totalAmount / Math.max(1, ingredient.requiredAmount());
                Log.debug("Crafting", "Total {} → {} crafts", totalAmount, possibleCrafts);
                max = Math.min(max, possibleCrafts);
            }
        }

        if (max == Integer.MAX_VALUE || max <= 0) return 1;

        int maxNeededSpace = data.result().getAmount() * max;
        int spaceForResult = countInventorySpace(player, data.result(), maxNeededSpace);
        int maxFromSpace = spaceForResult / data.result().getAmount();

        int result = Math.max(1, Math.min(max, maxFromSpace));
        Log.debug("Crafting", "Max crafts result = {}", result);

        return result;
    }

    private static void giveOrDrop(HumanEntity player, ItemStack item) {
        Log.debug("Crafting", "Giving result {}", itemInfo(item));

        player.getInventory().addItem(item.clone())
                .values()
                .forEach(leftover ->
                        player.getWorld().dropItemNaturally(
                                player.getLocation(), leftover));
    }

    private static boolean applyDamage(ItemStack item, int damage) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return false;

        int newDamage = damageable.getDamage() + damage;
        int maxDurability = item.getType().getMaxDurability();

        if (maxDurability > 0 && newDamage >= maxDurability) {
            return true;
        }

        damageable.setDamage(newDamage);
        item.setItemMeta(meta);
        return false;
    }

    private static int countInventorySpace(HumanEntity player, ItemStack template, int maxNeeded) {
        int space = 0;
        int maxStack = template.getMaxStackSize();
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (isAir(slot)) {
                space += maxStack;
            } else if (slot.isSimilar(template)) {
                space += maxStack - slot.getAmount();
            }
            if (space >= maxNeeded) return space;
        }
        return space;
    }

    private static NamespacedKey recipeKey(Recipe recipe) {
        if (recipe instanceof Keyed k) return k.getKey();
        return null;
    }

    private static int getRemainingDurability(ItemStack item) {
        int max = item.getType().getMaxDurability();
        if (max <= 0) return Integer.MAX_VALUE;

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return max;

        return max - damageable.getDamage();
    }

    private static boolean isAir(@Nullable ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private static String itemInfo(@Nullable ItemStack item) {
        if (item == null) return "null";
        return item.getType() + " x" + item.getAmount();
    }

    @Nullable
    private static ItemStack[] toNineSlot(ItemStack[] matrix) {
        if (matrix.length == 9) return matrix;
        if (matrix.length == 4) {
            return new ItemStack[]{
                    matrix[0], matrix[1], null,
                    matrix[2], matrix[3], null,
                    null, null, null
            };
        }
        return null;
    }

// ================= EVENTS =================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepare(PrepareItemCraftEvent event) {
        Log.debug("Crafting", "PrepareItemCraftEvent");

        CraftingRecipeData data = matchRecipe(event.getRecipe());
        if (data == null || !data.hasPredicates()) return;

        CraftingInventory inv = event.getInventory();

        if (!ingredientsSatisfied(data, inv.getMatrix())) {
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

            ItemStack[] padded = toNineSlot(inv.getMatrix());
            if (padded == null) return;

            Recipe matched = Bukkit.getCraftingRecipe(padded, player.getWorld());
            CraftingRecipeData data = matchRecipe(matched);
            if (data == null || !data.hasPredicates()) return;

            if (ingredientsSatisfied(data, inv.getMatrix())) {
                if (isAir(inv.getResult())) {
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
        if (data == null || !data.hasPredicates()) return;

        event.setCancelled(true);

        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();
        Player player = (Player) event.getWhoClicked();

        Log.debug("Crafting", "Matrix before craft: {}", Arrays.toString(matrix));

        if (data.permission() != null && !player.hasPermission(data.permission())) {
            Log.debug("Crafting", "Blocked (permission)");
            return;
        }

        if (!ingredientsSatisfied(data, matrix)) {
            Log.debug("Crafting", "Blocked (ingredients)");
            return;
        }

        int craftCount = event.isShiftClick()
                ? calculateMaxCrafts(data, matrix, player)
                : 1;

        Log.debug("Crafting", "Craft count = {}", craftCount);

        if (craftCount <= 0) return;

        ItemStack result = data.result().clone();
        result.setAmount(data.result().getAmount() * craftCount);

        if (event.isShiftClick()) {
            giveOrDrop(player, result);
        } else {
            event.getView().setCursor(result);
        }

        for (int c = 0; c < craftCount; c++) {
            applyPredicatesOnce(data, matrix);
            if (c < craftCount - 1 && !canCraftAgain(data, matrix)) break;
        }

        inv.setMatrix(matrix);
        player.updateInventory();
    }

    @Nullable
    private CraftingRecipeData matchRecipe(@Nullable Recipe recipe) {
        if (recipe == null) {
            Log.debug("Crafting", "matchRecipe: null recipe");
            return null;
        }

        NamespacedKey key = recipeKey(recipe);
        if (key == null) {
            Log.debug("Crafting", "matchRecipe: no key");
            return null;
        }

        for (CraftingRecipeData data : handler.predicateRecipes()) {
            Log.debug("Crafting", "Matching recipe against {}", data.key());
            if (data.key().equals(key)) {
                Log.debug("Crafting", "Matched recipe {}", key);
                return data;
            }
        }

        Log.debug("Crafting", "No match for {}", key);
        return null;
    }
}
