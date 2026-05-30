package toutouchien.itemsadderadditions.feature.behaviour.builtin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StackableBehaviourTest {
    private static YamlConfiguration yamlOf(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> steps(StackableBehaviour behaviour) throws Exception {
        Field f = StackableBehaviour.class.getDeclaredField("steps");
        f.setAccessible(true);
        return (List<Object>) f.get(behaviour);
    }

    private static Object field(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    @Test
    void configurePlainListCreatesOneStepPerEntry() throws Exception {
        StackableBehaviour behaviour = new StackableBehaviour();

        assertTrue(behaviour.configure(List.of("flower_2", "ns:flower_3"), "ns:flower_1"));

        List<Object> steps = steps(behaviour);
        assertEquals(2, steps.size());
        assertEquals("ns:flower_2", field(steps.get(0), "resultBlock"));
        assertEquals("ns:flower_3", field(steps.get(1), "resultBlock"));
        assertEquals(List.of("ns:flower_1"), field(steps.get(0), "items"));
    }

    @Test
    void configureSharedItemsSectionNormalizesItemsAndClampDecrement() throws Exception {
        StackableBehaviour behaviour = new StackableBehaviour();
        YamlConfiguration cfg = yamlOf("""
                blocks:
                  - flower_2
                  - other:flower_3
                items:
                  - bone_meal
                  - minecraft:stick
                decrement_amount: 999
                """);

        assertTrue(behaviour.configure(cfg, "ns:flower_1"));

        List<Object> steps = steps(behaviour);
        assertEquals(2, steps.size());
        assertEquals(List.of("ns:bone_meal", "minecraft:stick"), field(steps.getFirst(), "items"));
        assertEquals(256, field(steps.getFirst(), "decrementAmount"));
    }

    @Test
    void configureSharedItemsNegativeDecrementClampsToZero() throws Exception {
        StackableBehaviour behaviour = new StackableBehaviour();
        YamlConfiguration cfg = yamlOf("""
                blocks: [flower_2]
                items: [bone_meal]
                decrement_amount: -5
                """);

        assertTrue(behaviour.configure(cfg, "ns:flower_1"));

        assertEquals(0, field(steps(behaviour).getFirst(), "decrementAmount"));
    }

    @Test
    void configureNamedStepsSkipsMissingBlock() throws Exception {
        StackableBehaviour behaviour = new StackableBehaviour();
        YamlConfiguration cfg = yamlOf("""
                stage_2:
                  block: flower_2
                  items: [stick]
                broken_stage:
                  items: [stone]
                """);

        assertTrue(behaviour.configure(cfg, "ns:flower_1"));

        List<Object> steps = steps(behaviour);
        assertEquals(1, steps.size());
        assertEquals("ns:flower_2", field(steps.getFirst(), "resultBlock"));
        assertEquals(List.of("ns:stick"), field(steps.getFirst(), "items"));
    }

    @Test
    void configureNonSectionOrListReturnsFalse() {
        assertFalse(new StackableBehaviour().configure("wrong", "ns:flower"));
    }

    @Test
    void configureEmptyListReturnsFalse() {
        assertFalse(new StackableBehaviour().configure(List.of(), "ns:flower"));
    }

    @Test
    void configureBlankBlockEntriesAreIgnored() throws Exception {
        StackableBehaviour behaviour = new StackableBehaviour();

        assertFalse(behaviour.configure(List.of(" ", ""), "ns:flower"));
        assertTrue(steps(behaviour).isEmpty());
    }

    @Test
    void configureSoundSectionStoresParsedSound() throws Exception {
        StackableBehaviour behaviour = new StackableBehaviour();
        YamlConfiguration cfg = yamlOf("""
                blocks: [flower_2]
                sound:
                  name: minecraft:block.grass.place
                  source: block
                  volume: 0.8
                  pitch: 1.2
                """);

        assertTrue(behaviour.configure(cfg, "ns:flower"));

        assertNotNull(field(steps(behaviour).getFirst(), "sound"));
    }
}
