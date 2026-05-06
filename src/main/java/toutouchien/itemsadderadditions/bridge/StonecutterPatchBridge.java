package toutouchien.itemsadderadditions.bridge;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.recipes.stonecutter.StonecutterRecipeHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Shared state between the bytecode-patch injection point and the recipe handler.
 *
 * <p>ItemsAdder normally blocks custom items from being used in the stonecutter
 * (it closes the inventory whenever the player places one). Our patched version of
 * that check calls {@link #filterCustomItemCheck} instead, which allows the operation
 * through when the ingredient+result pair matches one of our registered recipes.
 *
 * <p>Registration lifecycle:
 * <ol>
 *   <li>{@link StonecutterRecipeHandler} calls {@link #register} for each loaded recipe.</li>
 *   <li>The bytecode patch calls {@link #filterCustomItemCheck} on every stonecutter click.</li>
 *   <li>{@link StonecutterRecipeHandler#unregisterAll()} calls {@link #clear()} on reload.</li>
 * </ol>
 */
@NullMarked
public final class StonecutterPatchBridge {
    /**
     * All custom stonecutter recipes that should bypass ItemsAdder's block.
     */
    private static final List<Entry> ALLOWED = new CopyOnWriteArrayList<>();

    private StonecutterPatchBridge() {
        throw new IllegalStateException("Static utility class");
    }

    /**
     * Registers an ingredient+result pair that should be allowed through the stonecutter.
     * Both items are cloned and the ingredient's amount is normalised to 1.
     */
    public static void register(@Nullable ItemStack ingredient, @Nullable ItemStack result) {
        if (isAir(ingredient) || isAir(result)) return;

        ItemStack normIngredient = ingredient.clone();
        normIngredient.setAmount(1);

        ALLOWED.add(new Entry(normIngredient, result.clone()));
    }

    /**
     * Clears all registered recipes. Must be called before each reload cycle.
     */
    public static void clear() {
        ALLOWED.clear();
    }

    /**
     * Intercepts ItemsAdder's custom-item check for the stonecutter.
     *
     * <p>ItemsAdder calls (roughly) {@code if (isCustomItem(input)) { closeInventory(); }}.
     * This method is injected in place of that check:
     * <ul>
     *   <li>If {@code original} is {@code false} - the item is not custom, so we agree.</li>
     *   <li>If the stonecutter's current input/result pair is one of our recipes - return
     *       {@code false} so ItemsAdder skips the close.</li>
     *   <li>Otherwise - return {@code true} to preserve ItemsAdder's original behaviour.</li>
     * </ul>
     *
     * @param original the result of ItemsAdder's own {@code isCustomItem(input)} call
     * @param event    the click event that triggered the check
     * @return {@code false} when IAA should skip closing the inventory; {@code true} otherwise
     */
    public static boolean filterCustomItemCheck(boolean original, @Nullable InventoryClickEvent event) {
        if (!original) return false;
        if (event == null) return true;

        Inventory top = event.getView().getTopInventory();
        if (top.getType() != InventoryType.STONECUTTER) return true;

        @Nullable ItemStack input = top.getItem(0);
        @Nullable ItemStack result = top.getItem(1);
        if (isAir(input) || isAir(result)) return true;

        for (Entry entry : ALLOWED) {
            if (sameItem(input, entry.ingredient)
                    && sameItem(result, entry.result)
                    && result.getAmount() == entry.result.getAmount()) {
                // Our recipe - let the crafting proceed.
                return false;
            }
        }

        // Not one of our recipes - preserve ItemsAdder's block.
        return true;
    }

    private static boolean sameItem(@Nullable ItemStack a, @Nullable ItemStack b) {
        return a != null && b != null && a.isSimilar(b);
    }

    private static boolean isAir(@Nullable ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private record Entry(ItemStack ingredient, ItemStack result) {}
}
