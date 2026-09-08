package toutouchien.itemsadderadditions.feature.recipe.crafting;

import dev.lone.itemsadder.api.CustomStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockbukkit.mockbukkit.MockBukkit;
import toutouchien.itemsadderadditions.common.namespace.CustomTagDefinition;
import toutouchien.itemsadderadditions.common.namespace.CustomTagRegistry;
import toutouchien.itemsadderadditions.common.namespace.CustomTagType;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.nms.api.INmsCraftingRecipeHandler;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CraftingRecipeListenerTest {
    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        NamespaceUtils.initVanillaCache();
    }

    @AfterEach
    void tearDown() {
        NamespaceUtils.invalidateCache();
        NamespaceUtils.clearCustomTagRegistry();
        MockBukkit.unmock();
    }

    @Test
    void prepareAndCraftSelectMatchingRecipeWhenPaperChoosesEquivalentShapelessRecipe() {
        CustomStack opener = customStack("can_opener", Material.FLINT_AND_STEEL);
        CustomStack beefCan = customStack("beef_can", Material.PAPER);
        CustomStack beefCanOpen = customStack("beef_can_open", Material.PAPER);
        CustomStack porkCan = customStack("pork_can", Material.PAPER);
        CustomStack porkCanOpen = customStack("pork_can_open", Material.PAPER);
        CustomStack vegetablesCan = customStack("vegetables_can", Material.PAPER);
        CustomStack vegetablesCanOpen = customStack("vegetables_can_open", Material.PAPER);
        List<CustomStack> items = List.of(
                opener,
                beefCan, beefCanOpen,
                porkCan, porkCanOpen,
                vegetablesCan, vegetablesCanOpen
        );
        NamespaceUtils.buildCache(items);
        NamespaceUtils.setCustomTagRegistry(CustomTagRegistry.resolve(List.of(
                new CustomTagDefinition(
                        "eightyseven",
                        "can_openers",
                        CustomTagType.ITEM,
                        List.of("can_opener"),
                        "test.yml")
        )));

        List<CraftingRecipeData> registered = new ArrayList<>();
        CraftingRecipeHandler handler = new CraftingRecipeHandler(new INmsCraftingRecipeHandler() {
            @Override
            public void register(CraftingRecipeData data) {
                registered.add(data);
            }

            @Override
            public void unregisterAll() {
            }
        });
        YamlConfiguration config = yaml(REPORTED_RECIPES);
        handler.load("eightyseven", config.getConfigurationSection("recipes"));
        assertEquals(3, registered.size(), "all three YAML recipes should register");

        ItemStack openerItem = opener.getItemStack();
        ItemStack beefItem = beefCan.getItemStack();
        ItemStack expectedResult = beefCanOpen.getItemStack();
        ItemStack paperSelectedResult = vegetablesCanOpen.getItemStack();
        ItemStack[] matrix = {openerItem, beefItem};
        CraftingRecipeData beefRecipe = handler.predicateRecipeByKey(
                new NamespacedKey("eightyseven", "beef_can_open"));
        CraftingRecipeData vegetablesRecipe = handler.predicateRecipeByKey(
                new NamespacedKey("eightyseven", "vegetables_can_open"));
        assertNotNull(beefRecipe);
        assertNotNull(vegetablesRecipe);
        assertEquals("eightyseven:beef_can", beefRecipe.ingredients().get('B').customNamespacedId());
        Map<ItemStack, CustomStack> customItemsByStack = new IdentityHashMap<>();
        for (CustomStack item : items) {
            customItemsByStack.put(item.getItemStack(), item);
        }

        CraftingInventory inventory = mock(CraftingInventory.class);
        when(inventory.getMatrix()).thenReturn(matrix);

        ShapelessRecipe paperSelectedRecipe = new ShapelessRecipe(
                new NamespacedKey("eightyseven", "vegetables_can_open"),
                paperSelectedResult);
        PrepareItemCraftEvent event = mock(PrepareItemCraftEvent.class);
        when(event.getRecipe()).thenReturn(paperSelectedRecipe);
        when(event.getInventory()).thenReturn(inventory);

        InventoryView view = mock(InventoryView.class);
        Player player = mock(Player.class);
        CraftItemEvent craftEvent = mock(CraftItemEvent.class);
        when(craftEvent.getRecipe()).thenReturn(paperSelectedRecipe);
        when(craftEvent.getInventory()).thenReturn(inventory);
        when(craftEvent.getWhoClicked()).thenReturn(player);
        when(craftEvent.getView()).thenReturn(view);

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(any(ItemStack.class)))
                    .thenAnswer(invocation -> customItemsByStack.get(invocation.getArgument(0)));

            assertTrue(CraftingPredicateEngine.ingredientsSatisfied(beefRecipe, matrix));
            assertFalse(CraftingPredicateEngine.ingredientsSatisfied(vegetablesRecipe, matrix));
            CraftingRecipeListener listener = new CraftingRecipeListener(handler, mock(Plugin.class));
            listener.onPrepare(event);
            listener.onCraft(craftEvent);
        }

        ArgumentCaptor<ItemStack> resultCaptor = ArgumentCaptor.forClass(ItemStack.class);
        verify(inventory).setResult(resultCaptor.capture());
        ItemStack actualResult = resultCaptor.getValue();
        assertNotNull(actualResult, "beef recipe must not be blocked by Paper selecting the equivalent vegetables recipe");
        assertEquals(expectedResult, actualResult);

        ArgumentCaptor<ItemStack> cursorCaptor = ArgumentCaptor.forClass(ItemStack.class);
        verify(craftEvent).setCancelled(true);
        verify(view).setCursor(cursorCaptor.capture());
        assertEquals(expectedResult, cursorCaptor.getValue());
    }

    private static CustomStack customStack(String id, Material material) {
        ItemStack item = ItemStack.of(material);
        item.editMeta(meta -> meta.displayName(Component.text(id)));

        CustomStack stack = mock(CustomStack.class);
        when(stack.getNamespace()).thenReturn("eightyseven");
        when(stack.getId()).thenReturn(id);
        when(stack.getNamespacedID()).thenReturn("eightyseven:" + id);
        when(stack.getItemStack()).thenReturn(item);
        return stack;
    }

    private static YamlConfiguration yaml(String text) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(text);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return yaml;
    }

    private static final String REPORTED_RECIPES = """
            recipes:
              beef_can_open:
                shapeless: true
                ingredients:
                  - item: "#eightyseven:can_openers"
                    amount: 1
                    damage: 2
                  - item: beef_can
                    amount: 1
                result:
                  item: beef_can_open
                  amount: 1
              pork_can_open:
                shapeless: true
                ingredients:
                  - item: "#eightyseven:can_openers"
                    amount: 1
                    damage: 2
                  - item: pork_can
                    amount: 1
                result:
                  item: pork_can_open
                  amount: 1
              vegetables_can_open:
                shapeless: true
                ingredients:
                  - item: "#eightyseven:can_openers"
                    amount: 1
                    damage: 2
                  - item: vegetables_can
                    amount: 1
                result:
                  item: vegetables_can_open
                  amount: 1
            """;
}
