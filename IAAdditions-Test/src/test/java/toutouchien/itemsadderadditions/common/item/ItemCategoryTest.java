package toutouchien.itemsadderadditions.common.item;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ItemCategoryTest {

    private YamlConfiguration yaml(String body) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    private CustomStack stack(String namespacedId, boolean isBlock) {
        CustomStack stack = mock(CustomStack.class);
        when(stack.getNamespacedID()).thenReturn(namespacedId);
        lenient().when(stack.isBlock()).thenReturn(isBlock);
        return stack;
    }

    @Test
    void complexFurnitureWins() {
        var cfg = yaml("""
                items:
                  chair:
                    behaviours:
                      complex_furniture: {}
                """);
        assertEquals(ItemCategory.COMPLEX_FURNITURE,
                ItemCategory.determine(stack("pack:chair", false), cfg, "chair"));
    }

    @Test
    void placedFurnitureEventsImplyFurniture() {
        var cfg = yaml("""
                items:
                  lamp:
                    events:
                      placed_furniture: {}
                """);
        assertEquals(ItemCategory.FURNITURE,
                ItemCategory.determine(stack("pack:lamp", false), cfg, "lamp"));
    }

    @Test
    void furnitureBehaviourImpliesFurniture() {
        var cfg = yaml("""
                items:
                  lamp:
                    behaviours:
                      furniture: {}
                """);
        assertEquals(ItemCategory.FURNITURE,
                ItemCategory.determine(stack("pack:lamp", false), cfg, "lamp"));
    }

    @Test
    void blockWhenIsBlockAndNoFurniture() {
        var cfg = yaml("""
                items:
                  ore: {}
                """);
        assertEquals(ItemCategory.BLOCK,
                ItemCategory.determine(stack("pack:ore", true), cfg, "ore"));
    }

    @Test
    void plainItemFallsBackToItem() {
        var cfg = yaml("""
                items:
                  gem: {}
                """);
        assertEquals(ItemCategory.ITEM,
                ItemCategory.determine(stack("pack:gem", false), cfg, "gem"));
    }

    @Test
    void sameFileVariantInheritsParentCategory() {
        // variant_of points at "base" in the same file.
        var cfg2 = yaml("""
                items:
                  base:
                    behaviours:
                      furniture: {}
                  child:
                    variant_of: base
                """);
        assertEquals(ItemCategory.FURNITURE,
                ItemCategory.determine(stack("pack:child", false), cfg2, "child"));
    }

    @Test
    void blankVariantOfBreaksChainAndUsesItem() {
        var cfg = yaml("""
                items:
                  child:
                    variant_of: ""
                """);
        assertEquals(ItemCategory.ITEM,
                ItemCategory.determine(stack("pack:child", false), cfg, "child"));
    }

    @Test
    void crossFileVariantConsultsItemsAdderInstance() {
        var cfg = yaml("""
                items:
                  child:
                    variant_of: otherpack:remote_base
                """);
        // Cross-file parent resolution calls CustomStack.getInstance, whose compileOnly IA stub
        // throws UnsupportedOperationException. Real ItemsAdder runtime needed to resolve it.
        assertThrows(UnsupportedOperationException.class,
                () -> ItemCategory.determine(stack("pack:child", false), cfg, "child"));
    }

    @Test
    void namespacedIdWithoutColonStillResolves() {
        var cfg = yaml("""
                items:
                  gem: {}
                """);
        assertEquals(ItemCategory.ITEM,
                ItemCategory.determine(stack("gem", false), cfg, "gem"));
    }
}
