package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AdvancementRuntimeServiceTest {
    private ServerMock server;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("AdvTest");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // NOTE: service.register(plugin) cannot run under MockBukkit: PluginManagerMock.getEventListeners
    // throws IllegalPluginAccessException(InvocationTargetException) while reflecting the HandlerList of
    // one of the advancement trigger events. See final report - candidate MockBukkit PR target.
    @Test
    void constructsAndUnregisterWithoutRegisterIsSafe() {
        AdvancementRuntimeService service =
                new AdvancementRuntimeService(new AdvancementRegistry(), plugin);

        assertDoesNotThrow(service::unregister);
    }
}
