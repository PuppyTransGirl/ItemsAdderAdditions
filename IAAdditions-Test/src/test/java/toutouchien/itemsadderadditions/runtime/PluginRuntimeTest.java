package toutouchien.itemsadderadditions.runtime;

import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.settings.PluginFeature;
import toutouchien.itemsadderadditions.settings.PluginSettings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginRuntimeTest {
    @Test
    void disabledCreativeIntegrationDoesNotAccessNmsOrInstallListeners() throws Exception {
        ItemsAdderAdditions plugin = mock(ItemsAdderAdditions.class);
        PluginSettings settings = mock(PluginSettings.class);
        when(settings.featureEnabled(PluginFeature.CREATIVE_INVENTORY_INTEGRATION))
                .thenReturn(false);

        PluginRuntime runtime = new PluginRuntime(plugin);
        Field settingsField = PluginRuntime.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(runtime, settings);

        Method setup = PluginRuntime.class.getDeclaredMethod(
                "setupCreativeInventoryIntegration"
        );
        setup.setAccessible(true);

        NmsManager.shutdown();
        try {
            assertDoesNotThrow(() -> setup.invoke(runtime));
            assertNull(runtime.creativeMenuManager());
        } finally {
            NmsManager.shutdown();
        }
    }
}
