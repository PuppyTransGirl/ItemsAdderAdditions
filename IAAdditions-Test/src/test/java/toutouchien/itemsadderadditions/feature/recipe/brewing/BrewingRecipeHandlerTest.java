package toutouchien.itemsadderadditions.feature.recipe.brewing;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrewingRecipeHandlerTest {
    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        NamespaceUtils.initVanillaCache();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void loadsInputAliasAndMatchesOnlySimilarBottleSlots() {
        BrewingRecipeHandler handler = new BrewingRecipeHandler(new NoopBrewingMixRegistry());
        YamlConfiguration yaml = yaml("""
                example:
                  input:
                    item: POTION
                  ingredient:
                    item: NETHER_WART
                  result:
                    item: GLASS_BOTTLE
                  brew_time: 300
                """);

        handler.load("testpack", yaml);

        BrewerInventory inventory = mock(BrewerInventory.class);
        when(inventory.getIngredient()).thenReturn(new ItemStack(Material.NETHER_WART));
        when(inventory.getItem(0)).thenReturn(new ItemStack(Material.POTION));
        when(inventory.getItem(1)).thenReturn(new ItemStack(Material.WATER_BUCKET));
        when(inventory.getItem(2)).thenReturn(new ItemStack(Material.POTION));

        BrewingRecipeMatch match = handler.match(inventory);

        assertNotNull(match);
        assertEquals(2, match.matchedSlots());
        assertEquals(300, match.recipe().brewTime());
        assertEquals(1, match.recipe().fuelCost());
    }

    @Test
    void skipsInvalidConsumeAmount() {
        BrewingRecipeHandler handler = new BrewingRecipeHandler(new NoopBrewingMixRegistry());
        YamlConfiguration yaml = yaml("""
                bad:
                  base:
                    item: POTION
                  ingredient:
                    item: NETHER_WART
                    consume: 0
                  result:
                    item: GLASS_BOTTLE
                """);

        handler.load("testpack", yaml);

        assertEquals(0, handler.loadedCount());
    }

    @Test
    void missingBaseAndInputDoesNotLoad() {
        BrewingRecipeHandler handler = new BrewingRecipeHandler(new NoopBrewingMixRegistry());
        YamlConfiguration yaml = yaml("""
                bad:
                  ingredient:
                    item: NETHER_WART
                  result:
                    item: GLASS_BOTTLE
                """);

        handler.load("testpack", yaml);

        assertEquals(0, handler.loadedCount());
    }

    @Test
    void matchRequiresEnoughIngredientAmount() {
        BrewingRecipeHandler handler = new BrewingRecipeHandler(new NoopBrewingMixRegistry());
        YamlConfiguration yaml = yaml("""
                example:
                  base:
                    item: POTION
                  ingredient:
                    item: NETHER_WART
                    consume: 2
                  result:
                    item: GLASS_BOTTLE
                """);
        handler.load("testpack", yaml);

        BrewerInventory inventory = mock(BrewerInventory.class);
        when(inventory.getIngredient()).thenReturn(new ItemStack(Material.NETHER_WART, 1));
        when(inventory.getItem(0)).thenReturn(new ItemStack(Material.POTION));

        assertNull(handler.match(inventory));
    }

    private static YamlConfiguration yaml(String source) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    private static final class NoopBrewingMixRegistry implements BrewingRecipeHandler.BrewingMixRegistry {
        @Override
        public void add(io.papermc.paper.potion.PotionMix mix) {
        }

        @Override
        public void remove(org.bukkit.NamespacedKey key) {
        }
    }
}
