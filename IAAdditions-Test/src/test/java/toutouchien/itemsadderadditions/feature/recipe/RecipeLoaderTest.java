package toutouchien.itemsadderadditions.feature.recipe;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import toutouchien.itemsadderadditions.common.loading.ConfigFileRegistry;
import toutouchien.itemsadderadditions.feature.recipe.brewing.BrewingRecipeHandler;
import toutouchien.itemsadderadditions.feature.recipe.campfire.CampfireRecipeHandler;
import toutouchien.itemsadderadditions.feature.recipe.crafting.CraftingRecipeHandler;
import toutouchien.itemsadderadditions.feature.recipe.stonecutter.StonecutterRecipeHandler;
import toutouchien.itemsadderadditions.nms.api.INmsHandler;
import toutouchien.itemsadderadditions.testsupport.FakeNms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class RecipeLoaderTest {
    @TempDir
    Path contents;

    private INmsHandler nms;
    private CampfireRecipeHandler campfire;
    private StonecutterRecipeHandler stonecutter;
    private BrewingRecipeHandler brewing;
    private CraftingRecipeHandler crafting;
    private RecipeLoader loader;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        toutouchien.itemsadderadditions.common.namespace.NamespaceUtils.initVanillaCache();
        nms = FakeNms.install();
        campfire = new CampfireRecipeHandler();
        stonecutter = new StonecutterRecipeHandler();
        brewing = new BrewingRecipeHandler(new NoopBrewingMixRegistry());
        crafting = new CraftingRecipeHandler(nms.craftingRecipes());
        loader = new RecipeLoader(campfire, stonecutter, brewing, crafting);
    }

    @AfterEach
    void tearDown() {
        FakeNms.uninstall();
        MockBukkit.unmock();
    }

    private void writeRecipeFile(String name, String body) throws IOException {
        Files.writeString(contents.resolve(name), body);
    }

    @Test
    void loadsCampfireAndStonecutterRecipes() throws IOException {
        writeRecipeFile("recipes.yml", """
                info:
                  namespace: testpack
                recipes:
                  campfire_cooking:
                    smelt_beef:
                      ingredient:
                        item: BEEF
                      result:
                        item: COOKED_BEEF
                      cook_time: 120
                      exp: 0.35
                  stonecutter:
                    cut_bricks:
                      ingredient:
                        item: STONE
                      result:
                        item: STONE_BRICKS
                        amount: 2
                """);

        ConfigFileRegistry registry = ConfigFileRegistry.scan(contents.toFile());
        loader.loadAll(registry);

        assertEquals(1, campfire.loadedCount());
        assertEquals(1, stonecutter.loadedCount());
        assertEquals(2, loader.totalLoadedCount());

        verify(nms.campfireRecipes())
                .register(eq("testpack"), eq("smelt_beef"), any(), any(), eq(120), eq(0.35f));
        verify(nms.stonecutterRecipes())
                .register(eq("testpack"), eq("cut_bricks"), any(), any());
    }

    @Test
    void skipsDisabledRecipe() throws IOException {
        writeRecipeFile("disabled.yml", """
                info:
                  namespace: testpack
                recipes:
                  campfire_cooking:
                    off_recipe:
                      enabled: false
                      ingredient:
                        item: BEEF
                      result:
                        item: COOKED_BEEF
                """);

        ConfigFileRegistry registry = ConfigFileRegistry.scan(contents.toFile());
        loader.loadAll(registry);

        assertEquals(0, campfire.loadedCount());
    }

    @Test
    void skipsRecipeWithMissingResult() throws IOException {
        writeRecipeFile("bad.yml", """
                info:
                  namespace: testpack
                recipes:
                  campfire_cooking:
                    incomplete:
                      ingredient:
                        item: BEEF
                """);

        ConfigFileRegistry registry = ConfigFileRegistry.scan(contents.toFile());
        loader.loadAll(registry);

        assertEquals(0, campfire.loadedCount());
    }

    @Test
    void ignoresFileWithoutNamespace() throws IOException {
        writeRecipeFile("nons.yml", """
                recipes:
                  campfire_cooking:
                    r:
                      ingredient:
                        item: BEEF
                      result:
                        item: COOKED_BEEF
                """);

        ConfigFileRegistry registry = ConfigFileRegistry.scan(contents.toFile());
        loader.loadAll(registry);

        assertEquals(0, campfire.loadedCount());
    }

    @Test
    void emptyRegistryLoadsNothing() {
        ConfigFileRegistry registry = ConfigFileRegistry.scan(contents.toFile());
        loader.loadAll(registry);
        assertEquals(0, loader.totalLoadedCount());
    }

    @Test
    void loadsBrewingRecipes() throws IOException {
        writeRecipeFile("brewing.yml", """
                info:
                  namespace: testpack
                recipes:
                  brewing:
                    night_vision_goggles:
                      base:
                        item: POTION
                      ingredient:
                        item: NETHER_WART
                      result:
                        item: GLASS_BOTTLE
                      brew_time: 120
                      fuel_cost: 1
                """);

        ConfigFileRegistry registry = ConfigFileRegistry.scan(contents.toFile());
        loader.loadAll(registry);

        assertEquals(1, brewing.loadedCount());
        assertEquals(1, loader.totalLoadedCount());
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
