package toutouchien.itemsadderadditions.feature.component;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;
import toutouchien.itemsadderadditions.settings.PluginSettings;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComponentsManagerTest {
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
        ComponentsManager manager = new ComponentsManager(settings(""));

        assertEquals("Components", manager.name());
        assertEquals(ReloadPhase.ITEM_BINDINGS, manager.phase());
    }

    @Test
    void constructorRegistersEnabledBuiltIns() {
        ComponentsManager manager = new ComponentsManager(settings(""));

        assertNotNull(manager.registry().getPrototype("rarity"));
        assertNotNull(manager.registry().getPrototype("use_cooldown"));
    }

    @Test
    void disabledBuiltInIsNotRegistered() {
        ComponentsManager manager = new ComponentsManager(settings("components:\n  rarity: false\n"));

        assertNull(manager.registry().getPrototype("rarity"));
        assertNotNull(manager.registry().getPrototype("use_cooldown"));
    }

    @Test
    void applySettingsReplacesRegistryState() {
        ComponentsManager manager = new ComponentsManager(settings("components:\n  rarity: false\n"));
        assertNull(manager.registry().getPrototype("rarity"));

        manager.applySettings(settings("components:\n  rarity: true\n"));

        assertNotNull(manager.registry().getPrototype("rarity"));
    }

    @Test
    void unknownKeyHasNoPrototype() {
        ComponentsManager manager = new ComponentsManager(settings(""));

        assertNull(manager.registry().getPrototype("does_not_exist"));
    }

    @Test
    void reloadEmptyContextReturnsLoadedZero() {
        ComponentsManager manager = new ComponentsManager(settings(""));

        ReloadStepResult result = manager.reload(new ContentReloadContext(List.of(), null));

        assertEquals("Components", result.system());
        assertEquals(0, result.loadedCount());
        assertFalse(result.registryChanged());
    }
}
