package toutouchien.itemsadderadditions.recipes.stonecutter;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class StonecutterPatchBridge {
    private static final List<Entry> ALLOWED = new CopyOnWriteArrayList<>();

    private StonecutterPatchBridge() {
        throw new IllegalStateException("Utility class");
    }

    public static void register(ItemStack ingredient, ItemStack result) {
        if (ingredient == null || result == null) return;

        ItemStack normalizedIngredient = ingredient.clone();
        normalizedIngredient.setAmount(1);

        ItemStack normalizedResult = result.clone();

        ALLOWED.add(new Entry(normalizedIngredient, normalizedResult));
    }

    public static void clear() {
        ALLOWED.clear();
    }

    /**
     * Returns the boolean that ItemsAdder should use for:
     * if (isCustomItem(input)) { ...close inventory... }
     * <p>
     * original = result of oy.isCustomItem(input)
     * <p>
     * We keep the original protection unless the currently shown
     * stonecutter input/result pair matches one of our registered recipes.
     */
    public static boolean filterCustomItemCheck(boolean original, InventoryClickEvent event) {
        if (!original) return false;
        if (event == null) return true;

        Inventory top = event.getView().getTopInventory();
        if (top.getType() != InventoryType.STONECUTTER) return true;

        ItemStack input = top.getItem(0);
        ItemStack result = top.getItem(1);
        if (isAir(input) || isAir(result)) return true;

        for (Entry entry : ALLOWED) {
            if (
                    sameItem(input, entry.ingredient) &&
                    sameItem(result, entry.result) &&
                    result.getAmount() == entry.result.getAmount()
            ) {
                // This is one of our allowed custom stonecutter recipes,
                // so pretend "isCustomItem" was false for this case only.
                return false;
            }
        }

        // Not one of our recipes: preserve ItemsAdder block.
        return true;
    }

    private static boolean sameItem(ItemStack a, ItemStack b) {
        return a != null && b != null && a.isSimilar(b);
    }

    private static boolean isAir(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private record Entry(ItemStack ingredient, ItemStack result) {

    }
}
