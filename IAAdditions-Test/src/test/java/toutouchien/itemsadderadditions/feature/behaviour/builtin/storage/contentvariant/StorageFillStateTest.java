package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.contentvariant;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageFillStateTest {
    @Test
    void nullEmptyAndAirOnlyContentsAreEmpty() {
        assertEquals(StorageFillState.EMPTY, StorageFillState.fromContents(null));
        assertEquals(StorageFillState.EMPTY, StorageFillState.fromContents(new ItemStack[0]));
        assertEquals(StorageFillState.EMPTY, StorageFillState.fromContents(new ItemStack[]{null, ItemStack.of(Material.AIR)}));
    }

    @Test
    void anyOccupiedButNotAllSlotsIsHalf() {
        assertEquals(StorageFillState.HALF, StorageFillState.fromContents(new ItemStack[]{
                ItemStack.of(Material.DIAMOND), null, null
        }));
    }

    @Test
    void everySlotMustReachItsOwnStackCapacityToBeFull() {
        assertEquals(StorageFillState.FULL, StorageFillState.fromContents(new ItemStack[]{
                ItemStack.of(Material.DIAMOND_SWORD), ItemStack.of(Material.DIRT, 64)
        }));
        assertEquals(StorageFillState.HALF, StorageFillState.fromContents(new ItemStack[]{
                ItemStack.of(Material.DIAMOND_SWORD), ItemStack.of(Material.DIRT, 63)
        }));
    }
}
