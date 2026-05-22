package toutouchien.itemsadderadditions.feature.recipe.crafting;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.recipe.crafting.ingredient.ParsedIngredient;
import toutouchien.itemsadderadditions.integration.hook.MMOItemsHook;

@NullMarked
final class CraftingIngredientMatcher {
    private CraftingIngredientMatcher() {
    }

    static boolean matches(ParsedIngredient ingredient, ItemStack slot) {
        if (ingredient.isCustomItem()) {
            return matchesCustomIngredient(ingredient, slot);
        }

        ItemStack toTest = ingredient.ignoreDurability() ? withoutDamage(slot) : slot;
        if (!ingredient.validationChoice().test(toTest)) {
            Log.debug("Crafting", "ValidationChoice failed for {}", itemInfo(slot));
            return false;
        }

        return ingredient.potionType() == null || matchesPotionType(ingredient, slot);
    }

    static boolean applyDamage(ItemStack item, int damage) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return false;

        int newDamage = damageable.getDamage() + damage;
        int maxDurability = item.getType().getMaxDurability();
        if (maxDurability > 0 && newDamage >= maxDurability) return true;

        damageable.setDamage(newDamage);
        item.setItemMeta(meta);
        return false;
    }

    static int remainingDurability(ItemStack item) {
        int max = item.getType().getMaxDurability();
        if (max <= 0) return Integer.MAX_VALUE;

        ItemMeta meta = item.getItemMeta();
        return meta instanceof Damageable damageable ? max - damageable.getDamage() : max;
    }

    static boolean isAir(@Nullable ItemStack item) {
        return item == null || item.getType().isAir();
    }

    static String itemInfo(@Nullable ItemStack item) {
        return item == null ? "null" : item.getType() + " x" + item.getAmount();
    }

    private static boolean matchesCustomIngredient(ParsedIngredient ingredient, ItemStack slot) {
        String customId = ingredient.customNamespacedId();
        assert customId != null; // guarded by isCustomItem()

        if (customId.startsWith("mmoitems:")) {
            return matchesMmoItem(ingredient, slot, customId);
        }

        CustomStack slotCustom = CustomStack.byItemStack(slot);
        if (slotCustom == null) return false;

        String slotId = slotCustom.getNamespacedID();
        if (slotId.hashCode() != ingredient.customNamespacedIdHash()) return false;
        if (!slotId.equals(customId)) return false;

        return ingredient.potionType() == null || matchesPotionType(ingredient, slot);
    }

    /**
     * Checks whether {@code slot} is the MMOItems item encoded in
     * {@code mmoId} (e.g. {@code "mmoitems:sword:fire_sword"}).
     */
    private static boolean matchesMmoItem(ParsedIngredient ingredient, ItemStack slot, String mmoId) {
        String rest = mmoId.substring("mmoitems:".length());
        int colon = rest.indexOf(':');
        if (colon <= 0) return false;

        String type = rest.substring(0, colon);
        String id = rest.substring(colon + 1);

        if (!MMOItemsHook.INSTANCE.isMmoItem(slot, type, id)) return false;
        return ingredient.potionType() == null || matchesPotionType(ingredient, slot);
    }

    private static ItemStack withoutDamage(ItemStack slot) {
        ItemMeta meta = slot.getItemMeta();
        if (!(meta instanceof Damageable damageable) || damageable.getDamage() == 0) {
            return slot;
        }

        ItemStack copy = slot.clone();
        damageable.setDamage(0);
        copy.setItemMeta(damageable);
        return copy;
    }

    private static boolean matchesPotionType(ParsedIngredient ingredient, ItemStack slot) {
        ItemMeta meta = slot.getItemMeta();
        if (!(meta instanceof PotionMeta potionMeta)) return false;

        PotionType type = potionMeta.getBasePotionType();
        return type != null && type.getKey().toString().equalsIgnoreCase(ingredient.potionType());
    }
}
