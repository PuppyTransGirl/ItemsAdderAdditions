package toutouchien.itemsadderadditions.common.item;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        int colon = namespacedId.indexOf(':');
        if (colon >= 0) {
            lenient().when(stack.getNamespace()).thenReturn(namespacedId.substring(0, colon));
            lenient().when(stack.getId()).thenReturn(namespacedId.substring(colon + 1));
        } else {
            lenient().when(stack.getNamespace()).thenReturn("");
            lenient().when(stack.getId()).thenReturn(namespacedId);
        }
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
    void sameFileNamespacedVariantInheritsParentCategory() {
        var cfg = yaml("""
                items:
                  base:
                    behaviours:
                      furniture: {}
                  child:
                    variant_of: pack:base
                """);
        assertEquals(ItemCategory.FURNITURE,
                ItemCategory.determine(stack("pack:child", false), cfg, "child"));
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
    void crossFileRegisteredVariantInheritsParentCategory() {
        var childCfg = yaml("""
                items:
                  child:
                    variant_of: otherpack:remote_base
                """);
        var parentCfg = yaml("""
                items:
                  remote_base:
                    behaviours:
                      furniture: {}
                """);
        CustomStack parent = stack("otherpack:remote_base", false);
        when(parent.getConfig()).thenReturn(parentCfg);

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("otherpack:remote_base")).thenReturn(parent);

            assertEquals(ItemCategory.FURNITURE,
                    ItemCategory.determine(stack("pack:child", false), childCfg, "child"));
        }
    }

    @Test
    void crossFileTemplateVariantInheritsParentCategoryWhenTemplateApiExists() {
        var childCfg = yaml("""
                items:
                  child:
                    variant_of: office_table_template
                """);
        var template = yaml("""
                behaviours:
                  furniture: {}
                """);

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("pack:office_table_template")).thenReturn(null);
            customStacks.when(() -> CustomStack.getConfigSectionOfTemplateCopy("pack:office_table_template"))
                    .thenReturn(template);

            assertEquals(ItemCategory.FURNITURE,
                    ItemCategory.determine(stack("pack:child", false), childCfg, "child"));
        }
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
