package toutouchien.itemsadderadditions.integration.valhalla;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValhallaManagerTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void nameAndPhaseAreStable() {
        ValhallaManager manager = new ValhallaManager();
        assertEquals("Valhalla", manager.name());
        assertEquals(ReloadPhase.ITEM_BINDINGS, manager.phase());
    }

    @Test
    void reloadEmptyContextReturnsLoadedZero() {
        ValhallaManager manager = new ValhallaManager();
        ReloadStepResult result = manager.reload(new ContentReloadContext(List.of(), null));

        assertEquals("Valhalla", result.system());
        assertEquals(0, result.loadedCount());
        assertFalse(result.registryChanged());
    }

    @Test
    void applyValhallaNoBindingReturnsOriginal() {
        ValhallaManager manager = new ValhallaManager();
        ItemStack stack = ItemStack.of(Material.DIAMOND_SWORD);
        assertSame(stack, manager.applyValhalla("test:item", stack));
    }

    @Test
    void shutdownMakesApplyValhallaNoOp() {
        ValhallaManager manager = new ValhallaManager();
        manager.shutdown();
        ItemStack stack = ItemStack.of(Material.DIAMOND_SWORD);
        assertSame(stack, manager.applyValhalla("test:item", stack));
    }

    @Test
    void reloadAfterShutdownIsSafe() {
        ValhallaManager manager = new ValhallaManager();
        manager.shutdown();
        ReloadStepResult result = manager.reload(new ContentReloadContext(List.of(), null));
        assertEquals(0, result.loadedCount());
    }
}
