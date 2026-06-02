package toutouchien.itemsadderadditions.feature.creative;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreativeItemModelResolverTest {

    private CustomStack item(String namespace, String id, String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        CustomStack stack = mock(CustomStack.class);
        when(stack.getConfig()).thenReturn(cfg);
        when(stack.getId()).thenReturn(id);
        when(stack.getNamespace()).thenReturn(namespace);
        when(stack.getNamespacedID()).thenReturn(namespace + ":" + id);
        return stack;
    }

    @Test
    void iconUsesAutoIconModel() {
        CustomStack item = item("pack", "ruby", """
                items:
                  ruby:
                    graphics:
                      icon: pack:item/ruby_icon
                """);
        assertEquals("pack:item/ia_auto/ruby_icon", CreativeItemModelResolver.resolveModel(item));
    }

    @Test
    void resourceIconAlsoCountsAsIcon() {
        CustomStack item = item("pack", "ruby", """
                items:
                  ruby:
                    resource:
                      icon: x.png
                """);
        assertEquals("pack:item/ia_auto/ruby_icon", CreativeItemModelResolver.resolveModel(item));
    }

    @Test
    void declaredGraphicsModelIsNormalizedAndExtensionStripped() {
        CustomStack item = item("pack", "ruby", """
                items:
                  ruby:
                    graphics:
                      model: custom/ruby.json
                """);
        assertEquals("pack:custom/ruby", CreativeItemModelResolver.resolveModel(item));
    }

    @Test
    void declaredModelWithNamespaceKeptAsIs() {
        CustomStack item = item("pack", "ruby", """
                items:
                  ruby:
                    graphics:
                      model: other:custom/ruby
                """);
        assertEquals("other:custom/ruby", CreativeItemModelResolver.resolveModel(item));
    }

    @Test
    void resourceModelPathHonoredWhenGenerateFalse() {
        CustomStack item = item("pack", "ruby", """
                items:
                  ruby:
                    resource:
                      model_path: custom/ruby.png
                      generate: false
                """);
        assertEquals("pack:custom/ruby", CreativeItemModelResolver.resolveModel(item));
    }

    @Test
    void generatedModelFallsBackToAutoPath() {
        CustomStack item = item("pack", "ruby", """
                items:
                  ruby:
                    resource:
                      generate: true
                      textures:
                        - x.png
                """);
        assertEquals("pack:item/ia_auto/ruby", CreativeItemModelResolver.resolveModel(item));
    }

    @Test
    void emptyConfigFallsBackToAutoPath() {
        CustomStack item = item("pack", "ruby", """
                items:
                  ruby: {}
                """);
        assertEquals("pack:item/ia_auto/ruby", CreativeItemModelResolver.resolveModel(item));
    }

    @Test
    void shouldSkipTemplateItem() {
        CustomStack item = item("pack", "tmpl", """
                items:
                  tmpl:
                    template: true
                """);
        assertTrue(CreativeItemModelResolver.shouldSkip(item));
    }

    @Test
    void shouldSkipHiddenItem() {
        CustomStack item = item("pack", "hid", """
                items:
                  hid:
                    hide_from_inventory: true
                """);
        assertTrue(CreativeItemModelResolver.shouldSkip(item));
    }

    @Test
    void shouldNotSkipNormalItem() {
        CustomStack item = item("pack", "normal", """
                items:
                  normal:
                    graphics:
                      texture: x.png
                """);
        assertFalse(CreativeItemModelResolver.shouldSkip(item));
    }

    @Test
    void directionalVariantConsultsRegistry() {
        CustomStack item = item("pack", "thing_north", """
                items:
                  thing_north:
                    graphics:
                      texture: x.png
                """);
        // Directional variant path reaches CustomStack.isInRegistry(base). The compileOnly IA
        // stub throws UnsupportedOperationException ("not meant to be shaded"); a real ItemsAdder
        // runtime is required to resolve the base item. This asserts the branch is reached.
        assertThrows(UnsupportedOperationException.class, () -> CreativeItemModelResolver.shouldSkip(item));
    }
}
