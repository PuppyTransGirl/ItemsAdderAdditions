package toutouchien.itemsadderadditions.integration.valhalla;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValhallaStatEntryTest {
    @Test
    void serializeSingleStat() {
        ValhallaStatEntry entry = new ValhallaStatEntry("CRIT_DAMAGE", 1000.0, "ADD_NUMBER", true);
        assertEquals("CRIT_DAMAGE:1000.0:ADD_NUMBER:true", entry.serialize());
    }

    @Test
    void serializeHiddenFalse() {
        ValhallaStatEntry entry = new ValhallaStatEntry("GENERIC_MOVEMENT_SPEED", 0.3, "ADD_SCALAR", false);
        assertEquals("GENERIC_MOVEMENT_SPEED:0.3:ADD_SCALAR:false", entry.serialize());
    }

    @Test
    void serializeNegativeAmount() {
        ValhallaStatEntry entry = new ValhallaStatEntry("DAMAGE_MELEE", -5.0, "ADD_NUMBER", false);
        assertEquals("DAMAGE_MELEE:-5.0:ADD_NUMBER:false", entry.serialize());
    }

    @Test
    void serializeMultiplyScalarOperation() {
        ValhallaStatEntry entry = new ValhallaStatEntry("GENERIC_MAX_HEALTH", 2.0, "MULTIPLY_SCALAR_1", false);
        assertEquals("GENERIC_MAX_HEALTH:2.0:MULTIPLY_SCALAR_1:false", entry.serialize());
    }
}
