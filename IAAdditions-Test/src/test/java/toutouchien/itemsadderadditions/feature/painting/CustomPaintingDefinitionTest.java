package toutouchien.itemsadderadditions.feature.painting;

import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.nms.api.painting.NmsPaintingVariant;

import static org.junit.jupiter.api.Assertions.*;

class CustomPaintingDefinitionTest {
    private static CustomPaintingDefinition definition(
            String variantId,
            String title,
            String author,
            boolean includeInRandom
    ) {
        return new CustomPaintingDefinition(
                variantId,
                32,
                16,
                "ns:asset",
                title,
                author,
                "ns:item",
                includeInRandom,
                "paintings.yml"
        );
    }

    @Test
    void recordStoresAllFields() {
        CustomPaintingDefinition definition = definition("ns:starry", "title", "author", true);

        assertEquals("ns:starry", definition.variantId());
        assertEquals(32, definition.width());
        assertEquals(16, definition.height());
        assertEquals("ns:asset", definition.assetId());
        assertEquals("title", definition.title());
        assertEquals("author", definition.author());
        assertEquals("ns:item", definition.itemId());
        assertTrue(definition.includeInRandom());
        assertEquals("paintings.yml", definition.sourceFile());
    }

    @Test
    void toNmsVariantCopiesCoreVariantFields() {
        CustomPaintingDefinition definition = definition("ns:starry", "Night", "Painter", false);

        NmsPaintingVariant variant = definition.toNmsVariant();

        assertEquals("ns:starry", variant.variantId());
        assertEquals(32, variant.width());
        assertEquals(16, variant.height());
        assertEquals("ns:asset", variant.assetId());
        assertEquals("Night", variant.title());
        assertEquals("Painter", variant.author());
    }

    @Test
    void toNmsVariantPreservesNullTitleAndAuthor() {
        CustomPaintingDefinition definition = definition("ns:untitled", null, null, true);

        NmsPaintingVariant variant = definition.toNmsVariant();

        assertNull(variant.title());
        assertNull(variant.author());
    }
}
