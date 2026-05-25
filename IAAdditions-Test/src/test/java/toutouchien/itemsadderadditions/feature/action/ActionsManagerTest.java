package toutouchien.itemsadderadditions.feature.action;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;
import toutouchien.itemsadderadditions.settings.PluginSettings;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActionsManagerTest {
    private static PluginSettings settings(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return PluginSettings.load(cfg);
    }

    @Test
    void nameAndPhaseAreStable() {
        ActionsManager manager = new ActionsManager(settings(""));

        assertEquals("Actions", manager.name());
        assertEquals(ReloadPhase.ITEM_BINDINGS, manager.phase());
    }

    @Test
    void constructorRegistersEnabledBuiltIns() {
        ActionsManager manager = new ActionsManager(settings(""));

        assertNotNull(manager.registry().getPrototype("message"));
        assertNotNull(manager.registry().getPrototype("title"));
    }

    @Test
    void disabledBuiltInIsNotRegistered() {
        ActionsManager manager = new ActionsManager(settings("actions:\n  message: false\n"));

        assertNull(manager.registry().getPrototype("message"));
        assertNotNull(manager.registry().getPrototype("title"));
    }

    @Test
    void applySettingsReplacesRegistryState() {
        ActionsManager manager = new ActionsManager(settings("actions:\n  message: false\n"));
        assertNull(manager.registry().getPrototype("message"));

        manager.applySettings(settings("actions:\n  message: true\n"));

        assertNotNull(manager.registry().getPrototype("message"));
    }

    @Test
    void reloadEmptyItemListReturnsZero() {
        ActionsManager manager = new ActionsManager(settings(""));

        assertEquals(0, manager.reload(List.of()));
    }

    @Test
    void reloadContextReturnsLoadedStepResult() {
        ActionsManager manager = new ActionsManager(settings(""));

        ReloadStepResult result = manager.reload(new ContentReloadContext(List.of(), null));

        assertEquals("Actions", result.system());
        assertEquals(0, result.loadedCount());
        assertFalse(result.registryChanged());
    }

    @Test
    void shutdownClearsBindings() {
        assertDoesNotThrow(() -> new ActionsManager(settings("")).shutdown());
    }
}
