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

class CraftingRecipeDataTest {
    private static ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static ParsedIngredient vanilla(Material mat) {
        RecipeChoice choice = new RecipeChoice.ExactChoice(new ItemStack(mat));
        return new ParsedIngredient(choice, 1, 0, null);
    }

    private static ParsedIngredient withPredicate(Material mat) {
        RecipeChoice choice = new RecipeChoice.ExactChoice(new ItemStack(mat));
        return new ParsedIngredient(choice, 2, 0, null);
    }

    private static CraftingRecipeData recipe(Map<Character, ParsedIngredient> ingredients) {
        return new CraftingRecipeData(
                new NamespacedKey("test", "recipe"),
                false, null,
                ingredients,
                new ItemStack(Material.STONE),
                null
        );
    }

    @Test
    void hasPredicates_allVanillaIngredients_isFalse() {
        CraftingRecipeData data = recipe(Map.of(
                'A', vanilla(Material.STONE),
                'B', vanilla(Material.DIRT)
        ));
        assertFalse(data.hasPredicates);
    }

    @Test
    void hasPredicates_singleVanillaIngredient_isFalse() {
        CraftingRecipeData data = recipe(Map.of('A', vanilla(Material.STONE)));
        assertFalse(data.hasPredicates);
    }

    @Test
    void hasPredicates_onePredicateIngredient_isTrue() {
        CraftingRecipeData data = recipe(Map.of(
                'A', vanilla(Material.STONE),
                'B', withPredicate(Material.DIRT)
        ));
        assertTrue(data.hasPredicates);
    }

    @Test
    void hasPredicates_allPredicateIngredients_isTrue() {
        CraftingRecipeData data = recipe(Map.of(
                'A', withPredicate(Material.STONE)
        ));
        assertTrue(data.hasPredicates);
    }

    @Test
    void ingredientList_containsAllIngredients() {
        ParsedIngredient stoneIng = vanilla(Material.STONE);
        ParsedIngredient dirtIng = vanilla(Material.DIRT);
        CraftingRecipeData data = recipe(Map.of('A', stoneIng, 'B', dirtIng));

        assertEquals(2, data.ingredientList.size());
        assertTrue(data.ingredientList.contains(stoneIng));
        assertTrue(data.ingredientList.contains(dirtIng));
    }

    @Test
    void materialIndex_containsMappedMaterial() {
        CraftingRecipeData data = recipe(Map.of('A', vanilla(Material.STONE)));
        assertTrue(data.materialIndex.containsKey(Material.STONE));
    }

    @Test
    void materialIndex_doesNotContainUnrelatedMaterial() {
        CraftingRecipeData data = recipe(Map.of('A', vanilla(Material.STONE)));
        assertFalse(data.materialIndex.containsKey(Material.DIRT));
    }

    @Test
    void materialIndex_multipleMaterials_indexedSeparately() {
        CraftingRecipeData data = recipe(Map.of(
                'A', vanilla(Material.STONE),
                'B', vanilla(Material.DIRT)
        ));
        assertTrue(data.materialIndex.containsKey(Material.STONE));
        assertTrue(data.materialIndex.containsKey(Material.DIRT));
    }

    @Test
    void materialIndex_ingredientInList() {
        ParsedIngredient stoneIng = vanilla(Material.STONE);
        CraftingRecipeData data = recipe(Map.of('A', stoneIng));
        assertTrue(data.materialIndex.get(Material.STONE).contains(stoneIng));
    }
}
