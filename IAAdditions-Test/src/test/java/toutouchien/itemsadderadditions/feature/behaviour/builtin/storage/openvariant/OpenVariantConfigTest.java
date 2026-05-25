package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant;

import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.common.item.ItemCategory;

import static org.junit.jupiter.api.Assertions.*;

class OpenVariantConfigTest {
    @Test
    void resolveNull_returnsNull() {
        assertNull(OpenVariantConfig.resolve(null, "ns:chest"));
    }

    @Test
    void resolveBlank_returnsNull() {
        assertNull(OpenVariantConfig.resolve("   ", "ns:chest"));
    }

    @Test
    void isFurnitureBased_trueForFurniture() {
        assertTrue(new OpenVariantConfig(ItemCategory.FURNITURE, "ns:open").isFurnitureBased());
    }

    @Test
    void isFurnitureBased_trueForComplexFurniture() {
        assertTrue(new OpenVariantConfig(ItemCategory.COMPLEX_FURNITURE, "ns:open").isFurnitureBased());
    }

    @Test
    void isFurnitureBased_falseForBlock() {
        assertFalse(new OpenVariantConfig(ItemCategory.BLOCK, "ns:open").isFurnitureBased());
    }

    @Test
    void isFurnitureBased_falseForItem() {
        assertFalse(new OpenVariantConfig(ItemCategory.ITEM, "ns:open").isFurnitureBased());
    }

    @Test
    void isItem_trueForItem() {
        assertTrue(new OpenVariantConfig(ItemCategory.ITEM, "ns:open").isItem());
    }

    @Test
    void isItem_falseForFurniture() {
        assertFalse(new OpenVariantConfig(ItemCategory.FURNITURE, "ns:open").isItem());
    }

    @Test
    void recordStoresCategoryAndId() {
        OpenVariantConfig config = new OpenVariantConfig(ItemCategory.BLOCK, "ns:open_block");

        assertEquals(ItemCategory.BLOCK, config.category());
        assertEquals("ns:open_block", config.id());
    }
}
