package toutouchien.itemsadderadditions.feature.recipe.crafting;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.Damageable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import toutouchien.itemsadderadditions.feature.recipe.crafting.ingredient.ParsedIngredient;

import static org.junit.jupiter.api.Assertions.*;

class CraftingIngredientMatcherTest {
    private static ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static ParsedIngredient vanillaIngredient(Material mat) {
        RecipeChoice choice = new RecipeChoice.ExactChoice(new ItemStack(mat));
        return new ParsedIngredient(choice, 1, 0, null);
    }

    // --- isAir ---

    @Test
    void isAir_null_returnsTrue() {
        assertTrue(CraftingIngredientMatcher.isAir(null));
    }

    @Test
    void isAir_airItem_returnsTrue() {
        assertTrue(CraftingIngredientMatcher.isAir(new ItemStack(Material.AIR)));
    }

    @Test
    void isAir_realItem_returnsFalse() {
        assertFalse(CraftingIngredientMatcher.isAir(new ItemStack(Material.STONE)));
    }

    // --- itemInfo ---

    @Test
    void itemInfo_null_returnsNullString() {
        assertEquals("null", CraftingIngredientMatcher.itemInfo(null));
    }

    @Test
    void itemInfo_item_containsMaterialAndAmount() {
        String info = CraftingIngredientMatcher.itemInfo(new ItemStack(Material.STONE, 5));
        assertTrue(info.contains("STONE"), "Expected STONE in: " + info);
        assertTrue(info.contains("5"), "Expected amount in: " + info);
    }

    // --- remainingDurability ---

    @Test
    void remainingDurability_undamageableItem_returnsMaxInt() {
        assertEquals(Integer.MAX_VALUE, CraftingIngredientMatcher.remainingDurability(
                new ItemStack(Material.STONE)));
    }

    @Test
    void remainingDurability_freshTool_returnsFullDurability() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        int full = Material.DIAMOND_SWORD.getMaxDurability();
        assertEquals(full, CraftingIngredientMatcher.remainingDurability(sword));
    }

    @Test
    void remainingDurability_damagedTool_returnsReducedValue() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        Damageable meta = (Damageable) sword.getItemMeta();
        meta.setDamage(10);
        sword.setItemMeta(meta);

        int expected = Material.DIAMOND_SWORD.getMaxDurability() - 10;
        assertEquals(expected, CraftingIngredientMatcher.remainingDurability(sword));
    }

    // --- applyDamage ---

    @Test
    void applyDamage_belowMaxDurability_returnsFalseAndAppliesDamage() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        boolean broken = CraftingIngredientMatcher.applyDamage(sword, 10);

        assertFalse(broken);
        Damageable meta = (Damageable) sword.getItemMeta();
        assertEquals(10, meta.getDamage());
    }

    @Test
    void applyDamage_exceedsMaxDurability_returnsTrue() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        int maxDurability = Material.DIAMOND_SWORD.getMaxDurability();

        boolean broken = CraftingIngredientMatcher.applyDamage(sword, maxDurability + 1);

        assertTrue(broken);
    }

    @Test
    void applyDamage_exactlyMaxDurability_returnsTrue() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        int maxDurability = Material.DIAMOND_SWORD.getMaxDurability();

        boolean broken = CraftingIngredientMatcher.applyDamage(sword, maxDurability);

        assertTrue(broken);
    }

    // --- matches ---

    @Test
    void matches_sameMaterial_returnsTrue() {
        ParsedIngredient ingredient = vanillaIngredient(Material.STONE);
        assertTrue(CraftingIngredientMatcher.matches(ingredient, new ItemStack(Material.STONE)));
    }

    @Test
    void matches_differentMaterial_returnsFalse() {
        ParsedIngredient ingredient = vanillaIngredient(Material.STONE);
        assertFalse(CraftingIngredientMatcher.matches(ingredient, new ItemStack(Material.DIRT)));
    }

    @Test
    void matches_ingredientWithRequiredAmountTwo_matchesSlotOfAny() {
        // requiredAmount is NOT checked by matches() itself - only material identity is tested
        RecipeChoice choice = new RecipeChoice.ExactChoice(new ItemStack(Material.STONE));
        ParsedIngredient ingredient = new ParsedIngredient(choice, 2, 0, null);

        assertTrue(CraftingIngredientMatcher.matches(ingredient, new ItemStack(Material.STONE, 1)));
    }

    // --- matches with ignoreDurability ---

    @Test
    void matches_ignoreDurability_undamagedItem_returnsTrue() {
        RecipeChoice choice = new RecipeChoice.ExactChoice(new ItemStack(Material.STONE));
        ParsedIngredient ingredient = new ParsedIngredient(
                choice, choice, 1, 0, null, true, null, null, 0);
        assertTrue(CraftingIngredientMatcher.matches(ingredient, new ItemStack(Material.STONE)));
    }

    @Test
    void matches_ignoreDurability_differentMaterial_returnsFalse() {
        RecipeChoice choice = new RecipeChoice.ExactChoice(new ItemStack(Material.STONE));
        ParsedIngredient ingredient = new ParsedIngredient(
                choice, choice, 1, 0, null, true, null, null, 0);
        assertFalse(CraftingIngredientMatcher.matches(ingredient, new ItemStack(Material.DIRT)));
    }

    @Test
    void matches_ignoreDurabilityFalse_exactMatch_returnsTrue() {
        RecipeChoice choice = new RecipeChoice.ExactChoice(new ItemStack(Material.STONE));
        ParsedIngredient ingredient = new ParsedIngredient(
                choice, choice, 1, 0, null, false, null, null, 0);
        assertTrue(CraftingIngredientMatcher.matches(ingredient, new ItemStack(Material.STONE)));
    }
}
