package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory;

import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.MenuType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("UnstableApiUsage")
class StorageInventoryTypesTest {
    @Test
    void resolveNull_returnsNull() {
        assertNull(StorageInventoryTypes.resolve(null, "ns:storage"));
    }

    @Test
    void resolveFurnace_returnsMenuSpec() {
        StorageInventorySpec spec = StorageInventoryTypes.resolve("furnace", "ns:storage");

        assertInstanceOf(StorageInventorySpec.Menu.class, spec);
        assertEquals(MenuType.FURNACE, ((StorageInventorySpec.Menu) spec).menuType());
    }

    @Test
    void resolveBlastFurnace_returnsMenuSpec() {
        StorageInventorySpec spec = StorageInventoryTypes.resolve("blast_furnace", "ns:storage");

        assertInstanceOf(StorageInventorySpec.Menu.class, spec);
        assertEquals(MenuType.BLAST_FURNACE, ((StorageInventorySpec.Menu) spec).menuType());
    }

    @Test
    void resolveSmoker_returnsMenuSpec() {
        StorageInventorySpec spec = StorageInventoryTypes.resolve("smoker", "ns:storage");

        assertInstanceOf(StorageInventorySpec.Menu.class, spec);
        assertEquals(MenuType.SMOKER, ((StorageInventorySpec.Menu) spec).menuType());
    }

    @Test
    void resolveBrewingStand_returnsMenuSpec() {
        StorageInventorySpec spec = StorageInventoryTypes.resolve("brewing_stand", "ns:storage");

        assertInstanceOf(StorageInventorySpec.Menu.class, spec);
        assertEquals(MenuType.BREWING_STAND, ((StorageInventorySpec.Menu) spec).menuType());
    }

    @Test
    void resolveDispenser_returnsTypedSpec() {
        StorageInventorySpec spec = StorageInventoryTypes.resolve("dispenser", "ns:storage");

        assertInstanceOf(StorageInventorySpec.Typed.class, spec);
        assertEquals(InventoryType.DISPENSER, ((StorageInventorySpec.Typed) spec).inventoryType());
    }

    @Test
    void resolveDropper_returnsTypedSpec() {
        StorageInventorySpec spec = StorageInventoryTypes.resolve("dropper", "ns:storage");

        assertInstanceOf(StorageInventorySpec.Typed.class, spec);
        assertEquals(InventoryType.DROPPER, ((StorageInventorySpec.Typed) spec).inventoryType());
    }

    @Test
    void resolveHopper_returnsTypedSpec() {
        StorageInventorySpec spec = StorageInventoryTypes.resolve("hopper", "ns:storage");

        assertInstanceOf(StorageInventorySpec.Typed.class, spec);
        assertEquals(InventoryType.HOPPER, ((StorageInventorySpec.Typed) spec).inventoryType());
    }

    @Test
    void resolveMixedCase_works() {
        StorageInventorySpec spec = StorageInventoryTypes.resolve("FuRnAcE", "ns:storage");

        assertInstanceOf(StorageInventorySpec.Menu.class, spec);
        assertEquals(MenuType.FURNACE, ((StorageInventorySpec.Menu) spec).menuType());
    }

    @Test
    void resolveUnknown_returnsNull() {
        assertNull(StorageInventoryTypes.resolve("chest", "ns:storage"));
    }
}
