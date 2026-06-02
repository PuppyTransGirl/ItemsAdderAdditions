package toutouchien.itemsadderadditions.feature.behaviour.builtin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.connectable.ConnectableType;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConnectableBehaviourTest {
    @Test
    void configureRejectsNonConfigurationSections() {
        assertFalse(new ConnectableBehaviour().configure(Map.of("type", "table"), "pack:table"));
    }

    @Test
    void configureDefaultsStairVariantIdsFromBaseNamespace() throws Exception {
        ConnectableBehaviour behaviour = new ConnectableBehaviour();
        YamlConfiguration config = new YamlConfiguration();
        config.set("type", "stair");

        assertTrue(behaviour.configure(config, "pack:oak_stair"));

        assertEquals(ConnectableType.STAIR, field(behaviour, "type"));
        assertEquals("pack:oak_stair", field(behaviour, "defaultVariant"));
        assertEquals("pack:oak_stair_straight", field(behaviour, "straightVariant"));
        assertEquals("pack:oak_stair_left", field(behaviour, "leftVariant"));
        assertEquals("pack:oak_stair_right", field(behaviour, "rightVariant"));
        assertEquals("pack:oak_stair_outer", field(behaviour, "outerVariant"));
        assertEquals("pack:oak_stair_inner", field(behaviour, "innerVariant"));
    }

    @Test
    void configureResolvesBareAndNamespacedTableVariants() throws Exception {
        ConnectableBehaviour behaviour = new ConnectableBehaviour();
        YamlConfiguration config = new YamlConfiguration();
        config.set("type", "TABLE");
        config.set("default", "isolated");
        config.set("straight", "other:line");
        config.set("middle", "center");
        config.set("border", "edge");
        config.set("corner", "turn");
        config.set("end", "cap");

        assertTrue(behaviour.configure(config, "pack:table"));

        assertEquals(ConnectableType.TABLE, field(behaviour, "type"));
        assertEquals("pack:isolated", field(behaviour, "defaultVariant"));
        assertEquals("other:line", field(behaviour, "straightVariant"));
        assertEquals("pack:center", field(behaviour, "middleVariant"));
        assertEquals("pack:edge", field(behaviour, "borderVariant"));
        assertEquals("pack:turn", field(behaviour, "cornerVariant"));
        assertEquals("pack:cap", field(behaviour, "endVariant"));
    }

    @Test
    void configureTreatsUnknownTypeAsStair() throws Exception {
        ConnectableBehaviour behaviour = new ConnectableBehaviour();
        YamlConfiguration config = new YamlConfiguration();
        config.set("type", "unknown");

        assertTrue(behaviour.configure(config, "pack:connectable"));

        assertEquals(ConnectableType.STAIR, field(behaviour, "type"));
        assertEquals("pack:connectable_straight", field(behaviour, "straightVariant"));
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(ConnectableBehaviour behaviour, String name) throws Exception {
        Field field = ConnectableBehaviour.class.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(behaviour);
    }
}
