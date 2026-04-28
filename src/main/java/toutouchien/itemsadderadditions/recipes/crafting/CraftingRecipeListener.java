package toutouchien.itemsadderadditions.recipes.crafting;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.recipes.crafting.ingredient.ParsedIngredient;

import java.util.List;

/**
 * Enforces ingredient predicates (amount, damage, replacement, ignoreDurability)
 * for our custom crafting recipes.
 *
 * <h3>Strategy</h3>
 * <ul>
 *   <li>{@link PrepareItemCraftEvent} - gates the result slot on:
 *     <ol>
 *       <li>Full ingredient validation (custom-item check via
 *           {@link ParsedIngredient#validationChoice} + amount predicate).</li>
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

    /**
     * Returns {@code true} when every ingredient in the recipe has at least
     * one matching slot in {@code matrix} and that slot holds at least
     * {@link ParsedIngredient#requiredAmount} items.
     *
     * <p>Matching uses {@link #testIngredient} which honours
     * {@link ParsedIngredient#ignoreDurability} and
     * {@link ParsedIngredient#validationChoice} (the strict choice), so vanilla
     * items that Bukkit matched via a broad MaterialChoice are still rejected
     * when the recipe actually requires a custom IA item.
     */
    private static boolean ingredientsSatisfied(
            CraftingRecipeData data, ItemStack[] matrix
    ) {
        for (ParsedIngredient ingredient : data.ingredients().values()) {
            boolean anyMatch = false;
            for (ItemStack slot : matrix) {
                if (isAir(slot)) continue;
                if (testIngredient(ingredient, slot)) {
                    anyMatch = true;
                    // Every matching slot must carry enough items.
                    if (slot.getAmount() < ingredient.requiredAmount) return false;
                }
            }
            // If no slot satisfies this ingredient the matrix is incomplete.
            if (!anyMatch) return false;
        }
        return true;
    }

    /**
     * Lightweight re-check used between successive shift-click crafts.
     * After one craft pass the matrix has already been mutated, so we test
     * the current state from scratch.
     */
    private static boolean canCraftAgain(
            CraftingRecipeData data, ItemStack[] matrix
    ) {
        for (ParsedIngredient ingredient : data.ingredients().values()) {
            boolean foundMatch = false;
            for (ItemStack slot : matrix) {
                if (isAir(slot)) continue;
                if (testIngredient(ingredient, slot)) {
                    if (slot.getAmount() < ingredient.requiredAmount) return false;
                    foundMatch = true;
                    break;
                }
            }
            // Replacement ingredients always restock their slot - they are
            // always "present". Damage/normal ingredients need an actual slot.
            if (!foundMatch
                    && ingredient.replacement == null
                    && ingredient.damageAmount == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Mutates {@code matrix} as if one craft just occurred:
     * <ul>
     *   <li>Normal ingredient → consume {@link ParsedIngredient#requiredAmount}
     *       items from the slot.</li>
     *   <li>Damage predicate  → subtract durability; null the slot if the item
     *       breaks.</li>
     *   <li>Replacement predicate → put the replacement into the slot.</li>
     * </ul>
     *
     * <p>Because {@link CraftItemEvent} is cancelled before this runs, Bukkit
     * has not touched the matrix, so we own 100 % of the mutations.
     */
    private static void applyPredicatesOnce(
            CraftingRecipeData data, ItemStack[] matrix
    ) {
        for (int i = 0; i < matrix.length; i++) {
            ItemStack slot = matrix[i];
            if (isAir(slot)) continue;

            ParsedIngredient ingredient = findIngredient(data, slot);
            if (ingredient == null) continue; // slot not part of this recipe

            if (ingredient.replacement != null) {
                matrix[i] = ingredient.replacement.clone();

            } else if (ingredient.damageAmount > 0) {
                ItemStack damaged = slot.clone();
                if (applyDamage(damaged, ingredient.damageAmount)) {
                    matrix[i] = null; // tool broke
                } else {
                    matrix[i] = damaged;
                }

            } else {
                int remaining = slot.getAmount() - ingredient.requiredAmount;
                if (remaining <= 0) {
                    matrix[i] = null;
                } else {
                    ItemStack leftover = slot.clone();
                    leftover.setAmount(remaining);
                    matrix[i] = leftover;
                }
            }
        }
    }

    /**
     * Returns the first {@link ParsedIngredient} whose
     * {@link ParsedIngredient#validationChoice} accepts {@code slot}
     * (respecting {@link ParsedIngredient#ignoreDurability}), or {@code null}
     * if no ingredient matches.
     */
    @Nullable
    private static ParsedIngredient findIngredient(
            CraftingRecipeData data, ItemStack slot
    ) {
        for (ParsedIngredient ingredient : data.ingredients().values()) {
            if (testIngredient(ingredient, slot)) return ingredient;
        }
        return null;
    }

    /**
     * Tests whether {@code slot} satisfies {@code ingredient}.
     *
     * <p>When {@link ParsedIngredient#ignoreDurability} is {@code true} a
     * damage-stripped clone of the slot is tested against
     * {@link ParsedIngredient#validationChoice}, so custom items at any
     * durability level match while vanilla items of the same material are
     * still rejected (their NBT / custom-model-data differ).
     */
    private static boolean testIngredient(ParsedIngredient ingredient, ItemStack slot) {
        if (!ingredient.ignoreDurability) {
            return ingredient.validationChoice.test(slot);
        }
        // Strip current damage before testing so any durability level matches.
        ItemStack stripped = slot.clone();
        ItemMeta meta = stripped.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(0);
            stripped.setItemMeta(meta);
        }
        return ingredient.validationChoice.test(stripped);
    }

    /**
     * Calculates the maximum number of crafts for one shift-click, bounded by:
     * <ol>
     *   <li>Available ingredient quantities in the matrix.</li>
     *   <li>Available space in the player's inventory.</li>
     * </ol>
     *
     * <p>Special capping:
     * <ul>
     *   <li><b>Replacement</b>: capped at 1 (the slot holds a different item
     *       after the swap; it may no longer match the recipe).</li>
     *   <li><b>Damage</b>: {@code floor(remainingDurability / damageAmount)}.</li>
     * </ul>
     */
    private static int calculateMaxCrafts(
            CraftingRecipeData data,
            ItemStack[] matrix,
            HumanEntity player
    ) {
        int max = Integer.MAX_VALUE;

        for (ParsedIngredient ingredient : data.ingredients().values()) {
            for (ItemStack slot : matrix) {
                if (isAir(slot) || !testIngredient(ingredient, slot)) continue;

                int craftsFromSlot;
                if (ingredient.replacement != null) {
                    craftsFromSlot = 1;
                } else if (ingredient.damageAmount > 0) {
                    int durability = getRemainingDurability(slot);
                    craftsFromSlot = (durability > 0)
                            ? Math.max(1, durability / ingredient.damageAmount)
                            : 1;
                } else {
                    craftsFromSlot = slot.getAmount()
                            / Math.max(1, ingredient.requiredAmount);
                }

                max = Math.min(max, craftsFromSlot);
            }
        }

        if (max == Integer.MAX_VALUE || max <= 0) return 1;

        // Clamp to available inventory space
        int spaceForResult = countInventorySpace(
                player, data.result(), data.result().getAmount());
        int maxFromSpace = spaceForResult / data.result().getAmount();

        return Math.max(1, Math.min(max, maxFromSpace));
    }

    /**
     * Adds {@code item} to the player's inventory, dropping any overflow at
     * their feet. Mirrors vanilla shift-click behaviour.
     */
    private static void giveOrDrop(HumanEntity player, ItemStack item) {
        player.getInventory().addItem(item.clone())
                .values()
                .forEach(leftover ->
                        player.getWorld().dropItemNaturally(
                                player.getLocation(), leftover));
    }

    /**
     * Returns the number of additional {@code template} items the player's
     * inventory can absorb, up to {@code maxNeeded}.
     */
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

    /**
     * Applies {@code damage} durability points to {@code item} via the modern
     * {@link Damageable} API.
     *
     * @return {@code true} if the item broke (accumulated damage ≥ max durability).
     */
    private static boolean applyDamage(ItemStack item, int damage) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return false;

        int newDamage = damageable.getDamage() + damage;
        int maxDurability = item.getType().getMaxDurability();

        if (maxDurability > 0 && newDamage >= maxDurability) {
            return true; // item broke
        }
        damageable.setDamage(newDamage);
        item.setItemMeta(meta);
        return false;
    }

    /**
     * Returns remaining durability (max durability − current damage), or
     * {@link Integer#MAX_VALUE} for items without a durability bar.
     */
    private static int getRemainingDurability(ItemStack item) {
        int max = item.getType().getMaxDurability();
        if (max <= 0) return Integer.MAX_VALUE;
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return max;
        return max - damageable.getDamage();
    }

    private static org.bukkit.NamespacedKey recipeKey(Recipe recipe) {
        if (recipe instanceof org.bukkit.inventory.ShapedRecipe r) return r.getKey();
        if (recipe instanceof org.bukkit.inventory.ShapelessRecipe r) return r.getKey();
        return null;
    }

    private static boolean isAir(@Nullable ItemStack item) {
        return item == null || item.getType().isAir();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepare(PrepareItemCraftEvent event) {
        CraftingRecipeData data = matchRecipe(event.getRecipe());
        if (data == null || !data.hasPredicates()) return;

        CraftingInventory inv = event.getInventory();

        // Validate ingredient amounts and custom-item identity.
        // This also rejects vanilla items that Bukkit matched via a broad
        // MaterialChoice used for ignore_durability custom ingredients.
        if (!ingredientsSatisfied(data, inv.getMatrix())) {
            inv.setResult(null);
            return;
        }

        // Permission check
        if (data.permission() != null) {
            boolean anyViewer = event.getViewers().stream()
                    .anyMatch(v -> v.hasPermission(data.permission()));
            if (!anyViewer) {
                inv.setResult(null);
            }
        }
    }

    /**
     * Schedules a 1-tick re-validation of the crafting result after any
     * inventory click while a crafting view is open.
     *
     * <h3>Why a broad trigger is necessary</h3>
     * Bukkit does <em>not</em> re-fire {@link PrepareItemCraftEvent} when the
     * recipe pattern is already matched and only stack sizes change. This breaks
     * in two distinct ways:
     * <ol>
     *   <li><b>Shift-click routing miss</b> - when a grid slot already holds a
     *       partial stack, Bukkit's shift-click routing may send the new item to a
     *       player-inventory slot instead of the grid, so the grid never changes
     *       and no prepare event fires.</li>
     *   <li><b>Same-recipe skip</b> - even when an item does land in the grid,
     *       Bukkit skips re-preparing if it considers the recipe unchanged, so our
     *       amount-predicate check never re-runs.</li>
     * </ol>
     * Filtering on a specific action (e.g. {@code MOVE_TO_OTHER_INVENTORY}) is
     * therefore too narrow; we schedule a re-validation after <em>every</em>
     * click. The 1-tick delay ensures Bukkit has already moved items before we
     * read the matrix. Result-slot clicks (raw slot 0) are skipped here because
     * they are handled by {@link CraftItemEvent}.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory() instanceof CraftingInventory inv)) return;

        // Result-slot clicks are fully handled by onCraft; skip them here.
        if (event.getRawSlot() == 0) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            // Guard: player may have closed the inventory in the meantime.
            if (player.getOpenInventory().getTopInventory() != inv) return;

            // Do NOT use inv.getRecipe() here: when onPrepare cleared the result
            // slot, some Bukkit/Paper builds return null from getRecipe() because
            // they tie the cached recipe to the result slot state. Re-derive the
            // recipe from the live matrix, which is always independent of the
            // result slot.
            ItemStack[] matrix = inv.getMatrix();
            Recipe matched = Bukkit.getCraftingRecipe(matrix, player.getWorld());
            CraftingRecipeData data = matchRecipe(matched);
            if (data == null || !data.hasPredicates()) return;

            if (ingredientsSatisfied(data, matrix)) {
                // Restore the result if our onPrepare previously cleared it.
                if (isAir(inv.getResult())) {
                    inv.setResult(data.result().clone());
                }
            } else {
                inv.setResult(null);
            }
            player.updateInventory();
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        CraftingRecipeData data = matchRecipe(event.getRecipe());
        if (data == null || !data.hasPredicates()) return;

        // Always cancel - we own 100 % of ingredient consumption and result delivery.
        event.setCancelled(true);

        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();
        Player player = (Player) event.getWhoClicked();

        // Permission re-check (PrepareItemCraftEvent might have been bypassed)
        if (data.permission() != null && !player.hasPermission(data.permission())) return;

        // Full ingredient validation (amounts + custom-item identity)
        if (!ingredientsSatisfied(data, matrix)) return;

        InventoryAction action = event.getAction();
        boolean isShift = event.isShiftClick();
        boolean isNumberKey = action == InventoryAction.HOTBAR_SWAP;

        // Determine how many times the recipe will be crafted.
        // Normal click and number-key → always 1.
        // Shift-click → as many as the matrix and inventory allow.
        int craftCount = isShift
                ? calculateMaxCrafts(data, matrix, player)
                : 1;
        if (craftCount <= 0) return;

        // Build the result stack
        ItemStack result = data.result().clone();
        result.setAmount(data.result().getAmount() * craftCount);

        if (isNumberKey) {
            // Number key: place result directly in the targeted hotbar slot.
            // Do nothing if the slot is already occupied.
            int hotbarSlot = event.getHotbarButton();
            ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
            if (!isAir(hotbarItem)) return; // occupied - abort without consuming

            player.getInventory().setItem(hotbarSlot, result);

        } else if (isShift) {
            // Shift-click: send all crafted items to the player's inventory
            // (overflow drops at their feet, mirroring vanilla behaviour).
            giveOrDrop(player, result);

        } else {
            // Normal click: place result on the player's cursor, exactly as
            // vanilla crafting does.
            ItemStack cursor = event.getCursor();
            if (isAir(cursor)) {
                event.getView().setCursor(result);
            } else if (cursor.isSimilar(result)) {
                int combined = cursor.getAmount() + result.getAmount();
                if (combined <= result.getMaxStackSize()) {
                    cursor.setAmount(combined);
                    event.getView().setCursor(cursor);
                } else {
                    return; // No room on cursor - abort without consuming ingredients
                }
            } else {
                return; // Different item on cursor - abort without consuming ingredients
            }
        }

        // Apply predicates (ingredient consumption, damage, replacement)
        // Loop once per craft; stop early if the matrix can no longer satisfy
        // the recipe (e.g. a tool just broke mid shift-click).
        for (int c = 0; c < craftCount; c++) {
            applyPredicatesOnce(data, matrix);
            if (c < craftCount - 1 && !canCraftAgain(data, matrix)) break;
        }

        inv.setMatrix(matrix);
        player.updateInventory();
    }

    @Nullable
    private CraftingRecipeData matchRecipe(@Nullable Recipe recipe) {
        if (recipe == null) return null;
        org.bukkit.NamespacedKey key = recipeKey(recipe);
        if (key == null) return null;
        List<CraftingRecipeData> list = handler.predicateRecipes();
        for (CraftingRecipeData data : list) {
            if (data.key().equals(key)) return data;
        }
        return null;
    }
}
