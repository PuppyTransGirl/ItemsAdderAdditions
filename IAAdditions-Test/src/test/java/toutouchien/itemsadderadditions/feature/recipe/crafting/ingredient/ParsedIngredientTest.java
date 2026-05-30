package toutouchien.itemsadderadditions.feature.recipe.crafting.ingredient;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;

class ParsedIngredientTest {
    private static ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static RecipeChoice stoneChoice() {
        return new RecipeChoice.ExactChoice(ItemStack.of(Material.STONE));
    }

    private static ParsedIngredient vanilla() {
        return new ParsedIngredient(stoneChoice(), 1, 0, null);
    }

    @Test
    void isCustomItem_nullNamespacedId_returnsFalse() {
        assertFalse(vanilla().isCustomItem());
    }

    @Test
    void isCustomItem_withNamespacedId_returnsTrue() {
        ParsedIngredient ing = new ParsedIngredient(
                stoneChoice(), stoneChoice(), 1, 0, null, false, null, "mypack:my_item", 0);
        assertTrue(ing.isCustomItem());
    }

    @Test
    void customNamespacedIdHash_nullId_isZero() {
        assertEquals(0, vanilla().customNamespacedIdHash());
    }

    @Test
    void customNamespacedIdHash_nonNullId_equalsStringHashCode() {
        String id = "mypack:my_item";
        ParsedIngredient ing = new ParsedIngredient(
                stoneChoice(), stoneChoice(), 1, 0, null, false, null, id, 0);
        assertEquals(id.hashCode(), ing.customNamespacedIdHash());
    }

    @Test
    void customNamespacedIdHash_overwritesSuppliedValue() {
        String id = "mypack:my_item";
        // Pass a wrong hash (999); the compact constructor must override it.
        ParsedIngredient ing = new ParsedIngredient(
                stoneChoice(), stoneChoice(), 1, 0, null, false, null, id, 999);
        assertEquals(id.hashCode(), ing.customNamespacedIdHash());
    }

    @Test
    void hasPredicate_plainVanilla_returnsFalse() {
        assertFalse(vanilla().hasPredicate());
    }

    @Test
    void hasPredicate_requiredAmountAboveOne_returnsTrue() {
        ParsedIngredient ing = new ParsedIngredient(stoneChoice(), 2, 0, null);
        assertTrue(ing.hasPredicate());
    }

    @Test
    void hasPredicate_damageAboveZero_returnsTrue() {
        ParsedIngredient ing = new ParsedIngredient(stoneChoice(), 1, 1, null);
        assertTrue(ing.hasPredicate());
    }

    @Test
    void hasPredicate_withReplacement_returnsTrue() {
        ParsedIngredient ing = new ParsedIngredient(stoneChoice(), 1, 0, ItemStack.of(Material.STICK));
        assertTrue(ing.hasPredicate());
    }

    @Test
    void hasPredicate_ignoreDurabilityTrue_returnsTrue() {
        ParsedIngredient ing = new ParsedIngredient(
                stoneChoice(), stoneChoice(), 1, 0, null, true, null, null, 0);
        assertTrue(ing.hasPredicate());
    }

    @Test
    void hasPredicate_withPotionType_returnsTrue() {
        ParsedIngredient ing = new ParsedIngredient(
                stoneChoice(), stoneChoice(), 1, 0, null, false, "minecraft:infested", null, 0);
        assertTrue(ing.hasPredicate());
    }

    @Test
    void hasPredicate_withCustomNamespacedId_returnsTrue() {
        ParsedIngredient ing = new ParsedIngredient(
                stoneChoice(), stoneChoice(), 1, 0, null, false, null, "mypack:sword", 0);
        assertTrue(ing.hasPredicate());
    }
}
