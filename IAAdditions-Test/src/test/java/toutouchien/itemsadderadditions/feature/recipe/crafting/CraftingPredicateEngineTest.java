package toutouchien.itemsadderadditions.feature.recipe.crafting;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import toutouchien.itemsadderadditions.feature.recipe.crafting.ingredient.ParsedIngredient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CraftingPredicateEngineTest {
    private static ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static CraftingRecipeData recipe(Material mat, int required) {
        RecipeChoice choice = new RecipeChoice.ExactChoice(new ItemStack(mat));
        ParsedIngredient ingredient = new ParsedIngredient(choice, required, 0, null);
        return new CraftingRecipeData(
                new NamespacedKey("test", "recipe"),
                false, null,
                Map.of('A', ingredient),
                new ItemStack(Material.STONE),
                null
        );
    }

    private static ItemStack[] matrix(ItemStack... items) {
        ItemStack[] m = new ItemStack[9];
        System.arraycopy(items, 0, m, 0, Math.min(items.length, 9));
        return m;
    }

    // --- toNineSlot ---

    @Test
    void toNineSlot_nineElements_returnsSameReference() {
        ItemStack[] nine = new ItemStack[9];
        assertSame(nine, CraftingPredicateEngine.toNineSlot(nine));
    }

    @Test
    void toNineSlot_fourElements_expandsToNineCorrectly() {
        ItemStack a = new ItemStack(Material.STONE);
        ItemStack b = new ItemStack(Material.DIRT);
        ItemStack c = new ItemStack(Material.OAK_LOG);
        ItemStack d = new ItemStack(Material.SAND);
        ItemStack[] four = {a, b, c, d};

        ItemStack[] nine = CraftingPredicateEngine.toNineSlot(four);

        assertNotNull(nine);
        assertEquals(9, nine.length);
        assertSame(a, nine[0]);
        assertSame(b, nine[1]);
        assertNull(nine[2]);
        assertSame(c, nine[3]);
        assertSame(d, nine[4]);
        assertNull(nine[5]);
        assertNull(nine[6]);
        assertNull(nine[7]);
        assertNull(nine[8]);
    }

    @Test
    void toNineSlot_otherSizes_returnNull() {
        assertNull(CraftingPredicateEngine.toNineSlot(new ItemStack[0]));
        assertNull(CraftingPredicateEngine.toNineSlot(new ItemStack[5]));
        assertNull(CraftingPredicateEngine.toNineSlot(new ItemStack[16]));
    }

    // --- isAir ---

    @Test
    void isAir_null_returnsTrue() {
        assertTrue(CraftingPredicateEngine.isAir(null));
    }

    @Test
    void isAir_airItem_returnsTrue() {
        assertTrue(CraftingPredicateEngine.isAir(new ItemStack(Material.AIR)));
    }

    @Test
    void isAir_nonAirItem_returnsFalse() {
        assertFalse(CraftingPredicateEngine.isAir(new ItemStack(Material.STONE)));
    }

    // --- itemInfo ---

    @Test
    void itemInfo_null_returnsNullString() {
        assertEquals("null", CraftingPredicateEngine.itemInfo(null));
    }

    @Test
    void itemInfo_item_containsMaterialAndAmount() {
        String info = CraftingPredicateEngine.itemInfo(new ItemStack(Material.STONE, 3));
        assertTrue(info.contains("STONE"), "Expected STONE in: " + info);
        assertTrue(info.contains("3"), "Expected amount in: " + info);
    }

    // --- ingredientsSatisfied ---

    @Test
    void ingredientsSatisfied_exactAmount_returnsTrue() {
        CraftingRecipeData data = recipe(Material.STONE, 2);
        assertTrue(CraftingPredicateEngine.ingredientsSatisfied(data,
                matrix(new ItemStack(Material.STONE, 2))));
    }

    @Test
    void ingredientsSatisfied_moreThanRequired_returnsTrue() {
        CraftingRecipeData data = recipe(Material.STONE, 1);
        assertTrue(CraftingPredicateEngine.ingredientsSatisfied(data,
                matrix(new ItemStack(Material.STONE, 5))));
    }

    @Test
    void ingredientsSatisfied_amountSummedAcrossSlots_returnsTrue() {
        CraftingRecipeData data = recipe(Material.STONE, 3);
        assertTrue(CraftingPredicateEngine.ingredientsSatisfied(data,
                matrix(new ItemStack(Material.STONE, 2), new ItemStack(Material.STONE, 1))));
    }

    @Test
    void ingredientsSatisfied_insufficientAmount_returnsFalse() {
        CraftingRecipeData data = recipe(Material.STONE, 3);
        assertFalse(CraftingPredicateEngine.ingredientsSatisfied(data,
                matrix(new ItemStack(Material.STONE, 2))));
    }

    @Test
    void ingredientsSatisfied_emptyMatrix_returnsFalse() {
        CraftingRecipeData data = recipe(Material.STONE, 1);
        assertFalse(CraftingPredicateEngine.ingredientsSatisfied(data, matrix()));
    }

    @Test
    void ingredientsSatisfied_wrongMaterial_returnsFalse() {
        CraftingRecipeData data = recipe(Material.STONE, 1);
        assertFalse(CraftingPredicateEngine.ingredientsSatisfied(data,
                matrix(new ItemStack(Material.DIRT, 3))));
    }

    // --- findIngredient ---

    @Test
    void findIngredient_matchingMaterial_returnsIngredient() {
        CraftingRecipeData data = recipe(Material.STONE, 1);
        assertNotNull(CraftingPredicateEngine.findIngredient(data, new ItemStack(Material.STONE)));
    }

    @Test
    void findIngredient_wrongMaterial_returnsNull() {
        CraftingRecipeData data = recipe(Material.STONE, 1);
        assertNull(CraftingPredicateEngine.findIngredient(data, new ItemStack(Material.DIRT)));
    }

    // --- canCraftAgain ---

    @Test
    void canCraftAgain_enoughItemsForAnotherCraft_returnsTrue() {
        CraftingRecipeData data = recipe(Material.STONE, 2);
        assertTrue(CraftingPredicateEngine.canCraftAgain(data,
                matrix(new ItemStack(Material.STONE, 4))));
    }

    @Test
    void canCraftAgain_exactlyEnoughForAnotherCraft_returnsTrue() {
        CraftingRecipeData data = recipe(Material.STONE, 2);
        assertTrue(CraftingPredicateEngine.canCraftAgain(data,
                matrix(new ItemStack(Material.STONE, 2))));
    }

    @Test
    void canCraftAgain_notEnoughForAnotherCraft_returnsFalse() {
        CraftingRecipeData data = recipe(Material.STONE, 2);
        assertFalse(CraftingPredicateEngine.canCraftAgain(data,
                matrix(new ItemStack(Material.STONE, 1))));
    }

    // --- applyPredicatesOnce ---

    @Test
    void applyPredicatesOnce_partialConsumption_reducesSlotAmount() {
        CraftingRecipeData data = recipe(Material.STONE, 1);
        ItemStack[] m = matrix(new ItemStack(Material.STONE, 3));
        CraftingPredicateEngine.applyPredicatesOnce(data, m);

        assertNotNull(m[0]);
        assertEquals(2, m[0].getAmount());
    }

    @Test
    void applyPredicatesOnce_exactConsumption_setsSlotToNull() {
        CraftingRecipeData data = recipe(Material.STONE, 1);
        ItemStack[] m = matrix(new ItemStack(Material.STONE, 1));
        CraftingPredicateEngine.applyPredicatesOnce(data, m);

        assertNull(m[0]);
    }

    @Test
    void applyPredicatesOnce_unrelatedSlots_untouched() {
        CraftingRecipeData data = recipe(Material.STONE, 1);
        ItemStack dirt = new ItemStack(Material.DIRT, 5);
        ItemStack[] m = matrix(new ItemStack(Material.STONE, 1), dirt);
        CraftingPredicateEngine.applyPredicatesOnce(data, m);

        assertSame(dirt, m[1]);
        assertEquals(5, m[1].getAmount());
    }
}
