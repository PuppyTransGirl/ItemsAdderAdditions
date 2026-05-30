package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WritableBookContentComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void configureRejectsNonList() {
        assertFalse(new WritableBookContentComponent().configure("page", "test:item"));
    }

    @Test
    void configureAcceptsPagesAndApplies() {
        WritableBookContentComponent component = new WritableBookContentComponent();
        assertTrue(component.configure(List.of("Page one", "Page two"), "test:item"));

        ItemStack stack = component.apply(ItemStack.of(Material.WRITABLE_BOOK), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.WRITABLE_BOOK_CONTENT));
    }

    @Test
    void configureCoercesNonStringEntries() {
        // Non-string entries are coerced via toString, so a numeric page is still accepted.
        assertTrue(new WritableBookContentComponent().configure(List.of(123, "text"), "test:item"));
    }

    @Test
    void configureRejectsEmptyList() {
        assertFalse(new WritableBookContentComponent().configure(List.of(), "test:item"));
    }
}
