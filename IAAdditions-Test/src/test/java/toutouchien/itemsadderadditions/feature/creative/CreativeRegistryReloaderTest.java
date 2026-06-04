package toutouchien.itemsadderadditions.feature.creative;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import toutouchien.itemsadderadditions.integration.hook.worldguard.WorldGuardSettings;
import toutouchien.itemsadderadditions.nms.api.INmsCreativeMenuHandler;
import toutouchien.itemsadderadditions.nms.api.INmsHandler;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;
import toutouchien.itemsadderadditions.settings.PluginFeature;
import toutouchien.itemsadderadditions.settings.PluginSettings;
import toutouchien.itemsadderadditions.settings.ToggleMap;
import toutouchien.itemsadderadditions.settings.UpdateCheckerSettings;
import toutouchien.itemsadderadditions.testsupport.FakeNms;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreativeRegistryReloaderTest {
    @AfterEach
    void teardown() {
        FakeNms.uninstall();
    }

    @Test
    void metadataMethodsDescribeClientRegistrySystem() {
        CreativeRegistryReloader reloader = new CreativeRegistryReloader(plugin(true));

        assertEquals("CreativeRegistry", reloader.name());
        assertEquals(ReloadPhase.CLIENT_REGISTRY, reloader.phase());
    }

    @Test
    void reloadReturnsFalseWhenFeatureDisabled() {
        CreativeRegistryReloader reloader = new CreativeRegistryReloader(plugin(false));

        assertFalse(reloader.reload(List.of(creativeItem("pack", "visible"))));
    }

    @Test
    void reloadReturnsFalseWhenNmsCreativeMenuIsUnavailable() {
        INmsHandler handler = FakeNms.install();
        when(handler.creativeMenu()).thenReturn(null);
        CreativeRegistryReloader reloader = new CreativeRegistryReloader(plugin(true));

        assertFalse(reloader.reload(List.of(creativeItem("pack", "visible"))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reloadFiltersHiddenTemplateAndRegisteredDirectionalVariants() {
        INmsHandler handler = FakeNms.install();
        INmsCreativeMenuHandler creativeMenu = mock(INmsCreativeMenuHandler.class);
        when(handler.creativeMenu()).thenReturn(creativeMenu);
        CreativeRegistryReloader reloader = new CreativeRegistryReloader(plugin(true));
        CustomStack visible = creativeItem("pack", "visible");
        CustomStack template = creativeItem("pack", "template");
        template.getConfig().set("items.template.template", true);
        CustomStack hidden = creativeItem("pack", "hidden");
        hidden.getConfig().set("items.hidden.hide_from_inventory", true);
        CustomStack directional = creativeItem("pack", "crate_north");

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.isInRegistry("pack:crate")).thenReturn(true);

            assertTrue(reloader.reload(List.of(visible, template, hidden, directional)));
            assertFalse(reloader.reload(List.of(visible, template, hidden, directional)));
        }

        ArgumentCaptor<List<CustomStack>> captor = ArgumentCaptor.forClass(List.class);
        verify(creativeMenu, times(2)).injectPaintingVariants(captor.capture());
        verify(creativeMenu, times(2)).updatePaintingCache(any());
        assertEquals(List.of(visible), captor.getAllValues().getFirst());
        assertEquals(List.of(visible), captor.getAllValues().getLast());
    }

    @Test
    void reloadContextReturnsRegistryStepResult() {
        INmsHandler handler = FakeNms.install();
        when(handler.creativeMenu()).thenReturn(mock(INmsCreativeMenuHandler.class));
        CreativeRegistryReloader reloader = new CreativeRegistryReloader(plugin(true));
        CustomStack visible = creativeItem("pack", "visible");

        ReloadStepResult result = reloader.reload(new ContentReloadContext(List.of(visible), mock()));

        assertEquals("CreativeRegistry", result.system());
        assertTrue(result.registryChanged());
        assertEquals(0, result.loadedCount());
    }

    private static ItemsAdderAdditions plugin(boolean creativeEnabled) {
        ItemsAdderAdditions plugin = mock(ItemsAdderAdditions.class);
        when(plugin.settings()).thenReturn(settings(creativeEnabled));
        when(plugin.creativeMenuManager()).thenReturn(null);
        return plugin;
    }

    private static PluginSettings settings(boolean creativeEnabled) {
        return new PluginSettings(
                Map.of(PluginFeature.CREATIVE_INVENTORY_INTEGRATION, creativeEnabled),
                new ToggleMap(Map.of(), true),
                new ToggleMap(Map.of(), true),
                new ToggleMap(Map.of(), true),
                new UpdateCheckerSettings(false, false),
                WorldGuardSettings.defaults()
        );
    }

    private static CustomStack creativeItem(String namespace, String id) {
        CustomStack stack = mock(CustomStack.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("items." + id + ".display_name", id);
        when(stack.getNamespace()).thenReturn(namespace);
        when(stack.getId()).thenReturn(id);
        when(stack.getNamespacedID()).thenReturn(namespace + ":" + id);
        when(stack.getConfig()).thenReturn(config);
        when(stack.getItemStack()).thenReturn(ItemStack.of(Material.STONE));
        return stack;
    }
}
