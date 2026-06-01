package toutouchien.itemsadderadditions.integration.valhalla;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValhallaConfigParserTest {
    private static YamlConfiguration yaml(String content) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return cfg;
    }

    @Test
    void parseSingleStat() {
        var valhallaCfg = yaml("""
                stats:
                  - stat: CRIT_DAMAGE
                    amount: 1000.0
                    operation: ADD_NUMBER
                    hidden: true
                """);
        var section = valhallaCfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertEquals(1, data.actualStats().size());
        assertEquals(1, data.defaultStats().size());

        ValhallaStatEntry entry = data.actualStats().get(0);
        assertEquals("CRIT_DAMAGE", entry.stat());
        assertEquals(1000.0, entry.amount());
        assertEquals("ADD_NUMBER", entry.operation());
        assertTrue(entry.hidden());
    }

    @Test
    void statsWritesBothActualAndDefault() {
        var cfg = yaml("""
                stats:
                  - stat: JUMPS
                    amount: 3.0
                    operation: ADD_NUMBER
                    hidden: false
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertEquals(data.actualStats(), data.defaultStats());
        assertEquals("JUMPS", data.actualStats().get(0).stat());
    }

    @Test
    void explicitActualStatsOverridesStats() {
        var cfg = yaml("""
                stats:
                  - stat: JUMPS
                    amount: 3.0
                    operation: ADD_NUMBER
                    hidden: false
                actual_stats:
                  - stat: CRIT_CHANCE
                    amount: 50.0
                    operation: ADD_NUMBER
                    hidden: false
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertEquals("CRIT_CHANCE", data.actualStats().get(0).stat());
        assertEquals("JUMPS", data.defaultStats().get(0).stat());
    }

    @Test
    void explicitDefaultStatsOverridesStats() {
        var cfg = yaml("""
                stats:
                  - stat: JUMPS
                    amount: 3.0
                    operation: ADD_NUMBER
                    hidden: false
                default_stats:
                  - stat: JUMP_HEIGHT
                    amount: 1.0
                    operation: ADD_NUMBER
                    hidden: false
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertEquals("JUMPS", data.actualStats().get(0).stat());
        assertEquals("JUMP_HEIGHT", data.defaultStats().get(0).stat());
    }

    @Test
    void multipleStatsPreservesOrder() {
        var cfg = yaml("""
                stats:
                  - stat: GENERIC_MOVEMENT_SPEED
                    amount: 0.3
                    operation: ADD_SCALAR
                    hidden: false
                  - stat: JUMPS
                    amount: 3.0
                    operation: ADD_NUMBER
                    hidden: false
                  - stat: JUMP_HEIGHT
                    amount: 1.0
                    operation: ADD_NUMBER
                    hidden: false
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertEquals(3, data.actualStats().size());
        assertEquals("GENERIC_MOVEMENT_SPEED", data.actualStats().get(0).stat());
        assertEquals("JUMPS", data.actualStats().get(1).stat());
        assertEquals("JUMP_HEIGHT", data.actualStats().get(2).stat());
    }

    @Test
    void statNamesNormalizesToUpperCase() {
        var cfg = yaml("""
                stats:
                  - stat: generic_movement_speed
                    amount: 0.3
                    operation: add_scalar
                    hidden: false
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertEquals("GENERIC_MOVEMENT_SPEED", data.actualStats().get(0).stat());
        assertEquals("ADD_SCALAR", data.actualStats().get(0).operation());
    }

    @Test
    void invalidOperationSkipsStat() {
        var cfg = yaml("""
                stats:
                  - stat: CRIT_DAMAGE
                    amount: 100.0
                    operation: INVALID_OP
                    hidden: false
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNull(data);
    }

    @Test
    void missingStatNameSkipsEntry() {
        var cfg = yaml("""
                stats:
                  - amount: 100.0
                    operation: ADD_NUMBER
                    hidden: false
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNull(data);
    }

    @Test
    void missingAmountSkipsEntry() {
        var cfg = yaml("""
                stats:
                  - stat: CRIT_DAMAGE
                    operation: ADD_NUMBER
                    hidden: false
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNull(data);
    }

    @Test
    void duplicateStatSkipsSecond() {
        var cfg = yaml("""
                stats:
                  - stat: CRIT_DAMAGE
                    amount: 100.0
                    operation: ADD_NUMBER
                    hidden: false
                  - stat: CRIT_DAMAGE
                    amount: 200.0
                    operation: ADD_NUMBER
                    hidden: false
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertEquals(1, data.actualStats().size());
        assertEquals(100.0, data.actualStats().get(0).amount());
    }

    @Test
    void parseEquipmentClass() {
        var cfg = yaml("equipment_class: TRINKET\n");
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertEquals("TRINKET", data.equipmentClass());
    }

    @Test
    void equipmentClassNormalizesToUpperCase() {
        var cfg = yaml("equipment_class: trinket\n");
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertEquals("TRINKET", data.equipmentClass());
    }

    @Test
    void invalidEquipmentClassIsSkipped() {
        var cfg = yaml("equipment_class: INVALID_CLASS\n");
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNull(data);
    }

    @Test
    void parseItemFlags() {
        var cfg = yaml("""
                item_flags:
                  - DISPLAY_ATTRIBUTES
                  - HIDE_TAGS
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertEquals(List.of("DISPLAY_ATTRIBUTES", "HIDE_TAGS"), data.itemFlags());
    }

    @Test
    void invalidItemFlagIsSkipped() {
        var cfg = yaml("""
                item_flags:
                  - DISPLAY_ATTRIBUTES
                  - INVALID_FLAG
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertEquals(List.of("DISPLAY_ATTRIBUTES"), data.itemFlags());
    }

    @Test
    void parseTrinketData() {
        var cfg = yaml("""
                trinkets:
                  trinket_id: 7
                  trinket_unique_id: 529
                  unique: true
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertNotNull(data.trinkets());
        assertEquals(7, data.trinkets().trinketId());
        assertEquals(529, data.trinkets().trinketUniqueId());
        assertEquals(Boolean.TRUE, data.trinkets().unique());
    }

    @Test
    void parseTrinketUniqueAs1b() {
        var cfg = yaml("""
                trinkets:
                  trinket_id: 5
                  unique: "1b"
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertNotNull(data.trinkets());
        assertEquals(Boolean.TRUE, data.trinkets().unique());
    }

    @Test
    void statNameWithColonIsRejected() {
        var cfg = yaml("""
                stats:
                  - stat: "SOME:STAT"
                    amount: 1.0
                    operation: ADD_NUMBER
                    hidden: false
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);
        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNull(data);
    }

    @Test
    void statNameWithSemicolonIsRejected() {
        var cfg = yaml("""
                stats:
                  - stat: "SOME;STAT"
                    amount: 1.0
                    operation: ADD_NUMBER
                    hidden: false
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);
        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNull(data);
    }

    @Test
    void nonFiniteAmountIsRejected() {
        List<ValhallaStatEntry> result = ValhallaConfigParser.parseStatList(
                List.of(Map.of("stat", "CRIT_DAMAGE", "amount", Double.NaN, "operation", "ADD_NUMBER")),
                "stats",
                "test:item"
        );
        assertTrue(result.isEmpty());
    }

    @Test
    void trinketIdDecimalIsRejected() {
        var cfg = yaml("""
                trinkets:
                  trinket_id: 7.9
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);
        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNull(data);
    }

    @Test
    void trinketUniqueIdTooLargeIsRejected() {
        var cfg = yaml("""
                trinkets:
                  trinket_unique_id: 999999999999
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);
        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNull(data);
    }

    @Test
    void trinketIdNegativeIsRejected() {
        var cfg = yaml("""
                trinkets:
                  trinket_id: -1
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);
        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNull(data);
    }

    @Test
    void trinketUniqueNumeric1AcceptsAsTrue() {
        var cfg = yaml("""
                trinkets:
                  unique: 1
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);
        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertNotNull(data.trinkets());
        assertEquals(Boolean.TRUE, data.trinkets().unique());
    }

    @Test
    void trinketUniqueNumeric0AcceptsAsFalse() {
        var cfg = yaml("""
                trinkets:
                  unique: 0
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);
        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertNotNull(data.trinkets());
        assertEquals(Boolean.FALSE, data.trinkets().unique());
    }

    @Test
    void trinketUniqueNumeric2IsRejected() {
        var cfg = yaml("""
                trinkets:
                  unique: 2
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);
        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNull(data);
    }

    @Test
    void trinketUniqueNumericNegativeIsRejected() {
        var cfg = yaml("""
                trinkets:
                  unique: -1
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);
        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNull(data);
    }

    @Test
    void trinketUniqueString1AcceptsAsTrue() {
        var cfg = yaml("""
                trinkets:
                  unique: "1"
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);
        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertNotNull(data.trinkets());
        assertEquals(Boolean.TRUE, data.trinkets().unique());
    }

    @Test
    void trinketUniqueString0AcceptsAsFalse() {
        var cfg = yaml("""
                trinkets:
                  unique: "0"
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);
        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNotNull(data);
        assertNotNull(data.trinkets());
        assertEquals(Boolean.FALSE, data.trinkets().unique());
    }

    @Test
    void trinketUniqueStringYesIsRejected() {
        var cfg = yaml("""
                trinkets:
                  unique: "yes"
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);
        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNull(data);
    }

    @Test
    void emptyValhallaSection_returnsNull() {
        var cfg = yaml("");
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:item");
        assertNull(data);
    }

    @Test
    void fullExampleParsesCorrectly() {
        var cfg = yaml("""
                stats:
                  - stat: GENERIC_MOVEMENT_SPEED
                    amount: 0.3
                    operation: ADD_SCALAR
                    hidden: false
                  - stat: JUMPS
                    amount: 3.0
                    operation: ADD_NUMBER
                    hidden: false
                equipment_class: TRINKET
                item_flags:
                  - DISPLAY_ATTRIBUTES
                trinkets:
                  trinket_id: 7
                  trinket_unique_id: 529
                  unique: true
                """);
        var section = cfg.getConfigurationSection("");
        assertNotNull(section);

        ValhallaItemData data = ValhallaConfigParser.parse(section, "test:speed_trinket");
        assertNotNull(data);
        assertEquals(2, data.actualStats().size());
        assertEquals(2, data.defaultStats().size());
        assertEquals("TRINKET", data.equipmentClass());
        assertEquals(List.of("DISPLAY_ATTRIBUTES"), data.itemFlags());
        assertNotNull(data.trinkets());
        assertEquals(7, data.trinkets().trinketId());
    }
}
