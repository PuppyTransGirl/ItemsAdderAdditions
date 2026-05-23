package toutouchien.itemsadderadditions.feature.action.loading;

import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TriggerCatalogTest {
    @Test
    void itemInteractIsArgumentized() {
        Map<String, TriggerDefinition> catalog = TriggerCatalog.forCategory(ItemCategory.ITEM);
        assertTrue(catalog.get("interact").argumentized());
        assertEquals(TriggerType.ITEM_INTERACT, catalog.get("interact").type());
    }

    @Test
    void itemInteractMainhandIsArgumentized() {
        Map<String, TriggerDefinition> catalog = TriggerCatalog.forCategory(ItemCategory.ITEM);
        assertTrue(catalog.get("interact_mainhand").argumentized());
    }

    @Test
    void itemBlockBreakNotArgumentized() {
        Map<String, TriggerDefinition> catalog = TriggerCatalog.forCategory(ItemCategory.ITEM);
        assertFalse(catalog.get("block_break").argumentized());
        assertEquals(TriggerType.ITEM_BREAK_BLOCK, catalog.get("block_break").type());
    }

    @Test
    void itemCategoryContainsGunEvents() {
        Map<String, TriggerDefinition> catalog = TriggerCatalog.forCategory(ItemCategory.ITEM);
        assertTrue(catalog.containsKey("gun_shot"));
        assertTrue(catalog.containsKey("gun_no_ammo"));
        assertTrue(catalog.containsKey("gun_reload"));
    }

    @Test
    void itemCategoryContainsFishingEvents() {
        Map<String, TriggerDefinition> catalog = TriggerCatalog.forCategory(ItemCategory.ITEM);
        assertTrue(catalog.containsKey("fishing_start"));
        assertTrue(catalog.containsKey("fishing_caught"));
        assertTrue(catalog.containsKey("fishing_failed"));
        assertTrue(catalog.containsKey("fishing_cancel"));
        assertTrue(catalog.containsKey("fishing_bite"));
        assertTrue(catalog.containsKey("fishing_in_ground"));
    }

    @Test
    void blockCategoryContainsInteractAndBreak() {
        Map<String, TriggerDefinition> catalog = TriggerCatalog.forCategory(ItemCategory.BLOCK);
        assertEquals(2, catalog.size());
        assertTrue(catalog.containsKey("interact"));
        assertTrue(catalog.containsKey("break"));
        assertEquals(TriggerType.BLOCK_INTERACT, catalog.get("interact").type());
        assertEquals(TriggerType.PLACED_BLOCK_BREAK, catalog.get("break").type());
    }

    @Test
    void blockCategoryNothingIsArgumentized() {
        Map<String, TriggerDefinition> catalog = TriggerCatalog.forCategory(ItemCategory.BLOCK);
        assertFalse(catalog.get("interact").argumentized());
        assertFalse(catalog.get("break").argumentized());
    }

    @Test
    void blockCategoryHasNoGunEvents() {
        Map<String, TriggerDefinition> catalog = TriggerCatalog.forCategory(ItemCategory.BLOCK);
        assertFalse(catalog.containsKey("gun_shot"));
        assertFalse(catalog.containsKey("fishing_start"));
    }

    @Test
    void furnitureInteractIsArgumentized() {
        Map<String, TriggerDefinition> catalog = TriggerCatalog.forCategory(ItemCategory.FURNITURE);
        assertTrue(catalog.get("interact").argumentized());
        assertEquals(TriggerType.FURNITURE_INTERACT, catalog.get("interact").type());
    }

    @Test
    void furnitureCategoryContainsWearEvents() {
        Map<String, TriggerDefinition> catalog = TriggerCatalog.forCategory(ItemCategory.FURNITURE);
        assertTrue(catalog.containsKey("wear"));
        assertTrue(catalog.containsKey("unwear"));
        assertEquals(TriggerType.FURNITURE_WEAR, catalog.get("wear").type());
        assertEquals(TriggerType.FURNITURE_UNWEAR, catalog.get("unwear").type());
    }

    @Test
    void complexFurnitureHasOnlyInteract() {
        Map<String, TriggerDefinition> catalog = TriggerCatalog.forCategory(ItemCategory.COMPLEX_FURNITURE);
        assertEquals(1, catalog.size());
        assertTrue(catalog.containsKey("interact"));
        assertEquals(TriggerType.COMPLEX_FURNITURE_INTERACT, catalog.get("interact").type());
    }

    @Test
    void itemCategoryContainsBucketEvents() {
        Map<String, TriggerDefinition> catalog = TriggerCatalog.forCategory(ItemCategory.ITEM);
        assertEquals(TriggerType.ITEM_BUCKET_EMPTY, catalog.get("bucket_empty").type());
        assertEquals(TriggerType.ITEM_BUCKET_FILL, catalog.get("bucket_fill").type());
    }
}
