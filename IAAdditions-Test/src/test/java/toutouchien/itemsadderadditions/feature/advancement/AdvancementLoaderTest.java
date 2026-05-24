package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdvancementLoaderTest {
    private static ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static YamlConfiguration yamlOf(String yaml) {
        var cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    private static List<AdvancementDefinition> load(String yaml) {
        return AdvancementLoader.loadAll("testns", yamlOf(yaml).getConfigurationSection("advancements"));
    }

    @Test
    void root_minimalValid_loads() {
        var result = load("""
                advancements:
                  myroot:
                    display:
                      title: "Root"
                      description: "desc"
                      icon: minecraft:stone
                      background: "minecraft:textures/block/stone.png"
                """);
        assertEquals(1, result.size());
        var def = result.getFirst();
        assertEquals("testns", def.key().getNamespace());
        assertEquals("myroot", def.key().getKey());
        assertNull(def.parent());
        assertTrue(def.isRoot());
        assertEquals("minecraft:textures/block/stone.png", def.display().background());
    }

    @Test
    void child_missingDisplay_skipped() {
        var result = load("""
                advancements:
                  bad:
                    parent: myroot
                """);
        assertEquals(0, result.size());
    }

    @Test
    void child_missingTitle_skipped() {
        var result = load("""
                advancements:
                  bad:
                    parent: myroot
                    display:
                      description: "desc"
                      icon: minecraft:stone
                """);
        assertEquals(0, result.size());
    }

    @Test
    void child_unknownTrigger_criterionSkipped() {
        var result = load("""
                advancements:
                  adv:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                    criteria:
                      c1:
                        trigger: not_a_trigger
                """);
        assertEquals(0, result.size());
    }

    @Test
    void null_section_returnsEmpty() {
        assertEquals(0, AdvancementLoader.loadAll("testns", null).size());
    }

    @Test
    void parent_withNamespace_keptAsIs() {
        var result = load("""
                advancements:
                  child:
                    parent: "otherns:root"
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                """);
        assertEquals(1, result.size());
        assertEquals("otherns", result.getFirst().parent().getNamespace());
        assertEquals("root", result.getFirst().parent().getKey());
    }

    @Test
    void parent_withoutNamespace_inheritsFileNamespace() {
        var result = load("""
                advancements:
                  child:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                """);
        assertEquals(1, result.size());
        assertEquals("testns", result.getFirst().parent().getNamespace());
        assertEquals("myroot", result.getFirst().parent().getKey());
    }
}
