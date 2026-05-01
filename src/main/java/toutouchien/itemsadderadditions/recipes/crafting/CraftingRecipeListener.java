package toutouchien.itemsadderadditions.recipes.crafting;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
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
import java.util.List;
import java.util.Map;

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
 *   <li><b>Material-indexed ingredient lookup</b> - {@link #ingredientsSatisfied}
 *       uses {@link CraftingRecipeData#materialIndex} to skip ingredients that
 *       can never match a given slot's material, reducing the inner loop from
 *       O(matrix × all_ingredients) to O(matrix × candidates_for_material)
 *       (usually 1).</li>
 *
 *   <li><b>Cached ingredient list</b> - {@link #applyPredicatesOnce} uses
 *       {@link CraftingRecipeData#ingredientList} instead of allocating a new
 *       {@code List.copyOf(values())} on every craft.</li>
 *
 *   <li><b>Clone avoidance in {@code testIngredient}</b> - the {@code ignoreDurability}
 *       path now skips the {@link ItemStack#clone()} when the item has no actual
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

    /**
     * Checks that every ingredient's {@link ParsedIngredient#requiredAmount()} is
     * met by the items present in {@code matrix}.
     *
     * <h4>Optimization: material-indexed ingredient lookup</h4>
     * <p>Rather than testing every ingredient against every slot (O(9 × n)),
     * we first look up the slot's {@link Material} in
     * {@link CraftingRecipeData#materialIndex} and only run
     * {@link #testIngredient} on the small subset of ingredients that could
     * possibly match (usually 1).  The outer loop is then a single O(9) pass.
     */
    private static boolean ingredientsSatisfied(
            CraftingRecipeData data, ItemStack[] matrix
    ) {
        Log.debug("Crafting", "Checking ingredients for '{}'", data.key());

        List<ParsedIngredient> ingredients = data.ingredientList;
        int size = ingredients.size();
        int[] totalFound = new int[size]; // per-ingredient accumulated amounts

        Map<Material, List<ParsedIngredient>> matIndex = data.materialIndex;

        for (ItemStack slot : matrix) {
            if (isAir(slot)) continue;

            List<ParsedIngredient> candidates = matIndex.get(slot.getType());
            if (candidates == null) continue; // no ingredient wants this material

            for (ParsedIngredient ing : candidates) {
                if (testIngredient(ing, slot)) {
                    totalFound[identityIndexOf(ingredients, ing)] += slot.getAmount();
                    break; // one slot contributes to at most one ingredient
                }
            }
        }

        for (int i = 0; i < size; i++) {
            if (totalFound[i] < ingredients.get(i).requiredAmount()) {
                Log.debug("Crafting", "Ingredient NOT satisfied: {}", ingredients.get(i));
                return false;
            }
        }

        Log.debug("Crafting", "All ingredients satisfied");
        return true;
    }

    /**
     * Like {@link #ingredientsSatisfied} but only considers ingredients that
     * require actual consumption (no replacement, no damage-only).
     * Uses the same material-index fast path.
     */
    private static boolean canCraftAgain(
            CraftingRecipeData data, ItemStack[] matrix
    ) {
        List<ParsedIngredient> ingredients = data.ingredientList;
        int size = ingredients.size();
        int[] totalFound = new int[size];

        Map<Material, List<ParsedIngredient>> matIndex = data.materialIndex;

        for (ItemStack slot : matrix) {
            if (isAir(slot)) continue;

            List<ParsedIngredient> candidates = matIndex.get(slot.getType());
            if (candidates == null) continue;

            for (ParsedIngredient ing : candidates) {
                // Skip ingredients that don't consume the slot
                if (ing.replacement() != null || ing.damageAmount() > 0) continue;
                if (testIngredient(ing, slot)) {
                    totalFound[identityIndexOf(ingredients, ing)] += slot.getAmount();
                    break;
                }
            }
        }

        for (int i = 0; i < size; i++) {
            ParsedIngredient ing = ingredients.get(i);
            if (ing.replacement() != null || ing.damageAmount() > 0) continue;
            if (totalFound[i] < ing.requiredAmount()) {
                Log.debug("Crafting", "Cannot craft again, missing {}", ing);
                return false;
            }
        }

        return true;
    }

    /**
     * Consumes / mutates one round of crafting from {@code matrix}.
     *
     * <h4>Optimization: cached ingredient list</h4>
     * <p>Uses {@link CraftingRecipeData#ingredientList} directly instead of
     * allocating a new {@code List.copyOf(ingredients().values())} on every
     * craft event.
     */
    private static void applyPredicatesOnce(
            CraftingRecipeData data, ItemStack[] matrix
    ) {
        // Use the pre-built stable list - no allocation
        List<ParsedIngredient> ingredients = data.ingredientList;
        int size = ingredients.size();

        int[] remaining = new int[size];
        for (int i = 0; i < size; i++) {
            remaining[i] = ingredients.get(i).requiredAmount();
        }

        // Map each matrix slot to its matching ingredient index (or -1)
        int[] slotToIngredient = new int[matrix.length];
        Arrays.fill(slotToIngredient, -1);

        Map<Material, List<ParsedIngredient>> matIndex = data.materialIndex;

        for (int s = 0; s < matrix.length; s++) {
            ItemStack slot = matrix[s];
            if (isAir(slot)) continue;

            List<ParsedIngredient> candidates = matIndex.get(slot.getType());
            if (candidates == null) continue;

            for (ParsedIngredient ing : candidates) {
                if (testIngredient(ing, slot)) {
                    slotToIngredient[s] = identityIndexOf(ingredients, ing);
                    break;
                }
            }
        }

        // Apply consumption / replacement / damage
        for (int s = 0; s < matrix.length; s++) {
            ItemStack slot = matrix[s];
            if (isAir(slot)) continue;

            int idx = slotToIngredient[s];
            if (idx == -1) continue;

            ParsedIngredient ingredient = ingredients.get(idx);

            if (ingredient.replacement() != null) {
                matrix[s] = ingredient.replacement().clone();

            } else if (ingredient.damageAmount() > 0) {
                ItemStack damaged = slot.clone();
                matrix[s] = applyDamage(damaged, ingredient.damageAmount())
                        ? null
                        : damaged;

            } else {
                int needed = remaining[idx];
                if (needed <= 0) continue;

                int toConsume = Math.min(slot.getAmount(), needed);
                int leftover = slot.getAmount() - toConsume;

                if (leftover <= 0) {
                    matrix[s] = null;
                } else {
                    matrix[s] = slot.clone();
                    matrix[s].setAmount(leftover);
                }

                remaining[idx] = needed - toConsume;
            }
        }
    }

    @Nullable
    private static ParsedIngredient findIngredient(
            CraftingRecipeData data, ItemStack slot
    ) {
        // Use material index as first-pass filter
        List<ParsedIngredient> candidates =
                data.materialIndex.get(slot.getType());
        if (candidates == null) return null;

        for (ParsedIngredient ingredient : candidates) {
            if (testIngredient(ingredient, slot)) return ingredient;
        }
        return null;
    }

    /**
     * Tests whether {@code slot} satisfies {@code ingredient}.
     *
     * <h3>Fast paths (in order of cheapness)</h3>
     * <ol>
     *   <li><b>Custom-item hash check</b> - when the ingredient is an ItemsAdder
     *       custom item, {@link CustomStack#byItemStack} resolves the slot's ID
     *       from its PDC in one map lookup.  A {@code null} result instantly
     *       rejects vanilla items in custom-item slots.  Then integer hash
     *       comparison rejects wrong custom items without touching
     *       {@link ItemStack#isSimilar} at all.  Full string equality only runs
     *       on an (extremely rare) hash collision.</li>
     *
     *   <li><b>Clone avoidance for {@code ignoreDurability}</b> - the slot is
     *       only cloned when it actually carries non-zero damage.  Undamaged
     *       items (the common case) skip the clone entirely.</li>
     *
     *   <li><b>Vanilla fallback</b> - RecipeChoice#test as before,
     *       reached only for non-custom-item ingredients.</li>
     * </ol>
     */
    private static boolean testIngredient(ParsedIngredient ingredient, ItemStack slot) {
        Log.debug("Crafting", "Testing {} against {}", itemInfo(slot), ingredient);

        if (ingredient.isCustomItem()) {
            // byItemStack does a single PDC key lookup - much cheaper than isSimilar
            CustomStack slotCustom = CustomStack.byItemStack(slot);

            if (slotCustom == null) {
                // Slot holds a vanilla item; ingredient requires a custom one
                Log.debug("Crafting", "Custom-item fast reject: slot is vanilla");
                return false;
            }

            String slotId = slotCustom.getNamespacedID();

            // Integer comparison first - String.hashCode() is cached after first call
            if (slotId.hashCode() != ingredient.customNamespacedIdHash()) {
                Log.debug("Crafting", "Custom-item fast reject: hash mismatch");
                return false;
            }

            // Guard against the (practically impossible) hash collision
            if (!slotId.equals(ingredient.customNamespacedId())) {
                Log.debug("Crafting", "Custom-item reject: ID mismatch (collision)");
                return false;
            }

            // ID matched - still need potion-type check if applicable
            if (ingredient.potionType() != null) {
                return checkPotionType(ingredient, slot);
            }

            Log.debug("Crafting", "Custom-item match: {}", slotId);
            return true;
        }

        ItemStack toTest = slot;

        if (ingredient.ignoreDurability()) {
            // Only clone when there is actual damage to strip - undamaged items
            // (the common case) skip the allocation entirely
            ItemMeta m = slot.getItemMeta();
            if (m instanceof Damageable d && d.getDamage() != 0) {
                Log.debug("Crafting", "Stripping damage for ignoreDurability check");
                d.setDamage(0);
                toTest = slot.clone();
                toTest.setItemMeta(d);
            }
        }

        if (!ingredient.validationChoice().test(toTest)) {
            Log.debug("Crafting", "ValidationChoice failed");
            return false;
        }

        if (ingredient.potionType() != null) {
            return checkPotionType(ingredient, slot);
        }

        return true;
    }

    /**
     * Extracted potion-type check, shared between custom and vanilla paths.
     */
    private static boolean checkPotionType(ParsedIngredient ingredient, ItemStack slot) {
        Log.debug("Crafting", "Checking potion type {}", ingredient.potionType());
        ItemMeta meta = slot.getItemMeta();
        if (!(meta instanceof PotionMeta potionMeta)) return false;

        PotionType type = potionMeta.getBasePotionType();
        if (type == null) return false;

        boolean match = type.getKey().toString().equalsIgnoreCase(ingredient.potionType());
        Log.debug("Crafting", "Potion match: {}", match);
        return match;
    }

    private static int calculateMaxCrafts(
            CraftingRecipeData data,
            ItemStack[] matrix,
            HumanEntity player
    ) {
        Log.debug("Crafting", "Calculating max crafts...");

        int max = Integer.MAX_VALUE;

        for (ParsedIngredient ingredient : data.ingredientList) {
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
                            itemInfo(slot), craftsFromSlot);

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

        if (maxDurability > 0 && newDamage >= maxDurability) return true;

        damageable.setDamage(newDamage);
        item.setItemMeta(meta);
        return false;
    }

    private static int countInventorySpace(
            HumanEntity player, ItemStack template, int maxNeeded
    ) {
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

    @Nullable
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

    /**
     * Identity-based indexOf on a list that is always ≤9 elements and
     * cache-hot at the call site. Faster than an IdentityHashMap lookup
     * for this size due to sequential pointer comparisons on contiguous memory.
     */
    private static int identityIndexOf(List<ParsedIngredient> list, ParsedIngredient target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == target) return i;
        }
        return -1; // unreachable: target always comes from the same list
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepare(PrepareItemCraftEvent event) {
        Log.debug("Crafting", "PrepareItemCraftEvent");

        CraftingRecipeData data = matchRecipe(event.getRecipe());
        // Use cached boolean field - no stream() allocation
        if (data == null || !data.hasPredicates) return;

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
            if (data == null || !data.hasPredicates) return;

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

        NamespacedKey key = recipeKey(recipe);
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
