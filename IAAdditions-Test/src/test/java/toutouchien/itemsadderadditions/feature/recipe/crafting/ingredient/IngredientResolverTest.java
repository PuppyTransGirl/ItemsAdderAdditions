package toutouchien.itemsadderadditions.feature.recipe.crafting.ingredient;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IngredientResolverTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static YamlConfiguration yamlOf(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    @Test
    void resolvePlainVanillaMaterial() {
        YamlConfiguration cfg = yamlOf("S: STONE\n");

        ParsedIngredient ingredient = IngredientResolver.resolve("ns", cfg, "S", "recipe");

        assertNotNull(ingredient);
        assertEquals(1, ingredient.requiredAmount());
        assertEquals(0, ingredient.damageAmount());
        assertNull(ingredient.replacement());
        assertNull(ingredient.customNamespacedId());
    }

    @Test
    void resolveUnknownMaterialReturnsNull() {
        YamlConfiguration cfg = yamlOf("S: definitely_not_a_material\n");

        assertNull(IngredientResolver.resolve("ns", cfg, "S", "recipe"));
    }

    @Test
    void resolveUnsupportedRawValueReturnsNull() {
        YamlConfiguration cfg = yamlOf("S: 123\n");

        assertNull(IngredientResolver.resolve("ns", cfg, "S", "recipe"));
    }

    @Test
    void resolveSectionWithAmountDamageIgnoreDurabilityAndPotion() {
        YamlConfiguration cfg = yamlOf("""
                S:
                  item: POTION
                  amount: 3
                  damage: 5
                  ignore_durability: true
                  potion_type: minecraft:healing
                """);

        ParsedIngredient ingredient = IngredientResolver.resolve("ns", cfg, "S", "recipe");

        assertNotNull(ingredient);
        assertEquals(3, ingredient.requiredAmount());
        assertEquals(5, ingredient.damageAmount());
        assertTrue(ingredient.ignoreDurability());
        assertEquals("minecraft:healing", ingredient.potionType());
    }

    @Test
    void resolveSectionMissingItemReturnsNull() {
        YamlConfiguration cfg = yamlOf("S:\n  amount: 2\n");

        assertNull(IngredientResolver.resolve("ns", cfg, "S", "recipe"));
    }

    @Test
    void resolveSectionWithVanillaReplacementResetsDamage() {
        YamlConfiguration cfg = yamlOf("""
                S:
                  item: STONE
                  damage: 9
                  replacement: DIRT
                """);

        ParsedIngredient ingredient = IngredientResolver.resolve("ns", cfg, "S", "recipe");

        assertNotNull(ingredient);
        assertEquals(0, ingredient.damageAmount());
        assertEquals(Material.DIRT, ingredient.replacement().getType());
    }

    @Test
    void resolveSectionWithUnknownReplacementReturnsNull() {
        YamlConfiguration cfg = yamlOf("""
                S:
                  item: STONE
                  replacement: missing_custom_item
                """);

        assertNull(IngredientResolver.resolve("ns", cfg, "S", "recipe"));
    }

    @Test
    void resolveMapEntry() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("item", "STONE");
        raw.put("amount", 4);
        raw.put("damage", 2);
        raw.put("ignore_durability", true);
        raw.put("potion_type", "minecraft:water");

        Map<Character, ParsedIngredient> result = IngredientResolver.resolveList("ns", List.of(raw), "recipe");

        assertNotNull(result);
        ParsedIngredient ingredient = result.get('A');
        assertEquals(4, ingredient.requiredAmount());
        assertEquals(2, ingredient.damageAmount());
        assertTrue(ingredient.ignoreDurability());
        assertEquals("minecraft:water", ingredient.potionType());
    }

    @Test
    void resolveMapMissingItemReturnsNullForList() {
        Map<String, Object> raw = Map.of("amount", 2);

        assertNull(IngredientResolver.resolveList("ns", List.of(raw), "recipe"));
    }

    @Test
    void resolveListAssignsSyntheticKeysInOrder() {
        Map<Character, ParsedIngredient> result = IngredientResolver.resolveList("ns", List.of("STONE", "DIRT", "OAK_PLANKS"), "recipe");

        assertNotNull(result);
        assertEquals(List.of('A', 'B', 'C'), new ArrayList<>(result.keySet()));
    }

    @Test
    void resolveListReturnsNullWhenAnyEntryFails() {
        assertNull(IngredientResolver.resolveList("ns", List.of("STONE", "not_a_material"), "recipe"));
    }

    @Test
    void resolveListTooLargeReturnsNull() {
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < 27; i++) entries.add("STONE");

        assertNull(IngredientResolver.resolveList("ns", entries, "recipe"));
    }

    @Test
    void resolveMmoItemsWhenUnavailableReturnsNull() {
        YamlConfiguration cfg = yamlOf("S: mmoitems:sword:test\n");

        assertNull(IngredientResolver.resolve("ns", cfg, "S", "recipe"));
    }

    @Test
    void resolveInvalidTagReturnsNull() {
        YamlConfiguration cfg = yamlOf("S: '#bad tag'\n");

        assertNull(IngredientResolver.resolve("ns", cfg, "S", "recipe"));
    }

    @Test
    void resolveKnownEmptyOrUnknownTagReturnsNull() {
        YamlConfiguration cfg = yamlOf("S: '#minecraft:not_a_real_tag'\n");

        assertNull(IngredientResolver.resolve("ns", cfg, "S", "recipe"));
    }

    @Test
    void vanillaMaterialRegistrationChoiceAcceptsSameType() {
        YamlConfiguration cfg = yamlOf("S: STONE\n");
        ParsedIngredient ingredient = IngredientResolver.resolve("ns", cfg, "S", "recipe");

        assertNotNull(ingredient);
        assertTrue(ingredient.choice().test(ItemStack.of(Material.STONE)));
    }
}
