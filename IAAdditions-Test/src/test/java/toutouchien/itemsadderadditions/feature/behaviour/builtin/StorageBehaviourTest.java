package toutouchien.itemsadderadditions.feature.behaviour.builtin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class StorageBehaviourTest {
    private static YamlConfiguration yamlOf(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    private static Object field(StorageBehaviour behaviour, String name) throws Exception {
        Field f = StorageBehaviour.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(behaviour);
    }

    @Test
    void configureValidMinimalStorage() throws Exception {
        StorageBehaviour behaviour = new StorageBehaviour();

        assertTrue(behaviour.configure(yamlOf("type: storage\n"), "ns:chest"));

        assertEquals("storage", field(behaviour, "typeName"));
        assertEquals(3, field(behaviour, "rows"));
        assertNull(field(behaviour, "openSound"));
        assertNull(field(behaviour, "closeSound"));
    }

    @Test
    void configureRowsAndTitleAndInventoryType() throws Exception {
        StorageBehaviour behaviour = new StorageBehaviour();

        assertTrue(behaviour.configure(yamlOf("""
                type: shulker
                rows: 6
                title: '<gold>Storage'
                inventory_type: hopper
                open_variant: ns:open_chest
                """), "ns:chest"));

        assertEquals("shulker", field(behaviour, "typeName"));
        assertEquals(6, field(behaviour, "rows"));
        assertEquals("<gold>Storage", field(behaviour, "titleRaw"));
        assertEquals("hopper", field(behaviour, "inventoryTypeName"));
        assertEquals("ns:open_chest", field(behaviour, "openVariant"));
    }

    @Test
    void configureWithSoundsStoresParsedSounds() throws Exception {
        StorageBehaviour behaviour = new StorageBehaviour();

        assertTrue(behaviour.configure(yamlOf("""
                type: storage
                open_sound:
                  name: block.chest.open
                close_sound:
                  name: block.chest.close
                """), "ns:chest"));

        assertNotNull(field(behaviour, "openSound"));
        assertNotNull(field(behaviour, "closeSound"));
    }

    @Test
    void configureMissingRequiredTypeFails() {
        assertFalse(new StorageBehaviour().configure(yamlOf("rows: 3\n"), "ns:chest"));
    }

    @Test
    void configureNonSectionFails() {
        assertFalse(new StorageBehaviour().configure("wrong", "ns:chest"));
    }

    @Test
    void configureMalformedSoundFails() {
        StorageBehaviour behaviour = new StorageBehaviour();

        assertFalse(behaviour.configure(yamlOf("""
                type: storage
                open_sound:
                  name: block.chest.open
                  source: bad_source
                """), "ns:chest"));
    }
}
