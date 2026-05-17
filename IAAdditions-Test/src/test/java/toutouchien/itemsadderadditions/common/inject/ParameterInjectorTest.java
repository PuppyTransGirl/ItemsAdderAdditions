package toutouchien.itemsadderadditions.common.inject;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.common.annotation.Parameter;

import static org.junit.jupiter.api.Assertions.*;

class ParameterInjectorTest {
    private static YamlConfiguration yamlOf(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    @Test
    void injectsStringField() {
        YamlConfiguration yaml = yamlOf("message: hello");
        SimpleTarget target = new SimpleTarget();
        assertTrue(ParameterInjector.inject(target, yaml, "test:item"));
        assertEquals("hello", target.message);
    }

    @Test
    void injectsIntegerField() {
        YamlConfiguration yaml = yamlOf("count: 42");
        SimpleTarget target = new SimpleTarget();
        ParameterInjector.inject(target, yaml, "test:item");
        assertEquals(42, target.count);
    }

    @Test
    void preservesDefaultWhenKeyAbsent() {
        YamlConfiguration yaml = new YamlConfiguration();
        SimpleTarget target = new SimpleTarget();
        ParameterInjector.inject(target, yaml, "test:item");
        assertEquals("default", target.message);
        assertEquals(0, target.count);
    }

    @Test
    void nullSectionPreservesDefaults() {
        SimpleTarget target = new SimpleTarget();
        boolean result = ParameterInjector.inject(target, null, "test:item");
        assertTrue(result);
        assertEquals("default", target.message);
    }

    @Test
    void requiredFieldMissingReturnsFalse() {
        YamlConfiguration yaml = new YamlConfiguration();
        RequiredTarget target = new RequiredTarget();
        boolean result = ParameterInjector.inject(target, yaml, "test:item");
        assertFalse(result);
    }

    @Test
    void requiredFieldPresentReturnsTrue() {
        YamlConfiguration yaml = yamlOf("name: sword");
        RequiredTarget target = new RequiredTarget();
        boolean result = ParameterInjector.inject(target, yaml, "test:item");
        assertTrue(result);
        assertEquals("sword", target.name);
    }

    @Test
    void valueWithinRangeNotClamped() {
        YamlConfiguration yaml = yamlOf("value: 5");
        RangeTarget target = new RangeTarget();
        ParameterInjector.inject(target, yaml, "test:item");
        assertEquals(5, target.value);
    }

    @Test
    void valueBelowMinClamped() {
        YamlConfiguration yaml = yamlOf("value: -5");
        RangeTarget target = new RangeTarget();
        ParameterInjector.inject(target, yaml, "test:item");
        assertEquals(1, target.value); // clamped to min
    }

    @Test
    void valueAboveMaxClamped() {
        YamlConfiguration yaml = yamlOf("value: 99");
        RangeTarget target = new RangeTarget();
        ParameterInjector.inject(target, yaml, "test:item");
        assertEquals(10, target.value); // clamped to max
    }

    @Test
    void subSectionFieldInjected() {
        YamlConfiguration yaml = yamlOf("""
                sound:
                  volume: 0.5
                """);
        SubSectionTarget target = new SubSectionTarget();
        ParameterInjector.inject(target, yaml, "test:item");
        assertEquals(0.5f, target.volume, 0.001f);
    }

    @Test
    void subSectionAbsentPreservesDefault() {
        YamlConfiguration yaml = new YamlConfiguration();
        SubSectionTarget target = new SubSectionTarget();
        ParameterInjector.inject(target, yaml, "test:item");
        assertEquals(1.0f, target.volume, 0.001f);
    }

    @Test
    void subSectionAbsentRequiredReturnsFalse() {
        YamlConfiguration yaml = new YamlConfiguration();
        SubSectionRequiredTarget target = new SubSectionRequiredTarget();
        boolean result = ParameterInjector.inject(target, yaml, "test:item");
        assertFalse(result);
    }

    @Test
    void wrongTypeNullsBoxedField() {
        // Give the 'message' String field a numeric value (wrong type) -> field should be nulled
        YamlConfiguration yaml = yamlOf("message: 123");
        SimpleTarget target = new SimpleTarget();
        ParameterInjector.inject(target, yaml, "test:item");
        // Integer 123 is not a String -> field set to null, default "default" overwritten
        assertNull(target.message);
    }

    private static class SimpleTarget {
        @Parameter(key = "message", type = String.class)
        String message = "default";

        @Parameter(key = "count", type = Integer.class)
        int count = 0;
    }

    private static class RequiredTarget {
        @Parameter(key = "name", type = String.class, required = true)
        String name;
    }

    private static class RangeTarget {
        @Parameter(key = "value", type = Integer.class, min = 1, max = 10)
        int value = 5;
    }

    private static class SubSectionTarget {
        @Parameter(key = "volume", path = "sound", type = Float.class)
        float volume = 1.0f;
    }

    private static class SubSectionRequiredTarget {
        @Parameter(key = "volume", path = "sound", type = Float.class, required = true)
        float volume = 1.0f;
    }
}
