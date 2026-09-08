package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.contentvariant;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantConfig;

import static org.junit.jupiter.api.Assertions.*;

class ContentVariantConfigTest {
    private static final OpenVariantConfig EMPTY = variant("pack:empty");
    private static final OpenVariantConfig FILLED = variant("pack:filled");
    private static final OpenVariantConfig HALF = variant("pack:half");
    private static final OpenVariantConfig FULL = variant("pack:full");

    @Test
    void twoStateConfigUsesFilledForAnyNonEmptyInventory() {
        ContentVariantConfig config = new ContentVariantConfig(EMPTY, FILLED, null, null);

        assertSame(EMPTY, config.variantFor(new ItemStack[2]));
        assertSame(FILLED, config.variantFor(new ItemStack[]{ItemStack.of(Material.STONE), null}));
        assertSame(FILLED, config.variantFor(new ItemStack[]{ItemStack.of(Material.STONE, 64)}));
    }

    @Test
    void threeStateConfigUsesHalfForPartialAndFullForAllOccupiedSlots() {
        ContentVariantConfig config = new ContentVariantConfig(EMPTY, null, HALF, FULL);

        assertSame(HALF, config.variantFor(new ItemStack[]{ItemStack.of(Material.STONE), null}));
        assertSame(FULL, config.variantFor(new ItemStack[]{ItemStack.of(Material.STONE, 64), ItemStack.of(Material.DIRT, 64)}));
    }

    @Test
    void omittedStateFallsBackToOriginalOrNearestNonEmptyVariant() {
        ContentVariantConfig config = new ContentVariantConfig(null, null, HALF, null);

        assertNull(config.variantFor(new ItemStack[2]));
        assertSame(HALF, config.variantFor(new ItemStack[]{ItemStack.of(Material.STONE), null}));
        assertSame(HALF, config.variantFor(new ItemStack[]{ItemStack.of(Material.STONE, 64)}));
    }

    @Test
    void reportsConfigurationAndKnownIds() {
        assertFalse(new ContentVariantConfig(null, null, null, null).isConfigured());
        ContentVariantConfig config = new ContentVariantConfig(EMPTY, FILLED, HALF, FULL);

        assertTrue(config.isConfigured());
        assertTrue(config.containsId("pack:half"));
        assertFalse(config.containsId("pack:other"));
        assertEquals(4, config.variants().count());
    }

    private static OpenVariantConfig variant(String id) {
        return new OpenVariantConfig(ItemCategory.FURNITURE, id);
    }
}
