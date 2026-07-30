package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import toutouchien.itemsadderadditions.nms.api.AdvancementDisplaySpec;
import toutouchien.itemsadderadditions.nms.api.AdvancementSpec;
import toutouchien.itemsadderadditions.nms.api.INmsAdvancementHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AdvancementManagerTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static AdvancementSpec spec(String id, String title) {
        return new AdvancementSpec(
                new NamespacedKey("test", id),
                null,
                new AdvancementDisplaySpec(
                        ItemStack.of(Material.STONE),
                        title,
                        "Description",
                        "task",
                        null,
                        true,
                        true,
                        false
                ),
                List.of("criterion"),
                false,
                0,
                List.of(),
                List.of()
        );
    }

    private static AdvancementManager manager(INmsAdvancementHandler handler) {
        return new AdvancementManager(mock(Plugin.class), handler);
    }

    @Test
    void unchangedSpecsAreNotRemovedAndRegisteredAgain() {
        INmsAdvancementHandler handler = mock(INmsAdvancementHandler.class);
        AdvancementManager manager = manager(handler);

        manager.synchronizeRegistrations(List.of(spec("whoops", "Whoops")));
        manager.synchronizeRegistrations(List.of(spec("whoops", "Whoops")));

        verify(handler, times(1)).replaceAll(anyCollection(), anyList());
    }

    @Test
    void changedSpecsAreReplaced() {
        INmsAdvancementHandler handler = mock(INmsAdvancementHandler.class);
        AdvancementManager manager = manager(handler);

        manager.synchronizeRegistrations(List.of(spec("whoops", "Whoops")));
        manager.synchronizeRegistrations(List.of(spec("whoops", "Whoops!")));

        verify(handler, times(2)).replaceAll(anyCollection(), anyList());
    }

    @Test
    void specOrderAloneDoesNotCauseReplacement() {
        INmsAdvancementHandler handler = mock(INmsAdvancementHandler.class);
        AdvancementManager manager = manager(handler);
        AdvancementSpec whoops = spec("whoops", "Whoops");
        AdvancementSpec trash = spec("this_is_trash", "This Is Trash");

        manager.synchronizeRegistrations(List.of(whoops, trash));
        manager.synchronizeRegistrations(List.of(trash, whoops));

        verify(handler, times(1)).replaceAll(anyCollection(), anyList());
    }
}
