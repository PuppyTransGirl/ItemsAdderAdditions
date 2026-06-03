package toutouchien.itemsadderadditions.common.namespace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomTagNamespaceUtilsTest {
    @AfterEach
    void clearRegistry() {
        NamespaceUtils.clearCustomTagRegistry();
    }

    @Test
    void localCustomTagReferenceNormalizesWhenLoaded() {
        install(new CustomTagDefinition(
                "my_pack", "ruby_items", CustomTagType.ITEM,
                List.of("minecraft:diamond"), "test.yml"));

        assertEquals("#my_pack:ruby_items", NamespaceUtils.normalizeItemIDOrTag("my_pack", "#ruby_items"));
    }

    @Test
    void vanillaTagReferenceFallsBackWhenNoCustomTagExists() {
        assertEquals("#minecraft:planks", NamespaceUtils.normalizeItemIDOrTag("my_pack", "#planks"));
        assertEquals("#minecraft:logs", NamespaceUtils.normalizeBlockIDOrTag("my_pack", "#minecraft:logs"));
    }

    @Test
    void itemTagMatchesAnyExpandedValue() {
        install(new CustomTagDefinition(
                "my_pack", "ruby_items", CustomTagType.ITEM,
                List.of("minecraft:diamond_sword", "mmoitems:sword:ruby_sword"), "test.yml"));

        assertTrue(NamespaceUtils.matchesContentIDOrTag(
                "minecraft:diamond_sword",
                "#my_pack:ruby_items",
                CustomTagType.ITEM));
        assertFalse(NamespaceUtils.matchesContentIDOrTag(
                "minecraft:stick",
                "#my_pack:ruby_items",
                CustomTagType.ITEM));
    }

    @Test
    void blockTagMatchesVanillaBlockId() {
        install(new CustomTagDefinition(
                "my_pack", "ruby_blocks", CustomTagType.BLOCK,
                List.of("minecraft:diamond_block"), "test.yml"));

        assertTrue(NamespaceUtils.matchesContentIDOrTag(
                "minecraft:diamond_block",
                "#my_pack:ruby_blocks",
                CustomTagType.BLOCK));
        assertFalse(NamespaceUtils.matchesContentIDOrTag(
                "minecraft:stone",
                "#my_pack:ruby_blocks",
                CustomTagType.BLOCK));
    }

    @Test
    void furnitureTagMatchesActualFurnitureIdsWithRotationSuffixes() {
        install(new CustomTagDefinition(
                "my_pack", "ruby_furnitures", CustomTagType.FURNITURE,
                List.of("ruby_chair"), "test.yml"));

        assertTrue(NamespaceUtils.matchesFurnitureIDOrTag(
                "my_pack:ruby_chair_south",
                "#my_pack:ruby_furnitures"));
    }

    @Test
    void recipeTagMatchesCustomAndVanillaRecipeKeys() {
        install(new CustomTagDefinition(
                "my_pack", "sword_recipes", CustomTagType.RECIPE,
                List.of("ruby_sword_recipe", "minecraft:diamond_sword"), "test.yml"));

        assertTrue(NamespaceUtils.matchesRecipeIDOrTag(
                "my_pack:ruby_sword_recipe",
                "#my_pack:sword_recipes"));
        assertTrue(NamespaceUtils.matchesRecipeIDOrTag(
                "minecraft:diamond_sword",
                "#my_pack:sword_recipes"));
        assertTrue(NamespaceUtils.matchesRecipeIDOrTag(
                "minecraft:iron_sword",
                "iron_sword"));
    }

    private void install(CustomTagDefinition... definitions) {
        NamespaceUtils.setCustomTagRegistry(CustomTagRegistry.resolve(List.of(definitions)));
    }
}
